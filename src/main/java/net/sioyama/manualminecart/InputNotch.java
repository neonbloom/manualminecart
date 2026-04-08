package net.sioyama.manualminecart;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InputNotch implements Listener {
	@EventHandler
	public void onPlayerInteract (PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			ItemStack item = e.getItem();
			if (item != null && item.getType() == Material.STICK) {
				
			}
		}
	}
}
