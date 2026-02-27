package plugin.attacks;

import org.bukkit.entity.Player;
import plugin.combomanager.ComboState;

public class Blank extends Attack {
    public boolean blank;

    public void setAttackType(AttackType attackType) {
        this.attackType = attackType;
    }

    public void setBlank(boolean blank) {
        this.blank = blank;
    }

    public void setCooldown(double cooldown) {
        this.cooldown = cooldown;
    }

    public AttackType attackType;
    public double cooldown;
    public Blank(boolean blank,AttackType attackType, int cooldown) {
        this.blank = blank;
        this.attackType = attackType;
        this.cooldown = cooldown;
    }
    public Blank() {}

    @Override
    public void attack(Player player, ComboState state) {

    }
}
