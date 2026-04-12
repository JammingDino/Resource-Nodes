package com.jamming_dino.jd_resource_nodes.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class RegistryPickerScreen extends Screen {
    public enum PickerType {
        ITEM,
        BLOCK
    }

    private static final int MIN_COLUMNS = 7;
    private static final int MAX_COLUMNS = 14;
    private static final int MAX_ROWS = 5;
    private static final int SLOT_SIZE = 18;

    private final Screen parent;
    private final PickerType type;
    private final Consumer<String> onPick;

    private final List<PickOption> allOptions = new ArrayList<>();
    private final List<PickOption> filteredOptions = new ArrayList<>();

    private EditBox searchBox;
    private int scrollRow;
    private int visibleRows;
    private int columns;
    private int panelWidth;

    private int gridLeft;
    private int gridTop;

    public RegistryPickerScreen(Screen parent, PickerType type, Consumer<String> onPick) {
        super(Component.literal(type == PickerType.ITEM ? "Select Drop Item" : "Select Block"));
        this.parent = parent;
        this.type = type;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        panelWidth = Math.max(220, this.width - 30);
        columns = Math.max(MIN_COLUMNS, Math.min(MAX_COLUMNS, panelWidth / SLOT_SIZE));
        int gridWidth = columns * SLOT_SIZE;

        gridLeft = this.width / 2 - gridWidth / 2;
        gridTop = 48;
        visibleRows = Math.max(3, Math.min(MAX_ROWS, (this.height - 106) / SLOT_SIZE));

        searchBox = this.addRenderableWidget(new EditBox(this.font, gridLeft, 24, gridWidth, 18, Component.literal("Search")));
        searchBox.setResponder(value -> applyFilter());

        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(parent);
                    }
                })
                .bounds(gridLeft, gridTop + visibleRows * SLOT_SIZE + 10, 80, 20)
                .build());

        rebuildOptions();
        applyFilter();
    }

    private void rebuildOptions() {
        allOptions.clear();

        if (type == PickerType.ITEM) {
            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                allOptions.add(new PickOption(id.toString(), new ItemStack(item)));
            }
        } else {
            for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                Item asItem = block.asItem();
                if (asItem == Items.AIR) {
                    continue;
                }
                ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
                allOptions.add(new PickOption(id.toString(), new ItemStack(asItem)));
            }
        }

        allOptions.sort(Comparator.comparing(PickOption::id));
    }

    private void applyFilter() {
        String term = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        filteredOptions.clear();

        for (PickOption option : allOptions) {
            if (term.isEmpty() || option.id().contains(term)) {
                filteredOptions.add(option);
            }
        }
        scrollRow = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && scrollRow > 0) {
            scrollRow--;
            return true;
        }

        int maxRows = Math.max(0, (filteredOptions.size() - 1) / columns - (visibleRows - 1));
        if (scrollY < 0 && scrollRow < maxRows) {
            scrollRow++;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = gridLeft + col * SLOT_SIZE;
                int y = gridTop + row * SLOT_SIZE;
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    int index = (scrollRow + row) * columns + col;
                    if (index >= 0 && index < filteredOptions.size()) {
                        PickOption picked = filteredOptions.get(index);
                        onPick.accept(picked.id());
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(parent);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = gridLeft + col * SLOT_SIZE;
                int y = gridTop + row * SLOT_SIZE;

                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF4A4A4A);
                guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF1A1A1A);

                int index = (scrollRow + row) * columns + col;
                if (index >= 0 && index < filteredOptions.size()) {
                    PickOption option = filteredOptions.get(index);
                    guiGraphics.renderItem(option.stack(), x + 1, y + 1);

                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        guiGraphics.renderTooltip(this.font, Component.literal(option.id()), mouseX, mouseY);
                    }
                }
            }
        }
    }

    private record PickOption(String id, ItemStack stack) {
    }
}



