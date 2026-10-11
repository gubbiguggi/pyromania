package com.example.weapons;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pyromania - a netherite sword (same damage, enchantable) with two abilities:
 *   F (swap hands)        -> Ember Dash (15s cooldown)
 *   sneak + F             -> Fireball   (30s cooldown)
 * Anywhere in your inventory it gives permanent Fire Resistance.
 */
public class Pyromania extends CustomWeapon {
    // ---- tweak these ----
    private static final long DASH_COOLDOWN_MS = 15_000;
    private static final double DASH_DAMAGE_HP = 4.0;      // 2 hearts
    private static final double DASH_SPEED = 1.5;
    private static final int DASH_TICKS = 9;
    private static final int BURN_SECONDS = 5;

    private static final long FIREBALL_COOLDOWN_MS = 30_000;
    private static final double FIREBALL_DAMAGE_HP = 6.0;  // 3 hearts
    private static final double FIREBALL_RADIUS = 3.0;

    private static final Color ORANGE = Color.fromRGB(255, 106, 0);

    public Pyromania(WeaponsPlugin plugin) { super(plugin); }

    @Override public String id() { return "pyromania"; }

    @Override
    public ItemStack create() {
        ItemStack it = new ItemStack(Material.NETHERITE_SWORD); // vanilla netherite damage, enchantable as normal
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text("Pyromania", TextColor.color(ORANGE.asRGB()), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
                line("F: Ember Dash (15s cooldown)"),
                line("Sneak + F: Fireball (30s cooldown)"),
                line("In your inventory: permanent Fire Resistance")));
        m.getPersistentDataContainer().set(plugin.weaponKey, PersistentDataType.STRING, id());
        it.setItemMeta(m);
        return it;
    }

    private Component line(String s) {
        return Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    // ---------------------------------------------------------------- passive
    @Override
    public void inventoryTick(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false, true));
    }

    // -------------------------------------------------------------- abilities
    @Override
    public void onUse(Player p, boolean sneaking) {
        if (sneaking) fireball(p);
        else emberDash(p);
    }

    private boolean tryStart(Player p, String ability, long ms, String label, BossBar.Color color) {
        if (!plugin.cooldowns().ready(p, ability)) {
            p.sendActionBar(Component.text(label + " is on cooldown", NamedTextColor.RED));
            return false;
        }
        plugin.cooldowns().start(p, ability, ms, label, color);
        return true;
    }

    // ----- Ability 1: Ember Dash -----
    private void emberDash(Player p) {
        if (!tryStart(p, "ember_dash", DASH_COOLDOWN_MS, "Ember Dash", BossBar.Color.RED)) return;

        Vector dir = p.getLocation().getDirection().normalize();
        if (dir.getY() < 0.1) dir.setY(0.1);
        dir.normalize();
        Set<UUID> hit = new HashSet<>();
        World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.8f);
        w.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline() || p.isDead() || t++ >= DASH_TICKS) { cancel(); return; }
                p.setVelocity(dir.clone().multiply(DASH_SPEED));
                p.setFallDistance(0);

                // blazing tail
                Location body = p.getLocation().add(0, 0.9, 0);
                w.spawnParticle(Particle.FLAME, body, 10, 0.3, 0.4, 0.3, 0.03);
                w.spawnParticle(Particle.LAVA, body, 2, 0.3, 0.3, 0.3, 0);
                w.spawnParticle(Particle.SMOKE, body, 3, 0.25, 0.3, 0.25, 0.01);
                Fx.dust(body, ORANGE, 1.6f, 6, 0.35);

                // anything you pass through
                for (LivingEntity e : Combat.targets(p, body, 2.2)) {
                    if (!hit.add(e.getUniqueId())) continue;
                    Combat.damage(e, p, DASH_DAMAGE_HP, true);
                    Combat.burn(plugin, e, p, BURN_SECONDS);
                    w.spawnParticle(Particle.FLAME, e.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ----- Ability 2: Fireball -----
    private void fireball(Player p) {
        if (!tryStart(p, "fireball", FIREBALL_COOLDOWN_MS, "Fireball", BossBar.Color.YELLOW)) return;

        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        Location start = eye.clone().add(dir.clone().multiply(0.8));
        p.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.9f);

        Bolt.launch(plugin, p, start, dir, new ItemStack(Material.FIRE_CHARGE), 0.8f, 1.3, 60, 0.45,
                loc -> {
                    World w = loc.getWorld();
                    w.spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.01);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.05, 0.05, 0.05, 0);
                    Fx.dust(loc, ORANGE, 1.4f, 3, 0.1);
                },
                at -> explode(p, at));
    }

    private void explode(Player owner, Location at) {
        World w = at.getWorld();
        // visual only: no blocks are destroyed
        w.spawnParticle(Particle.EXPLOSION, at, 2, 0.3, 0.3, 0.3, 0);
        w.spawnParticle(Particle.FLAME, at, 60, 0.8, 0.8, 0.8, 0.1);
        w.spawnParticle(Particle.LAVA, at, 15, 0.6, 0.6, 0.6, 0);
        Fx.dust(at, ORANGE, 2f, 40, 1.0);
        w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.1f);

        for (LivingEntity e : Combat.targets(owner, at, FIREBALL_RADIUS)) {
            Combat.damage(e, owner, FIREBALL_DAMAGE_HP, true);
            Combat.burn(plugin, e, owner, BURN_SECONDS);
        }
    }
}
