package com.alexpsvet.commands;

import com.alexpsvet.Survival;
import com.alexpsvet.clan.Clan;
import com.alexpsvet.clan.ClanManager;
import com.alexpsvet.clan.ClanRank;
import com.alexpsvet.clan.menu.ClanInfoMenu;
import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Clan commands
 */
public class ClanCommand implements CommandExecutor {
    private final ClanManager clanManager;
    private final EconomyManager economyManager;
    
    public ClanCommand() {
        this.clanManager = Survival.getInstance().getClanManager();
        this.economyManager = Survival.getInstance().getEconomyManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendError(sender, "Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Open clan menu
            ClanInfoMenu.open(player);
            return true;
        }
        
        String subCmd = args[0].toLowerCase();
        
        switch (subCmd) {
            case "create":
            case "creer":
                return handleCreate(player, args);
            case "disband":
            case "dissoudre":
                return handleDisband(player);
            case "invite":
            case "inviter":
                return handleInvite(player, args);
            case "join":
            case "rejoindre":
                return handleJoin(player, args);
            case "leave":
            case "quitter":
                return handleLeave(player);
            case "kick":
            case "expulser":
                return handleKick(player, args);
            case "promote":
            case "promouvoir":
                return handlePromote(player, args);
            case "demote":
            case "retrograder":
                return handleDemote(player, args);
            case "sethome":
                return handleSetHome(player);
            case "home":
                return handleHome(player);
            case "list":
            case "liste":
                ClanInfoMenu.openClanList(player);
                return true;
            case "info":
                ClanInfoMenu.open(player);
                return true;
            case "war":
            case "guerre":
                return handleWar(player, args);
            case "description":
            case "desc":
                return handleDescription(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }
    
    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(player, "Usage: /clan create <nom> <tag>");
            return true;
        }
        
        if (clanManager.isInClan(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous êtes déjà dans un clan!");
            return true;
        }
        
        String name = args[1];
        String tag = args[2];
        
        int minLength = Survival.getInstance().getConfig().getInt("clans.name-min-length", 3);
        int maxLength = Survival.getInstance().getConfig().getInt("clans.name-max-length", 16);
        
        if (name.length() < minLength || name.length() > maxLength) {
            MessageUtil.sendError(player, "Le nom doit faire entre " + minLength + " et " + maxLength + " caractères!");
            return true;
        }
        
        if (tag.length() > 6) {
            MessageUtil.sendError(player, "Le tag ne peut pas dépasser 6 caractères!");
            return true;
        }
        
        double cost = Survival.getInstance().getConfig().getDouble("clans.creation-cost", 5000.0);
        if (!economyManager.removeBalance(player.getUniqueId(), cost)) {
            String symbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
            MessageUtil.sendError(player, "Vous avez besoin de " + cost + " " + symbol + " pour créer un clan!");
            return true;
        }
        
        if (clanManager.createClan(name, tag, player.getUniqueId())) {
            economyManager.addTransaction(player.getUniqueId(), TransactionType.CLAN_CREATION, -cost, "Clan creation: " + name);
            MessageUtil.sendMessage(player, 
                MessageUtil.format("&aClan &e{clan} &acréé avec succès!",
                    "{clan}", name));
        } else {
            economyManager.addBalance(player.getUniqueId(), cost);
            MessageUtil.sendError(player, "Un clan avec ce nom existe déjà!");
        }
        
        return true;
    }
    
    private boolean handleDisband(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        if (!clan.getLeader().equals(player.getUniqueId())) {
            MessageUtil.sendError(player, "Seul le chef peut dissoudre le clan!");
            return true;
        }
        
        clanManager.disbandClan(clan.getName());
        MessageUtil.sendSuccess(player, "Clan dissous!");
        
        return true;
    }
    
    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /clan invite <joueur>");
            return true;
        }
        
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        ClanRank rank = clan.getRank(player.getUniqueId());
        if (rank.getLevel() < ClanRank.MODERATOR.getLevel()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission (rang insuffisant)!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendError(player, "Joueur introuvable!");
            return true;
        }
        
        if (clanManager.isInClan(target.getUniqueId())) {
            MessageUtil.sendError(player, "Ce joueur est déjà dans un clan!");
            return true;
        }
        
        clanManager.invitePlayer(target.getUniqueId(), clan.getName());
        MessageUtil.sendMessage(player, 
            MessageUtil.format("&e{player} &aa été invité dans le clan!",
                "{player}", target.getName()));
        MessageUtil.sendMessage(target, 
            MessageUtil.format("&aVous avez été invité dans le clan &e{clan}&a! Utilisez /clan join {clan}",
                "{clan}", clan.getName()));
        
