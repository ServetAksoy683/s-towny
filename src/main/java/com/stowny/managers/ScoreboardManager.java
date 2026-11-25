package com.stowny.managers;

import com.stowny.STowny;
import com.stowny.model.Kingdom;
import com.stowny.model.Royal;
import com.stowny.model.Town;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Towny dunyasinda gorunen sidebar. KURALLAR (titremeyi onlemek icin):
 *  1) Tahta oyuncu basina BIR KEZ kurulur; sonra sadece degisen satirlar yerinde guncellenir.
 *  2) Towny dunyasi disinda oyuncunun tahtasina HIC dokunulmaz (sunucunun baska scoreboard'u kalir).
 *  3) Dunyadan cikinca oyuncunun onceki tahtasi geri verilir.
 *  4) Baska bir plugin tahtayi surekli eziyorsa kavga edilmez; uyari verilip birakilir.
 */
public class ScoreboardManager {

    private static final int LINES = 8;

    private static class View {
        Scoreboard board;
        Objective objective;
        Scoreboard previous;
        Component title;
        final Team[] teams = new Team[LINES];
        final String[] entries = new String[LINES];
        final Component[] last = new Component[LINES];
        final boolean[] shown = new boolean[LINES];
        int reapply;
        long windowStart;
        boolean gaveUp;
    }

    private final STowny plugin;
    private final Set<UUID> enabledPlayers = new HashSet<>();
    private final Map<UUID, View> views = new HashMap<>();

    public ScoreboardManager(STowny plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(UUID uuid) {
        return enabledPlayers.contains(uuid);
    }

    public void enable(UUID uuid) {
        enabledPlayers.add(uuid);
    }

    public void disable(UUID uuid) {
        enabledPlayers.remove(uuid);
        View v = views.get(uuid);
        if (v == null) {
            return;
        }
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            detach(p, v);
        } else {
            views.remove(uuid);
        }
    }

    public void shutdown() {
        for (UUID id : new HashSet<>(views.keySet())) {
            disable(id);
        }
    }

    public void startTask() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            return;
        }
        int intervalSeconds = plugin.getConfig().getInt("scoreboard.update-interval-seconds", 1);
        long ticks = Math.max(10L, intervalSeconds * 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(enabledPlayers)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    update(player);
                }
            }
        }, 20L, ticks);
    }

    private void update(Player p) {
        UUID id = p.getUniqueId();
        View v = views.get(id);

        if (!plugin.worlds().isAllowed(p.getWorld())) {
            if (v != null) {
                detach(p, v);
            }
            return; // Towny dunyasi disinda hicbir sey yapma
        }

        if (v == null) {
            v = create(p);
            views.put(id, v);
            p.setScoreboard(v.board);
        } else if (p.getScoreboard() != v.board) {
            if (v.gaveUp) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - v.windowStart > 10000) {
                v.windowStart = now;
                v.reapply = 0;
            }
            v.reapply++;
            if (v.reapply > 4) {
                v.gaveUp = true;
                plugin.getLogger().warning(p.getName() + " icin baska bir scoreboard plugini tahtayi surekli eziyor, "
                        + "S-Towny tahtasi birakildi. O plugin'de Towny dunyasini scoreboard'dan hariç tut "
                        + "ya da %stowny_...% placeholder'larini onun tahtasinda kullan.");
                return;
            }
            v.previous = p.getScoreboard();
            p.setScoreboard(v.board);
        }
        render(p, v);
    }

    private View create(Player p) {
        View v = new View();
        v.previous = p.getScoreboard();
        v.board = Bukkit.getScoreboardManager().getNewScoreboard();
        v.title = plugin.lang().get("sb-title");
        v.objective = v.board.registerNewObjective("stowny", "dummy", v.title);
        v.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 0; i < LINES; i++) {
            v.entries[i] = ChatColor.values()[i].toString() + ChatColor.RESET;
            v.teams[i] = v.board.registerNewTeam("stw" + i);
            v.teams[i].addEntry(v.entries[i]);
        }
        return v;
    }

    private void render(Player p, View v) {
        Town town = plugin.data().getTownOfPlayer(p.getUniqueId());
        Kingdom kingdom = town != null && town.kingdomName != null ? plugin.data().getKingdom(town.kingdomName) : null;
        Royal royal = kingdom != null && kingdom.royalName != null ? plugin.data().getRoyal(kingdom.royalName) : null;
        Component none = plugin.lang().get("sb-none");

        Component[] lines = new Component[LINES];
        lines[0] = plugin.lang().get("sb-town").append(town != null ? Component.text(town.name) : none);
        lines[1] = plugin.lang().get("sb-kingdom").append(kingdom != null ? Component.text(kingdom.name) : none);
        lines[2] = plugin.lang().get("sb-royal").append(royal != null ? Component.text(royal.name) : none);
        lines[3] = Component.empty();
        lines[4] = plugin.lang().get("sb-balance").append(Component.text(town != null ? plugin.economy().format(town.balance) : "-"));
        lines[5] = plugin.lang().get("sb-claims").append(Component.text(town != null ? String.valueOf(town.claims.size()) : "-"));
        if (town != null) {
            lines[6] = Component.empty();
            lines[7] = plugin.lang().get(town.pvpEnabled ? "sb-pvp-on" : "sb-pvp-off");
        }

        Component title = plugin.lang().get("sb-title");
        if (!title.equals(v.title)) {
            v.objective.displayName(title);
            v.title = title;
        }

        for (int i = 0; i < LINES; i++) {
            if (lines[i] == null) {
                if (v.shown[i]) {
                    v.board.resetScores(v.entries[i]);
                    v.shown[i] = false;
                    v.last[i] = null;
                }
                continue;
            }
            if (!lines[i].equals(v.last[i])) {
                v.teams[i].prefix(lines[i]);
                v.last[i] = lines[i];
            }
            if (!v.shown[i]) {
                v.objective.getScore(v.entries[i]).setScore(LINES - i);
                v.shown[i] = true;
            }
        }
    }

    private void detach(Player p, View v) {
        if (p.getScoreboard() == v.board) {
            p.setScoreboard(v.previous != null ? v.previous : Bukkit.getScoreboardManager().getMainScoreboard());
        }
        views.remove(p.getUniqueId());
    }
}
