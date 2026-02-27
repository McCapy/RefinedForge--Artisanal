package plugin.combodefinition;

import plugin.attacks.Attack;
import plugin.attacks.Blank;

import java.util.List;

public class ComboDefinition {

    public String key;   // YAML section key
    public String name;  // Logical combo name from YAML "name"

    public boolean allowDualWield;
    public final List<Attack> attacks;
    public double duration;

    public ComboDefinition(String key, String name, boolean allowDualWield, List<Attack> attacks, double duration) {
        this.key = key;
        this.name = name;
        this.allowDualWield = allowDualWield;
        this.attacks = attacks;
        this.duration = duration;
    }

    public ComboDefinition() {
        this.key = "blank";
        this.name = "blank";
        this.allowDualWield = false;
        this.attacks = List.of(new Blank());
        this.duration = 0d;
    }
}
