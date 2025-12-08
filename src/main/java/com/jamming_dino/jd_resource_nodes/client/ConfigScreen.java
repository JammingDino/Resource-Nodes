package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    // Text fields for numeric settings
    private EditBox textScaleField;
    private EditBox impureTicksField;
    private EditBox normalTicksField;
    private EditBox pureTicksField;

    // Toggle button
    private CycleButton<Boolean> textEnabledButton;

    // Action buttons
    private Button saveButton;
    private Button cancelButton;

    // Temporary values (only saved when "Save" is clicked)
    private float tempTextScale;
    private int tempImpureTicks;
    private int tempNormalTicks;
    private int tempPureTicks;
    private boolean tempTextEnabled;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Resource Nodes Config"));
        this.parent = parent;

        // Load current values
        this.tempTextScale = ResourceNodesConfig.getTextScale();
        this.tempImpureTicks = ResourceNodesConfig.getImpureTicks();
        this.tempNormalTicks = ResourceNodesConfig.getNormalTicks();
        this.tempPureTicks = ResourceNodesConfig.getPureTicks();
        this.tempTextEnabled = ResourceNodesConfig.isTextEnabled();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = 200;
        int fieldHeight = 20;
        int spacing = 35; // Increased spacing between elements
        int startY = 60; // Start lower to give more room

        // Text Enabled Toggle
        textEnabledButton = CycleButton.booleanBuilder(
                        Component.literal("ON"),
                        Component.literal("OFF")
                )
                .withInitialValue(tempTextEnabled)
                .create(
                        centerX - fieldWidth / 2,
                        startY,
                        fieldWidth,
                        fieldHeight,
                        Component.literal("Pinger Text: "),
                        (button, value) -> {
                            tempTextEnabled = value;
                        }
                );
        this.addRenderableWidget(textEnabledButton);

        // Text Scale Field
        textScaleField = new EditBox(
                this.font,
                centerX - fieldWidth / 2,
                startY + spacing,
                fieldWidth,
                fieldHeight,
                Component.literal("Text Scale")
        );
        textScaleField.setValue(String.format("%.2f", tempTextScale));
        textScaleField.setMaxLength(6);
        textScaleField.setResponder(this::onTextScaleChanged);
        this.addRenderableWidget(textScaleField);

        // Impure Ticks Field
        impureTicksField = new EditBox(
                this.font,
                centerX - fieldWidth / 2,
                startY + spacing * 2,
                fieldWidth,
                fieldHeight,
                Component.literal("Impure Ticks")
        );
        impureTicksField.setValue(String.valueOf(tempImpureTicks));
        impureTicksField.setMaxLength(6);
        impureTicksField.setResponder(this::onImpureTicksChanged);
        this.addRenderableWidget(impureTicksField);

        // Normal Ticks Field
        normalTicksField = new EditBox(
                this.font,
                centerX - fieldWidth / 2,
                startY + spacing * 3,
                fieldWidth,
                fieldHeight,
                Component.literal("Normal Ticks")
        );
        normalTicksField.setValue(String.valueOf(tempNormalTicks));
        normalTicksField.setMaxLength(6);
        normalTicksField.setResponder(this::onNormalTicksChanged);
        this.addRenderableWidget(normalTicksField);

        // Pure Ticks Field
        pureTicksField = new EditBox(
                this.font,
                centerX - fieldWidth / 2,
                startY + spacing * 4,
                fieldWidth,
                fieldHeight,
                Component.literal("Pure Ticks")
        );
        pureTicksField.setValue(String.valueOf(tempPureTicks));
        pureTicksField.setMaxLength(6);
        pureTicksField.setResponder(this::onPureTicksChanged);
        this.addRenderableWidget(pureTicksField);

        // Save Button
        saveButton = Button.builder(
                        Component.literal("Save"),
                        button -> saveAndClose()
                )
                .bounds(
                        centerX - fieldWidth / 2,
                        startY + spacing * 5 + 10,
                        fieldWidth / 2 - 5,
                        fieldHeight
                )
                .build();
        this.addRenderableWidget(saveButton);

        // Cancel Button
        cancelButton = Button.builder(
                        Component.literal("Cancel"),
                        button -> this.minecraft.setScreen(parent)
                )
                .bounds(
                        centerX + 5,
                        startY + spacing * 5 + 10,
                        fieldWidth / 2 - 5,
                        fieldHeight
                )
                .build();
        this.addRenderableWidget(cancelButton);
    }

    private void onTextScaleChanged(String value) {
        try {
            float parsed = Float.parseFloat(value);
            if (parsed >= 0.1f && parsed <= 5.0f) {
                tempTextScale = parsed;
            }
        } catch (NumberFormatException ignored) {
            // Invalid input, ignore
        }
    }

    private void onImpureTicksChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                tempImpureTicks = parsed;
            }
        } catch (NumberFormatException ignored) {
            // Invalid input, ignore
        }
    }

    private void onNormalTicksChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                tempNormalTicks = parsed;
            }
        } catch (NumberFormatException ignored) {
            // Invalid input, ignore
        }
    }

    private void onPureTicksChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                tempPureTicks = parsed;
            }
        } catch (NumberFormatException ignored) {
            // Invalid input, ignore
        }
    }

    private void saveAndClose() {
        // Save all values to config
        ResourceNodesConfig.setTextEnabled(tempTextEnabled);
        ResourceNodesConfig.setTextScale(tempTextScale);

        // We need new setters in the config class for these
        ResourceNodesConfig.setImpureTicks(tempImpureTicks);
        ResourceNodesConfig.setNormalTicks(tempNormalTicks);
        ResourceNodesConfig.setPureTicks(tempPureTicks);

        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int fieldWidth = 200;
        int startY = 60;
        int spacing = 35;

        // Draw title
        graphics.drawCenteredString(
                this.font,
                this.title,
                centerX,
                20,
                0xFFFFFF
        );

        // Draw labels ABOVE each field (not overlapping)
        graphics.drawString(
                this.font,
                "Text Scale (0.1 - 5.0):",
                centerX - fieldWidth / 2,
                startY + spacing - 12,
                0xAAAAAA
        );

        graphics.drawString(
                this.font,
                "Impure Ticks:",
                centerX - fieldWidth / 2,
                startY + spacing * 2 - 12,
                0xAAAAAA
        );

        graphics.drawString(
                this.font,
                "Normal Ticks:",
                centerX - fieldWidth / 2,
                startY + spacing * 3 - 12,
                0xAAAAAA
        );

        graphics.drawString(
                this.font,
                "Pure Ticks:",
                centerX - fieldWidth / 2,
                startY + spacing * 4 - 12,
                0xAAAAAA
        );

        // Draw info text at bottom
        String infoText = "These settings affect the Resource Node Scanner (V key)";
        graphics.drawCenteredString(
                this.font,
                infoText,
                centerX,
                this.height - 30,
                0x888888
        );

        String tickInfo = "20 ticks = 1 second";
        graphics.drawCenteredString(
                this.font,
                tickInfo,
                centerX,
                this.height - 18,
                0x666666
        );
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}