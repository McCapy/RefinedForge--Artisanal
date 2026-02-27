package plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import plugin.RefinedForgeArtisanal;
import plugin.attacks.*;
import plugin.combodefinition.ComboDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigReader {

    public static ComboDefinition readToComboDef(String identifier) {

        ConfigurationSection config = RefinedForgeArtisanal.baseConfig;
        ConfigurationSection combo = config.getConfigurationSection(identifier);

        if (combo == null) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null) continue;
                String name = section.getString("name");
                if (name != null && name.equals(identifier)) {
                    combo = section;
                    identifier = key;
                    break;
                }
            }
        }

        if (combo == null) return null;

        List<Attack> attacks = new ArrayList<>();

        List<String> keys = new ArrayList<>(combo.getKeys(false));
        keys.remove("allowDualWield");
        keys.remove("duration");
        keys.remove("name");

        Yaml yaml = new Yaml(
                new CustomClassLoaderConstructor(
                        Attack.class.getClassLoader(),
                        new LoaderOptions()
                )
        );

        for (String key : keys) {

            ConfigurationSection section = combo.getConfigurationSection(key);
            if (section == null) continue;

            AttackType type = AttackType.valueOf(
                    Objects.requireNonNull(section.getString("attackType"))
            );

            String yamlString = yaml.dumpAsMap(section.getValues(true));

            switch (type) {

                case SLASH: {
                    Slash slash = yaml.loadAs(yamlString, Slash.class);
                    attacks.add(slash);
                    break;
                }

                case THRUST: {
                    Thrust thrust = yaml.loadAs(yamlString, Thrust.class);
                    attacks.add(thrust);
                    break;
                }

                case MAGICSHOT: {
                    MagicShot magic = yaml.loadAs(yamlString, MagicShot.class);
                    attacks.add(magic);
                    break;
                }

                case BLANK: {
                    Blank blank = yaml.loadAs(yamlString, Blank.class);
                    attacks.add(blank);
                    break;
                }

                default: {
                    RefinedForgeArtisanal.log("SEVERE", "The AttackType " + type + " doesn't exist in enumValues.AttackType[]");
                    break;
                }
            }
        }

        String name = combo.getString("name", identifier);

        return new ComboDefinition(
                identifier,
                name,
                combo.getBoolean("allowDualWield", false),
                attacks,
                combo.getDouble("duration")
        );
    }
}
