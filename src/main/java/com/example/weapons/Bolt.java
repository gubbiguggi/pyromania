package com.example.weapons;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.function.Consumer;

/** A custom projectile: a floating item (e.g. a fire charge) that is ray-traced every tick. No vanilla damage. */
public final class Bolt {
    private Bolt() {}

    public static void launch(Plugin plugin, Player owner, Location start, Vector dir, ItemStack item,
                              float scale, double speed, double maxDist, double hitbox,
                              Consumer<Location> trail, Consumer<Location> onHit) {
        World w = start.getWorld();
        Vector d = dir.clone().normalize();
        ItemDisplay disp = w.spawn(start, ItemDisplay.class, di -> {
            di.setItemStack(item);
            di.setPersistent(false);
            di.setTeleportDuration(1);
            di.setBillboard(Display.Billboard.CENTER);
            di.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(scale), new AxisAngle4f()));
        });

        new BukkitRunnable() {
            final Location loc = start.clone();
            double travelled = 0;

            @Override public void run() {
                RayTraceResult r = w.rayTrace(loc, d, speed, FluidCollisionMode.NEVER, true, hitbox,
                        e -> Combat.isTarget(owner, e));
                if (r != null) {
                    disp.remove();
                    cancel();
                    onHit.accept(r.getHitPosition().toLocation(w));
                    return;
                }
                loc.add(d.clone().multiply(speed));
                travelled += speed;
                disp.teleport(loc);
                trail.accept(loc);
                if (travelled >= maxDist) { disp.remove(); cancel(); }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
