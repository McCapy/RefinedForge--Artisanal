package plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import plugin.combodefinition.ComboDefinition;
import plugin.combomanager.ComboManager;
import plugin.combomanager.ComboState;
import plugin.commands.RefinedForgeCommand;
import plugin.config.ConfigReader;
import plugin.config.CustomConfig;
import plugin.execution.ParticleExecutor;
import org.bukkit.configuration.file.YamlConfiguration;

import plugin.generation.TemplateGenerator;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"deprecation", "UnstableApiUsage"})
public final class RefinedForgeArtisanal extends JavaPlugin {

    public static YamlConfiguration infoConfig;
    public static YamlConfiguration templatesConfig;
    public static YamlConfiguration baseConfig;
    public static TemplateGenerator templateGenerator;
    public static Logger logger;
    public static File templatesFile;
    public static File configFile;
    public static File infoFile;

    public static RefinedForgeArtisanal plugin;
    public static File getInfoFile() {
        return infoFile;
    }
    public static File getConfigFile() {
        return configFile;
    }
    public static File getTemplatesFile() {
        return templatesFile;
    }
    public static RefinedForgeArtisanal getPlugin() {
        return plugin;
    }
    public static YamlConfiguration getInfoConfig() {
        return infoConfig;
    }
    public static TemplateGenerator getTemplateGenerator() {
        return templateGenerator;
    }
    public static YamlConfiguration getTemplatesConfig() {
        return templatesConfig;
    }
    public static YamlConfiguration getBaseConfig() {
        return baseConfig;
    }
    public static Logger getLoggingInstance() {
        return logger;
    }
    public static ComboManager comboManager;

    public static double toSeconds(long ms) {
        return ms / 1000d;
    }

    public static void log(String level, String message) {
        logger.log(Level.parse(level), "[RefinedForge] " + message);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onEnable() {

        long time = System.currentTimeMillis();

        plugin = this;
        logger = Bukkit.getLogger();
        templateGenerator = TemplateGenerator.create();
        comboManager = new ComboManager(this);

        comboManager.resetDefinitions();
        comboManager.resetStates();

        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this).build();
        lamp.register(new RefinedForgeCommand());

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
            log("INFO", "Successfully created the plugin directory.");
        }

        templatesFile = new File(dataFolder, "generated_templates.yml");
        configFile = new File(dataFolder, "config.yml");
        infoFile = new File(dataFolder, "info.yml");

        this.saveResource("info.yml", true);
        if (!configFile.exists()) {
            this.saveResource("config.yml", false);
        }
        this.saveResource("tutorial.yml", true);
        this.saveResource("generated_templates.yml", true);

        infoConfig = CustomConfig.create(plugin, infoFile, true);
        templatesConfig = CustomConfig.create(plugin, templatesFile, true);
        baseConfig = CustomConfig.create(plugin, configFile, false);

        String newVersion = this.getDescription().getVersion();
        String oldVersion = infoConfig.getString("plugin-info.version");
        if (!Objects.requireNonNull(oldVersion).equals("ERROR") && !newVersion.equals(oldVersion)) {
            log("INFO", "You should consider updating the RefinedForgeArtisanal directory:");
            log("INFO", "delete: tutorial.yml, generated_templates.yml, and info.yml");
        }
        infoConfig.set("plugin-info.version", "v" + newVersion);
        try {
            infoConfig.save(RefinedForgeArtisanal.getInfoFile());
        }
        catch (IOException e) {
            log("SEVERE", "Severe error info.yml couldn't be saved.");
            throw new RuntimeException(e);
        }

        for (String key : baseConfig.getKeys(false)) {
            ComboDefinition def = ConfigReader.readToComboDef(key);
            if (def == null) continue;
            comboManager.registerCombo(def, def.name);
        }

        PluginManager manager = this.getServer().getPluginManager();
        manager.registerEvents(new ParticleExecutor(this, comboManager), this);

        log("INFO", "Plugin finished loading in " + toSeconds((System.currentTimeMillis() - time)) + " seconds");
        log("INFO", "Successfully started Periodic-Loop");

        Bukkit.getScheduler().runTaskTimer(this, () -> Bukkit.getOnlinePlayers().forEach((player) -> {
            ComboState state = comboManager.getState(player);
            ComboDefinition mainDefinition = comboManager.getDefinition(player, true);
            if (mainDefinition == null) {
                return;
            }
            ComboDefinition sideDefinition = comboManager.getDefinition(player, false);
            int count;
            if (sideDefinition != null) {
                count = Math.max(mainDefinition.attacks.size(), sideDefinition.attacks.size());
            }
            else {
                count = mainDefinition.attacks.size();
            }
            int index = state.comboIndex;
            player.sendActionBar(Component.text(getActionBarMessage(count, index)));
        }), 1, 20L);
    }

    public static void sendActionBarInfo(Player player) {
        ComboState state = comboManager.getState(player);
        ComboDefinition mainDefinition = comboManager.getDefinition(player, true);
        if (mainDefinition == null) {
            return;
        }
        ComboDefinition sideDefinition = comboManager.getDefinition(player, false);
        int count;
        if (sideDefinition != null) {
            count = Math.max(mainDefinition.attacks.size(), sideDefinition.attacks.size());
        }
        else {
            count = mainDefinition.attacks.size();
        }
        int index = state.comboIndex;
        player.sendActionBar(Component.text(getActionBarMessage(count, index)));
    }

    public static @NotNull String getActionBarMessage(int count, int index) {

        StringBuilder message = new StringBuilder(count * 8);
        for (int i = 1; i <= count; i++) {
            message.append(i <= index ? "§f§l" + i : "§7" + i);
            if (i < count) message.append("§8-");
        }
        return message.toString();
    }

    @Override
    public void onDisable() {
        long time = System.currentTimeMillis();
        comboManager.resetDefinitions();
        comboManager.resetStates();
        log("INFO", "Plugin finished unloading in " + toSeconds((System.currentTimeMillis() - time)) + " seconds");
    }
}
