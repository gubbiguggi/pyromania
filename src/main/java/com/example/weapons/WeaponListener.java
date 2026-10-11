package com.example.weapons;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class WeaponListener implements Listener {
    private final WeaponsPlugin plugin;

    public WeaponListener(WeaponsPlugin plugin) { this.plugin = plugin; }

    /**
     * Pressing the offhand key (default F) while holding a custom weapon uses its ability.
     * The swap itself is cancelled, so the weapon stays in your main hand.
     */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        // getOffHandItem() = the item that is in the main hand right now and would move to the offhand
        CustomWeapon w = plugin.weaponOf(e.getOffHandItem());
        if (w == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        w.onUse(p, p.isSneaking());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.cooldowns().resume(e.getPlayer());
    }
}
