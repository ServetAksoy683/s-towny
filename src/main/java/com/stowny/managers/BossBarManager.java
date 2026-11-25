package com.stowny.managers;

import com.stowny.STowny;
import com.stowny.model.ChunkPos;
import com.stowny.model.Town;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Towny dunyasinda ekranin ustunde SURENLI bir boss bar gosterir:
 * kasabada "Kasaba (Krallik) - (PVP/Guvenli) - [Ev/Misafir]", dogada "Doga". Hic kaybolmaz,
 * sadece icerigi degisir. Towny dunyasi disinda gizlenir.
 */
public class BossBarManager {

    private final STowny plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, String> keys = new HashMap<>();

    public BossBarManager(STowny plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        long ticks = Math.max(5L, plugin.getConfig().getLong("bossbar.update-ticks", 10L));
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, ticks);
    }

    private void tick() {
        boolean enabled = plugin.getConfig().getBoolean("bossbar.enabled", true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!enabled || !plugin.worlds().isAllowed(p.getWorld())) {
                remove(p.getUniqueId());
                continue;
            }
            update(p);
        }
    }

    private void update(Player p) {
        UUID id = p.getUniqueId();
        ChunkPos pos = new ChunkPos(p.getWorld().getName(), p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
        String owner = plugin.data().getClaimOwner(pos);
        Town town = owner == null ? null : plugin.data().getTown(owner);

        String key;
        Component name;
        BossBar.Color color;
        if (town == null) {
            key = "wild";
            name = plugin.lang().get("bb-wild");
            color = BossBar.Color.WHITE;
        } else {
            boolean home = town.isResident(id);
            key = town.name + "|" + town.kingdomName + "|" + town.pvpEnabled + "|" + home;
            name = plugin.lang().get("bb-town", "%town%", town.name);
            if (town.kingdomName != null) {
                name = name.append(plugin.lang().get("bb-kingdom", "%kingdom%", town.kingdomName));
            }
            name = name.append(plugin.lang().get(town.pvpEnabled ? "bb-pvp-on" : "bb-pvp-off"))
                    .append(plugin.lang().get(home ? "bb-role-home" : "bb-role-guest"));
            color = town.pvpEnabled ? BossBar.Color.RED : (home ? BossBar.Color.GREEN : BossBar.Color.YELLOW);
        }

        BossBar bar = bars.get(id);
        if (bar == null) {
            bar = BossBar.bossBar(name, 1.0f, color, BossBar.Overlay.PROGRESS);
            bars.put(id, bar);
            keys.put(id, key);
            p.showBossBar(bar);
        } else if (!key.equals(keys.get(id))) {
            bar.name(name);
            bar.color(color);
            keys.put(id, key);
        }
    }

    public void remove(UUID id) {
        BossBar bar = bars.remove(id);
        keys.remove(id);
        if (bar != null) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.hideBossBar(bar);
            }
        }
    }

    /** Dil/ayar degisince bir sonraki tick'te yeniden cizilmesini saglar. */
    public void refreshAll() {
        keys.replaceAll((k, v) -> "");
    }

    public void shutdown() {
        for (UUID id : new ArrayList<>(bars.keySet())) {
            remove(id);
        }
    }
}
