package plugin.attacks;

import org.bukkit.entity.Player;
import plugin.RefinedForgeArtisanal;
import plugin.combomanager.ComboState;

public abstract class Attack {
    public void attack(Player player, ComboState state) {
        RefinedForgeArtisanal.log("SEVERE","Invalid use of Class<?> extends Attack .attack()");
    }
}
