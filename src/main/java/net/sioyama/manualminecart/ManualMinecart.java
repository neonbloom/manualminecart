package net.sioyama.manualminecart;

import com.bergerkiller.bukkit.tc.controller.MinecartGroup;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.controller.MinecartMemberStore;
import com.bergerkiller.bukkit.tc.properties.TrainProperties;
import com.bergerkiller.bukkit.tc.properties.TrainPropertiesStore;
import com.bergerkiller.bukkit.tc.rails.type.RailType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.util.Vector;

public final class ManualMinecart extends JavaPlugin {
    private static final double MAX_SPEED_KMH = 50.0;
    private static final double MAX_SPEED = MAX_SPEED_KMH / 72.0;
    private static final double STOPPED_SPEED = 0.0001;
    private static final double TICKS_PER_SECOND_SQUARED = 20.0 * 20.0;

    private final Map<String, TrainState> trainStates = new HashMap<>();
    private NamespacedKey minecartKey;
    private NamespacedKey stickKey;
    private BukkitTask movementTask;
    private RailType manualPoweredRail;
    private RailType manualUnpoweredRail;

    @Override
    public void onEnable() {
        minecartKey = new NamespacedKey(this, "manualminecart");
        stickKey = new NamespacedKey(this, "manualminecartstick");
        loadTrainStates();

        // Replace TrainCarts' powered-rail handlers with variants that keep the
        // normal rail geometry but skip boost/brake physics for MMC trains.
        // Non-MMC trains still delegate to TrainCarts' original behavior.
        manualPoweredRail = new ManualPoweredRailType(this, true);
        manualUnpoweredRail = new ManualPoweredRailType(this, false);
        RailType.register(manualPoweredRail, true);
        RailType.register(manualUnpoweredRail, true);

        getServer().getPluginManager().registerEvents(new InputNotch(this), this);
        movementTask = getServer().getScheduler().runTaskTimer(this, this::tickTrains, 1L, 1L);
        getLogger().info("ManualMinecart started.");
    }

    @Override
    public void onDisable() {
        if (movementTask != null) {
            movementTask.cancel();
        }
        if (manualPoweredRail != null) {
            RailType.unregister(manualPoweredRail);
        }
        if (manualUnpoweredRail != null) {
            RailType.unregister(manualUnpoweredRail);
        }
        saveTrainStates();
    }

    NamespacedKey getMinecartKey() {
        return minecartKey;
    }

    NamespacedKey getStickKey() {
        return stickKey;
    }

    boolean isManualMinecart(Minecart minecart) {
        Integer value = minecart.getPersistentDataContainer().get(
                minecartKey, PersistentDataType.INTEGER);
        return Integer.valueOf(1).equals(value);
    }

    boolean isManualGroup(MinecartGroup group) {
        if (group == null) {
            return false;
        }
        for (MinecartMember<?> member : group) {
            Entity entity = member.getEntity().getEntity();
            if (entity instanceof Minecart minecart && isManualMinecart(minecart)) {
                return true;
            }
        }
        return false;
    }

    TrainState getTrainState(MinecartGroup group) {
        String trainName = group.getProperties().getTrainName();
        return trainStates.computeIfAbsent(trainName, ignored -> new TrainState());
    }

    void setDirectionFromView(
            MinecartGroup group, MinecartMember<?> member, TrainState state, Player player) {
        // Direction changes are only allowed while stopped. Clear the tiny
        // residual velocity that can remain in TrainCarts at displayed 0 km/h.
        if (Math.abs(group.getAverageForce()) > STOPPED_SPEED) {
            return;
        }
        group.stop();

        Vector view = player.getEyeLocation().getDirection().setY(0.0);
        Vector trainForward = getGroupForwardAtMember(group, member);
        if (view.lengthSquared() < 0.0001 || trainForward.lengthSquared() < 0.0001) {
            return;
        }

        // Reverse TrainCarts' own head/tail and per-member direction state while
        // the train is stopped. Positive force can then be used on every tick.
        // Repeating this procedure at the next stop makes any number of
        // reversals possible without alternating negative force every tick.
        if (view.dot(trainForward) < 0.0) {
            group.reverse();
        }
        state.setDirection(1);
    }

    private Vector getGroupForwardAtMember(MinecartGroup group, MinecartMember<?> member) {
        int index = group.indexOf(member);
        if (index < 0 || group.size() == 1) {
            // A minecart's visual orientation does not change when TrainCarts
            // reverses a one-cart train. Its remembered rail direction does.
            // Using the visual orientation here is what made the first reversal
            // work and every later reversal choose the wrong side.
            return member.getDirection().getDirection().setY(0.0);
        }

        Vector memberPosition = member.getEntity().getLocation().toVector();
        Vector groupForward;
        if (index == 0) {
            Vector behindPosition = group.get(1).getEntity().getLocation().toVector();
            groupForward = memberPosition.subtract(behindPosition);
        } else {
            Vector aheadPosition = group.get(index - 1).getEntity().getLocation().toVector();
            groupForward = aheadPosition.subtract(memberPosition);
        }
        return groupForward.setY(0.0);
    }

