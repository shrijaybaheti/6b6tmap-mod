package net.map6b6t.gui;

import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.network.UploadService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MapConfigScreen extends Screen {
    private enum Page { MAIN, AREA, UPLOAD }

    private final Runnable resetCache;
    private final Page page;
    private final Screen parent;
    private EditBox areaField, serverField, tokenField;
    private String message = "";

    public MapConfigScreen(Runnable resetCache) { this(resetCache, Page.MAIN, null); }

    public MapConfigScreen(Runnable resetCache, Screen parent) { this(resetCache, Page.MAIN, parent); }

    private MapConfigScreen(Runnable resetCache, Page page, Screen parent) {
        super(Component.literal("6b6t Map Settings"));
        this.resetCache = resetCache;
        this.page = page;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = Math.min(190, (width - 28) / 2);
        int left = (width - buttonWidth * 2 - 8) / 2;
        int right = left + buttonWidth + 8;
        int top = 60;
        if (page == Page.MAIN) {
            ModConfig c = ConfigManager.get();
            toggle("Mapping", () -> c.enabled, value -> c.enabled = value, left, top, buttonWidth);
            toggle("Status HUD", () -> c.showHud, value -> c.showHud = value, right, top, buttonWidth);
            toggle("Minimap", () -> c.showMinimap, value -> c.showMinimap = value, left, top + 26, buttonWidth);
            toggle("Void floor", () -> c.showVoidHud, value -> c.showVoidHud = value, right, top + 26, buttonWidth);
            toggle("Nametag counts", () -> c.showNametagHud, value -> c.showNametagHud = value, left, top + 52, buttonWidth);
            addRenderableWidget(Button.builder(Component.literal("Change Position"), button ->
                    ScreenBridge.set(new HudEditorScreen(this))).bounds(right, top + 52, buttonWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Area Settings"), button -> open(Page.AREA))
                    .bounds(left, top + 78, buttonWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Server and Token"), button -> open(Page.UPLOAD))
                    .bounds(right, top + 78, buttonWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Reset session cache"), button -> {
                resetCache.run();
                message = "Session cache cleared";
            }).bounds(left, top + 104, buttonWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                    .bounds(right, top + 104, buttonWidth, 20).build());
        } else if (page == Page.AREA) {
            int fieldWidth = Math.min(360, width - 32);
            int x = (width - fieldWidth) / 2;
            int y = Math.max(70, height / 2 - 18);
            ModConfig c = ConfigManager.get();
            areaField = new EditBox(font, x, y, fieldWidth, 20, Component.literal("Area diameter or world"));
            areaField.setMaxLength(12);
            areaField.setValue(c.recordWorld ? "world" : Integer.toString(c.spawnRadius * 2));
            addRenderableWidget(areaField);
            addRenderableWidget(Button.builder(Component.literal("Set area"), button -> setArea())
                    .bounds(x, y + 28, fieldWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                    .bounds((width - 100) / 2, height - 34, 100, 20).build());
        } else {
            int fieldWidth = Math.min(360, width - 32);
            int x = (width - fieldWidth) / 2;
            int y = Math.max(70, height / 2 - 48);
            serverField = field(x, y + 12, fieldWidth, "Server URL", ConfigManager.get().serverUrl);
            addRenderableWidget(serverField);
            addRenderableWidget(Button.builder(Component.literal("Save URL"), button -> saveUrl())
                    .bounds(x, y + 38, fieldWidth, 20).build());
            tokenField = field(x, y + 74, fieldWidth, "Upload token", ConfigManager.get().submitToken);
            addRenderableWidget(tokenField);
            addRenderableWidget(Button.builder(Component.literal("Save token"), button -> saveToken())
                    .bounds(x, y + 100, fieldWidth, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                    .bounds((width - 100) / 2, height - 34, 100, 20).build());
        }
    }

    private void toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set, int x, int y, int width) {
        addRenderableWidget(Button.builder(toggleLabel(name, get.get()), button -> {
            set.accept(!get.get());
            ConfigManager.save();
            button.setMessage(toggleLabel(name, get.get()));
        }).bounds(x, y, width, 20).build());
    }

    private EditBox field(int x, int y, int width, String label, String value) {
        EditBox result = new EditBox(font, x, y, width, 20, Component.literal(label));
        result.setMaxLength(2048);
        result.setValue(value == null ? "" : value);
        return result;
    }

    private void open(Page next) { ScreenBridge.set(new MapConfigScreen(resetCache, next, this)); }

    private void setArea() {
        String value = areaField.getValue().trim();
        ModConfig config = ConfigManager.get();
        if (value.equalsIgnoreCase("world")) {
            config.setRecordWorld(true);
            ConfigManager.save();
            message = "Recording the whole world";
            return;
        }
        try {
            long diameter = Long.parseLong(value);
            if (!config.setAreaDiameter(diameter)) {
                message = "Diameter must be between 32 and " + ModConfig.MAX_AREA_DIAMETER;
                return;
            }
            ConfigManager.save();
            message = "Recording " + config.formatBounds();
        } catch (NumberFormatException exception) { message = "Enter a diameter or world"; }
    }

    private void saveUrl() {
        String value = ModConfig.sanitizeServerUrl(serverField.getValue());
        if (!(value.startsWith("http://") || value.startsWith("https://"))) {
            message = "URL must start with http:// or https://";
            return;
        }
        ConfigManager.get().serverUrl = value;
        ConfigManager.save();
        message = "Server URL saved";
    }

    private void saveToken() {
        if (tokenField.getValue().isBlank()) { message = "Token cannot be blank"; return; }
        ConfigManager.get().submitToken = tokenField.getValue();
        ConfigManager.save();
        message = "Upload token saved";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, pageTitle(), width / 2, 22, 0xFFFFFFFF);
        if (page == Page.MAIN) {
            g.centeredText(font, UploadService.get().stats().snapshot() + "   ·   Area " + ConfigManager.get().formatBounds(),
                    width / 2, 40, 0xFFDDDDDD);
        } else if (page == Page.AREA) {
            g.centeredText(font, "Enter the total diameter centered at (0, 0), or use world.", width / 2, height / 2 - 46, 0xFFDDDDDD);
        } else {
            g.centeredText(font, "The server URL receives chunk data; the token authorizes uploads.", width / 2, height / 2 - 78, 0xFFDDDDDD);
            g.text(font, "Server URL", width / 2 - Math.min(180, (width - 32) / 2), height / 2 - 56, 0xFFFFFFFF);
            g.text(font, "Upload token", width / 2 - Math.min(180, (width - 32) / 2), height / 2 + 6, 0xFFFFFFFF);
        }
        if (!message.isEmpty()) g.centeredText(font, message, width / 2, height - 52, 0xFFFFD166);
    }

    private Component pageTitle() {
        return Component.literal(switch (page) {
            case MAIN -> "6b6t Map Settings (1/1)";
            case AREA -> "Area Settings";
            case UPLOAD -> "Server and Upload Settings";
        });
    }

    @Override
    public void onClose() { ScreenBridge.set(parent); }

    private static Component toggleLabel(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }
}
