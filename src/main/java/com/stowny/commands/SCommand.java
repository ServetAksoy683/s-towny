package com.stowny.commands;

import com.stowny.STowny;
import com.stowny.model.ChunkPos;
import com.stowny.model.Invite;
import com.stowny.model.Kingdom;
import com.stowny.model.PendingCreation;
import com.stowny.model.Royal;
import com.stowny.model.Town;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class SCommand implements CommandExecutor {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    private final STowny plugin;

    public SCommand(STowny plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = shift(args);

        if (!sub.equals("reload") && sender instanceof Player p && !plugin.worlds().isAllowed(p.getWorld())) {
            sender.sendMessage(plugin.lang().get("wrong-world"));
            return true;
        }

        switch (sub) {
            case "create" -> handleCreate(sender, rest);
            case "confirm" -> handleConfirm(sender, rest);
            case "accept" -> handleAccept(sender);
            case "deny" -> handleDeny(sender);
            case "invite" -> handleInvite(sender, rest);
            case "leave" -> handleLeave(sender);
            case "kick" -> handleKick(sender, rest);
            case "assistant" -> handleAssistant(sender, rest);
            case "claim" -> handleClaim(sender);
            case "unclaim" -> handleUnclaim(sender);
            case "pvp" -> handlePvp(sender);
            case "town" -> handleTownSub(sender, rest);
            case "land" -> handleLand(sender);
            case "deposit" -> handleDeposit(sender, rest);
            case "withdraw" -> handleWithdraw(sender, rest);
            case "tax" -> handleTax(sender, rest);
            case "info" -> handleInfo(sender, rest);
            case "list" -> handleList(sender, rest);
            case "spawn" -> handleSpawn(sender, rest);
            case "setspawn" -> handleSetSpawn(sender);
            case "rename" -> handleRename(sender, rest);
            case "scoreboard" -> handleScoreboard(sender, rest);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(plugin.lang().get("unknown-subcommand"));
        }
        return true;
    }

    // ========================= CREATE / CONFIRM =========================

    private void handleCreate(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        if (args.length < 2) {
            sender.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st create [town|kingdom|royal] [isim]"));
            return;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        String name = args[1];

        if (!NAME_PATTERN.matcher(name).matches()) {
            player.sendMessage(plugin.lang().get("invalid-name"));
            return;
        }
        if (plugin.data().nameTaken(name)) {
            player.sendMessage(plugin.lang().get("name-taken", "%name%", name));
            return;
        }

        switch (type) {
            case "town" -> {
                if (plugin.data().getTownOfPlayer(player.getUniqueId()) != null) {
                    player.sendMessage(plugin.lang().get("already-in-town"));
                    return;
                }
                startPending(player, PendingCreation.Type.TOWN, name, plugin.getConfig().getDouble("economy.town-creation-cost"), "Kasaba");
            }
            case "kingdom" -> {
                Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
                if (town == null || !town.mayor.equals(player.getUniqueId())) {
                    player.sendMessage(plugin.lang().get("not-mayor"));
                    return;
                }
                if (town.kingdomName != null) {
                    player.sendMessage(plugin.lang().get("already-in-kingdom"));
                    return;
                }
                startPending(player, PendingCreation.Type.KINGDOM, name, plugin.getConfig().getDouble("economy.kingdom-creation-cost"), "Krallık");
            }
            case "royal" -> {
                Kingdom kingdom = plugin.data().getKingdomOfKing(player.getUniqueId());
                if (kingdom == null) {
                    player.sendMessage(plugin.lang().get("not-king"));
                    return;
                }
                if (kingdom.royalName != null) {
                    player.sendMessage(plugin.lang().get("already-in-royal"));
                    return;
                }
                startPending(player, PendingCreation.Type.ROYAL, name, plugin.getConfig().getDouble("economy.royal-creation-cost"), "Kraliyet");
            }
            default -> sender.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st create [town|kingdom|royal] [isim]"));
        }
    }

    private void startPending(Player player, PendingCreation.Type type, String name, double cost, String typeLabel) {
        plugin.invites().setPendingCreation(player.getUniqueId(), new PendingCreation(type, name, cost));
        player.sendMessage(plugin.lang().get("create-pending",
                "%type%", typeLabel, "%name%", name, "%cost%", plugin.economy().format(cost)));
    }

    private void handleConfirm(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        PendingCreation pending = plugin.invites().getPendingCreation(player.getUniqueId());
        if (pending == null || args.length < 1 || !pending.name.equalsIgnoreCase(args[0])) {
            player.sendMessage(plugin.lang().get("confirm-nothing-pending"));
            return;
        }

        if (plugin.economy().isAvailable() && !plugin.economy().has(player, pending.cost)) {
            player.sendMessage(plugin.lang().get("confirm-insufficient-funds", "%cost%", plugin.economy().format(pending.cost)));
            return;
        }
        if (plugin.economy().isAvailable()) {
            plugin.economy().withdraw(player, pending.cost);
        }

        String typeLabel = switch (pending.type) {
            case TOWN -> {
                Town town = new Town(pending.name, player.getUniqueId());
                plugin.data().addTown(town);
                yield "Kasaba";
            }
            case KINGDOM -> {
                Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
                Kingdom kingdom = new Kingdom(pending.name, player.getUniqueId(), town.name);
                town.kingdomName = kingdom.name;
                plugin.data().addKingdom(kingdom);
                plugin.data().save();
                yield "Krallık";
            }
            case ROYAL -> {
                Kingdom kingdom = plugin.data().getKingdomOfKing(player.getUniqueId());
                Royal royal = new Royal(pending.name, player.getUniqueId(), kingdom.name);
                kingdom.royalName = royal.name;
                plugin.data().addRoyal(royal);
                plugin.data().save();
                yield "Kraliyet";
            }
        };

        plugin.invites().clearPendingCreation(player.getUniqueId());
        plugin.sounds().play(player, "create");
        player.sendMessage(plugin.lang().get("created", "%type%", typeLabel, "%name%", pending.name));
    }

    // ========================= INVITE / ACCEPT / DENY =========================

    private void handleInvite(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        if (args.length < 1) {
            player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st invite [oyuncu|kasaba|krallık]"));
            return;
        }
        String targetArg = args[0];

        Player targetPlayer = Bukkit.getPlayerExact(targetArg);
        if (targetPlayer != null) {
            Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
            if (town == null || !town.isMayorOrAssistant(player.getUniqueId())) {
                player.sendMessage(plugin.lang().get("not-mayor"));
                return;
            }
            plugin.invites().invitePlayer(targetPlayer.getUniqueId(), new Invite(Invite.Type.TOWN, town.name, player.getUniqueId()));
            player.sendMessage(plugin.lang().get("invite-sent", "%target%", targetPlayer.getName()));
            targetPlayer.sendMessage(plugin.lang().get("invite-received", "%inviter%", player.getName(), "%name%", town.name, "%type%", "Kasaba"));
            plugin.sounds().play(targetPlayer, "invite");
            return;
        }

        Town targetTown = plugin.data().getTown(targetArg);
        if (targetTown != null) {
            Kingdom kingdom = plugin.data().getKingdomOfKing(player.getUniqueId());
            if (kingdom == null) {
                player.sendMessage(plugin.lang().get("not-king"));
                return;
            }
            plugin.invites().inviteTown(targetTown.name, new Invite(Invite.Type.KINGDOM, kingdom.name, player.getUniqueId()));
            player.sendMessage(plugin.lang().get("invite-sent", "%target%", targetTown.name));
            notifyMayor(targetTown, plugin.lang().get("invite-received", "%inviter%", player.getName(), "%name%", kingdom.name, "%type%", "Krallık"));
            return;
        }

        Kingdom targetKingdom = plugin.data().getKingdom(targetArg);
        if (targetKingdom != null) {
            Royal royal = plugin.data().getRoyalOfEmperor(player.getUniqueId());
            if (royal == null) {
                player.sendMessage(plugin.lang().get("not-emperor"));
                return;
            }
            plugin.invites().inviteKingdom(targetKingdom.name, new Invite(Invite.Type.ROYAL, royal.name, player.getUniqueId()));
            player.sendMessage(plugin.lang().get("invite-sent", "%target%", targetKingdom.name));
            Player king = Bukkit.getPlayer(targetKingdom.king);
            if (king != null) {
                king.sendMessage(plugin.lang().get("invite-received", "%inviter%", player.getName(), "%name%", royal.name, "%type%", "Kraliyet"));
            }
            return;
        }

        player.sendMessage(plugin.lang().get("player-not-found", "%player%", targetArg));
    }

    private void handleAccept(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Invite playerInvite = plugin.invites().getPlayerInvite(player.getUniqueId());
        if (playerInvite != null) {
            if (plugin.data().getTownOfPlayer(player.getUniqueId()) != null) {
                player.sendMessage(plugin.lang().get("already-in-town"));
                return;
            }
            Town town = plugin.data().getTown(playerInvite.targetOrgName);
            if (town == null) {
                player.sendMessage(plugin.lang().get("not-found", "%name%", playerInvite.targetOrgName));
                return;
            }
            plugin.data().addResident(town, player.getUniqueId());
            plugin.invites().clearPlayerInvite(player.getUniqueId());
            player.sendMessage(plugin.lang().get("invite-accepted", "%name%", town.name, "%type%", "Kasaba"));
            plugin.sounds().play(player, "success");
            return;
        }

        Town myTown = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (myTown != null && myTown.mayor.equals(player.getUniqueId())) {
            Invite townInvite = plugin.invites().getTownInvite(myTown.name);
            if (townInvite != null) {
                Kingdom kingdom = plugin.data().getKingdom(townInvite.targetOrgName);
                if (kingdom != null) {
                    myTown.kingdomName = kingdom.name;
                    kingdom.memberTowns.add(myTown.name);
                    plugin.data().save();
                    plugin.invites().clearTownInvite(myTown.name);
                    player.sendMessage(plugin.lang().get("invite-accepted", "%name%", kingdom.name, "%type%", "Krallık"));
                    plugin.sounds().play(player, "success");
                    return;
                }
            }
        }

        Kingdom myKingdom = plugin.data().getKingdomOfKing(player.getUniqueId());
        if (myKingdom != null) {
            Invite kingdomInvite = plugin.invites().getKingdomInvite(myKingdom.name);
            if (kingdomInvite != null) {
                Royal royal = plugin.data().getRoyal(kingdomInvite.targetOrgName);
                if (royal != null) {
                    myKingdom.royalName = royal.name;
                    royal.memberKingdoms.add(myKingdom.name);
                    plugin.data().save();
                    plugin.invites().clearKingdomInvite(myKingdom.name);
                    player.sendMessage(plugin.lang().get("invite-accepted", "%name%", royal.name, "%type%", "Kraliyet"));
                    plugin.sounds().play(player, "success");
                    return;
                }
            }
        }

        player.sendMessage(plugin.lang().get("no-pending-invite"));
    }

    private void handleDeny(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        boolean found = false;
        if (plugin.invites().getPlayerInvite(player.getUniqueId()) != null) {
            plugin.invites().clearPlayerInvite(player.getUniqueId());
            found = true;
        }
        Town myTown = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (myTown != null && plugin.invites().getTownInvite(myTown.name) != null) {
            plugin.invites().clearTownInvite(myTown.name);
            found = true;
        }
        Kingdom myKingdom = plugin.data().getKingdomOfKing(player.getUniqueId());
        if (myKingdom != null && plugin.invites().getKingdomInvite(myKingdom.name) != null) {
            plugin.invites().clearKingdomInvite(myKingdom.name);
            found = true;
        }

        player.sendMessage(plugin.lang().get(found ? "invite-denied" : "no-pending-invite"));
    }

    // ========================= LEAVE / KICK / ASSISTANT =========================

    private void handleLeave(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.lang().get("not-in-town"));
            return;
        }
        if (town.mayor.equals(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("cannot-kick-leader"));
            return;
        }
        plugin.data().removeResident(town, player.getUniqueId());
        player.sendMessage(plugin.lang().get("left", "%type%", "Kasaba"));
    }

    private void handleKick(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.isMayorOrAssistant(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st kick [oyuncu]"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!town.isResident(target.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-resident-of-target", "%name%", town.name));
            return;
        }
        if (target.getUniqueId().equals(town.mayor)) {
            player.sendMessage(plugin.lang().get("cannot-kick-leader"));
            return;
        }

        plugin.data().removeResident(town, target.getUniqueId());
        player.sendMessage(plugin.lang().get("kicked", "%player%", args[0]));
    }

    private void handleAssistant(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.mayor.equals(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st assistant [add|remove] [oyuncu]"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!town.isResident(target.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-resident-of-target", "%name%", town.name));
            return;
        }

        if (args[0].equalsIgnoreCase("add")) {
            town.assistants.add(target.getUniqueId());
            plugin.data().save();
            player.sendMessage(plugin.lang().get("assistant-added", "%player%", args[1]));
        } else {
            town.assistants.remove(target.getUniqueId());
            plugin.data().save();
            player.sendMessage(plugin.lang().get("assistant-removed", "%player%", args[1]));
        }
    }

    // ========================= CLAIM / UNCLAIM =========================

    private void handleClaim(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.isMayorOrAssistant(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }

        int maxClaims = plugin.getConfig().getInt("economy.max-claims-per-town", 0);
        if (maxClaims > 0 && town.claims.size() >= maxClaims) {
            player.sendMessage(plugin.lang().get("claim-max-reached"));
            return;
        }

        ChunkPos pos = chunkOf(player.getLocation());
        String owner = plugin.data().getClaimOwner(pos);
        if (owner != null) {
            if (owner.equalsIgnoreCase(town.name)) {
                player.sendMessage(plugin.lang().get("claim-already-yours"));
            } else {
                player.sendMessage(plugin.lang().get("claim-already-other", "%town%", owner));
            }
            return;
        }

        double baseCost = plugin.getConfig().getDouble("economy.claim-base-cost", 100);
        double multiplier = plugin.getConfig().getDouble("economy.claim-cost-multiplier", 1.4);
        double cost = baseCost * Math.pow(multiplier, town.claims.size());

        if (plugin.economy().isAvailable() && !plugin.economy().has(player, cost)) {
            player.sendMessage(plugin.lang().get("claim-insufficient-funds", "%cost%", plugin.economy().format(cost)));
            return;
        }
        if (plugin.economy().isAvailable()) {
            plugin.economy().withdraw(player, cost);
        }

        plugin.data().addClaim(town, pos);
        plugin.dynmap().refreshTown(town);
        plugin.sounds().play(player, "claim");
        spawnClaimBorderEffect(player, pos);
        player.sendMessage(plugin.lang().get("claim-success",
                "%cost%", plugin.economy().format(cost), "%total%", String.valueOf(town.claims.size())));
    }

    /** Yeni claim edilen chunk'in sinirlarinda kisa bir parcacik efekti gosterir. */
    private void spawnClaimBorderEffect(Player player, ChunkPos pos) {
        org.bukkit.World world = player.getWorld();
        int baseX = pos.x() * 16;
        int baseZ = pos.z() * 16;
        int y = player.getLocation().getBlockY();

        for (int i = 0; i <= 16; i++) {
            spawnBorderParticle(world, baseX + i, y, baseZ);
            spawnBorderParticle(world, baseX + i, y, baseZ + 16);
            spawnBorderParticle(world, baseX, y, baseZ + i);
            spawnBorderParticle(world, baseX + 16, y, baseZ + i);
        }
    }

    private void spawnBorderParticle(org.bukkit.World world, int x, int y, int z) {
        world.spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, x + 0.5, y + 1, z + 0.5, 3, 0.1, 0.3, 0.1, 0);
    }

    private void handleUnclaim(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.isMayorOrAssistant(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }

        ChunkPos pos = chunkOf(player.getLocation());
        String owner = plugin.data().getClaimOwner(pos);
        if (owner == null || !owner.equalsIgnoreCase(town.name)) {
            player.sendMessage(plugin.lang().get("unclaim-not-claimed-by-you"));
            return;
        }

        plugin.data().removeClaim(town, pos);
        plugin.dynmap().refreshTown(town);
        player.sendMessage(plugin.lang().get("unclaim-success"));
    }

    // ========================= PVP / TOWN ALT-KOMUTLARI / LAND =========================

    private void handlePvp(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.mayor.equals(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }

        town.pvpEnabled = !town.pvpEnabled;
        plugin.data().save();
        player.sendMessage(plugin.lang().get(town.pvpEnabled ? "pvp-enabled" : "pvp-disabled"));
        plugin.sounds().play(player, "success");
    }

    private void handleTownSub(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        if (args.length < 1) {
            player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st town [breakblock|placeblock|land] [oyuncu]"));
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        if (action.equals("land")) {
            handleLand(sender);
            return;
        }

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.mayor.equals(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st town [breakblock|placeblock] [oyuncu]"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!town.isResident(target.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-resident-of-target", "%name%", town.name));
            return;
        }

        switch (action) {
            case "breakblock" -> {
                boolean nowAllowed = toggle(town.canBreak, target.getUniqueId());
                plugin.data().save();
                player.sendMessage(plugin.lang().get(nowAllowed ? "permission-granted" : "permission-revoked",
                        "%player%", args[1], "%permission%", "breakblock"));
            }
            case "placeblock" -> {
                boolean nowAllowed = toggle(town.canPlace, target.getUniqueId());
                plugin.data().save();
                player.sendMessage(plugin.lang().get(nowAllowed ? "permission-granted" : "permission-revoked",
                        "%player%", args[1], "%permission%", "placeblock"));
            }
            default -> player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st town [breakblock|placeblock|land] [oyuncu]"));
        }
    }

    private boolean toggle(Set<UUID> set, UUID uuid) {
        if (set.contains(uuid)) {
            set.remove(uuid);
            return false;
        }
        set.add(uuid);
        return true;
    }

    private void handleLand(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.lang().get("not-in-town"));
            return;
        }

        player.sendActionBar(plugin.lang().get("land-size", "%count%", String.valueOf(town.claims.size())));
    }

    // ========================= BANK (DEPOSIT / WITHDRAW / TAX) =========================

    private void handleDeposit(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.lang().get("not-in-town"));
            return;
        }
        Double amount = parseAmount(player, args);
        if (amount == null) return;

        if (!plugin.economy().isAvailable() || !plugin.economy().has(player, amount)) {
            player.sendMessage(plugin.lang().get("insufficient-personal-funds"));
            return;
        }

        plugin.economy().withdraw(player, amount);
        town.balance += amount;
        plugin.data().save();
        plugin.sounds().play(player, "bank");
        player.sendMessage(plugin.lang().get("deposit-success",
                "%amount%", plugin.economy().format(amount), "%balance%", plugin.economy().format(town.balance)));
    }

    private void handleWithdraw(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null) {
            player.sendMessage(plugin.lang().get("not-in-town"));
            return;
        }
        if (!town.isMayorOrAssistant(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-bank-permitted"));
            return;
        }
        Double amount = parseAmount(player, args);
        if (amount == null) return;

        if (town.balance < amount) {
            player.sendMessage(plugin.lang().get("insufficient-bank-funds"));
            return;
        }

        town.balance -= amount;
        plugin.data().save();
        if (plugin.economy().isAvailable()) {
            plugin.economy().deposit(player, amount);
        }
        plugin.sounds().play(player, "bank");
        player.sendMessage(plugin.lang().get("withdraw-success",
                "%amount%", plugin.economy().format(amount), "%balance%", plugin.economy().format(town.balance)));
    }

    private void handleTax(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.mayor.equals(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }
        Double amount = parseAmount(player, args);
        if (amount == null) return;

        town.dailyTax = amount;
        plugin.data().save();
        player.sendMessage(plugin.lang().get("tax-set", "%amount%", plugin.economy().format(amount)));
    }

    private Double parseAmount(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st [komut] [miktar]"));
            return null;
        }
        try {
            double value = Double.parseDouble(args[0]);
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.lang().get("invalid-usage", "%usage%", "/st [komut] [miktar]"));
            return null;
        }
    }

    // ========================= INFO / LIST =========================

    private void handleInfo(CommandSender sender, String[] args) {
        String name;
        if (args.length >= 1) {
            name = args[0];
        } else {
            Player player = asPlayer(sender);
            if (player == null) return;
            Town own = plugin.data().getTownOfPlayer(player.getUniqueId());
            if (own == null) {
                sender.sendMessage(plugin.lang().get("not-in-town"));
                return;
            }
            name = own.name;
        }

        Town town = plugin.data().getTown(name);
        if (town != null) {
            sender.sendMessage(plugin.lang().get("info-title", "%name%", town.name, "%type%", "Kasaba"));
            sender.sendMessage(plugin.lang().get("info-leader", "%leader%", nameOf(town.mayor)));
            sender.sendMessage(plugin.lang().get("info-residents", "%count%", String.valueOf(town.residents.size())));
            sender.sendMessage(plugin.lang().get("info-balance", "%balance%", plugin.economy().format(town.balance)));
            sender.sendMessage(plugin.lang().get("info-claims", "%count%", String.valueOf(town.claims.size())));
            sender.sendMessage(plugin.lang().get("info-tax", "%amount%", plugin.economy().format(town.dailyTax)));
            if (town.kingdomName != null) {
                sender.sendMessage(plugin.lang().get("info-parent", "%parent%", town.kingdomName));
            }
            return;
        }

        Kingdom kingdom = plugin.data().getKingdom(name);
        if (kingdom != null) {
            sender.sendMessage(plugin.lang().get("info-title", "%name%", kingdom.name, "%type%", "Krallık"));
            sender.sendMessage(plugin.lang().get("info-leader", "%leader%", nameOf(kingdom.king)));
            sender.sendMessage(plugin.lang().get("info-residents", "%count%", String.valueOf(kingdom.memberTowns.size())));
            sender.sendMessage(plugin.lang().get("info-balance", "%balance%", plugin.economy().format(kingdom.balance)));
            if (kingdom.royalName != null) {
                sender.sendMessage(plugin.lang().get("info-parent", "%parent%", kingdom.royalName));
            }
            return;
        }

        Royal royal = plugin.data().getRoyal(name);
        if (royal != null) {
            sender.sendMessage(plugin.lang().get("info-title", "%name%", royal.name, "%type%", "Kraliyet"));
            sender.sendMessage(plugin.lang().get("info-leader", "%leader%", nameOf(royal.emperor)));
            sender.sendMessage(plugin.lang().get("info-residents", "%count%", String.valueOf(royal.memberKingdoms.size())));
            sender.sendMessage(plugin.lang().get("info-balance", "%balance%", plugin.economy().format(royal.balance)));
            return;
        }

        sender.sendMessage(plugin.lang().get("not-found", "%name%", name));
    }

    private void handleList(CommandSender sender, String[] args) {
        String type = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "town";

        Map<String, ?> source = switch (type) {
            case "kingdom" -> plugin.data().allKingdoms();
            case "royal" -> plugin.data().allRoyals();
            default -> plugin.data().allTowns();
        };

        sender.sendMessage(plugin.lang().get("list-title", "%type%", type));
        if (source.isEmpty()) {
            sender.sendMessage(plugin.lang().get("list-empty"));
            return;
        }
        StringBuilder names = new StringBuilder();
        for (Object value : source.values()) {
            String name = "?";
            if (value instanceof Town t) {
                name = t.name;
            } else if (value instanceof Kingdom k) {
                name = k.name;
            } else if (value instanceof Royal r) {
                name = r.name;
            }
            if (!names.isEmpty()) names.append(", ");
            names.append(name);
        }
        sender.sendMessage(Component.text(names.toString(), NamedTextColor.GRAY));
    }

    // ========================= SPAWN / SETSPAWN / RENAME =========================

    private void handleSpawn(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town;
        if (args.length >= 1) {
            town = plugin.data().getTown(args[0]);
            if (town == null) {
                player.sendMessage(plugin.lang().get("not-found", "%name%", args[0]));
                return;
            }
        } else {
            town = plugin.data().getTownOfPlayer(player.getUniqueId());
            if (town == null) {
                player.sendMessage(plugin.lang().get("spawn-no-town"));
                return;
            }
        }

        if (town.spawn == null) {
            player.sendMessage(plugin.lang().get("not-found", "%name%", town.name));
            return;
        }

        player.teleportAsync(town.spawn);
        player.sendMessage(plugin.lang().get("spawn-success", "%name%", town.name));
    }

    private void handleSetSpawn(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.isMayorOrAssistant(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }

        town.spawn = player.getLocation();
        plugin.data().save();
        player.sendMessage(plugin.lang().get("spawn-set"));
    }

    private void handleRename(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        Town town = plugin.data().getTownOfPlayer(player.getUniqueId());
        if (town == null || !town.mayor.equals(player.getUniqueId())) {
            player.sendMessage(plugin.lang().get("not-mayor"));
            return;
        }
        if (args.length < 1 || !NAME_PATTERN.matcher(args[0]).matches()) {
            player.sendMessage(plugin.lang().get("invalid-name"));
            return;
        }
        if (plugin.data().nameTaken(args[0])) {
            player.sendMessage(plugin.lang().get("name-taken", "%name%", args[0]));
            return;
        }

        String old = town.name;
        plugin.data().removeTown(town);
        town.name = args[0];
        plugin.data().addTown(town);

        if (town.kingdomName != null) {
            Kingdom kingdom = plugin.data().getKingdom(town.kingdomName);
            if (kingdom != null) {
                kingdom.memberTowns.remove(old);
                kingdom.memberTowns.add(town.name);
                plugin.data().save();
            }
        }

        plugin.dynmap().removeTown(old);
        plugin.dynmap().refreshTown(town);

        player.sendMessage(plugin.lang().get("renamed", "%old%", old, "%new%", town.name));
    }

    // ========================= SCOREBOARD / RELOAD =========================

    private void handleScoreboard(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) return;

        boolean enable = args.length < 1 || !args[0].equalsIgnoreCase("off");
        if (enable) {
            plugin.scoreboard().enable(player.getUniqueId());
            player.sendMessage(plugin.lang().get("scoreboard-enabled"));
        } else {
            plugin.scoreboard().disable(player.getUniqueId());
            player.sendMessage(plugin.lang().get("scoreboard-disabled"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (sender instanceof Player p && !p.isOp() && !p.hasPermission("stowny.admin")) {
            sender.sendMessage(plugin.lang().get("no-permission"));
            return;
        }
        plugin.reloadConfig();
        plugin.lang().reload();
        sender.sendMessage(plugin.lang().get("reload-success"));
    }

    // ========================= YARDIMCI METODLAR =========================

    private Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(plugin.lang().get("only-players"));
        return null;
    }

    private void notifyMayor(Town town, Component message) {
        Player mayor = Bukkit.getPlayer(town.mayor);
        if (mayor != null) {
            mayor.sendMessage(message);
            plugin.sounds().play(mayor, "invite");
        }
    }

    private String nameOf(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() != null ? p.getName() : uuid.toString();
    }

    private ChunkPos chunkOf(Location loc) {
        return new ChunkPos(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private String[] shift(String[] args) {
        String[] result = new String[args.length - 1];
        System.arraycopy(args, 1, result, 0, result.length);
        return result;
    }

    private void sendHelp(CommandSender sender) {
        String[] lines = {
                "<gold><bold>== S-Towny ==",
                "<white>/st create [town|kingdom|royal] [isim] <gray>- kuruluş başlat",
                "<white>/st confirm [isim] <gray>- kuruluşu onayla",
                "<white>/st invite [oyuncu|kasaba|krallık] <gray>- davet gönder",
                "<white>/st accept <gray>| <white>/st deny",
                "<white>/st leave <gray>| <white>/st kick [oyuncu]",
                "<white>/st assistant [add|remove] [oyuncu]",
                "<white>/st claim <gray>| <white>/st unclaim <gray>| <white>/st land",
                "<white>/st pvp <gray>- PVP aç/kapat",
                "<white>/st town [breakblock|placeblock] [oyuncu] <gray>- izin ver/al",
                "<white>/st deposit [miktar] <gray>| <white>/st withdraw [miktar]",
                "<white>/st tax [miktar]",
                "<white>/st info [isim] <gray>| <white>/st list [town|kingdom|royal]",
                "<white>/st spawn [isim] <gray>| <white>/st setspawn",
                "<white>/st rename [yeni-isim]",
                "<white>/st scoreboard [on|off]"
        };
        MiniMessage mm = MiniMessage.miniMessage();
        for (String line : lines) {
            sender.sendMessage(mm.deserialize(line));
        }
    }
}
