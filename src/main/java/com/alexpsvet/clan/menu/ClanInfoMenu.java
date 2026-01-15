package com.alexpsvet.clan.menu;

import com.alexpsvet.Survival;
import com.alexpsvet.clan.Clan;
import com.alexpsvet.clan.ClanManager;
import com.alexpsvet.clan.ClanRank;
import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Menu for clan information - Using Menu system
 */
public class ClanInfoMenu {
    
    /**
     * Open the clan info menu for a player
     */
    public static void open(Player player) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.sendError(player, "Vous n'êtes pas dans un clan!");
            return;
        }
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lClan: &e" + clan.getName()))
            .rows(6);
        
        // Clan info
        ItemStack info = createItem(Material.PAPER, "&6&lInformations du Clan",
            "&7Nom: &e" + clan.getName(),
            "&7Tag: &e[" + clan.getTag() + "]",
            "&7Chef: &e" + Bukkit.getOfflinePlayer(clan.getLeader()).getName(),
            "&7Membres: &e" + clan.getMemberCount() + "/" + Survival.getInstance().getConfig().getInt("clans.max-members"),
            "&7Créé le: &e" + new SimpleDateFormat("dd/MM/yyyy").format(new Date(clan.getCreatedAt())),
            "",
            "&7Description:",
            "&f" + ((clan.getDescription() == null || clan.getDescription().isEmpty()) ? "Aucune description" : clan.getDescription())
        );
        builder.button(new Button.Builder().slot(4).item(info).build());
        
        // Members list
        ItemStack members = createItem(Material.PLAYER_HEAD, "&6&lMembres",
            "&7Cliquez pour voir la liste",
            "&7des membres du clan"
        );
        builder.button(new Button.Builder().slot(10).item(members)
            .onClick((p, clickType) -> openMembersList(p))
            .build());
        
        // Allies
        ItemStack allies = createItem(Material.EMERALD, "&a&lAlliés",
            "&7Alliés: &e" + clan.getAllies().size() + "/" + Survival.getInstance().getConfig().getInt("clans.max-allies"),
            "",
            "&7Cliquez pour gérer les alliances"
        );
        builder.button(new Button.Builder().slot(12).item(allies)
            .onClick((p, clickType) -> openAlliesMenu(p))
            .build());
        
        // Enemies
        ItemStack enemies = createItem(Material.REDSTONE, "&c&lEnnemis",
            "&7Ennemis: &e" + clan.getEnemies().size(),
            "",
            "&7Cliquez pour gérer les ennemis"
        );
        builder.button(new Button.Builder().slot(14).item(enemies).build());
        
        // Home
        ItemStack home = createItem(
            clan.getHome() != null ? Material.ENDER_PEARL : Material.ENDER_EYE,
            "&d&lHome du Clan",
            clan.getHome() != null 
                ? Arrays.asList("&7Home défini!", "", "&aCliquez pour vous téléporter")
                : Arrays.asList("&7Aucun home défini", "", "&eCliquez pour définir le home")
        );
        builder.button(new Button.Builder().slot(16).item(home).build());
        
        // Bank (future feature)
        ItemStack bank = createItem(Material.GOLD_INGOT, "&e&lBanque du Clan",
            "&7Fonctionnalité à venir..."
        );
        builder.button(new Button.Builder().slot(28).item(bank).build());
        
        // Settings (for admins+)
        ClanRank playerRank = clan.getRank(player.getUniqueId());
        if (playerRank.getLevel() >= ClanRank.ADMIN.getLevel()) {
            ItemStack settings = createItem(Material.COMPARATOR, "&6&lParamètres",
                "&7Gérer les paramètres du clan",
                "",
                "&7Friendly Fire: " + (clan.isFriendlyFire() ? "&aActivé" : "&cDésactivé"),
                "",
                "&eCliquez pour changer"
            );
            builder.button(new Button.Builder().slot(30).item(settings)
                .onClick((p, clickType) -> openSettingsMenu(p))
                .build());
        }
        
        // Invite player (for moderators+)
        if (playerRank.getLevel() >= ClanRank.MODERATOR.getLevel()) {
            ItemStack invite = createItem(Material.WRITABLE_BOOK, "&a&lInviter un Joueur",
                "&7Cliquez pour inviter un joueur",
                "&7dans le clan",
                "",
                "&eUtilisez: /clan invite <joueur>"
            );
            builder.button(new Button.Builder().slot(32).item(invite)
                .onClick((p, clickType) -> {
                    p.closeInventory();
                    p.sendMessage(MessageUtil.colorize("&eUtilisez &6/clan invite <joueur> &epour inviter un joueur!"));
                })
                .build());
        }
        
        // Leave clan
        if (!clan.getLeader().equals(player.getUniqueId())) {
            ItemStack leave = createItem(Material.BARRIER, "&c&lQuitter le Clan",
                "&7Cliquez pour quitter le clan"
            );
            builder.button(new Button.Builder().slot(49).item(leave)
                .onClick((p, clickType) -> {
                    p.closeInventory();
                    p.performCommand("clan leave");
                })
                .build());
        } else {
            ItemStack disband = createItem(Material.TNT, "&4&lDissoudre le Clan",
                "&cATTENTION: Cette action est irréversible!",
                "",
                "&7Cliquez pour dissoudre le clan"
            );
            builder.button(new Button.Builder().slot(49).item(disband)
                .onClick((p, clickType) -> {
                    p.closeInventory();
                    p.performCommand("clan disband");
                })
                .build());
        }
        
        builder.build().open(player);
    }
    
    /**
     * Open members list menu
     */
    public static void openMembersList(Player player) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lMembres - " + clan.getName()))
            .rows(6);
        
        int slot = 0;
        for (Map.Entry<UUID, ClanRank> entry : clan.getMembers().entrySet()) {
            if (slot >= 45) break;
            
            UUID memberUuid = entry.getKey();
            ClanRank rank = entry.getValue();
            String memberName = Bukkit.getOfflinePlayer(memberUuid).getName();
            boolean isOnline = Bukkit.getPlayer(memberUuid) != null;
            
            ItemStack memberItem = createItem(
                Material.PLAYER_HEAD,
                (isOnline ? "&a" : "&7") + memberName,
                "&7Rang: " + getRankColor(rank) + rank.getDisplayName(),
                "&7Statut: " + (isOnline ? "&aEn ligne" : "&7Hors ligne"),
                "",
                "&eCliquez pour gérer ce membre"
            );
            
            builder.button(new Button.Builder().slot(slot++).item(memberItem).build());
        }
        
        // Back button
        ItemStack back = createItem(Material.ARROW, "&cRetour",
            "&7Retour au menu principal"
        );
        builder.button(new Button.Builder().slot(49).item(back)
            .onClick((p, clickType) -> open(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open allies menu
     */
    public static void openAlliesMenu(Player player) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&a&lAlliés - " + clan.getName()))
            .rows(6);
        
        int slot = 0;
        for (String allyName : clan.getAllies()) {
            if (slot >= 45) break;
            
            Clan ally = ClanManager.getInstance().getClan(allyName);
            if (ally == null) continue;
            
            ItemStack allyItem = createItem(
                Material.EMERALD,
                "&a" + allyName,
                "&7Tag: &e[" + ally.getTag() + "]",
                "&7Membres: &e" + ally.getMemberCount(),
                "&7Chef: &e" + Bukkit.getOfflinePlayer(ally.getLeader()).getName(),
                "",
                "&cCliquez pour rompre l'alliance"
            );
            
            builder.button(new Button.Builder().slot(slot++).item(allyItem).build());
        }
        
        // Add ally button (if has permission)
        ClanRank playerRank = clan.getRank(player.getUniqueId());
        if (playerRank.getLevel() >= ClanRank.ADMIN.getLevel()) {
            ItemStack addAlly = createItem(Material.LIME_DYE, "&a&lAjouter un Allié",
                "&7Proposer une alliance à un clan"
            );
            builder.button(new Button.Builder().slot(48).item(addAlly).build());
        }
        
        // Back button
        ItemStack back = createItem(Material.ARROW, "&cRetour",
            "&7Retour au menu principal"
        );
        builder.button(new Button.Builder().slot(49).item(back)
            .onClick((p, clickType) -> open(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Open clan list menu
     */
    public static void openClanList(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lListe des Clans"))
            .rows(6);
        
        List<Clan> clanList = new ArrayList<>(ClanManager.getInstance().getAllClans());
        clanList.sort((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()));
        
        int slot = 0;
        for (Clan clan : clanList) {
            if (slot >= 45) break;
            
            ItemStack clanItem = createItem(
                Material.WHITE_BANNER,
                "&6&l" + clan.getName(),
                "&7Tag: &e[" + clan.getTag() + "]",
                "&7Chef: &e" + Bukkit.getOfflinePlayer(clan.getLeader()).getName(),
                "&7Membres: &e" + clan.getMemberCount(),
                "&7Alliés: &e" + clan.getAllies().size(),
                "",
                "&7" + ((clan.getDescription() == null || clan.getDescription().isEmpty()) ? "Aucune description" : clan.getDescription())
            );
            
            builder.button(new Button.Builder().slot(slot++).item(clanItem).build());
        }
        
        // Create clan button
        if (!ClanManager.getInstance().isInClan(player.getUniqueId())) {
            ItemStack create = createItem(Material.NETHER_STAR, "&a&lCréer un Clan",
                "&7Créer votre propre clan",
                "",
                "&7Coût: &e" + Survival.getInstance().getConfig().getDouble("clans.creation-cost") + " " +
                Survival.getInstance().getConfig().getString("economy.currency-symbol")
            );
            builder.button(new Button.Builder().slot(49).item(create)
                .onClick((p, clickType) -> {
                    p.closeInventory();
                    p.sendMessage(MessageUtil.colorize("&7Utilisez &e/clan create <nom> <tag> &7pour créer un clan!"));
                })
                .build());
        }
        
        builder.build().open(player);
    }
    
    /**
     * Create an item with name and lore
     */
    private static ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, Arrays.asList(lore));
    }
    
    /**
     * Create an item with name and lore
     */
    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(MessageUtil.colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Open settings menu
     */
    public static void openSettingsMenu(Player player) {
        Clan clan = ClanManager.getInstance().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        ClanRank playerRank = clan.getRank(player.getUniqueId());
        if (playerRank.getLevel() < ClanRank.ADMIN.getLevel()) {
            player.sendMessage(MessageUtil.colorize("&cVous n'avez pas la permission!"));
            return;
        }
        
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lParamètres - " + clan.getName()))
            .rows(3);
        
        // Friendly Fire toggle
        ItemStack friendlyFire = createItem(
            clan.isFriendlyFire() ? Material.LIME_DYE : Material.RED_DYE,
            "&6&lFriendly Fire",
            "&7Permet aux membres du clan",
            "&7de se blesser mutuellement",
            "",
            "&7Statut: " + (clan.isFriendlyFire() ? "&aActivé" : "&cDésactivé"),
            "",
            "&eCliquez pour " + (clan.isFriendlyFire() ? "désactiver" : "activer")
        );
        builder.button(new Button.Builder().slot(11).item(friendlyFire)
            .onClick((p, clickType) -> {
                clan.setFriendlyFire(!clan.isFriendlyFire());
                ClanManager.getInstance().updateClanSettings(clan.getName());
                p.sendMessage(MessageUtil.colorize("&aFriendly Fire " + (clan.isFriendlyFire() ? "&aactivé" : "&cdésactivé") + "!"));
                openSettingsMenu(p);
            })
            .build());
        
        // Description editor
        ItemStack description = createItem(Material.WRITABLE_BOOK, "&6&lDescription du Clan",
            "&7Description actuelle:",
            "&f" + (clan.getDescription() == null || clan.getDescription().isEmpty() ? "Aucune" : clan.getDescription()),
            "",
            "&eUtilisez: /clan description <texte>"
        );
        builder.button(new Button.Builder().slot(13).item(description)
            .onClick((p, clickType) -> {
                p.closeInventory();
                p.sendMessage(MessageUtil.colorize("&eUtilisez &6/clan description <texte> &epour changer la description!"));
            })
            .build());
        
        // Set home
        ItemStack setHome = createItem(Material.ENDER_PEARL, "&6&lDéfinir le Home",
            "&7Définir le point de téléportation",
            "&7du clan à votre position",
            "",
            "&eCliquez pour définir"
        );
        builder.button(new Button.Builder().slot(15).item(setHome)
            .onClick((p, clickType) -> {
                p.closeInventory();
                p.performCommand("clan sethome");
            })
            .build());
        
        // Back button
        ItemStack back = createItem(Material.ARROW, "&cRetour",
            "&7Retour au menu principal"
        );
        builder.button(new Button.Builder().slot(22).item(back)
            .onClick((p, clickType) -> open(p))
            .build());
        
        builder.build().open(player);
    }
    
    /**
     * Get color code for a rank
     */
    private static String getRankColor(ClanRank rank) {
        switch (rank) {
            case LEADER: return "&c";
            case ADMIN: return "&6";
            case MODERATOR: return "&e";
            case MEMBER: return "&7";
            default: return "&f";
        }
    }
}
