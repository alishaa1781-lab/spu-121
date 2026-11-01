package com.spu;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Бинд хранится одним числом:
 * -1 — не назначен, 0..348 — клавиша GLFW, 1000+N — кнопка мыши N (0 = ЛКМ).
 * Состояние клавиш читается напрямую из GLFW, поэтому бинды можно менять без KeyBinding.
 */
public final class Binds {
	public static final int NONE = -1;
	public static final int MOUSE_OFFSET = 1000;

	/** Только реальные коды клавиш: GLFW ругается в лог на несуществующие. */
	public static final int[] VALID_KEYS = buildValidKeys();

	private Binds() {}

	private static int[] buildValidKeys() {
		List<Integer> keys = new ArrayList<>();
		keys.add(32);
		keys.add(39);
		for (int k = 44; k <= 57; k++) keys.add(k);
		keys.add(59);
		keys.add(61);
		for (int k = 65; k <= 93; k++) keys.add(k);
		keys.add(96);
		for (int k = 256; k <= 269; k++) keys.add(k);
		for (int k = 280; k <= 284; k++) keys.add(k);
		for (int k = 290; k <= 314; k++) keys.add(k);
		for (int k = 320; k <= 336; k++) keys.add(k);
		for (int k = 340; k <= 348; k++) keys.add(k);
		return keys.stream().mapToInt(Integer::intValue).toArray();
	}

	public static boolean isKeyDown(long window, int key) {
		return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
	}

	public static boolean isMouseDown(long window, int button) {
		return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
	}

	public static boolean isDown(long window, int bind) {
		if (bind == NONE) return false;
		if (bind >= MOUSE_OFFSET) return isMouseDown(window, bind - MOUSE_OFFSET);
		return isKeyDown(window, bind);
	}

	public static String name(int bind) {
		if (bind == NONE) return "NONE";
		if (bind >= MOUSE_OFFSET) return "MOUSE " + (bind - MOUSE_OFFSET + 1);
		if (bind >= 65 && bind <= 90) return String.valueOf((char) bind);
		if (bind >= 48 && bind <= 57) return String.valueOf((char) bind);
		if (bind >= 290 && bind <= 301) return "F" + (bind - 289);
		if (bind >= 320 && bind <= 329) return "NUM " + (bind - 320);
		return switch (bind) {
			case 32 -> "SPACE";
			case 39 -> "'";
			case 44 -> ",";
			case 45 -> "-";
			case 46 -> ".";
			case 47 -> "/";
			case 59 -> ";";
			case 61 -> "=";
			case 91 -> "[";
			case 92 -> "\\";
			case 93 -> "]";
			case 96 -> "`";
			case 256 -> "ESC";
			case 257 -> "ENTER";
			case 258 -> "TAB";
			case 259 -> "BACKSPACE";
			case 260 -> "INSERT";
			case 261 -> "DELETE";
			case 262 -> "RIGHT";
			case 263 -> "LEFT";
			case 264 -> "DOWN";
			case 265 -> "UP";
			case 266 -> "PG UP";
			case 267 -> "PG DOWN";
			case 268 -> "HOME";
			case 269 -> "END";
			case 280 -> "CAPS";
			case 340 -> "L SHIFT";
			case 341 -> "L CTRL";
			case 342 -> "L ALT";
			case 344 -> "R SHIFT";
			case 345 -> "R CTRL";
			case 346 -> "R ALT";
			default -> "KEY " + bind;
		};
	}
}
