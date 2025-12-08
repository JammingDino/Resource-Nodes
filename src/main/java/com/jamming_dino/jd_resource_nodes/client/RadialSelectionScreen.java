package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class RadialSelectionScreen extends Screen {

    private final List<ResourceNodeData> categories = new ArrayList<>();
    private int hoveredIndex = -1;
    private final int centerX;
    private final int centerY;
    private final float radius = 80.0f;

    public RadialSelectionScreen() {
        super(Component.literal("Select Resource Node"));
        this.centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        this.centerY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
    }

    public void setCategories(List<ResourceNodeData> categories) {
        this.categories.clear();
        this.categories.addAll(categories);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Override to prevent default background rendering (which includes blur)
        // We'll draw our own semi-transparent background in render()
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Semi-transparent background
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        if (categories.isEmpty()) {
            return;
        }

        // Update hovered index based on mouse position
        updateHoveredIndex(mouseX, mouseY);

        // Draw center circle
        drawCircle(graphics, centerX, centerY, 30, 0xFF444444);

        // Draw each category
        float angleStep = 360.0f / categories.size();
        for (int i = 0; i < categories.size(); i++) {
            float angle = (float) Math.toRadians(i * angleStep - 90); // Start at top

            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;

            boolean isHovered = (i == hoveredIndex);

            // Draw selection indicator if hovered
            if (isHovered) {
                drawCircle(graphics, (int) x, (int) y, 20, 0xFF00FF00);
            } else {
                drawCircle(graphics, (int) x, (int) y, 18, 0xFF666666);
            }

            // Draw item (the output item for this category)
            ResourceNodeData category = categories.get(i);
            graphics.renderItem(category.getDisplayStack(), (int) x - 8, (int) y - 8);

            // Draw category name if hovered
            if (isHovered) {
                Component name = Component.literal(formatCategoryName(category.getCategory()));
                int textWidth = minecraft.font.width(name);
                graphics.drawString(minecraft.font, name, (int) x - textWidth / 2, (int) y + 20, 0xFFFFFFFF);
            }
        }

        // Draw instruction text
        Component instruction = Component.literal("Release V to scan");
        int textWidth = minecraft.font.width(instruction);
        graphics.drawString(minecraft.font, instruction, centerX - textWidth / 2, centerY + (int) radius + 40, 0xFFFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String formatCategoryName(String category) {
        // Convert "deepslate_iron" to "Deepslate Iron"
        String[] parts = category.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(Character.toUpperCase(part.charAt(0)));
            formatted.append(part.substring(1));
        }
        return formatted.toString();
    }

    private void updateHoveredIndex(int mouseX, int mouseY) {
        hoveredIndex = -1;

        if (categories.isEmpty()) {
            return;
        }

        float dx = mouseX - centerX;
        float dy = mouseY - centerY;
        float distanceFromCenter = (float) Math.sqrt(dx * dx + dy * dy);

        // Only select if mouse is outside center circle but not too far
        if (distanceFromCenter < 30 || distanceFromCenter > radius + 40) {
            return;
        }

        // Find the closest item to the mouse cursor
        float closestDist = Float.MAX_VALUE;
        int closestIndex = -1;

        float angleStep = 360.0f / categories.size();
        for (int i = 0; i < categories.size(); i++) {
            float angle = (float) Math.toRadians(i * angleStep - 90); // Start at top
            float entryX = centerX + (float) Math.cos(angle) * radius;
            float entryY = centerY + (float) Math.sin(angle) * radius;

            float distToEntry = (float) Math.sqrt(Math.pow(mouseX - entryX, 2) + Math.pow(mouseY - entryY, 2));

            if (distToEntry < closestDist) {
                closestDist = distToEntry;
                closestIndex = i;
            }
        }

        // Only select if close enough (within 30 pixels of an item)
        if (closestDist <= 30) {
            hoveredIndex = closestIndex;
        }
    }

    private void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        // Draw a filled circle using multiple rectangles (approximation)
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    public List<Block> getSelectedBlocks() {
        if (hoveredIndex >= 0 && hoveredIndex < categories.size()) {
            return categories.get(hoveredIndex).getNodes();
        }
        return new ArrayList<>();
    }

    public String getSelectedCategoryName() {
        if (hoveredIndex >= 0 && hoveredIndex < categories.size()) {
            return formatCategoryName(categories.get(hoveredIndex).getCategory());
        }
        return null;
    }
}