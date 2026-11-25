package com.stowny.managers;

import com.stowny.STowny;
import com.stowny.model.ChunkPos;
import com.stowny.model.Town;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** /st border: Dynmap'siz, yakindaki kasaba sinirlarini parcacikla gosterir (3 dk sonra kendiliginden kapanir). */
public class BorderViewManager {

    private static final long DURATION_MS = 180_000L;
    private static final int RADIUS = 3;

    private final STowny plugin;
    private final Map<UUID, Long> viewers = new HashMap<>();

    public BorderViewManager(STowny plugin) {
        this.plugin = plugin;
    }

    public boolean toggle(UUID id) {
        if (viewers.remove(id) != null) {
            return false;
        }
        viewers.put(id, System.currentTimeMillis() + DURATION_MS);
        return true;
    }

    public void remove(UUID id) {
        viewers.remove(id);
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    private void tick() {
        if (viewers.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = viewers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || now > e.getValue()) {
                it.remove();
                if (p != null) {
                    p.sendMessage(plugin.lang().get("border-off"));
                }
                continue;
            }
            if (plugin.worlds().isAllowed(p.getWorld())) {
                draw(p);
            }
        }
    }

    private void draw(Player p) {
        Town mine = plugin.data().getTownOfPlayer(p.getUniqueId());
        String w = p.getWorld().getName();
        int cx = p.getLocation().getBlockX() >> 4;
        int cz = p.getLocation().getBlockZ() >> 4;
        double y = p.getLocation().getY();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                String owner = plugin.data().getClaimOwner(new ChunkPos(w, x, z));
                if (owner == null) {
                    continue;
                }
                boolean own = mine != null && mine.name.equalsIgnoreCase(owner);
                Particle particle = own ? Particle.FLAME : Particle.SOUL_FIRE_FLAME;
                int bx = x * 16;
                int bz = z * 16;
                if (!same(owner, w, x, z - 1)) line(p, particle, bx, bz, true, y);
                if (!same(owner, w, x, z + 1)) line(p, particle, bx, bz + 16, true, y);
                if (!same(owner, w, x - 1, z)) line(p, particle, bx, bz, false, y);
                if (!same(owner, w, x + 1, z)) line(p, particle, bx + 16, bz, false, y);
            }
        }
    }

    private boolean same(String owner, String world, int x, int z) {
        return owner.equalsIgnoreCase(plugin.data().getClaimOwner(new ChunkPos(world, x, z)));
    }

    private void line(Player p, Particle particle, int x0, int z0, boolean alongX, double y) {
        for (int i = 0; i <= 16; i += 2) {
            double x = alongX ? x0 + i : x0;
            double z = alongX ? z0 : z0 + i;
            for (double h : new double[]{0.3, 1.3, 2.3}) {
                p.spawnParticle(particle, x, y + h, z, 1, 0, 0, 0, 0);
            }
        }
    }
}
