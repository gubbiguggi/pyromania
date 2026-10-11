package com.example.weapons;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Cooldowns shown as a boss bar that starts full and drains. When it is empty the ability is ready. */
public class Cooldowns {
    private static final class Entry {
        final UUID owner;
        final long start, end;
        final String label;
        final BossBar bar;
        Entry(UUID owner, long start, long end, String label, BossBar bar) {
            this.owner = owner; this.start = start; this.end = end; this.label = label; this.bar = bar;
        }
    }

    private final Map<String, Entry> entries = new HashMap<>();

    private static String key(Player p, String ability) { return p.getUniqueId() + ":" + ability; }

    public boolean ready(Player p, String ability) {
        Entry e = entries.get(key(p, ability));
        return e == null || System.currentTimeMillis() >= e.end;
    }

    public void start(Player p, String ability, long millis, String label, BossBar.Color color) {
        Entry old = entries.remove(key(p, ability));
        if (old != null) p.hideBossBar(old.bar);
        long now = System.currentTimeMillis();
        BossBar bar = BossBar.bossBar(Component.text(label), 1f, color, BossBar.Overlay.PROGRESS);
        p.showBossBar(bar);
        entries.put(key(p, ability), new Entry(p.getUniqueId(), now, now + millis, label, bar));
    }

    public void update() {
        long now = System.currentTimeMillis();
        Iterator<Entry> it = entries.values().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            Player p = Bukkit.getPlayer(e.owner);
            if (now >= e.end) {
                if (p != null) p.hideBossBar(e.bar);
                it.remove();
                continue;
            }
            float left = (float) (e.end - now) / (e.end - e.start);
            e.bar.progress(Math.max(0f, Math.min(1f, left)));
            long secs = (e.end - now + 999) / 1000;
            e.bar.name(Component.text(e.label + " - " + secs + "s"));
        }
    }

    public void resume(Player p) {
        for (Entry e : entries.values()) if (e.owner.equals(p.getUniqueId())) p.showBossBar(e.bar);
    }
}
