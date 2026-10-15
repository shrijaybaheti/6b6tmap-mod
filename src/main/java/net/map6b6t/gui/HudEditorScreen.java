package net.map6b6t.gui;

import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private enum Target { STATUS, MAP, VOID }
    private Target target = Target.MAP;
    private boolean dragging;
    private boolean resizing;
    private boolean changed;
    private double dragX, dragY;
    private TextFieldWidget rangeInput;

    public HudEditorScreen() { this(null); }

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Change HUD Position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - 268) / 2;
        int controlsY = height - 28;
        addDrawableChild(tab("Status", Target.STATUS, left, controlsY));
        addDrawableChild(tab("Map", Target.MAP, left + 72, controlsY));
        addDrawableChild(tab("Void", Target.VOID, left + 144, controlsY));
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(left + 216, controlsY, 52, 20).build());
        rangeInput = new TextFieldWidget(textRenderer, width / 2 - 60, height - 54, 120, 20, Text.literal("Chunk radius"));
        rangeInput.setText(Integer.toString(ConfigManager.get().voidHudSize));
        rangeInput.setChangedListener(value -> {
            try { int radius = Integer.parseInt(value); if (radius > 0) { ConfigManager.get().voidHudSize = radius; ConfigManager.save(); } }
            catch (NumberFormatException ignored) {}
        });
        rangeInput.setVisible(target == Target.VOID);
        addDrawableChild(rangeInput);
    }

    private ButtonWidget tab(String label, Target value, int x, int y) {
        return ButtonWidget.builder(Text.literal(label), button -> { target = value; rangeInput.setVisible(value == Target.VOID); })
                .dimensions(x, y, 68, 20).build();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x99000000);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                target == Target.VOID ? "Enter the chunk radius below; loaded chunks remain visible in range."
                        : "Drag the HUD preview to move it; use the lower-right handle to resize.",
                width / 2, 8, 0xFFFFFFFF);
        ModConfig c = ConfigManager.get();
        int x, y, w, h;
        switch (target) {
            case STATUS -> { x = c.statusHudX; y = c.statusHudY; w = statusWidth(c); h = 24 * c.statusHudScale / 100; }
            case MAP -> { w = c.minimapHudSize + 8; h = c.minimapHudSize + 38; x = c.minimapHudX < 0 ? width - w - 8 : c.minimapHudX; y = c.minimapHudY; }
            case VOID -> { x = width / 2 - 90; y = height / 2 - 50; w = 180; h = 100; }
            default -> throw new IllegalStateException();
        }
        x = Math.max(0, Math.min(Math.max(0, width - w), x));
        y = Math.max(0, Math.min(Math.max(0, height - h), y));
        boolean mapPreview = target == Target.MAP
                && net.minecraft.client.MinecraftClient.getInstance().player != null
                && net.minecraft.client.MinecraftClient.getInstance().world != null;
        if (mapPreview) {
            MinimapOverlay.renderPreview(context, net.minecraft.client.MinecraftClient.getInstance(), x, y, c.minimapHudSize);
        } else if (target != Target.VOID) {
            context.fill(x, y, x + w, y + h, 0x66000000);
            context.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
            context.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
            context.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
            context.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);
            String preview = switch (target) {
                case STATUS -> "6b6t Map: SCANNING\nUploaded: 120  Queue: 2  Now: 2  Failed: 0";
                case MAP -> "Join a world to preview the minimap";
                case VOID -> "";
            };
            if (target == Target.STATUS) {
                float scale = Math.max(0.6f, Math.min(2f, c.statusHudScale / 100f));
                var matrices = context.getMatrices();
                matrices.push();
                matrices.translate(x, y, 0);
                matrices.scale(scale, scale, 1f);
                int line = 6;
                for (String text : preview.split("\n")) {
                    context.drawTextWithShadow(textRenderer, text, 7, line, 0xFFFFFFFF);
                    line += 12;
                }
                matrices.pop();
            } else {
                int line = y + 6;
                for (String text : preview.split("\n")) {
                    context.drawTextWithShadow(textRenderer, text, x + 7, line, 0xFFFFFFFF);
                    line += 12;
                }
            }
        }
        if (target != Target.VOID) context.fill(x + w - 8, y + h - 8, x + w, y + h, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && target != Target.VOID) {
            int[] box = bounds();
            if (mouseX >= box[0] && mouseX <= box[0] + box[2] && mouseY >= box[1] && mouseY <= box[1] + box[3]) {
                resizing = mouseX >= box[0] + box[2] - 12 && mouseY >= box[1] + box[3] - 12;
                dragging = !resizing && target != Target.VOID;
                dragX = mouseX; dragY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if ((dragging || resizing) && button == 0) {
            ModConfig c = ConfigManager.get();
            int dx = (int) Math.round(mouseX - dragX), dy = (int) Math.round(mouseY - dragY);
            dragX = mouseX; dragY = mouseY;
            if (target == Target.STATUS && dragging) { c.statusHudX = Math.max(0, Math.min(Math.max(0, width - bounds()[2]), c.statusHudX + dx)); c.statusHudY = Math.max(0, Math.min(Math.max(0, height - bounds()[3]), c.statusHudY + dy)); }
            if (target == Target.STATUS && resizing) c.statusHudScale = Math.max(60, Math.min(200, c.statusHudScale + dx + dy));
            if (target == Target.MAP && dragging) {
                if (c.minimapHudX < 0) c.minimapHudX = width - c.minimapHudSize - 16;
                c.minimapHudX = Math.max(0, Math.min(Math.max(0, width - bounds()[2]), c.minimapHudX + dx));
                c.minimapHudY = Math.max(0, Math.min(Math.max(0, height - bounds()[3]), c.minimapHudY + dy));
            }
            if (target == Target.MAP && resizing) {
                int[] box = bounds();
                int availableWidth = c.minimapHudX < 0 ? width - 16 : width - box[0] - 8;
                int max = Math.max(128, Math.min(512, Math.min(availableWidth, height - box[1] - 38)));
                c.minimapHudSize = Math.max(128, Math.min(max, c.minimapHudSize + dx + dy));
            }
            changed = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { dragging = false; resizing = false; if (changed) { ConfigManager.save(); changed = false; } }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int[] bounds() {
        ModConfig c = ConfigManager.get();
        return switch (target) {
            case STATUS -> new int[]{c.statusHudX, c.statusHudY, statusWidth(c), 24 * c.statusHudScale / 100};
            case MAP -> new int[]{c.minimapHudX < 0 ? width - c.minimapHudSize - 16 : c.minimapHudX, c.minimapHudY, c.minimapHudSize + 8, c.minimapHudSize + 38};
            case VOID -> new int[]{width / 2 - 90, height / 2 - 50, 180, 100};
        };
    }

    private int statusWidth(ModConfig c) {
        float scale = Math.max(0.6f, Math.min(2f, c.statusHudScale / 100f));
        return (int) (Math.max(textRenderer.getWidth("6b6t Map: SCANNING"),
                textRenderer.getWidth("Uploaded: 120  Queue: 2  Now: 2  Failed: 0")) * scale) + 14;
    }

    @Override
    public void close() {
        if (changed) ConfigManager.save();
        ScreenBridge.set(parent);
    }
}
