package plugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.RefinedForgeArtisanal;
import plugin.attacks.Attack;
import plugin.attacks.AttackType;
import plugin.combodefinition.ComboDefinition;
import plugin.config.ConfigReader;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.Suggest;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static plugin.RefinedForgeArtisanal.log;
import static plugin.RefinedForgeArtisanal.toSeconds;

@SuppressWarnings({"DataFlowIssue"})
@Command({"rfa", "refinedforge", "refinedforgeartisanal"})
public class RefinedForgeCommand {

    public boolean check(Player p) {
        if (p == null) {
            log("ERROR", "Console isn't able to use these commands.");
            return false;
        }
        if (!p.isOp()) {
            p.sendMessage("§f§lRefinedForge Commands");
            p.sendMessage("§f- §7Insufficient permissions.");
            return true;
        }
        return false;
    }

    @Command({"rfa", "refinedforge", "refinedforgeartisanal"})
    public void rfa(BukkitCommandActor sender) {

        List<String> rfaDefault = RefinedForgeArtisanal.getInfoConfig().getStringList("rfa");
        rfaDefault.forEach(sender::reply);
        rfaDefault.forEach((str) -> log("INFO", str));
    }

    @Subcommand("help")
    public void help(BukkitCommandActor sender) {

        if (check(Bukkit.getPlayer(sender.name()))) return;
        List<String> help = RefinedForgeArtisanal.getInfoConfig().getStringList("help");
        help.forEach(sender::reply);
        help.forEach((str) -> log("INFO", str));
    }

    @Subcommand("reload")
    public void reload(BukkitCommandActor sender) {

        long time = System.currentTimeMillis();

        RefinedForgeArtisanal plugin = RefinedForgeArtisanal.getPlugin();

        sender.reply("§f§lRefinedForge Reload");
        sender.reply("§7Reloading configuration files...");

        plugin.reloadConfig();

        RefinedForgeArtisanal.baseConfig = YamlConfiguration.loadConfiguration(RefinedForgeArtisanal.getConfigFile());
        RefinedForgeArtisanal.infoConfig = YamlConfiguration.loadConfiguration(RefinedForgeArtisanal.getInfoFile());
        RefinedForgeArtisanal.templatesConfig = YamlConfiguration.loadConfiguration(RefinedForgeArtisanal.getTemplatesFile());
        sender.reply("§7Reloading combos...");

        RefinedForgeArtisanal.comboManager.resetDefinitions();
        RefinedForgeArtisanal.comboManager.resetStates();

        for (String key : RefinedForgeArtisanal.baseConfig.getKeys(false)) {

            ComboDefinition def = ConfigReader.readToComboDef(key);
            if (def == null) continue;

            RefinedForgeArtisanal.comboManager.registerCombo(def, def.name);

            int slashes = 0;
            int blanks = 0;
            int thrusts = 0;

            for (Attack atk : def.attacks) {

                if (atk instanceof plugin.attacks.Slash) slashes++;
                else if (atk instanceof plugin.attacks.Blank) blanks++;
                else if (atk instanceof plugin.attacks.Thrust) thrusts++;
            }

            sender.reply("§8- §f" + def.name +
                    " §7(§f" + def.attacks.size() + " attacks§7, " +
                    "slashes: §f" + slashes + "§7, " +
                    "blanks: §f" + blanks + "§7, " +
                    "thrusts: §f" + thrusts + "§7, " +
                    "dual: §f" + def.allowDualWield + "§7)");
        }

        sender.reply("§7Reload completed in: §f" + toSeconds((System.currentTimeMillis() - time)) + " §7seconds");
    }

    @Subcommand({"credits"})
    public void credits(BukkitCommandActor sender) {

        List<String> credits = RefinedForgeArtisanal.getInfoConfig().getStringList("credits");
        credits.forEach(sender::reply);
        credits.forEach((str) -> log("INFO", str));
    }

