package plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CustomConfig {

    private CustomConfig() {}

    public static YamlConfiguration create(JavaPlugin plugin, File file, boolean replace) {

        if (!file.exists() || replace) {
            plugin.saveResource(file.getName(), true);
        }

        return YamlConfiguration.loadConfiguration(file);
    }

}
