package com.spu;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/** Шесть предметов FT helper. Каждый можно включить/выключить и повесить на свой бинд. */
public enum SpuModule {
	FREEZE_SNOWBALL("Снежок заморозки", Items.SNOWBALL),
	TRAP("Трапка", Items.NETHERITE_SCRAP),
	DISORIENTATION("Дезориентация", Items.ENDER_EYE),
	PLAST("Пласт", Items.DRIED_KELP),
	DUST("Явная пыль", Items.SUGAR),
	FIREBALL("Огненный шар", Items.FIRE_CHARGE);

	public final String title;
	public final Item item;
	public int bind = Binds.NONE;
	public boolean enabled = true;

	SpuModule(String title, Item item) {
		this.title = title;
		this.item = item;
	}
}
