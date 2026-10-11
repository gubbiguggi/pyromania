package com.example.weapons;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public final class Fx {
    private Fx() {}

    public static void dust(Location l, Color c, float size, int count, double spread) {
        Color to = c.mixColors(Color.WHITE);
        l.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, l, count, spread, spread, spread, 0,
                new Particle.DustTransition(c, to, size), true);
    }
}
