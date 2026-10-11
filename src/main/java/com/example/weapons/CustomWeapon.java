package com.example.weapons;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Base class: to add another weapon, extend this and register it in WeaponsPlugin.onEnable(). */
public abstract class CustomWeapon {
    protected final WeaponsPlugin plugin;

    protected CustomWeapon(WeaponsPlugin plugin) { this.plugin = plugin; }

    /** Lowercase id used in /weapon give <id> and stored on the item. */
    public abstract String id();

    /** A fresh copy of the item. It must carry the id in its PersistentDataContainer (see Pyromania). */
    public abstract ItemStack create();

    /** Pressing F (swap hands) with the weapon in the main hand. sneaking = Shift was held. */
    public abstract void onUse(Player p, boolean sneaking);

    /** Called once a second while the weapon is anywhere in the player's inventory. */
    public void inventoryTick(Player p) {}
}