    @Subcommand({"info"})
    public void information(BukkitCommandActor sender) {

        List<String> help = RefinedForgeArtisanal.getInfoConfig().getStringList("help");
        help.forEach(sender::reply);
        help.forEach((str) -> log("INFO", str));
    }

    @Subcommand("discord")
    public void discord(BukkitCommandActor sender) {

        List<String> discord = RefinedForgeArtisanal.getInfoConfig().getStringList("discord");
        discord.forEach(sender::reply);
        discord.forEach((str) -> log("INFO", str));
    }

    @Subcommand({"commands"})
    public void commands(BukkitCommandActor sender) {

        List<String> commands = RefinedForgeArtisanal.getInfoConfig().getStringList("commands");
        commands.forEach(sender::reply);
        commands.forEach((str) -> log("INFO", str));
    }

    @Subcommand("addCombo")
    public void addCombo(BukkitCommandActor sender, @Suggest("<combo_name>") String comboName) {

        Player player = (Player) sender;

        if (check(Bukkit.getPlayer(sender.name()))) return;
        long time = System.currentTimeMillis();
        ComboDefinition def = ConfigReader.readToComboDef(comboName);
        if (def == null) {
            sender.reply("§cCombo '" + comboName + "' does not exist in config.");
            return;
        }

        ItemStack item = Bukkit.getPlayer(sender.name()).getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore();

        if (lore == null || lore.isEmpty()) {

            lore = new ArrayList<>();
            lore.add(Component.text(def.name).color(NamedTextColor.DARK_GRAY));

        }
        else lore.set(0, Component.text(def.name).color(NamedTextColor.DARK_GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);

        sender.reply("§aAdded plugin.combo §f" + def.name + " §ato your item.");
        sender.reply("§7Task Completed in: §f" + toSeconds((System.currentTimeMillis() - time)) + " §7seconds");
    }

    @Subcommand({"generate"})
    public void generate(BukkitCommandActor sender, @Suggest({"BLANK,SLASH,THRUST,MAGICSHOT"}) String attack) {

        long time = System.currentTimeMillis();
        if (check(Bukkit.getPlayer(sender.name()))) return;

        List<AttackType> attacks = Arrays.stream(attack.split(",")).map(AttackType::valueOf).toList();
        RefinedForgeArtisanal.getTemplateGenerator().generate(sender, time, attacks);
    }

    @Subcommand({"give"})
    public void giveCombo(BukkitCommandActor sender, @Suggest("<player>") String player, @Suggest("<combo_name>") String comboName, @Suggest("<material>") String material) {

        Player p = Bukkit.getPlayer(player);
        if (check(p)) return;
        long time = System.currentTimeMillis();
        if (p == null) {

            sender.reply("§cThat player, " + player + " doesn't exist.");
            return;
        }

        ComboDefinition def = ConfigReader.readToComboDef(comboName);
        if (def == null) {

            sender.reply("§cCombo '" + comboName + "' does not exist in config.");
            return;
        }

        Material type = Material.getMaterial(material);
        if (type == null) {

            sender.reply("§cThat material, " + material + " doesn't exist.");
            return;
        }

        ItemStack item = new ItemStack(type, 1);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>(5);
        lore.add(Component.text(def.name).color(NamedTextColor.DARK_GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        p.give(item);

        sender.reply("§7Task Completed in: §f" + toSeconds((System.currentTimeMillis() - time)) + " §7seconds");
    }

    @Subcommand({"combos"})
    public void getCombos(BukkitCommandActor sender) {

        long time = System.currentTimeMillis();
        sender.reply("§f§lRefinedForge Combos");

        for (String key : RefinedForgeArtisanal.getBaseConfig().getKeys(false)) {

            ComboDefinition def = ConfigReader.readToComboDef(key);
            if (def == null) continue;
            sender.reply("§8- §f" + def.name + " §7(§f" + def.attacks.size() + " slashes§7, dual: §f" + def.allowDualWield + "§7)");
        }
        sender.reply("§7Task Completed in: §f" + toSeconds((System.currentTimeMillis() - time)) + " §7seconds");
    }
}
