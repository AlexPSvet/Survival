package com.alexpsvet.commands;

import com.alexpsvet.economy.EconomyManager;
import com.alexpsvet.economy.TransactionType;
import com.alexpsvet.Survival;
import com.alexpsvet.commands.base.BaseCommand;
import com.alexpsvet.commands.base.SubCommand;
import com.alexpsvet.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin economy command using sub-commands: give, take, set
 */
public class EconomyAdminCommand extends BaseCommand {

    private final EconomyManager economyManager;
    private final String currencySymbol;

    public EconomyAdminCommand() {
        this.economyManager = Survival.getInstance().getEconomyManager();
        this.currencySymbol = Survival.getInstance().getConfig().getString("economy.currency-symbol", "⛁");

        // register sub-commands
        registerSubCommand(new GiveSub());
        registerSubCommand(new TakeSub());
        registerSubCommand(new SetSub());
    }

    @Override
    public String getName() {
        return "ecoadmin";
    }

    @Override
    protected void executeDefault(CommandSender sender, String[] args) {
        sendHelp(sender);
    }

    // give <player> <amount>
    private class GiveSub extends SubCommand {
        @Override
        public String getName() { return "give"; }

        @Override
        public String getDescription() { return "Ajouter de l'argent à un joueur"; }

        @Override
        public String getSyntax() { return "/ecoadmin give <joueur> <montant>"; }

        @Override
        public String getPermission() { return "survival.economy.admin"; }

        @Override
        public boolean canConsoleExecute() { return true; }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                MessageUtil.sendError(sender, "Usage: " + getSyntax());
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendError(sender, "Joueur introuvable!");
                return;
            }

            double amount;
            try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
                MessageUtil.sendError(sender, "Montant invalide!");
                return;
            }

            economyManager.addBalance(target.getUniqueId(), amount);
            economyManager.addTransaction(target.getUniqueId(), TransactionType.ADMIN_ADD, amount, "Admin give by " + sender.getName());
            MessageUtil.sendSuccess(sender, "Ajouté " + amount + " " + currencySymbol + " à " + target.getName());
            MessageUtil.sendSuccess(target, "Vous avez reçu " + amount + " " + currencySymbol);
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String[] args) {
            if (args.length == 1) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return names;
            }
            return new ArrayList<>();
        }
    }

    // take <player> <amount>
    private class TakeSub extends SubCommand {
        @Override
        public String getName() { return "take"; }

        @Override
        public String getDescription() { return "Retirer de l'argent à un joueur"; }

        @Override
        public String getSyntax() { return "/ecoadmin take <joueur> <montant>"; }

        @Override
        public String getPermission() { return "survival.economy.admin"; }

        @Override
        public boolean canConsoleExecute() { return true; }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                MessageUtil.sendError(sender, "Usage: " + getSyntax());
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendError(sender, "Joueur introuvable!");
                return;
            }

            double amount;
            try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
                MessageUtil.sendError(sender, "Montant invalide!");
                return;
            }

            if (economyManager.removeBalance(target.getUniqueId(), amount)) {
                economyManager.addTransaction(target.getUniqueId(), TransactionType.ADMIN_REMOVE, -amount, "Admin take by " + sender.getName());
                MessageUtil.sendSuccess(sender, "Retiré " + amount + " " + currencySymbol + " de " + target.getName());
                MessageUtil.sendWarning(target, amount + " " + currencySymbol + " ont été retirés de votre compte");
            } else {
                MessageUtil.sendError(sender, "Le joueur n'a pas assez d'argent!");
            }

        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String[] args) {
            if (args.length == 1) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return names;
            }
            return new ArrayList<>();
        }
    }

    // set <player> <amount>
    private class SetSub extends SubCommand {
        @Override
        public String getName() { return "set"; }

        @Override
        public String getDescription() { return "Définir le solde d'un joueur"; }

        @Override
        public String getSyntax() { return "/ecoadmin set <joueur> <montant>"; }

        @Override
        public String getPermission() { return "survival.economy.admin"; }

        @Override
        public boolean canConsoleExecute() { return true; }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                MessageUtil.sendError(sender, "Usage: " + getSyntax());
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendError(sender, "Joueur introuvable!");
                return;
            }

            double amount;
            try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
                MessageUtil.sendError(sender, "Montant invalide!");
                return;
            }

            economyManager.setBalance(target.getUniqueId(), target.getName(), amount);
            MessageUtil.sendSuccess(sender, "Solde de " + target.getName() + " défini à " + amount + " " + currencySymbol);
        }

        @Override
        public List<String> getTabCompletions(CommandSender sender, String[] args) {
            if (args.length == 1) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return names;
            }
            return new ArrayList<>();
        }
    }
}
