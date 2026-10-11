package com.example.weapons;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Combat {
    private Combat() {}

    private static final Map<UUID, BukkitTask> burning = new HashMap<>();

    public static boolean isTarget(Player owner, Entity e) {
        if (!(e instanceof LivingEntity le) || e.equals(owner) || e instanceof ArmorStand) return false;
        if (e instanceof Player p && (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE)) return false;
        return !le.isDead();
    }

    public static List<LivingEntity> targets(Player owner, Location center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (isTarget(owner, e) && e.getLocation().distanceSquared(center) <= radius * radius) {
                out.add((LivingEntity) e);
            }
        }
        return out;
    }

    /** hp = half-hearts (2 hp = 1 heart). Armor still applies. */
    public static void damage(LivingEntity target, Player source, double hp, boolean knockback) {
        if (target.isDead()) return;
        Vector vel = target.getVelocity();
        target.setNoDamageTicks(0);
        target.damage(hp, source);
        if (!knockback) target.setVelocity(vel);
    }

    /**
     * Burning that ignores Fire Resistance: it is ordinary damage dealt by the player, not fire damage.
     * 1 hp (half a heart) per second, like vanilla fire. Hitting the same target again restarts it.
     */
    public static void burn(Plugin plugin, LivingEntity t, Player src, int seconds) {
        UUID id = t.getUniqueId();
        BukkitTask old = burning.remove(id);
        if (old != null) old.cancel();
        t.setFireTicks(Math.max(t.getFireTicks(), seconds * 20));
        BukkitTask task = new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (!t.isValid() || t.isDead() || i++ >= seconds) {
                    burning.remove(id);
                    cancel();
                    return;
                }
                t.setFireTicks(Math.max(t.getFireTicks(), 40)); // keep the flames visible
                damage(t, src, 1.0, false);
                t.getWorld().spawnParticle(Particle.FLAME, t.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
            }
        }.runTaskTimer(plugin, 20L, 20L);
        burning.put(id, task);
    }
}
