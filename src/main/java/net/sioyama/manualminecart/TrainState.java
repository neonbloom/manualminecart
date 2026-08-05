package net.sioyama.manualminecart;

public final class TrainState {
    public static final int EMERGENCY = -4;
    public static final int B3 = -3;
    public static final int B2 = -2;
    public static final int B1 = -1;
    public static final int NEUTRAL = 0;
    public static final int P1 = 1;
    public static final int P2 = 2;
    public static final int P3 = 3;

    private int notch;
    private int direction;

    public TrainState() {
        this(NEUTRAL, 1);
    }

    public TrainState(int notch, int direction) {
        this.notch = clampNotch(notch);
        this.direction = direction < 0 ? -1 : 1;
    }

    public int getNotch() {
        return notch;
    }

    public void changeNotch(int amount) {
        notch = clampNotch(notch + amount);
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction < 0 ? -1 : 1;
    }

    public void syncDirection(double force) {
        if (Math.abs(force) >= 0.02) {
            direction = force < 0.0 ? -1 : 1;
        }
    }

    public String getDisplayName() {
        return switch (notch) {
            case EMERGENCY -> "非　常";
            case B3 -> "制動３";
            case B2 -> "制動２";
            case B1 -> "制動１";
            case P1 -> "力行１";
            case P2 -> "力行２";
            case P3 -> "力行３";
            default -> "惰　性";
        };
    }

    private static int clampNotch(int notch) {
        return Math.max(EMERGENCY, Math.min(P3, notch));
    }
}
