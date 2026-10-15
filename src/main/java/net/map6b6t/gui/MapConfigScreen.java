package net.map6b6t.gui;

import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.network.UploadService;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MapConfigScreen extends Screen {
    private enum Page { MAIN, AREA, UPLOAD }

    private final Runnable resetCache;
    private final Page page;
    private final Screen parent;
    private TextFieldWidget areaField, serverField, tokenField;
    private String message = "";

    public MapConfigScreen(Runnable resetCache) { this(resetCache, Page.MAIN, null); }

    public MapConfigScreen(Runnable resetCache, Screen parent) { this(resetCache, Page.MAIN, parent); }

    private MapConfigScreen(Runnable resetCache, Page page, Screen parent) {
        super(Text.literal("6b6t Map Settings"));
        this.resetCache = resetCache;
        this.page = page;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = Math.min(190, (width - 28) / 2);
        int gap = 8;
        int left = (width - buttonWidth * 2 - gap) / 2;
        int right = left + buttonWidth + gap;
        int top = 60;
        if (page == Page.MAIN) {
            ModConfig c = ConfigManager.get();
            toggle("Mapping", () -> c.enabled, value -> c.enabled = value, left, top, buttonWidth);
            toggle("Status HUD", () -> c.showHud, value -> c.showHud = value, right, top, buttonWidth);
            toggle("Minimap", () -> c.showMinimap, value -> c.showMinimap = value, left, top + 26, buttonWidth);
            toggle("Void floor", () -> c.showVoidHud, value -> c.showVoidHud = value, right, top + 26, buttonWidth);
            toggle("Nametag counts", () -> c.showNametagHud, value -> c.showNametagHud = value, left, top + 52, buttonWidth);
            addDrawableChild(ButtonWidget.builder(Text.literal("Change Position"), button ->
                    ScreenBridge.set(HudEditorScreens.create(this)))
                    .dimensions(right, top + 52, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Area Settings"), button -> open(Page.AREA))
                    .dimensions(left, top + 78, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Server and Token"), button -> open(Page.UPLOAD))
                    .dimensions(right, top + 78, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Reset session cache"), button -> {
                resetCache.run();
                message = "Session cache cleared";
            }).dimensions(left, top + 104, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                    .dimensions(right, top + 104, buttonWidth, 20).build());
        } else if (page == Page.AREA) {
            int fieldWidth = Math.min(360, width - 32);
            int x = (width - fieldWidth) / 2;
            int y = Math.max(70, height / 2 - 18);
            ModConfig c = ConfigManager.get();
            areaField = new TextFieldWidget(textRenderer, x, y, fieldWidth, 20, Text.literal("Area diameter or world"));
            areaField.setMaxLength(12);
            areaField.setText(c.recordWorld ? "world" : Integer.toString(c.spawnRadius * 2));
            addDrawableChild(areaField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Set area"), button -> setArea())
                    .dimensions(x, y + 28, fieldWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                    .dimensions((width - 100) / 2, height - 34, 100, 20).build());
        } else {
            int fieldWidth = Math.min(360, width - 32);
            int x = (width - fieldWidth) / 2;
            int y = Math.max(70, height / 2 - 48);
            serverField = field(x, y + 12, fieldWidth, "Server URL", ConfigManager.get().serverUrl);
            addDrawableChild(serverField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Save URL"), button -> saveUrl())
                    .dimensions(x, y + 38, fieldWidth, 20).build());
            tokenField = field(x, y + 74, fieldWidth, "Upload token", ConfigManager.get().submitToken);
            addDrawableChild(tokenField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Save token"), button -> saveToken())
                    .dimensions(x, y + 100, fieldWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                    .dimensions((width - 100) / 2, height - 34, 100, 20).build());
        }
    }

    private void toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set, int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(toggleLabel(name, get.get()), button -> {
            set.accept(!get.get());
            ConfigManager.save();
            button.setMessage(toggleLabel(name, get.get()));
        }).dimensions(x, y, width, 20).build());
    }

    private TextFieldWidget field(int x, int y, int width, String label, String value) {
        TextFieldWidget result = new TextFieldWidget(textRenderer, x, y, width, 20, Text.literal(label));
        result.setMaxLength(2048);
        result.setText(value == null ? "" : value);
        return result;
    }

    private void open(Page next) { ScreenBridge.set(new MapConfigScreen(resetCache, next, this)); }

    private void setArea() {
        String value = areaField.getText().trim();
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
        String value = ModConfig.sanitizeServerUrl(serverField.getText());
        if (!(value.startsWith("http://") || value.startsWith("https://"))) {
            message = "URL must start with http:// or https://";
            return;
        }
        ConfigManager.get().serverUrl = value;
        ConfigManager.save();
        message = "Server URL saved";
    }

    private void saveToken() {
        if (tokenField.getText().isBlank()) { message = "Token cannot be blank"; return; }
        ConfigManager.get().submitToken = tokenField.getText();
        ConfigManager.save();
        message = "Upload token saved";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, pageTitle(), width / 2, 22, 0xFFFFFFFF);
        if (page == Page.MAIN) {
            context.drawCenteredTextWithShadow(textRenderer,
                    UploadService.get().stats().snapshot() + "   ·   Area " + ConfigManager.get().formatBounds(),
                    width / 2, 40, 0xFFDDDDDD);
        } else if (page == Page.AREA) {
            context.drawCenteredTextWithShadow(textRenderer, "Enter the total diameter centered at (0, 0), or use world.", width / 2, height / 2 - 46, 0xFFDDDDDD);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, "The server URL receives chunk data; the token authorizes uploads.", width / 2, height / 2 - 78, 0xFFDDDDDD);
            context.drawTextWithShadow(textRenderer, Text.literal("Server URL"), width / 2 - Math.min(180, (width - 32) / 2), height / 2 - 56, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, Text.literal("Upload token"), width / 2 - Math.min(180, (width - 32) / 2), height / 2 + 6, 0xFFFFFFFF);
        }
        if (!message.isEmpty()) context.drawCenteredTextWithShadow(textRenderer, message, width / 2, height - 52, 0xFFFFD166);
    }

    private Text pageTitle() {
        return Text.literal(switch (page) {
            case MAIN -> "6b6t Map Settings (1/1)";
            case AREA -> "Area Settings";
            case UPLOAD -> "Server and Upload Settings";
        });
    }

    @Override
    public void close() { ScreenBridge.set(parent); }

    private static Text toggleLabel(String name, boolean enabled) {
        return Text.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }
}
