package plugin.combomanager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.RefinedForgeArtisanal;
import plugin.combodefinition.ComboDefinition;
import plugin.config.ConfigReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ComboManager {

    public final RefinedForgeArtisanal plugin;
    public final Map<String, ComboDefinition> combos = new HashMap<>(15, 1f);
    public final Map<UUID, ComboState> states = new HashMap<>(10, 1f);

    public final NamespacedKey mainHandKey;
    public final NamespacedKey offHandKey;

    public ComboManager(RefinedForgeArtisanal plugin) {
        this.plugin = plugin;

        mainHandKey = new NamespacedKey(RefinedForgeArtisanal.getPlugin(), "mainHandKey");
        offHandKey = new NamespacedKey(RefinedForgeArtisanal.getPlugin(), "offHandKey");
    }

    public ComboState getState(Player p) {
        UUID uuid = p.getUniqueId();
        states.computeIfAbsent(uuid, (currentPlayer) -> new ComboState(uuid).reset());
        return states.get(uuid);
    }

    public String getLoreFirst(Player p, boolean mainHand) {

        PlayerInventory inventory = p.getInventory();
        ItemStack item = mainHand ? inventory.getItemInMainHand() : inventory.getItemInOffHand();

        if (item.getType() == Material.AIR) return "no_item";

        List<Component> tempLore = item.lore();
        if (tempLore == null || tempLore.isEmpty()) return "no_item";

        String raw = PlainTextComponentSerializer.plainText().serialize(tempLore.getFirst());

        raw = raw.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        raw = raw.replaceAll("&[0-9A-FK-ORa-fk-or]", "");

        return raw.trim();
    }

    public boolean hasDefinitionFromName(String name) {
        return combos.containsKey(name);
    }

    public ComboDefinition getDefinition(Player p, boolean mainHand) {
        String comboName = getLoreFirst(p, mainHand);
        if (comboName.equals("no_item")) {
            return null;
        }

        if (!hasDefinitionFromName(comboName)) {
            ComboDefinition comboDefinition = ConfigReader.readToComboDef(comboName);
            if (comboDefinition == null) {
                return null;
            }
            registerCombo(comboDefinition, comboName);
            return comboDefinition;
        } else {
            return combos.get(comboName);
        }
    }

    public void resetDefinitions() {
        combos.values().forEach((current) -> RefinedForgeArtisanal.log("INFO", "Successfully unloaded the combo: " + current.name));
        combos.clear();
    }

    public void resetStates() {
        states.clear();
    }

    public void registerCombo(ComboDefinition combo, String ignored) {

        if (combo == null || combo.name == null) {
            RefinedForgeArtisanal.log("SEVERE", "Failed registering combo, UNKNOWN");
            return;
        }
        if (!hasDefinitionFromName(combo.name)) {
            YamlConfiguration config = RefinedForgeArtisanal.getBaseConfig();
            combos.put(combo.name, combo);
            RefinedForgeArtisanal.log("INFO", "Loaded " + combo.name + ": Attacks: " + combo.attacks.size() + ", DualWieldable: " + combo.allowDualWield + ", Duration: " + combo.duration);
        } else {
            RefinedForgeArtisanal.log("SEVERE", "Failed registering combo, " + combo.name);
        }
    }

    public double getDuration(ComboDefinition main, ComboDefinition side) {

        if (main.duration < 0) main.duration = 0;
        if (side.duration < 0) side.duration = 0;

        return (side.duration + main.duration) / 2;
    }

    public void startTimer(Player player, ComboState state, ComboDefinition main) {

        if (state.started && state.timeSince > 0) return;

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {

            if (!player.isOnline()) {

                state.reset();
                task.cancel();
                return;

            }

            if (!state.started) {

                task.cancel();
                return;

            }
            if (state.timeSince >= main.duration) {

                state.reset();
                task.cancel();
                return;

            }
            state.timeSince += 0.25;

        }, 0L, 5L);
    }

    public void startTimer(Player player, ComboState state, ComboDefinition main, ComboDefinition side) {

        if (state.started && state.timeSince > 0) return;

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {

            if (!player.isOnline()) {

                state.reset();
                task.cancel();
                return;

            }

            if (!state.started) {

                task.cancel();
                return;

            }
            if (state.timeSince >= getDuration(main, side)) {

                state.reset();
                task.cancel();
                return;

            }
            state.timeSince += 0.25;

        }, 0L, 5L);
    }
}
