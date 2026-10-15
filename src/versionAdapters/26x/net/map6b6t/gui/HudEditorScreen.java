package net.map6b6t.gui;

import net.map6b6t.config.ConfigManager;
import net.map6b6t.config.ModConfig;
import net.map6b6t.network.UploadService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private enum Target { STATUS, MAP, VOID }
    private Target target = Target.MAP;
    private boolean dragging, resizing, changed;
    private double lastX, lastY;
    private EditBox rangeInput;

    public HudEditorScreen() { this(null); }

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Change HUD Position"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - 244) / 2;
        int y = height - 28;
        addRenderableWidget(tab("Status", Target.STATUS, left, y));
        addRenderableWidget(tab("Map", Target.MAP, left + 64, y));
        addRenderableWidget(tab("Void", Target.VOID, left + 128, y));
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(left + 192, y, 52, 20).build());
        rangeInput = new EditBox(font, width / 2 - 60, height - 54, 120, 20, Component.literal("Chunk radius"));
        rangeInput.setValue(Integer.toString(ConfigManager.get().voidHudSize));
        rangeInput.setResponder(value -> {
            try { int radius = Integer.parseInt(value); if (radius > 0) { ConfigManager.get().voidHudSize = radius; ConfigManager.save(); } }
            catch (NumberFormatException ignored) {}
        });
        rangeInput.visible = target == Target.VOID;
        addRenderableWidget(rangeInput);
    }

    private net.minecraft.client.gui.components.Button tab(String name, Target value, int x, int y) {
        return net.minecraft.client.gui.components.Button.builder(Component.literal(name), b -> { target = value; rangeInput.visible = value == Target.VOID; })
                .bounds(x, y, 60, 20).build();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x66000000);
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(font, target == Target.VOID ? "Enter the chunk radius below; loaded chunks remain visible in range."
                : "Drag the HUD preview to move it; use the lower-right handle to resize.", width / 2, 8, 0xFFFFFFFF);
        int[] b = bounds();
        Minecraft client = Minecraft.getInstance();
        boolean mapPreview = target == Target.MAP && client.player != null && client.level != null;
        if (mapPreview) {
            HudOverlay.renderMinimapPreview(g, client, b[0], b[1], ConfigManager.get().minimapHudSize);
        } else if (target != Target.VOID) {
            g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x66000000);
            g.outline(b[0], b[1], b[2], b[3], 0xFFFFFFFF);
            String[] preview = switch (target) {
                case STATUS -> new String[]{"6b6t Map: scanning", "Uploaded: " + UploadService.get().stats().uploaded()
                        + "  Queue: " + UploadService.get().stats().queueSize() + "  Now: " + UploadService.get().stats().uploading()};
                case MAP -> new String[]{"Join a world to preview the minimap"};
                case VOID -> new String[0];
            };
            if (target == Target.STATUS) {
                float scale = Math.max(0.6f, Math.min(2f, ConfigManager.get().statusHudScale / 100f));
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(b[0], b[1]);
                pose.scale(scale, scale);
                int y = 3;
                for (String line : preview) { g.text(font, line, 4, y, 0xFFFFFFFF); y += 11; }
                pose.popMatrix();
            } else {
                int y = b[1] + 3;
                for (String line : preview) { g.text(font, line, b[0] + 4, y, 0xFFFFFFFF); y += 11; }
            }
        }
        if (target != Target.VOID) g.fill(b[0] + b[2] - 8, b[1] + b[3] - 8, b[0] + b[2], b[1] + b[3], 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && target != Target.VOID) {
            int[] b = bounds();
            if (event.x() >= b[0] && event.x() <= b[0] + b[2] && event.y() >= b[1] && event.y() <= b[1] + b[3]) {
                resizing = event.x() >= b[0] + b[2] - 12 && event.y() >= b[1] + b[3] - 12;
                dragging = !resizing && target != Target.VOID;
                lastX = event.x(); lastY = event.y();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if ((dragging || resizing) && event.button() == 0) {
            ModConfig c = ConfigManager.get();
            int mx = (int) Math.round(event.x() - lastX), my = (int) Math.round(event.y() - lastY);
            lastX = event.x(); lastY = event.y();
            if (target == Target.STATUS && dragging) {
                c.statusHudX = clamp(c.statusHudX + mx, 0, Math.max(0, width - bounds()[2]));
                c.statusHudY = clamp(c.statusHudY + my, 0, Math.max(0, height - bounds()[3]));
            } else if (target == Target.STATUS && resizing) {
                c.statusHudScale = clamp(c.statusHudScale + mx + my, 60, 200);
            } else if (target == Target.MAP && dragging) {
                int[] b = bounds();
                c.minimapHudX = clamp((c.minimapHudX < 0 ? b[0] : c.minimapHudX) + mx, 0, Math.max(0, width - b[2]));
                c.minimapHudY = clamp(c.minimapHudY + my, 0, Math.max(0, height - b[3]));
            } else if (target == Target.MAP && resizing) {
                int[] b = bounds();
                int availableWidth = c.minimapHudX < 0 ? width - 8 : width - b[0];
                int max = Math.max(128, Math.min(512, Math.min(availableWidth, height - b[1] - 42)));
                c.minimapHudSize = clamp(c.minimapHudSize + mx + my, 128, max);
            }
            changed = true;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            dragging = false;
            resizing = false;
            if (changed) { ConfigManager.save(); changed = false; }
        }
        return super.mouseReleased(event);
    }

    private int[] bounds() {
        ModConfig c = ConfigManager.get();
        return switch (target) {
            case STATUS -> new int[]{c.statusHudX, c.statusHudY, statusWidth(c), 24 * c.statusHudScale / 100};
            case MAP -> {
                int mapSize = Math.max(1, c.minimapHudSize / 65) * 65;
                yield new int[]{c.minimapHudX < 0 ? width - c.minimapHudSize - 8 : c.minimapHudX,
                        c.minimapHudY, c.minimapHudSize, mapSize + 42};
            }
            case VOID -> new int[]{width / 2 - 100, height / 2 - 20, 200, 42};
        };
    }

    private int statusWidth(ModConfig c) {
        float scale = Math.max(0.6f, Math.min(2f, c.statusHudScale / 100f));
        return (int) (Math.max(font.width("6b6t Map: scanning"), font.width("Uploaded: 120  Queue: 2  Now: 4  Failed: 0")) * scale) + 14;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    @Override
    public void onClose() { if (changed) ConfigManager.save(); ScreenBridge.set(parent); }
}
