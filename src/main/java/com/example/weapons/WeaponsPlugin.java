package com.example.weapons;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WeaponsPlugin extends JavaPlugin implements TabExecutor {
    public NamespacedKey weaponKey;
    private final Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
    private final Cooldowns cooldowns = new Cooldowns();

    public Cooldowns cooldowns() { return cooldowns; }

    @Override
    public void onEnable() {
        weaponKey = new NamespacedKey(this, "weapon_id");

        register(new Pyromania(this));
        // register(new YourNextWeapon(this));   <- add more weapons here

        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
        getCommand("weapon").setExecutor(this);
        getCommand("weapon").setTabCompleter(this);

        getServer().getScheduler().runTaskTimer(this, cooldowns::update, 2L, 2L);
        // passives for weapons that are anywhere in a player's inventory
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Set<String> seen = new HashSet<>();
                for (ItemStack it : p.getInventory().getContents()) {
                    String id = idOf(it);
                    if (id == null || !seen.add(id)) continue;
                    CustomWeapon w = weapons.get(id);
                    if (w != null) w.inventoryTick(p);
                }
            }
        }, 20L, 20L);
    }

    private void register(CustomWeapon w) { weapons.put(w.id(), w); }

    private String idOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
    }

    public CustomWeapon weaponOf(ItemStack it) {
        String id = idOf(it);
        return id == null ? null : weapons.get(id);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        String usage = "Usage: /weapon give <id> [player]  |  /weapon list";
        if (a.length == 0) { s.sendMessage(usage); return true; }
        switch (a[0].toLowerCase()) {
            case "list" -> s.sendMessage("Weapons: " + String.join(", ", weapons.keySet()));
            case "give" -> {
                if (a.length < 2) { s.sendMessage(usage); return true; }
                CustomWeapon w = weapons.get(a[1].toLowerCase());
                Player t = a.length > 2 ? Bukkit.getPlayerExact(a[2]) : (s instanceof Player p ? p : null);
                if (w == null) { s.sendMessage("Unknown weapon. /weapon list"); return true; }
                if (t == null) { s.sendMessage("Player not found."); return true; }
                t.getInventory().addItem(w.create()).values()
                        .forEach(left -> t.getWorld().dropItemNaturally(t.getLocation(), left));
                s.sendMessage("Gave " + w.id() + " to " + t.getName() + ".");
            }
            default -> s.sendMessage(usage);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) out.addAll(List.of("give", "list"));
        else if (a.length == 2 && a[0].equalsIgnoreCase("give")) out.addAll(weapons.keySet());
        else if (a.length == 3 && a[0].equalsIgnoreCase("give")) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}
