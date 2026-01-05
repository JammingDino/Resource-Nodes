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
import net.neoforged.fml.ModList; // Import ModList to read metadata

public class ConfigScreen extends Screen {
    private final Screen parent;

    // Text fields for numeric settings
    private EditBox textScaleField;
    private EditBox scannerRadiusField;
    private EditBox impureTicksField;
    private EditBox normalTicksField;
    private EditBox pureTicksField;

    // Toggle button
    private CycleButton<Boolean> textEnabledButton;

    // Action buttons (Fixed at bottom)
    private Button saveButton;
    private Button cancelButton;

    // Temporary values
    private float tempTextScale;
    private int tempScannerRadius;
    private int tempImpureTicks;
    private int tempNormalTicks;
    private int tempPureTicks;
    private boolean tempTextEnabled;

    // Scroll variables
    private double scrollAmount = 0;
    private int contentHeight = 0;
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 40;
    private static final int ENTRY_HEIGHT = 45; // Height reserved per setting

    public ConfigScreen(Screen parent) {
        // Fetch the version from NeoForge metadata (which is set by your GitHub Action)
        super(Component.literal("Resource Nodes Config v" + getModVersion()));
        this.parent = parent;

        // Load current values
        this.tempTextScale = ResourceNodesConfig.getTextScale();
        this.tempScannerRadius = ResourceNodesConfig.getScannerRadius();
        this.tempImpureTicks = ResourceNodesConfig.getImpureTicks();
        this.tempNormalTicks = ResourceNodesConfig.getNormalTicks();
        this.tempPureTicks = ResourceNodesConfig.getPureTicks();
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
        // We create them, but we set their positions dynamically in updateWidgetPositions()

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

        impureTicksField = new EditBox(this.font, 0, 0, fieldWidth, fieldHeight, Component.literal("Impure Ticks"));
        impureTicksField.setValue(String.valueOf(tempImpureTicks));
        impureTicksField.setMaxLength(6);
        impureTicksField.setResponder(this::onImpureTicksChanged);
        this.addRenderableWidget(impureTicksField);

        normalTicksField = new EditBox(this.font, 0, 0, fieldWidth, fieldHeight, Component.literal("Normal Ticks"));
        normalTicksField.setValue(String.valueOf(tempNormalTicks));
        normalTicksField.setMaxLength(6);
        normalTicksField.setResponder(this::onNormalTicksChanged);
        this.addRenderableWidget(normalTicksField);

        pureTicksField = new EditBox(this.font, 0, 0, fieldWidth, fieldHeight, Component.literal("Pure Ticks"));
        pureTicksField.setValue(String.valueOf(tempPureTicks));
        pureTicksField.setMaxLength(6);
        pureTicksField.setResponder(this::onPureTicksChanged);
        this.addRenderableWidget(pureTicksField);

        // Calculate total content height (6 items * height per item)
        this.contentHeight = 6 * ENTRY_HEIGHT;

        // 2. Fixed Buttons (Footer)
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
        int startY = HEADER_HEIGHT - (int) scrollAmount; // Start drawing content here

        // Helper to position widget and increment Y
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

        // 4. Impure Ticks
        impureTicksField.setPosition(centerX - fieldWidth / 2, currentY + 12);
        currentY += ENTRY_HEIGHT;

        // 5. Normal Ticks
        normalTicksField.setPosition(centerX - fieldWidth / 2, currentY + 12);
        currentY += ENTRY_HEIGHT;

        // 6. Pure Ticks
        pureTicksField.setPosition(centerX - fieldWidth / 2, currentY + 12);
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

    private void onImpureTicksChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) tempImpureTicks = parsed;
        } catch (NumberFormatException ignored) {}
    }

    private void onNormalTicksChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) tempNormalTicks = parsed;
        } catch (NumberFormatException ignored) {}
    }

    private void onPureTicksChanged(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) tempPureTicks = parsed;
        } catch (NumberFormatException ignored) {}
    }

    private void saveAndClose() {
        ResourceNodesConfig.setTextEnabled(tempTextEnabled);
        ResourceNodesConfig.setTextScale(tempTextScale);
        ResourceNodesConfig.setScannerRadius(tempScannerRadius);
        ResourceNodesConfig.setImpureTicks(tempImpureTicks);
        ResourceNodesConfig.setNormalTicks(tempNormalTicks);
        ResourceNodesConfig.setPureTicks(tempPureTicks);
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // --- Render Scrollable Content ---

        // Use Scissor to clip content between Header and Footer
        graphics.enableScissor(0, HEADER_HEIGHT, this.width, this.height - FOOTER_HEIGHT);

        // Render widgets (Super handles buttons/editboxes)
        super.render(graphics, mouseX, mouseY, partialTick);

        // Render Labels Manually based on scroll position
        int centerX = this.width / 2;
        int fieldWidth = 200;
        int startY = HEADER_HEIGHT - (int) scrollAmount;
        int currentY = startY;

        // 1. Text Enabled (Label handled by CycleButton)
        currentY += ENTRY_HEIGHT;

        // 2. Text Scale
        graphics.drawString(this.font, "Text Scale (0.1 - 5.0):", centerX - fieldWidth / 2, currentY, 0xAAAAAA);
        currentY += ENTRY_HEIGHT;

        // 3. Scanner Radius
        graphics.drawString(this.font, "Scanner Radius (16 - 512):", centerX - fieldWidth / 2, currentY, 0xAAAAAA);
        currentY += ENTRY_HEIGHT;

        // 4. Impure Ticks
        graphics.drawString(this.font, "Impure Ticks:", centerX - fieldWidth / 2, currentY, 0xAAAAAA);
        currentY += ENTRY_HEIGHT;

        // 5. Normal Ticks
        graphics.drawString(this.font, "Normal Ticks:", centerX - fieldWidth / 2, currentY, 0xAAAAAA);
        currentY += ENTRY_HEIGHT;

        // 6. Pure Ticks
        graphics.drawString(this.font, "Pure Ticks:", centerX - fieldWidth / 2, currentY, 0xAAAAAA);

        graphics.disableScissor();

        // --- Render Header (Overlay) ---
        graphics.fill(0, 0, this.width, HEADER_HEIGHT, 0xDD000000); // Dark background
        graphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);
        graphics.hLine(0, this.width, HEADER_HEIGHT - 1, 0xFF555555);

        // --- Render Footer (Overlay) ---
        graphics.fill(0, this.height - FOOTER_HEIGHT, this.width, this.height, 0xDD000000);
        graphics.hLine(0, this.width, this.height - FOOTER_HEIGHT, 0xFF555555);

        // Re-render buttons so they appear ON TOP of the footer background
        saveButton.render(graphics, mouseX, mouseY, partialTick);
        cancelButton.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}