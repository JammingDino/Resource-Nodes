package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;

public class ConfigScreen extends Screen {
    private final Screen parent;

    // Text fields for numeric settings
    private EditBox textScaleField;
    private EditBox scannerRadiusField;
    private EditBox regenerateTicksField; // Replaced the 3 individual fields

    // Toggle button
    private CycleButton<Boolean> textEnabledButton;

    // Action buttons
    private Button saveButton;
    private Button cancelButton;

    // Temporary values
    private float tempTextScale;
    private int tempScannerRadius;
    private int tempRegenerateTicks; // Single value
    private boolean tempTextEnabled;

    // Scroll variables
    private double scrollAmount = 0;
    private int contentHeight = 0;
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 40;
    private static final int ENTRY_HEIGHT = 45;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Resource Nodes Config v" + getModVersion()));
        this.parent = parent;

        this.tempTextScale = ResourceNodesConfig.getTextScale();
        this.tempScannerRadius = ResourceNodesConfig.getScannerRadius();
        this.tempRegenerateTicks = ResourceNodesConfig.getRegenerateTicks(); // Load single value
        this.tempTextEnabled = ResourceNodesConfig.isTextEnabled();
    }

    private static String getModVersion() {
        return ModList.get().getModContainerById(ResourceNodes.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("Unknown");
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = 200;
        int fieldHeight = 20;

        // 1. Create Scrollable Content

        textEnabledButton = CycleButton.booleanBuilder(Component.literal("ON"), Component.literal("OFF"))
                .withInitialValue(tempTextEnabled)
                .create(0, 0, fieldWidth, fieldHeight, Component.literal("Pinger Text: "), (button, value) -> tempTextEnabled = value);
        this.addRenderableWidget(textEnabledButton);

        textScaleField = new EditBox(this.font, 0, 0, fieldWidth, fieldHeight, Component.literal("Text Scale"));
        textScaleField.setValue(String.format("%.2f", tempTextScale));
        textScaleField.setMaxLength(6);
        textScaleField.setResponder(this::onTextScaleChanged);
        this.addRenderableWidget(textScaleField);

        scannerRadiusField = new EditBox(this.font, 0, 0, fieldWidth, fieldHeight, Component.literal("Scanner Radius"));
        scannerRadiusField.setValue(String.valueOf(tempScannerRadius));
        scannerRadiusField.setMaxLength(6);
        scannerRadiusField.setResponder(this::onScannerRadiusChanged);
        this.addRenderableWidget(scannerRadiusField);

        regenerateTicksField = new EditBox(this.font, 0, 0, fieldWidth, fieldHeight, Component.literal("Regeneration Ticks"));
        regenerateTicksField.setValue(String.valueOf(tempRegenerateTicks));
        regenerateTicksField.setMaxLength(6);
        regenerateTicksField.setResponder(this::onRegenerateTicksChanged);
        this.addRenderableWidget(regenerateTicksField);

        // Calculate total content height (4 items)
        this.contentHeight = 4 * ENTRY_HEIGHT;

        // 2. Fixed Buttons
        saveButton = Button.builder(Component.literal("Save"), button -> saveAndClose())
                .bounds(centerX - fieldWidth / 2, this.height - 30, fieldWidth / 2 - 5, 20)
                .build();
        this.addRenderableWidget(saveButton);

        cancelButton = Button.builder(Component.literal("Cancel"), button -> this.minecraft.setScreen(parent))
                .bounds(centerX + 5, this.height - 30, fieldWidth / 2 - 5, 20)
                .build();
        this.addRenderableWidget(cancelButton);

        updateWidgetPositions();
    }

    private void updateWidgetPositions() {
        int centerX = this.width / 2;
        int fieldWidth = 200;
        int startY = HEADER_HEIGHT - (int) scrollAmount;

        int currentY = startY;

        // 1. Text Enabled
        textEnabledButton.setPosition(centerX - fieldWidth / 2, currentY + 12);
        currentY += ENTRY_HEIGHT;

        // 2. Text Scale
        textScaleField.setPosition(centerX - fieldWidth / 2, currentY + 12);
        currentY += ENTRY_HEIGHT;

        // 3. Scanner Radius
        scannerRadiusField.setPosition(centerX - fieldWidth / 2, currentY + 12);
        currentY += ENTRY_HEIGHT;

        // 4. Regeneration Ticks
        regenerateTicksField.setPosition(centerX - fieldWidth / 2, currentY + 12);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            int maxScroll = Math.max(0, contentHeight - (this.height - HEADER_HEIGHT - FOOTER_HEIGHT));
            this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * 20, 0, maxScroll);
            updateWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void onTextScaleChanged(String value) {
        try {
            float parsed = Float.parseFloat(value);
            if (parsed >= 0.1f && parsed <= 5.0f) tempTextScale = parsed;
        } catch (NumberFormatException ignored) {}
    }

    private void onScannerRadiusChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) tempScannerRadius = parsed;
        } catch (NumberFormatException ignored) {}
    }

    private void onRegenerateTicksChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) tempRegenerateTicks = parsed;
        } catch (NumberFormatException ignored) {}
    }

    private void saveAndClose() {
        ResourceNodesConfig.setTextEnabled(tempTextEnabled);
        ResourceNodesConfig.setTextScale(tempTextScale);
        ResourceNodesConfig.setScannerRadius(tempScannerRadius);
        ResourceNodesConfig.setRegenerateTicks(tempRegenerateTicks);
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(0, HEADER_HEIGHT, this.width, this.height - FOOTER_HEIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int fieldWidth = 200;
        int startY = HEADER_HEIGHT - (int) scrollAmount;
        int currentY = startY;

        // 1. Text Enabled
        currentY += ENTRY_HEIGHT;

        // 2. Text Scale
        graphics.drawString(this.font, "Text Scale (0.1 - 5.0):", centerX - fieldWidth / 2, currentY, 0xAAAAAA);
        currentY += ENTRY_HEIGHT;

        // 3. Scanner Radius
        graphics.drawString(this.font, "Scanner Radius (16 - 512):", centerX - fieldWidth / 2, currentY, 0xAAAAAA);
        currentY += ENTRY_HEIGHT;

        // 4. Regeneration Ticks
        graphics.drawString(this.font, "Regeneration Ticks (All Tiers):", centerX - fieldWidth / 2, currentY, 0xAAAAAA);

        graphics.disableScissor();

        // Header
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, 0xDD000000);
        graphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        graphics.hLine(0, this.width, HEADER_HEIGHT - 1, 0xFF555555);

        // Footer
        graphics.fill(0, this.height - FOOTER_HEIGHT, this.width, this.height, 0xDD000000);
        graphics.hLine(0, this.width, this.height - FOOTER_HEIGHT, 0xFF555555);

        saveButton.render(graphics, mouseX, mouseY, partialTick);
        cancelButton.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}