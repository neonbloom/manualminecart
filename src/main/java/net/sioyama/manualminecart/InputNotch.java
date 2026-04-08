package net.sioyama.manualminecart;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InputNotch implements Listener {
	@EventHandler
	public void onPlayerInteract (PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR) {
			
		}
	}
}