    private void tickTrains() {
        Map<MinecartGroup, TrainState> activeGroups = new IdentityHashMap<>();
        Map<MinecartGroup, String> activeNames = new IdentityHashMap<>();
        List<String> staleAliases = new ArrayList<>();

        Iterator<Map.Entry<String, TrainState>> iterator = trainStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TrainState> entry = iterator.next();
            TrainProperties properties = TrainPropertiesStore.get(entry.getKey());
            if (properties == null) {
                iterator.remove();
                continue;
            }

            MinecartGroup group = properties.getHolder();
            if (group != null && !group.isEmpty()) {
                String activeName = group.getProperties().getTrainName();
                boolean canonicalState = entry.getKey().equals(activeName);
                if (canonicalState || !activeGroups.containsKey(group)) {
                    activeGroups.put(group, entry.getValue());
                    activeNames.put(group, activeName);
                }
                if (!canonicalState) {
                    staleAliases.add(entry.getKey());
                }
            }
        }

        for (String alias : staleAliases) {
            trainStates.remove(alias);
        }
        for (Map.Entry<MinecartGroup, TrainState> entry : activeGroups.entrySet()) {
            String activeName = activeNames.get(entry.getKey());
            TrainState state = trainStates.computeIfAbsent(activeName, ignored -> entry.getValue());
            moveTrain(entry.getKey(), state);
        }
    }

    private void moveTrain(MinecartGroup group, TrainState state) {
        if (isGroupDerailed(group)) {
            if (!state.isDerailed()) {
                // Cut traction once at the moment of derailment. Do not keep
                // calling stop(), because a derailed cart must still be able to
                // fall under gravity on later ticks.
                group.stop();
                state.setNeutral();
                state.setDerailed(true);
            }
            showTrainStatus(group, state, 0.0);
            return;
        }
        state.setDerailed(false);

        double speed = Math.abs(group.getAverageForce());
        double acceleration = switch (state.getNotch()) {
            case TrainState.P1 -> powerAcceleration(0.35, speed);
            case TrainState.P2 -> powerAcceleration(0.62, speed);
            case TrainState.P3 -> powerAcceleration(0.90, speed);
            case TrainState.B1 -> -0.45;
            case TrainState.B2 -> -0.75;
            case TrainState.B3 -> -1.05;
            case TrainState.EMERGENCY -> -1.35;
            default -> -(0.035 + speed * 20.0 * 0.0015);
        };

        double nextSpeed = speed + acceleration / TICKS_PER_SECOND_SQUARED;
        if (state.getNotch() > TrainState.NEUTRAL) {
            nextSpeed = Math.min(MAX_SPEED, nextSpeed);
        }

        if (nextSpeed <= STOPPED_SPEED) {
            group.stop();
        } else {
            group.setForwardForce(nextSpeed);
        }

        showTrainStatus(group, state, Math.max(0.0, nextSpeed));
    }

    private boolean isGroupDerailed(MinecartGroup group) {
        for (MinecartMember<?> member : group) {
            if (member.isDerailed()) {
                return true;
            }
        }
        return false;
    }

    private void showTrainStatus(MinecartGroup group, TrainState state, double speed) {
        long speedKmh = Math.round(speed * 72.0);
        String message = state.getDisplayName() + "　" + toFullWidth(speedKmh) + "ｋｍ／ｈ";

        for (Player player : getServer().getOnlinePlayers()) {
            Entity vehicle = player.getVehicle();
            if (!(vehicle instanceof Minecart minecart)) {
                continue;
            }

            MinecartMember<?> member = MinecartMemberStore.getFromEntity(minecart);
            if (member != null && member.getGroup() == group) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent(message));
            }
        }
    }

    private String toFullWidth(long number) {
        String halfWidth = Long.toString(number);
        StringBuilder fullWidth = new StringBuilder(halfWidth.length());
        for (int i = 0; i < halfWidth.length(); i++) {
            fullWidth.append((char) (halfWidth.charAt(i) - '0' + '０'));
        }
        return fullWidth.toString();
    }

    private double powerAcceleration(double lowSpeedAcceleration, double speed) {
        double speedKmh = speed * 72.0;
        if (speedKmh <= 30.0) {
            return lowSpeedAcceleration;
        }
        return lowSpeedAcceleration * 30.0 / speedKmh;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("manualminecart")) {
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
            sender.sendMessage("右クリック: 力行 / 左クリック: 制動");
            sender.sendMessage("EB - B3 - B2 - B1 - N - P1 - P2 - P3");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤー専用です。");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            setMinecart(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("stick")) {
            giveStick(player);
            return true;
        }
        return false;
    }

    private void setMinecart(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Minecart minecart)) {
            player.sendMessage("トロッコに乗ってください。");
            return;
        }

        MinecartMember<?> member = MinecartMemberStore.getFromEntity(minecart);
        if (member == null || member.getGroup() == null) {
            player.sendMessage("このトロッコはTrainCartsの列車ではありません。");
            return;
        }

        minecart.getPersistentDataContainer().set(
                minecartKey, PersistentDataType.INTEGER, 1);

        MinecartGroup group = member.getGroup();
        TrainProperties properties = group.getProperties();
        properties.setSpeedLimit(MAX_SPEED);
        properties.setSlowingDown(false);
        properties.setManualMovementAllowed(false);
        getTrainState(group);

        player.sendMessage("ManualMinecartを設定しました。最高速度: ５０ｋｍ／ｈ");
    }

    private void giveStick(Player player) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("ManualMinecartStick");
        meta.getPersistentDataContainer().set(
                stickKey, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("manualminecart") || args.length != 1) {
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
