package net.sioyama.manualminecart;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class ManualMinecart extends JavaPlugin implements Listener {
	@Override
	public void onEnable() {
		getLogger().info("ManualMinecart started successfully.");
		getServer().getPluginManager().registerEvents(this, this);
		// トロッコの処理
		getServer().getScheduler().runTaskTimer(this, () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				Entity vehicle = player.getVehicle();
				if (vehicle instanceof Minecart) {
					Minecart minecart = (Minecart) vehicle;
					minecart.setMaxSpeed(2.0);
					Vector v = minecart.getVelocity();
					minecart.setVelocity(v.multiply(1.05));
				}
			}
		}, 0L, 1L);
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
			// カート
			if (args.length == 1 && args[0].equalsIgnoreCase("cart")) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					ItemStack mmc = new ItemStack(Material.MINECART, 1);
					ItemMeta mmcmeta = mmc.getItemMeta();
					NamespacedKey mmckey = new NamespacedKey(this, "mmc");
					mmcmeta.setDisplayName("ManualMinecart");
					mmcmeta.getPersistentDataContainer().set(mmckey, PersistentDataType.INTEGER, 0);
					mmc.setItemMeta(mmcmeta);
					player.getInventory().addItem(mmc);
					return true;
				}
				sender.sendMessage("This command can only be used by a player.");
				return true;
			}
			// スティック
			if (args.length == 1 && args[0].equalsIgnoreCase("stick")) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					ItemStack stick = new ItemStack(Material.STICK, 1);
					ItemMeta stickmeta = stick.getItemMeta();
					NamespacedKey stickkey = new NamespacedKey(this, "stick");
					stickmeta.setDisplayName("MMCStick");
					stickmeta.getPersistentDataContainer().set(stickkey, PersistentDataType.INTEGER, 0);
					stick.setItemMeta(stickmeta);
					player.getInventory().addItem(stick);
					return true;
				}
				sender.sendMessage("This command can only be used by a player.");
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
	public void onInteract(PlayerInteractEvent event) {
		ItemStack item = event.getItem();
		if (item == null) {
			return;
		}
		if (item.getType() != Material.STICK) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return;
		}
		NamespacedKey key = new NamespacedKey(this, "stick");
		if (!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
			return;
		}
		Integer mode = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
		if (mode == null) {
			mode = 0;
		}
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (mode <= 2) {
				mode++;
				meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, mode);
				item.setItemMeta(meta);
				return;
			}
		}
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (mode >= -3) {
				mode--;
				meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, mode);
				item.setItemMeta(meta);
				return;
			}
		}	
	}
}