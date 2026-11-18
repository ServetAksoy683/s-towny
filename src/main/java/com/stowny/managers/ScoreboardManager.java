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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Sadece S-Towny'nin aktif oldugu dunyada gorunen sidebar scoreboard. */
public class ScoreboardManager {

    private final STowny plugin;
    private final Set<UUID> enabledPlayers = new HashSet<>();

    public ScoreboardManager(STowny plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(UUID uuid) { return enabledPlayers.contains(uuid); }

    public void enable(UUID uuid) { enabledPlayers.add(uuid); }

    public void disable(UUID uuid) {
        enabledPlayers.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void startTask() {
        int intervalSeconds = plugin.getConfig().getInt("scoreboard.update-interval-seconds", 1);
        long ticks = Math.max(20L, intervalSeconds * 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(enabledPlayers)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) update(player);
            }
        }, 0L, ticks);
    }

    private void update(Player player) {
        if (!plugin.worlds().isAllowed(player.getWorld())) {
            if (player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("stowny", "dummy", plugin.lang().get("sb-title"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        Kingdom kingdom = town != null && town.kingdomName != null ? plugin.data().getKingdom(town.kingdomName) : null;
        Royal royal = kingdom != null && kingdom.royalName != null ? plugin.data().getRoyal(kingdom.royalName) : null;
        Component none = plugin.lang().get("sb-none");

        int line = 12;
        line = addLine(board, objective, line, plugin.lang().get("sb-town").append(town != null ? Component.text(town.name) : none));
        line = addLine(board, objective, line, plugin.lang().get("sb-kingdom").append(kingdom != null ? Component.text(kingdom.name) : none));
        line = addLine(board, objective, line, plugin.lang().get("sb-royal").append(royal != null ? Component.text(royal.name) : none));
        line = addLine(board, objective, line, Component.empty());
        line = addLine(board, objective, line, plugin.lang().get("sb-balance").append(Component.text(town != null ? plugin.economy().format(town.balance) : "-")));
        line = addLine(board, objective, line, plugin.lang().get("sb-claims").append(Component.text(town != null ? String.valueOf(town.claims.size()) : "-")));
        if (town != null) {
            line = addLine(board, objective, line, Component.empty());
            addLine(board, objective, line, plugin.lang().get(town.pvpEnabled ? "sb-pvp-on" : "sb-pvp-off"));
        }

        player.setScoreboard(board);
    }

    private int addLine(Scoreboard board, Objective objective, int score, Component text) {
        String entry = ChatColor.values()[score % ChatColor.values().length].toString() + ChatColor.RESET;
        String teamName = "stw_line_" + score;
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(entry);
        }
        team.prefix(text);
        objective.getScore(entry).setScore(score);
        return score - 1;
    }
}
