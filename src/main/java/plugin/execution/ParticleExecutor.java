package plugin.execution;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import plugin.RefinedForgeArtisanal;
import plugin.attacks.Attack;
import plugin.combodefinition.ComboDefinition;
import plugin.combomanager.ComboManager;
import plugin.combomanager.ComboState;

import static plugin.RefinedForgeArtisanal.sendActionBarInfo;

public class ParticleExecutor implements Listener {

    private final ComboManager comboManager;
    private final NamespacedKey trueDelayKey;

    public ParticleExecutor(RefinedForgeArtisanal plugin, ComboManager comboManager) {

        this.comboManager = comboManager;
        this.trueDelayKey = new NamespacedKey(plugin, "trueDelay");

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(trueDelayKey, PersistentDataType.BOOLEAN, false);

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        PersistentDataContainer container = player.getPersistentDataContainer();
        container.set(trueDelayKey, PersistentDataType.BOOLEAN, false);
        comboManager.states.remove(player.getUniqueId());

    }

    private void executeCombo(Player player, ComboManager comboManager) {

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        boolean onCooldown = pdc.getOrDefault(trueDelayKey, PersistentDataType.BOOLEAN, false);
        if (onCooldown) return;

        ComboState state = comboManager.getState(player);

        ComboDefinition main = comboManager.getDefinition(player, true);
        ComboDefinition off  = comboManager.getDefinition(player, false);

        if (main == null) return;

        if (state.comboIndex >= main.attacks.size()) {
            state.reset();
        }

        if (!state.started) {
            state.started = true;
            if (off == null) {
                comboManager.startTimer(player, state, main);
            }
            else {
                comboManager.startTimer(player,state,main,off);
            }
        }

        Attack mainAttack = main.attacks.get(state.comboIndex);
        Attack offAttack  = null;

        boolean dual = false;

        if (off != null && off.allowDualWield && state.comboIndex < off.attacks.size()) {
            offAttack = off.attacks.get(state.comboIndex);
            dual = true;
        }

        if (mainAttack != null) {
            mainAttack.attack(player, state);
        }

        if (dual && offAttack != null) {
            offAttack.attack(player, state);
        }

        double fullCooldownTicks = getFullCooldownTicks(mainAttack) * 20;

        ItemStack mainItem = player.getInventory().getItemInMainHand();
        if (mainItem.getType() != Material.AIR) {
            player.setCooldown(mainItem.getType(), (int) fullCooldownTicks);
        }

        ItemStack offItem = player.getInventory().getItemInOffHand();
        if (offItem.getType() != Material.AIR) {
            player.setCooldown(offItem.getType(), (int) fullCooldownTicks);
        }

        pdc.set(trueDelayKey, PersistentDataType.BOOLEAN, true);
        state.comboIndex++;
        if (state.comboIndex > main.attacks.size()) {
            state.reset();
        }
        Bukkit.getScheduler().runTaskLater(plugin.RefinedForgeArtisanal.getPlugin(),
                () -> pdc.set(trueDelayKey, PersistentDataType.BOOLEAN, false), (int) fullCooldownTicks
        );

    }

    private static double getFullCooldownTicks(Attack mainAttack) {
        double fullCooldownTicks = 1;

        if (mainAttack instanceof plugin.attacks.Slash s) {
            fullCooldownTicks = Math.max(1, s.cooldown);
        }
        else if (mainAttack instanceof plugin.attacks.Thrust t) {
            fullCooldownTicks = Math.max(1d, t.cooldown);
        }
        else if (mainAttack instanceof plugin.attacks.MagicShot m) {
            fullCooldownTicks = Math.max(1,m.cooldown);
        }
        else if (mainAttack instanceof plugin.attacks.Blank b) {
            fullCooldownTicks = Math.max(1,b.cooldown);
        }
        return fullCooldownTicks;
    }

    @EventHandler
    public void onLeftClick(PlayerArmSwingEvent event) {
        Player player = event.getPlayer();
        executeCombo(player, comboManager);
        sendActionBarInfo(player);
    }

}
