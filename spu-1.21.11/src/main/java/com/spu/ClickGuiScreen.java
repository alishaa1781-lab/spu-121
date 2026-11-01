package com.spu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Click GUI. Ввод читается из GLFW прямо в render(), поэтому код не зависит от
 * сигнатур обработчиков ввода Screen, которые менялись между версиями Minecraft.
 *
 * ЛКМ по кнопке бинда: назначить (потом нажать клавишу или кнопку мыши 3..8).
 * ESC при назначении: отмена. DELETE или ПКМ: убрать бинд. J: закрыть.
 */
public class ClickGuiScreen extends Screen {
	private static final int W = 300;
	private static final int ROW = 28;
	private static final int HEADER = 30;
	private static final int PAD = 10;
	private static final int FOOTER = 34;

	private static final int ACCENT = 0xFF7C5CFF;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int MUTED = 0xFF8888AA;

	private final SpuModule[] modules = SpuModule.values();
	private final ItemStack[] icons = new ItemStack[modules.length];
	private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];
	private final boolean[] prevMouse = new boolean[8];
	private int listening = -1;

	public ClickGuiScreen() {
		super(Text.literal("SPU"));
		for (int i = 0; i < modules.length; i++) icons[i] = new ItemStack(modules[i].item);
		// Запоминаем уже зажатые клавиши, чтобы J, открывший окно, не закрыл его сразу
		long window = MinecraftClient.getInstance().getWindow().getHandle();
		for (int key : Binds.VALID_KEYS) prevKeys[key] = Binds.isKeyDown(window, key);
		for (int b = 0; b < prevMouse.length; b++) prevMouse[b] = Binds.isMouseDown(window, b);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		// Во время назначения бинда ESC только отменяет назначение
		return listening < 0;
	}

	@Override
	public void removed() {
		SpuConfig.save();
	}

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		long window = MinecraftClient.getInstance().getWindow().getHandle();

		boolean[] keyRise = new boolean[GLFW.GLFW_KEY_LAST + 1];
		for (int key : Binds.VALID_KEYS) {
			boolean down = Binds.isKeyDown(window, key);
			keyRise[key] = down && !prevKeys[key];
			prevKeys[key] = down;
		}
		boolean[] mouseRise = new boolean[prevMouse.length];
		for (int b = 0; b < prevMouse.length; b++) {
			boolean down = Binds.isMouseDown(window, b);
			mouseRise[b] = down && !prevMouse[b];
			prevMouse[b] = down;
		}

		int panelH = HEADER + modules.length * ROW + FOOTER;
		int px = (this.width - W) / 2;
		int py = (this.height - panelH) / 2;

		handleInput(keyRise, mouseRise, mx, my, px, py);
		if (this.client == null || this.client.currentScreen != this) return;

		// Фон и панель
		ctx.fill(0, 0, this.width, this.height, 0x88000000);
		ctx.fill(px - 1, py - 1, px + W + 1, py + panelH + 1, ACCENT);
		ctx.fill(px, py, px + W, py + panelH, 0xFF16161C);
		ctx.fill(px, py, px + W, py + HEADER, 0xFF22222C);
		ctx.drawText(this.textRenderer, "SPU  |  FT helper", px + PAD, py + 11, TEXT, true);
		String ver = "1.21.11";
		ctx.drawText(this.textRenderer, ver, px + W - PAD - this.textRenderer.getWidth(ver), py + 11, MUTED, false);

		for (int i = 0; i < modules.length; i++) {
			SpuModule m = modules[i];
			int ry = py + HEADER + i * ROW;
			boolean hover = my >= ry && my < ry + ROW && mx >= px && mx < px + W;
			ctx.fill(px + 4, ry + 2, px + W - 4, ry + ROW - 2, hover ? 0xFF262633 : 0xFF1D1D26);

			ctx.drawItem(icons[i], px + PAD, ry + 6);
			ctx.drawText(this.textRenderer, m.title, px + PAD + 24, ry + 10, m.enabled ? TEXT : 0xFF777788, true);

			// Переключатель вкл/выкл
			int tx1 = toggleX1(px), ty1 = ry + 8;
			ctx.fill(tx1 - 1, ty1 - 1, tx1 + 19, ty1 + 13, 0xFF000000);
			ctx.fill(tx1, ty1, tx1 + 18, ty1 + 12, m.enabled ? 0xFF3DDC84 : 0xFF55555F);

			// Кнопка бинда
			int bx1 = bindX1(px), by1 = ry + 5;
			boolean isListening = listening == i;
			ctx.fill(bx1 - 1, by1 - 1, bx1 + 101, by1 + ROW - 9, isListening ? 0xFFFFD04D : 0xFF3A3A4A);
			ctx.fill(bx1, by1, bx1 + 100, by1 + ROW - 10, isListening ? 0xFF4A3A10 : 0xFF2C2C3A);
			String label = isListening ? "..." : Binds.name(m.bind);
			int lw = this.textRenderer.getWidth(label);
			ctx.drawText(this.textRenderer, label, bx1 + (100 - lw) / 2, by1 + 5,
				isListening ? 0xFFFFD04D : (m.bind == Binds.NONE ? MUTED : TEXT), true);
		}

		int fy = py + HEADER + modules.length * ROW + 6;
		ctx.drawText(this.textRenderer, "ЛКМ по бинду: назначить  |  ПКМ или DEL: убрать", px + PAD, fy, MUTED, false);
		ctx.drawText(this.textRenderer, "ESC: отмена  |  J: закрыть", px + PAD, fy + 12, MUTED, false);
	}

	private void handleInput(boolean[] keyRise, boolean[] mouseRise, int mx, int my, int px, int py) {
		if (listening >= 0) {
			SpuModule target = modules[listening];
			if (keyRise[GLFW.GLFW_KEY_ESCAPE]) {
				listening = -1;
				return;
			}
			if (keyRise[GLFW.GLFW_KEY_DELETE]) {
				target.bind = Binds.NONE;
				listening = -1;
				SpuConfig.save();
				return;
			}
			for (int key : Binds.VALID_KEYS) {
				if (keyRise[key]) {
					target.bind = key;
					listening = -1;
					SpuConfig.save();
					return;
				}
			}
			// ЛКМ и ПКМ нужны для самого окна, поэтому на бинд берутся кнопки мыши с третьей
			for (int b = 2; b < mouseRise.length; b++) {
				if (mouseRise[b]) {
					target.bind = Binds.MOUSE_OFFSET + b;
					listening = -1;
					SpuConfig.save();
					return;
				}
			}
		} else if (keyRise[GLFW.GLFW_KEY_J]) {
			this.close();
			return;
		}

		boolean lmb = mouseRise[0];
		boolean rmb = mouseRise[1];
		if (!lmb && !rmb) return;

		for (int i = 0; i < modules.length; i++) {
			int ry = py + HEADER + i * ROW;
			SpuModule m = modules[i];

			if (inside(mx, my, bindX1(px), ry + 5, 100, ROW - 10)) {
				if (lmb) {
					listening = (listening == i) ? -1 : i;
				} else {
					m.bind = Binds.NONE;
					if (listening == i) listening = -1;
					SpuConfig.save();
				}
				return;
			}
			if (lmb && inside(mx, my, toggleX1(px), ry + 8, 18, 12)) {
				m.enabled = !m.enabled;
				SpuConfig.save();
				return;
			}
		}
	}

	private static int bindX1(int px) {
		return px + W - 110;
	}

	private static int toggleX1(int px) {
		return px + W - 142;
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
