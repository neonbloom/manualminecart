package net.sioyama.manualminecart;

import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.rails.type.RailTypePowered;

/**
 * Powered rail behavior that leaves MMC train speed entirely to the notch
 * controller while preserving TrainCarts' standard behavior for other trains.
 */
final class ManualPoweredRailType extends RailTypePowered {
    private final ManualMinecart plugin;

    ManualPoweredRailType(ManualMinecart plugin, boolean powered) {
        super(powered);
        this.plugin = plugin;
    }

    @Override
    public void onPreMove(MinecartMember<?> member) {
        if (!plugin.isManualGroup(member.getGroup())) {
            super.onPreMove(member);
        }
    }
}
