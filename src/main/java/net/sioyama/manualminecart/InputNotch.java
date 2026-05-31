package net.sioyama.manualminecart;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class InputNotch implements Listener {
	private final ManualMinecart plugin;
	public InputNotch(ManualMinecart plugin) {
		this.plugin = plugin;
	}
	@EventHandler
	public void onPlayerInteract (PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			ItemStack item = e.getItem();
			if (item != null && item.getType() == Material.STICK) {
				if (!item.hasItemMeta()) {
					return;
				}
				ItemMeta meta = item.getItemMeta();
				NamespacedKey key = new NamespacedKey(plugin, "manualminecartstick");
				Integer value = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
				if (value == null || value != 1) {
					return;
				}
			
			}
		}
	}
}
