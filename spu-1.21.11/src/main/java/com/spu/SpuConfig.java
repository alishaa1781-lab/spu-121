package com.spu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Настройки лежат в config/spu.json. Задержку (delayMs) можно поменять прямо в файле. */
public final class SpuConfig {
	public static int delayMs = 200;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private SpuConfig() {}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("spu.json");
	}

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		try (Reader reader = Files.newBufferedReader(f)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (root.has("delayMs")) delayMs = Math.max(0, root.get("delayMs").getAsInt());
			if (!root.has("modules")) return;
			JsonObject modules = root.getAsJsonObject("modules");
			for (SpuModule m : SpuModule.values()) {
				if (!modules.has(m.name())) continue;
				JsonObject o = modules.getAsJsonObject(m.name());
				if (o.has("bind")) m.bind = o.get("bind").getAsInt();
				if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
			}
		} catch (Exception e) {
			Spu.LOGGER.warn("Не удалось прочитать spu.json, использую значения по умолчанию", e);
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		root.addProperty("delayMs", delayMs);
		JsonObject modules = new JsonObject();
		for (SpuModule m : SpuModule.values()) {
			JsonObject o = new JsonObject();
			o.addProperty("bind", m.bind);
			o.addProperty("enabled", m.enabled);
			modules.add(m.name(), o);
		}
		root.add("modules", modules);
		try (Writer writer = Files.newBufferedWriter(file())) {
			GSON.toJson(root, writer);
		} catch (Exception e) {
			Spu.LOGGER.warn("Не удалось сохранить spu.json", e);
		}
	}
}
