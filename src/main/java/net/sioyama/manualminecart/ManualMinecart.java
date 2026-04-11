package net.sioyama.manualminecart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ManualMinecart extends JavaPlugin implements Listener {
	Map<UUID, TrainState> ts = new HashMap<>();
	
	@Override
	public void onEnable() {
		getLogger().info("ManualMinecart started successfully.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("manualminecart")) {
			// バージョン
			if (args.length == 0) {
				sender.sendMessage("ManualMinecart Version 1.0.0");
				return true;				
			}
			// ヘルプ
			if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
				sender.sendMessage("Help: https://example.com");
				return true;
			}
			// セット
			if (args.length == 1 && args[0].equalsIgnoreCase("set")) {
				if (!(sender instanceof Player player)) {
					sender.sendMessage("This command can only be used by a player.");
					return true;
				}
				Entity vehicle = player.getVehicle();
				if (!(vehicle instanceof Minecart minecart)) {
					sender.sendMessage("You are not on a minecart.");
					return true;
				}
				NamespacedKey key = new NamespacedKey(this, "manualminecart");
				minecart.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
				sender.sendMessage("The processing was successful.");
				return true;
			}
			// スティック
			if (args.length == 1 && args[0].equalsIgnoreCase("stick")) {
				if (!(sender instanceof Player)) {
					sender.sendMessage("This command can only be used by a player.");
					return true;
				}
				Player player = (Player) sender;
				ItemStack item = new ItemStack(Material.STICK, 1);
				ItemMeta meta = item.getItemMeta();
				NamespacedKey key = new NamespacedKey(this, "manualminecartstick");
				meta.setDisplayName("ManualMinecartStick");
				meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
				item.setItemMeta(meta);
				player.getInventory().addItem(item);
				return true;
			}
		}
	return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("manualminecart") && args.length == 1) {
			// タブ補完
			List<String> suggestions = new ArrayList<>();
			suggestions.add("help");
			suggestions.add("cart");
			suggestions.add("stick");
			return suggestions;
		}
	return null;
	}
	
	@EventHandler
	public void onEntityPlace(EntityPlaceEvent e) {
		Entity entity = e.getEntity();
		if (!(entity instanceof Minecart minecart)) {
			return;
		}
	}
}