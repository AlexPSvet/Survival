package com.alexpsvet.commands;

import com.alexpsvet.utils.MessageUtil;
import com.alexpsvet.utils.menu.Button;
import com.alexpsvet.utils.menu.Menu;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Guide command - Shows the survival guide menu
 */
public class GuideCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande ne peut être exécutée que par un joueur!");
            return true;
        }
        
        Player player = (Player) sender;
        openGuideMenu(player);
        
        return true;
    }
    
    private void openGuideMenu(Player player) {
        Menu.Builder builder = new Menu.Builder()
            .title(MessageUtil.colorize("&6&lGuide - Serveur Periquito"))
            .rows(6);
        
        // Economy Guide
        ItemStack economy = createGuideItem(
            Material.GOLD_INGOT,
            "&e&lÉconomie",
            "&7Le serveur utilise un système",
            "&7d'économie avec des Coins (⛁)",
            "",
            "&7Commandes principales:",
            "&e/balance &7- Voir votre solde",
            "&e/pay <joueur> <montant> &7- Payer",
            "&e/shop &7- Ouvrir la boutique",
            "&e/ah &7- Auction House",
            "",
            "&7Gagnez des coins en jouant",
            "&7et en vendant des items!"
        );
        builder.button(new Button.Builder().slot(10).item(economy).build());
        
        // Clans Guide
        ItemStack clans = createGuideItem(
            Material.WHITE_BANNER,
            "&6&lClans",
            "&7Créez ou rejoignez un clan",
            "&7pour jouer en équipe!",
            "",
            "&7Commandes principales:",
            "&e/clan &7- Menu du clan",
            "&e/clan create <nom> <tag> &7- Créer",
            "&e/clan invite <joueur> &7- Inviter",
            "&e/clan home &7- Téléportation",
            "&e/clan war challenge <clan> &7- Guerre",
            "",
            "&7Coopérez avec vos alliés",
            "&7et dominez vos ennemis!"
        );
        builder.button(new Button.Builder().slot(12).item(clans).build());
        
        // Protection Guide
        ItemStack protection = createGuideItem(
            Material.SPONGE,
            "&b&lProtection de Territoire",
            "&7Protégez vos constructions",
            "&7avec des blocs de protection!",
            "",
            "&7Commandes:",
            "&e/protection &7- Acheter des blocs",
            "&e/territory info &7- Infos zone",
            "&e/territory trust <joueur> &7- Ajouter",
            "",
            "&7Les blocs protègent une zone",
            "&7autour de leur placement.",
            "&73 tailles disponibles!"
        );
        builder.button(new Button.Builder().slot(14).item(protection).build());
        
        // Teleportation Guide
        ItemStack teleport = createGuideItem(
            Material.ENDER_PEARL,
            "&d&lTéléportation",
            "&7Déplacez-vous rapidement",
            "&7avec les commandes de TP!",
            "",
            "&7Commandes:",
            "&e/tpa <joueur> &7- Demander TP",
            "&e/tpaccept &7- Accepter",
            "&e/tpdeny &7- Refuser",
            "",
            "&7Respectez les demandes",
            "&7et voyagez en toute sécurité!"
        );
        builder.button(new Button.Builder().slot(16).item(teleport).build());
        
        // Chat Guide
        ItemStack chat = createGuideItem(
            Material.WRITABLE_BOOK,
            "&a&lChat & Messages",
            "&7Communiquez avec les autres",
            "&7joueurs!",
            "",
            "&7Commandes:",
            "&e/msg <joueur> <message> &7- MP",
            "&e/r <message> &7- Répondre",
            "",
            "&7Le chat de clan est automatique",
            "&7quand vous êtes dans un clan!"
        );
        builder.button(new Button.Builder().slot(28).item(chat).build());
        
        // Shop Guide
        ItemStack shop = createGuideItem(
            Material.EMERALD,
            "&2&lBoutique",
            "&7Achetez et vendez des items",
            "&7dans la boutique du serveur!",
            "",
            "&7Commandes:",
            "&e/shop &7- Ouvrir la boutique",
            "&e/ah &7- Auction House",
            "&e/ah sell <prix> &7- Vendre",
            "",
            "&7Faites du commerce et",
            "&7devenez riche!"
        );
        builder.button(new Button.Builder().slot(30).item(shop).build());
        
        // Clan Wars Guide
        ItemStack wars = createGuideItem(
            Material.DIAMOND_SWORD,
            "&c&lGuerres de Clans",
            "&7Défiez d'autres clans",
            "&7dans des batailles épiques!",
            "",
            "&7Comment ça marche:",
            "&71. Défiez un clan: &e/clan war challenge",
            "&72. L'autre clan accepte/refuse",
            "&73. 20min de préparation",
            "&74. 15min de bataille",
            "&75. La bordure rétrécit!",
            "",
            "&7Le dernier clan debout gagne!"
        );
        builder.button(new Button.Builder().slot(32).item(wars).build());
        
        // Rules
        ItemStack rules = createGuideItem(
            Material.WRITTEN_BOOK,
            "&4&lRègles du Serveur",
            "&7Respectez ces règles pour",
            "&7une expérience agréable:",
            "",
            "&c1. &7Pas de griefing",
            "&c2. &7Respectez les joueurs",
            "&c3. &7Pas de triche/hack",
            "&c4. &7Pas de spam",
            "&c5. &7Amusez-vous!",
            "",
            "&7Les infractions peuvent",
            "&7entraîner un ban!"
        );
        builder.button(new Button.Builder().slot(34).item(rules).build());
        
        // Premium/VIP Info
        ItemStack premium = createGuideItem(
            Material.NETHER_STAR,
            "&5&lRangs Premium",
            "&7Soutenez le serveur et obtenez",
            "&7des avantages exclusifs!",
            "",
            "&e&lVIP &7- Accès aux items premium",
            "&6&lVIP+ &7- Élytres & Totems",
            "&c&lMVP &7- Balises & plus",
            "",
            "&7Contactez un admin pour",
            "&7plus d'informations!"
        );
        builder.button(new Button.Builder().slot(49).item(premium).build());
        
        // Server Info
        ItemStack info = createGuideItem(
            Material.BEACON,
            "&6&lBienvenue sur Periquito!",
            "&7Serveur Survival Premium",
            "",
            "&7Fonctionnalités:",
            "&a✓ &7Économie avancée",
            "&a✓ &7Système de clans",
            "&a✓ &7Guerres de clans",
            "&a✓ &7Protection de territoire",
            "&a✓ &7Auction House",
            "&a✓ &7Boutique complète",
            "&a✓ &7Et bien plus!",
            "",
            "&eAmusez-vous bien!"
        );
        builder.button(new Button.Builder().slot(4).item(info).build());
        
        builder.build().open(player);
    }
    
    private ItemStack createGuideItem(Material material, String name, String... lore) {
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
}