        return true;
    }
    
    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /clan join <nom>");
            return true;
        }
        
        if (clanManager.isInClan(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous êtes déjà dans un clan!");
            return true;
        }
        
        String clanName = args[1];
        Clan clan = clanManager.getClan(clanName);
        
        if (clan == null) {
            MessageUtil.sendError(player, "Ce clan n'existe pas!");
            return true;
        }
        
        if (!clanManager.hasInvitation(player.getUniqueId())) {
            MessageUtil.sendError(player, "Vous n'avez pas d'invitation pour ce clan!");
            return true;
        }
        
        if (clanManager.addMember(clanName, player.getUniqueId())) {
            clanManager.removeInvitation(player.getUniqueId());
            MessageUtil.sendMessage(player, 
                MessageUtil.format("&aVous avez rejoint le clan &e{clan}&a!",
                    "{clan}", clanName));
        } else {
            MessageUtil.sendError(player, "Le clan est plein!");
        }
        
        return true;
    }
    
    private boolean handleLeave(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        if (clan.getLeader().equals(player.getUniqueId())) {
            MessageUtil.sendError(player, "Le chef doit dissoudre le clan ou transférer le leadership!");
            return true;
        }
        
        clanManager.removeMember(clan.getName(), player.getUniqueId());
        MessageUtil.sendSuccess(player, "Vous avez quitté le clan!");
        
        return true;
    }
    
    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /clan kick <joueur>");
            return true;
        }
        
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendError(player, "Joueur introuvable!");
            return true;
        }
        
        ClanRank playerRank = clan.getRank(player.getUniqueId());
        ClanRank targetRank = clan.getRank(target.getUniqueId());
        
        if (!playerRank.canManageRank(targetRank)) {
            MessageUtil.sendError(player, "Vous ne pouvez pas expulser ce joueur (rang insuffisant)!");
            return true;
        }
        
        clanManager.removeMember(clan.getName(), target.getUniqueId());
        MessageUtil.sendMessage(player, 
            MessageUtil.format("&e{player} &ca été expulsé du clan!",
                "{player}", target.getName()));
        MessageUtil.sendError(target, "Vous avez été expulsé du clan!");
        
        return true;
    }
    
    private boolean handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /clan promote <joueur>");
            return true;
        }
        
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendError(player, "Joueur introuvable!");
            return true;
        }
        
        ClanRank playerRank = clan.getRank(player.getUniqueId());
        ClanRank targetRank = clan.getRank(target.getUniqueId());
        
        if (!playerRank.canManageRank(targetRank)) {
            MessageUtil.sendError(player, "Vous ne pouvez pas promouvoir ce joueur!");
            return true;
        }
        
        ClanRank newRank = null;
        switch (targetRank) {
            case MEMBER: newRank = ClanRank.MODERATOR; break;
            case MODERATOR: newRank = ClanRank.ADMIN; break;
            case ADMIN: 
                MessageUtil.sendError(player, "Ce joueur a déjà le rang maximum!");
                return true;
            case LEADER:
                MessageUtil.sendError(player, "Impossible de promouvoir le chef!");
                return true;
        }
        
        clanManager.setRank(clan.getName(), target.getUniqueId(), newRank);
        MessageUtil.sendMessage(player, 
            MessageUtil.format("&e{player} &aa été promu!",
                "{player}", target.getName()));
        MessageUtil.sendSuccess(target, "Vous avez été promu à " + newRank.getDisplayName() + "!");
        
        return true;
    }
    
    private boolean handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /clan demote <joueur>");
            return true;
        }
        
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendError(player, "Joueur introuvable!");
            return true;
        }
        
        ClanRank playerRank = clan.getRank(player.getUniqueId());
        ClanRank targetRank = clan.getRank(target.getUniqueId());
        
        if (!playerRank.canManageRank(targetRank)) {
            MessageUtil.sendError(player, "Vous ne pouvez pas rétrograder ce joueur!");
            return true;
        }
        
        ClanRank newRank = null;
        switch (targetRank) {
            case ADMIN: newRank = ClanRank.MODERATOR; break;
            case MODERATOR: newRank = ClanRank.MEMBER; break;
            case MEMBER: 
                MessageUtil.sendError(player, "Ce joueur a déjà le rang minimum!");
                return true;
            case LEADER:
                MessageUtil.sendError(player, "Impossible de rétrograder le chef!");
                return true;
        }
        
        clanManager.setRank(clan.getName(), target.getUniqueId(), newRank);
        MessageUtil.sendMessage(player, 
            MessageUtil.format("&e{player} &ca été rétrogradé!",
                "{player}", target.getName()));
        MessageUtil.sendWarning(target, "Vous avez été rétrogradé à " + newRank.getDisplayName());
        
        return true;
    }
    
    private boolean handleSetHome(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        ClanRank rank = clan.getRank(player.getUniqueId());
        if (rank.getLevel() < ClanRank.ADMIN.getLevel()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission (rang insuffisant)!");
            return true;
        }
        
        double cost = Survival.getInstance().getConfig().getDouble("clans.clan-home.set-cost", 1000.0);
        if (!economyManager.removeBalance(player.getUniqueId(), cost)) {
            String symbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");
            MessageUtil.sendError(player, "Vous avez besoin de " + cost + " " + symbol + " pour définir le home!");
            return true;
        }
        
        clanManager.setHome(clan.getName(), player.getLocation());
        economyManager.addTransaction(player.getUniqueId(), TransactionType.CLAN_HOME, -cost, "Clan home set");
        MessageUtil.sendSuccess(player, "Point de téléportation du clan défini!");
        
        return true;
    }
    
    private boolean handleHome(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        if (clan.getHome() == null) {
            MessageUtil.sendError(player, "Le clan n'a pas de home défini!");
            return true;
        }
        
        int cooldown = Survival.getInstance().getConfig().getInt("clans.clan-home.teleport-cooldown", 10);
        MessageUtil.sendMessage(player, 
            MessageUtil.format("&aTéléportation au home du clan dans &e{seconds}&a secondes...",
                "{seconds}", String.valueOf(cooldown)));
        
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            player.teleport(clan.getHome());
            MessageUtil.sendSuccess(player, "Téléporté au home du clan!");
        }, cooldown * 20L);
        
        return true;
    }
    
    private boolean handleWar(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /clan war <challenge|accept|deny> [clan]");
            return true;
        }
        
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        ClanRank rank = clan.getRank(player.getUniqueId());
        if (rank.getLevel() < ClanRank.ADMIN.getLevel()) {
            MessageUtil.sendError(player, "Vous devez être Admin ou Leader pour gérer les guerres!");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        if (action.equals("challenge") || action.equals("defier")) {
            if (args.length < 3) {
                MessageUtil.sendError(player, "Usage: /clan war challenge <clan>");
                return true;
            }
            
            String targetClan = args[2];
            Clan target = clanManager.getClan(targetClan);
            
            if (target == null) {
                MessageUtil.sendError(player, "Clan introuvable!");
                return true;
            }
            
            if (target.getName().equals(clan.getName())) {
                MessageUtil.sendError(player, "Vous ne pouvez pas défier votre propre clan!");
                return true;
            }
            
            if (Survival.getInstance().getClanWarManager().sendWarInvitation(clan.getName(), targetClan, player.getUniqueId())) {
                MessageUtil.sendSuccess(player, "Défi de guerre envoyé à " + targetClan + "!");
            } else {
                MessageUtil.sendError(player, "Ce clan a déjà une invitation en attente!");
            }
            
        } else if (action.equals("accept") || action.equals("accepter")) {
            if (!Survival.getInstance().getClanWarManager().hasWarInvitation(clan.getName())) {
                MessageUtil.sendError(player, "Vous n'avez aucune invitation de guerre!");
                return true;
            }
            
            Survival.getInstance().getClanWarManager().acceptWarInvitation(clan.getName());
            MessageUtil.sendSuccess(player, "Guerre acceptée! Préparez-vous au combat!");
            
        } else if (action.equals("deny") || action.equals("refuser")) {
            if (!Survival.getInstance().getClanWarManager().hasWarInvitation(clan.getName())) {
                MessageUtil.sendError(player, "Vous n'avez aucune invitation de guerre!");
                return true;
            }
            
            Survival.getInstance().getClanWarManager().denyWarInvitation(clan.getName());
            MessageUtil.sendSuccess(player, "Invitation de guerre refusée.");
        } else {
            MessageUtil.sendError(player, "Action invalide! Utilisez: challenge, accept, ou deny");
        }
        
        return true;
    }
    
    private boolean handleDescription(Player player, String[] args) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return true;
        }
        
        ClanRank rank = clan.getRank(player.getUniqueId());
        if (rank.getLevel() < ClanRank.ADMIN.getLevel()) {
            MessageUtil.sendError(player, "Vous n'avez pas la permission (rang insuffisant)!");
            return true;
        }
        
        if (args.length < 2) {
            MessageUtil.sendError(player, "Usage: /clan description <texte>");
            return true;
        }
        
        StringBuilder description = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            description.append(args[i]).append(" ");
        }
        
        clan.setDescription(description.toString().trim());
        clanManager.updateClanSettings(clan.getName());
        MessageUtil.sendSuccess(player, "Description du clan mise à jour!");
        
        return true;
    }
    
    private void sendHelp(Player player) {
        MessageUtil.sendMessages(player, java.util.Arrays.asList(
            "&6&l===== Commandes Clan =====",
            "&e/clan &7- Ouvrir le menu du clan",
            "&e/clan create <nom> <tag> &7- Créer un clan",
            "&e/clan invite <joueur> &7- Inviter un joueur",
            "&e/clan join <nom> &7- Rejoindre un clan",
            "&e/clan leave &7- Quitter le clan",
            "&e/clan kick <joueur> &7- Expulser un joueur",
            "&e/clan promote <joueur> &7- Promouvoir un joueur",
            "&e/clan demote <joueur> &7- Rétrograder un joueur",
            "&e/clan sethome &7- Définir le home du clan",
            "&e/clan home &7- Se téléporter au home",
            "&e/clan list &7- Voir tous les clans",
            "&e/clan description <texte> &7- Changer la description",
            "&e/clan war challenge <clan> &7- Défier un clan en guerre",
            "&e/clan war accept &7- Accepter une guerre",
            "&e/clan war deny &7- Refuser une guerre",
            "&e/clan disband &7- Dissoudre le clan (chef uniquement)"
        ));
    }
}
