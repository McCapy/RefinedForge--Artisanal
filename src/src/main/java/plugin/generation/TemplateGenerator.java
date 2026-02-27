package plugin.generation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import plugin.RefinedForgeArtisanal;
import plugin.attacks.AttackType;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static plugin.RefinedForgeArtisanal.toSeconds;

public class TemplateGenerator {

    public Map<Object, Object> slashMap;
    public Map<Object, Object> thrustMap;
    public Map<Object, Object> blankMap;
    public Map<Object, Object> magicShotMap;

    public static TemplateGenerator create() {
        return new TemplateGenerator();
    }

    public TemplateGenerator() {
        this.thrustMap = of(
                "attackType", "THRUST",
                "blank", false,
                "damageRegion", 0,
                "thrustStrength", 0,
                "thrustDelay", 0,
                "damage", 0,
                "length", 0,
                "sound", List.of("entity.player.attack.sweep", "1", "1"),
                "gradient", List.of("rgb(0,0,0)", "rgb(0,0,0)"),
                "handleColor", "rgb(0,0,0)",
                "cooldown", 0,
                "startRadius", 0
        );

        this.slashMap = of(
                "attackType", "SLASH",
                "blank", false,
                "arcRadius", 0,
                "cooldown", 0,
                "detail", 0,
                "layers", 0,
                "layerSpace", 0,
                "layerColors", List.of("rgb(0,0,0)", "rgb(0,0,0)", "rgb(0,0,0)"),
                "particleSize", List.of(0, 0, 0),
                "damage", 0,
                "delay", 0,
                "rotation", 0,
                "jitter", 0,
                "distanceFromPlayer", 0,
                "sound", List.of("entity.player.attack.sweep", "1", "1"),
                "originAboveHead", 0,
                "horizontalRotation", 0,
                "horizontalOffset", 0,
                "damageRegion", 0
        );

        this.magicShotMap = of(
                "attackType", "MAGICSHOT",
                "spiralQuantity", 0,
                "length", 0,
                "radius", 0,
                "step", 0,
                "radiusFallOff", 0,
                "blank", false,
                "originAboveHead", 0,
                "gradient", List.of("rgb(0,0,0)", "rgb(0,0,0)"),
                "damageRegion", 0,
                "damage", 0.0,
                "cooldown", 0,
                "sound", List.of("block.beacon.power_select", "1", "1.2")
        );

        this.blankMap = of(
                "attackType", "BLANK",
                "blank", true,
                "cooldown", 0
        );
    }

    public Map<Object, Object> of(Object... places) {
        Map<Object, Object> result = new HashMap<>((int) Math.ceil(places.length / 2f));
        for (int i = 0; i < places.length / 2; i++) {
            int temp = i * 2;
            result.put(places[temp], places[temp + 1]);
        }
        return result;
    }

    public void generate(BukkitCommandActor sender, long time, List<AttackType> attacks) {

        RefinedForgeArtisanal plugin = RefinedForgeArtisanal.getPlugin();
        File dataFolder = plugin.getDataFolder();

        YamlConfiguration templatesConfig = RefinedForgeArtisanal.getTemplatesConfig();
        ConfigurationSection keys = templatesConfig.getConfigurationSection("template");

        String config = "template.";

        sender.reply("§f§lRefinedForge Generating:");

        templatesConfig.set(config + "name", "Example");
        templatesConfig.set(config + "allowDualWield", false);
        templatesConfig.set(config + "duration", 0);

        File file = RefinedForgeArtisanal.getTemplatesFile();
        file.delete();

        try {
            file.createNewFile();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        templatesConfig = YamlConfiguration.loadConfiguration(file);
        sender.reply("§a");

        int combo = 1;

        for (AttackType currentAttack : attacks) {

            sender.reply("§7§lAttack: §f§l" + currentAttack + " §7§lCombo Number: §f§l" + combo);
            String attackSectionRoot = currentAttack.toString().toLowerCase().concat("-").concat(String.valueOf(combo)).concat(".");
            switch (currentAttack) {

                case THRUST -> {
                    for (Object thrustRoot : thrustMap.keySet()) {
                        templatesConfig.set(config + attackSectionRoot + thrustRoot, thrustMap.get(thrustRoot));
                        sender.reply("§f- §7Generated Field: §f" + thrustRoot);
                    }
                }

                case SLASH -> {
                    for (Object slashRoot : slashMap.keySet()) {
                        templatesConfig.set(config + attackSectionRoot + slashRoot, slashMap.get(slashRoot));
                        sender.reply("§f- §7Generated Field: §f" + slashRoot);
                    }
                }

                case BLANK -> {
                    for (Object blankRoot : blankMap.keySet()) {
                        templatesConfig.set(config + attackSectionRoot + blankRoot, blankMap.get(blankRoot));
                        sender.reply("§f- §7Generated Field: §f" + blankRoot);
                    }
                }

                case MAGICSHOT -> {
                    for (Object magicRoot : magicShotMap.keySet()) {
                        templatesConfig.set(config + attackSectionRoot + magicRoot, magicShotMap.get(magicRoot));
                        sender.reply("§f- §7Generated Field: §f" + magicRoot);
                    }
                }
            }

            combo++;
        }

        try {
            templatesConfig.save(file);
            sender.reply("§aSaved generated_templates.yml successfully.");
        }
        catch (Exception e) {
            RefinedForgeArtisanal.log("SEVERE", "Failed saving generated_templates.yml.");
            sender.reply("§7Task Completed in: §f" + toSeconds((System.currentTimeMillis() - time)) + " §7seconds");
            return;
        }
        sender.reply("§7Task Completed in: §f" + toSeconds((System.currentTimeMillis() - time)) + " §7seconds");
    }
}
