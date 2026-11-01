package com.spu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Spu implements ClientModInitializer {
	public static final String MOD_ID = "spu";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final boolean[] WAS_DOWN = new boolean[SpuModule.values().length];
	private static boolean wasGuiKeyDown;

	@Override
	public void onInitializeClient() {
		SpuConfig.load();
		ClientTickEvents.START_CLIENT_TICK.register(ItemUser::applyLock);
		ClientTickEvents.END_CLIENT_TICK.register(Spu::onTick);
		LOGGER.info("spu-1.21.11 загружен: click GUI на J");
	}

	private static void onTick(MinecraftClient client) {
		if (client.getWindow() == null) return;
		long window = client.getWindow().getHandle();
		boolean canAct = client.player != null && client.currentScreen == null;

		// J открывает click GUI
		boolean guiKey = Binds.isKeyDown(window, GLFW.GLFW_KEY_J);
		if (guiKey && !wasGuiKeyDown && canAct) {
			client.setScreen(new ClickGuiScreen());
		}
		wasGuiKeyDown = guiKey;

		// Бинды предметов: срабатывают по нажатию, не по удержанию
		SpuModule[] modules = SpuModule.values();
		for (int i = 0; i < modules.length; i++) {
			SpuModule m = modules[i];
			boolean down = m.enabled && Binds.isDown(window, m.bind);
			if (down && !WAS_DOWN[i] && canAct) {
				ItemUser.start(client, m);
			}
			WAS_DOWN[i] = down;
		}

		ItemUser.tick(client);
	}
}
