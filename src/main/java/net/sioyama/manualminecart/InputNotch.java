package net.sioyama.manualminecart;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class InputNotch implements Listener {
    private final ManualMinecart plugin;
    private final Set<UUID> clickCooldown = new HashSet<>();

    public InputNotch(ManualMinecart plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        int change;
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            change = 1;
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            change = -1;
        } else {
            return;
        }

        ItemStack item = event.getItem();
        if (!isControlStick(item)) {
            return;
        }

        // The control stick is an input device, so left-clicking a block with it
        // must never start normal block interaction or block destruction.
        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }

        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Minecart minecart) || !plugin.isManualMinecart(minecart)) {
            return;
        }

        MinecartMember<?> member = MinecartMemberStore.getFromEntity(minecart);
        if (member == null || member.getGroup() == null) {
            player.sendMessage("このトロッコはTrainCartsの列車ではありません。");
            return;
        }

        MinecartGroup group = member.getGroup();
        UUID playerId = player.getUniqueId();
        if (!clickCooldown.add(playerId)) {
            event.setCancelled(true);
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(
                plugin, () -> clickCooldown.remove(playerId), 2L);

        TrainState state = plugin.getTrainState(group);
        int previousNotch = state.getNotch();
        state.changeNotch(change);
        if (previousNotch <= TrainState.NEUTRAL
                && state.getNotch() > TrainState.NEUTRAL) {
            plugin.setDirectionFromView(group, member, state, player);
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDamage(BlockDamageEvent event) {
        if (isControlStick(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isControlStick(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    private boolean isControlStick(ItemStack item) {
        if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        Integer value = meta.getPersistentDataContainer().get(
                plugin.getStickKey(), PersistentDataType.INTEGER);
        return Integer.valueOf(1).equals(value);
    }
}
