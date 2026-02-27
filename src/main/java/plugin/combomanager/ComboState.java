package plugin.combomanager;

import java.util.UUID;

public class ComboState {

    public final UUID playerId;
    public boolean started = false;
    public int comboIndex = 0;
    public double timeSince = 1d;

    public ComboState(UUID playerId) {
        this.playerId = playerId;
        this.reset();
    }

    public ComboState reset() {

        started = false;
        comboIndex = 0;
        timeSince = 0;
        return this;

    }
}
