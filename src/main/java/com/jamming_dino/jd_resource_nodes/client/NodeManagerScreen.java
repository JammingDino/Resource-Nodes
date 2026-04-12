package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.CustomNodePurityMode;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.network.AddCustomNodeConfigPacket;
import com.jamming_dino.jd_resource_nodes.network.RequestNodeSettingsPacket;
import com.jamming_dino.jd_resource_nodes.network.UpdateNodeTogglePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NodeManagerScreen extends Screen {
    private final List<String> nodeIds = new ArrayList<>();
    private int customCount;

    private NodeToggleList nodeToggleList;
    private CycleButton<CustomNodePurityMode> purityButton;
    private Button pickDropButton;
    private Button pickOriginalButton;
    private Button pickRegenButton;

    private String selectedDropItem = "minecraft:raw_iron";
    private String selectedOriginalBlock = "minecraft:iron_ore";
    private String selectedRegeneratingBlock = "minecraft:stone";
    private CustomNodePurityMode selectedPurity = CustomNodePurityMode.ALL;

    private Component statusMessage = Component.empty();

    private int panelLeft;
    private int panelWidth;
    private int panelTop;
    private int listBottom;
    private int lastNodeHash;

    public NodeManagerScreen() {
        super(Component.literal("Resource Node Manager"));
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(RequestNodeSettingsPacket.INSTANCE);
        rebuildNodeIds();
        lastNodeHash = computeNodeHash();

        panelWidth = Math.min(460, this.width - 20);
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = 20;

        int listTop = panelTop + 8;
        listBottom = Math.max(listTop + 50, this.height - 132);
        int actionButtonWidth = Math.max(90, (panelWidth - 4) / 2);

        nodeToggleList = this.addRenderableWidget(new NodeToggleList(this.minecraft, this.width, listBottom - listTop, listTop, 22));
        nodeToggleList.rebuildEntries(nodeIds);

        int customTop = listBottom + 8;
        purityButton = this.addRenderableWidget(CycleButton.<CustomNodePurityMode>builder(mode -> Component.literal("Purity: " + mode.getDisplayName()))
                .withValues(CustomNodePurityMode.values())
                .withInitialValue(selectedPurity)
                .create(panelLeft, customTop, panelWidth, 20, Component.literal(""), (button, value) -> selectedPurity = value));

        pickDropButton = this.addRenderableWidget(Button.builder(Component.literal("Pick Drop Item"), button ->
                        this.minecraft.setScreen(new RegistryPickerScreen(this, RegistryPickerScreen.PickerType.ITEM, picked -> {
                            selectedDropItem = picked;
                            updatePickerButtonLabels();
                        })))
                .bounds(panelLeft, customTop + 24, panelWidth, 20)
                .build());

        pickOriginalButton = this.addRenderableWidget(Button.builder(Component.literal("Pick Original Block"), button ->
                        this.minecraft.setScreen(new RegistryPickerScreen(this, RegistryPickerScreen.PickerType.BLOCK, picked -> {
                            selectedOriginalBlock = picked;
                            updatePickerButtonLabels();
                        })))
                .bounds(panelLeft, customTop + 48, panelWidth, 20)
                .build());

        pickRegenButton = this.addRenderableWidget(Button.builder(Component.literal("Pick Regenerating Block"), button ->
                        this.minecraft.setScreen(new RegistryPickerScreen(this, RegistryPickerScreen.PickerType.BLOCK, picked -> {
                            selectedRegeneratingBlock = picked;
                            updatePickerButtonLabels();
                        })))
                .bounds(panelLeft, customTop + 72, panelWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Add Custom (saved to config, restart)"), button -> addCustomNode())
                .bounds(panelLeft, customTop + 98, actionButtonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> this.onClose())
                .bounds(panelLeft + actionButtonWidth + 4, customTop + 98, actionButtonWidth, 20)
                .build());

        updatePickerButtonLabels();
    }

    private void rebuildNodeIds() {
        List<String> builtin = new ArrayList<>();
        for (DeferredBlock<?> holder : ResourceNodes.REGISTERED_NODES) {
            builtin.add(holder.getId().toString());
        }

        List<String> custom = new ArrayList<>(NodeSettingsClientCache.getCustomNodeIds());
        custom.sort(Comparator.naturalOrder());
        builtin.sort(Comparator.naturalOrder());

        nodeIds.clear();
        nodeIds.addAll(custom);
        nodeIds.addAll(builtin);
        customCount = custom.size();

        List<String> unique = new ArrayList<>();
        String previous = null;
        for (String id : nodeIds) {
            if (!id.equals(previous)) {
                unique.add(id);
            }
            previous = id;
        }
        nodeIds.clear();
        nodeIds.addAll(unique);
    }

    private int computeNodeHash() {
        int hash = 1;
        for (String id : nodeIds) {
            hash = 31 * hash + id.hashCode();
        }
        return hash;
    }

    private void updatePickerButtonLabels() {
        pickDropButton.setMessage(Component.literal("Drop Item: " + trimId(selectedDropItem, 42)));
        pickOriginalButton.setMessage(Component.literal("Original Block: " + trimId(selectedOriginalBlock, 40)));
        pickRegenButton.setMessage(Component.literal("Regenerating Block: " + trimId(selectedRegeneratingBlock, 36)));
    }

    private void sendToggle(String blockId, boolean enabled) {
        PacketDistributor.sendToServer(new UpdateNodeTogglePacket(blockId, enabled));
        PacketDistributor.sendToServer(RequestNodeSettingsPacket.INSTANCE);
    }

    private String trimId(String id, int max) {
        if (id.length() <= max) {
            return id;
        }
        int half = (max - 3) / 2;
        return id.substring(0, half) + "..." + id.substring(id.length() - (max - 3 - half));
    }

    private void addCustomNode() {
        if (selectedOriginalBlock.isBlank() || selectedRegeneratingBlock.isBlank() || selectedDropItem.isBlank()) {
            statusMessage = Component.literal("Pick original block, regenerating block, and drop item first.");
            return;
        }

        String id = buildCustomId();

        PacketDistributor.sendToServer(new AddCustomNodeConfigPacket(
                id,
                selectedPurity.getId(),
                selectedOriginalBlock,
                selectedRegeneratingBlock,
                selectedDropItem
        ));
        statusMessage = Component.literal("Custom node saved. It should appear in this list shortly; restart for Creative tab registration.");
    }

    private String buildCustomId() {
        String source = selectedOriginalBlock + "_" + selectedDropItem + "_" + selectedPurity.getId();
        return source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
    }

    @Override
    public void tick() {
        super.tick();

        rebuildNodeIds();
        int currentHash = computeNodeHash();
        if (nodeToggleList != null && currentHash != lastNodeHash) {
            nodeToggleList.rebuildEntries(nodeIds);
            lastNodeHash = currentHash;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int footerY = listBottom + 124;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, 12, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Node List (Custom first: " + customCount + ")", panelLeft + 4, panelTop + 22, 0xA0A0A0, false);
        guiGraphics.drawString(this.font, "Custom Node Setup", panelLeft + 4, listBottom + 2, 0xA0A0A0, false);
        if (!statusMessage.getString().isEmpty()) {
            guiGraphics.drawString(this.font, statusMessage, panelLeft + 4, Math.min(this.height - 12, footerY), 0xFFFF80, false);
        }
    }

    private class NodeToggleList extends ObjectSelectionList<NodeEntry> {
        public NodeToggleList(net.minecraft.client.Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
        }

        public void rebuildEntries(List<String> ids) {
            this.clearEntries();
            for (String id : ids) {
                this.addEntry(new NodeEntry(id));
            }
        }

        @Override
        public int getRowWidth() {
            return panelWidth - 12;
        }

        @Override
        protected int getScrollbarPosition() {
            return panelLeft + panelWidth - 6;
        }

        public void refreshStates() {
            for (NodeEntry entry : this.children()) {
                entry.refreshState();
            }
        }
    }

    private class NodeEntry extends ObjectSelectionList.Entry<NodeEntry> {
        private final String blockId;
        private boolean enabled;
        private int buttonLeft;
        private int buttonTop;

        private NodeEntry(String blockId) {
            this.blockId = blockId;
            this.enabled = NodeSettingsClientCache.isEnabled(blockId, false);
        }

        private void refreshState() {
            this.enabled = NodeSettingsClientCache.isEnabled(this.blockId, false);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            refreshState();

            boolean isCustom = blockId.contains(":node_custom_");
            String cleanId = blockId.replace("jd_resource_nodes:node_custom_", "").replace("jd_resource_nodes:", "");
            String visibleId = trimId(cleanId, 40);
            guiGraphics.drawString(font, (isCustom ? "[Custom] " : "") + visibleId, left + 4, top + 6, isCustom ? 0x9FE6A0 : 0xFFFFFF, false);

            int controlsRight = left + width - 4;

            buttonLeft = controlsRight - 108;
            buttonTop = top + 2;

            drawMiniButton(guiGraphics, buttonLeft, buttonTop, 104, 18, enabled ? "Disable" : "Enable", mouseX, mouseY);
        }

        private void drawMiniButton(GuiGraphics guiGraphics, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            int border = hover ? 0xFFFFFFFF : 0xFF808080;
            int fill = hover ? 0xFF3A3A3A : 0xFF2A2A2A;
            guiGraphics.fill(x, y, x + w, y + h, border);
            guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
            guiGraphics.drawCenteredString(font, label, x + w / 2, y + 5, 0xFFFFFF);
        }

        @Override
        public Component getNarration() {
            return Component.literal(blockId + (enabled ? " enabled" : " disabled"));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }

            if (mouseX >= buttonLeft && mouseX < buttonLeft + 104 && mouseY >= buttonTop && mouseY < buttonTop + 18) {
                sendToggle(blockId, !enabled);
                return true;
            }

            return false;
        }
    }
}

