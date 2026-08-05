package net.sioyama.manualminecart;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class InputNotch implements Listener {
	private final ManualMinecart plugin;

	public InputNotch(ManualMinecart plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getHand() != EquipmentSlot.HAND) {
			return;
		}

		int amount;
		Action action = e.getAction();
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			amount = 1;
		} else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			amount = -1;
		} else {
			return;
		}

		ItemStack item = e.getItem();
		if (item == null || item.getType() != Material.STICK || !item.hasItemMeta()) {
			return;
		}

		ItemMeta meta = item.getItemMeta();
		Integer stickValue = meta.getPersistentDataContainer().get(
				plugin.getStickKey(), PersistentDataType.INTEGER);
		if (!Integer.valueOf(1).equals(stickValue)) {
			return;
		}

		Player player = e.getPlayer();
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof Minecart minecart)) {
			return;
		}

		Integer minecartValue = minecart.getPersistentDataContainer().get(
				plugin.getMinecartKey(), PersistentDataType.INTEGER);
		if (!Integer.valueOf(1).equals(minecartValue)) {
			return;
		}

		MinecartMember<?> member = MinecartMemberStore.getFromEntity(minecart);
		if (member == null) {
			player.sendMessage("このトロッコはTrainCartsの列車ではありません。");
			return;
		}

		MinecartGroup group = member.getGroup();
		TrainState state = plugin.getTrainState(group);
		state.changeNotch(amount);
		e.setCancelled(true);

		long speed = Math.round(Math.abs(group.getAverageForce()) * 72.0);
		String text = "ノッチ " + state.getNotchName() + "  速度 " + speed + " km/h";
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
	}
}