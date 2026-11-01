package com.spu;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Последовательность по нажатию бинда (между шагами задержка SpuConfig.delayMs, по умолчанию 200 мс):
 * <ol>
 *   <li>Полная остановка: движение блокируется, скорость по горизонтали обнуляется.</li>
 *   <li>Предмет из инвентаря меняется местами со слотом 1, выбирается слот 1.</li>
 *   <li>ПКМ (использование предмета).</li>
 *   <li>Обратный обмен: в слот 1 возвращается то, что там лежало, предмет уходит на своё место,
 *       выбранный слот восстанавливается.</li>
 *   <li>Блокировка снимается, и если клавиши движения всё ещё зажаты, движение продолжается.</li>
 * </ol>
 */
public final class ItemUser {
	private enum Phase { IDLE, STOPPING, SWAPPED, USED }

	private static final KeyBinding[] NO_KEYS = new KeyBinding[0];

	private static Phase phase = Phase.IDLE;
	private static SpuModule current;
	private static int invIndex;
	private static int prevSlot;
	private static long nextAt;
	private static long startedAt;

	private ItemUser() {}

	public static boolean busy() {
		return phase != Phase.IDLE;
	}

	public static void start(MinecraftClient c, SpuModule module) {
		ClientPlayerEntity p = c.player;
		if (p == null || c.interactionManager == null || c.currentScreen != null || busy()) return;

		int idx = find(p, module.item);
		if (idx < 0) {
			p.sendMessage(Text.literal("[SPU] Нет предмета: " + module.title), true);
			return;
		}
		current = module;
		invIndex = idx;
		prevSlot = p.getInventory().getSelectedSlot();
		startedAt = System.currentTimeMillis();
		phase = Phase.STOPPING;
	}

	/** Начало каждого тика: пока идёт последовательность, игрок стоит на месте. */
	public static void applyLock(MinecraftClient c) {
		if (!busy() || c.player == null) return;
		for (KeyBinding k : movementKeys(c)) k.setPressed(false);
		c.player.setSprinting(false);
		Vec3d v = c.player.getVelocity();
		c.player.setVelocity(0.0, v.y, 0.0);
	}

	/** Конец каждого тика: шаги последовательности. */
	public static void tick(MinecraftClient c) {
		if (!busy()) return;
		ClientPlayerEntity p = c.player;
		if (p == null || c.interactionManager == null) {
			phase = Phase.IDLE;
			return;
		}
		long now = System.currentTimeMillis();

		switch (phase) {
			case STOPPING -> {
				Vec3d v = p.getVelocity();
				boolean stopped = Math.hypot(v.x, v.z) < 0.01;
				if (stopped || now - startedAt > 300) {
					if (invIndex != 0) swapWithFirstSlot(c, p);
					p.getInventory().setSelectedSlot(0);
					phase = Phase.SWAPPED;
					nextAt = now + SpuConfig.delayMs;
				}
			}
			case SWAPPED -> {
				if (now >= nextAt) {
					if (p.getMainHandStack().isOf(current.item)) {
						c.interactionManager.interactItem(p, Hand.MAIN_HAND);
						p.swingHand(Hand.MAIN_HAND);
					}
					phase = Phase.USED;
					nextAt = now + SpuConfig.delayMs;
				}
			}
			case USED -> {
				if (now >= nextAt) {
					if (invIndex != 0) swapWithFirstSlot(c, p);
					p.getInventory().setSelectedSlot(prevSlot);
					phase = Phase.IDLE;
					resumeMovement(c);
				}
			}
			default -> {}
		}
	}

	/** Сначала слот 1, потом остальная хотбар-панель, потом основной инвентарь. */
	private static int find(ClientPlayerEntity p, Item item) {
		PlayerInventory inv = p.getInventory();
		if (inv.getStack(0).isOf(item)) return 0;
		for (int i = 1; i < 36; i++) {
			if (inv.getStack(i).isOf(item)) return i;
		}
		return -1;
	}

	/** Обмен слота предмета со слотом 1; тот же вызов второй раз возвращает всё как было. */
	private static void swapWithFirstSlot(MinecraftClient c, ClientPlayerEntity p) {
		// Номера слотов в окне инвентаря игрока: хотбар 36..44, основной инвентарь 9..35.
		int slotId = invIndex < 9 ? 36 + invIndex : invIndex;
		c.interactionManager.clickSlot(p.playerScreenHandler.syncId, slotId, 0, SlotActionType.SWAP, p);
	}

	private static KeyBinding[] movementKeys(MinecraftClient c) {
		GameOptions o = c.options;
		if (o == null) return NO_KEYS;
		return new KeyBinding[] { o.forwardKey, o.backKey, o.leftKey, o.rightKey, o.jumpKey, o.sprintKey };
	}

	/**
	 * Продолжение движения: пока шла блокировка, нажатия клавиш были сброшены, поэтому
	 * состояние каждой клавиши заново берётся из реального положения кнопки на клавиатуре.
	 * Если W (и Shift для бега) всё ещё зажаты, игрок сразу идёт дальше.
	 */
	private static void resumeMovement(MinecraftClient c) {
		long window = c.getWindow().getHandle();
		for (KeyBinding k : movementKeys(c)) {
			int code = KeyBindingHelper.getBoundKeyOf(k).getCode();
			if (code >= 32 && code <= 348) {
				k.setPressed(GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS);
			}
		}
	}
}
