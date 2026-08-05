package net.sioyama.manualminecart;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import com.bergerkiller.bukkit.tc.properties.TrainPropertiesStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class ManualMinecart extends JavaPlugin {
	private static final double MAX_SPEED_KMH = 50.0;
	private static final double MAX_SPEED = MAX_SPEED_KMH / 72.0;
	private static final double TICKS_SQUARED = 20.0 * 20.0;

	private final Map<String, TrainState> trainStates = new HashMap<>();
	private NamespacedKey minecartKey;
	private NamespacedKey stickKey;
	private BukkitTask movementTask;

	@Override
	public void onEnable() {
		minecartKey = new NamespacedKey(this, "manualminecart");
		stickKey = new NamespacedKey(this, "manualminecartstick");
		loadTrainStates();

		getServer().getPluginManager().registerEvents(new InputNotch(this), this);
		movementTask = getServer().getScheduler().runTaskTimer(this, this::tickTrains, 1L, 1L);
		getLogger().info("ManualMinecart started.");
	}

	@Override
	public void onDisable() {
		if (movementTask != null) {
			movementTask.cancel();
		}
		saveTrainStates();
	}

	NamespacedKey getMinecartKey() {
		return minecartKey;
	}

	NamespacedKey getStickKey() {
		return stickKey;
	}

	TrainState getTrainState(MinecartGroup group) {
		String trainName = group.getProperties().getTrainName();
		TrainState state = trainStates.computeIfAbsent(trainName, name -> new TrainState());
		state.syncDirection(group.getAverageForce());
		return state;
	}

	private void tickTrains() {
		trainStates.entrySet().removeIf(entry -> {
			TrainProperties properties = TrainPropertiesStore.get(entry.getKey());
			if (properties == null) {
				return true;
			}

			MinecartGroup group = properties.getHolder();
			if (group != null && !group.isEmpty()) {
				moveTrain(group, entry.getValue());
			}
			return false;
		});
	}

	private void moveTrain(MinecartGroup group, TrainState state) {
		double speed = Math.abs(group.getAverageForce());
		double acceleration;

		switch (state.getNotch()) {
			case TrainState.P1 -> acceleration = powerAcceleration(0.35, speed);
			case TrainState.P2 -> acceleration = powerAcceleration(0.62, speed);
			case TrainState.P3 -> acceleration = powerAcceleration(0.90, speed);
			case TrainState.B1 -> acceleration = -0.45;
			case TrainState.B2 -> acceleration = -0.75;
			case TrainState.B3 -> acceleration = -1.05;
			case TrainState.EMERGENCY -> acceleration = -1.35;
			default -> acceleration = -(0.035 + speed * 20.0 * 0.0015);
		}

		double nextSpeed = speed + acceleration / TICKS_SQUARED;
		if (state.getNotch() > TrainState.NEUTRAL) {
			nextSpeed = Math.min(MAX_SPEED, nextSpeed);
		}

		if (nextSpeed <= 0.0001) {
			group.stop();
		} else {
			group.setForwardForce(state.getDirection() * nextSpeed);
		}
	}

	private double powerAcceleration(double lowSpeedAcceleration, double speed) {
		double speedKmh = speed * 72.0;
		if (speedKmh <= 30.0) {
			return lowSpeedAcceleration;
		}

		return lowSpeedAcceleration * 30.0 / speedKmh;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("manualminecart")) {
			return false;
		}

		if (args.length == 0) {
			sender.sendMessage("ManualMinecart 1.0.0");
			return true;
		}

		if (args.length != 1) {
			return false;
		}

		if (args[0].equalsIgnoreCase("help")) {
			sender.sendMessage("右クリック: 力行側 / 左クリック: 制動側");
			sender.sendMessage("非常 - B3 - B2 - B1 - N - P1 - P2 - P3");
			return true;
		}

		if (!(sender instanceof Player player)) {
			sender.sendMessage("このコマンドはプレイヤー専用です。");
			return true;
		}

		if (args[0].equalsIgnoreCase("set")) {
			return setMinecart(player);
		}

		if (args[0].equalsIgnoreCase("stick")) {
			giveStick(player);
			return true;
		}

		return false;
	}

	private boolean setMinecart(Player player) {
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof Minecart minecart)) {
			player.sendMessage("トロッコに乗ってください。");
			return true;
		}

		MinecartMember<?> member = MinecartMemberStore.getFromEntity(minecart);
		if (member == null) {
			player.sendMessage("このトロッコはTrainCartsの列車ではありません。");
			return true;
		}

		minecart.getPersistentDataContainer().set(minecartKey, PersistentDataType.INTEGER, 1);

		MinecartGroup group = member.getGroup();
		TrainProperties properties = group.getProperties();
		properties.setSpeedLimit(MAX_SPEED);
		properties.setSlowingDown(false);
		properties.setManualMovementAllowed(false);
		getTrainState(group);

		player.sendMessage("ManualMinecartを設定しました。最高速度: 50 km/h");
		return true;
	}

	private void giveStick(Player player) {
		ItemStack item = new ItemStack(Material.STICK);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("ManualMinecartStick");
		meta.getPersistentDataContainer().set(stickKey, PersistentDataType.INTEGER, 1);
		item.setItemMeta(meta);
		player.getInventory().addItem(item);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("manualminecart") || args.length != 1) {
			return null;
		}

		String input = args[0].toLowerCase(Locale.ROOT);
		List<String> suggestions = new ArrayList<>();
		for (String option : List.of("help", "set", "stick")) {
			if (option.startsWith(input)) {
				suggestions.add(option);
			}
		}
		return suggestions;
	}

	private void loadTrainStates() {
		for (Map<?, ?> data : getConfig().getMapList("trains")) {
			Object name = data.get("name");
			if (!(name instanceof String trainName)) {
				continue;
			}

			int notch = numberValue(data.get("notch"), TrainState.NEUTRAL);
			int direction = numberValue(data.get("direction"), 1);
			trainStates.put(trainName, new TrainState(notch, direction));
		}
	}

	private int numberValue(Object value, int defaultValue) {
		return value instanceof Number number ? number.intValue() : defaultValue;
	}

	private void saveTrainStates() {
		List<Map<String, Object>> saved = new ArrayList<>();
		for (Map.Entry<String, TrainState> entry : trainStates.entrySet()) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("name", entry.getKey());
			data.put("notch", entry.getValue().getNotch());
			data.put("direction", entry.getValue().getDirection());
			saved.add(data);
		}

		getConfig().set("trains", saved);
		saveConfig();
	}
}