package net.sioyama.manualminecart;

public class TrainState {
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
		this.notch = Math.max(EMERGENCY, Math.min(P3, notch));
		this.direction = direction < 0 ? -1 : 1;
	}

	public int getNotch() {
		return notch;
	}

	public int changeNotch(int amount) {
		notch = Math.max(EMERGENCY, Math.min(P3, notch + amount));
		return notch;
	}

	public int getDirection() {
		return direction;
	}

	public void syncDirection(double force) {
		if (Math.abs(force) >= 0.02) {
			direction = force < 0.0 ? -1 : 1;
		}
	}

	public String getNotchName() {
		return switch (notch) {
			case EMERGENCY -> "非常";
			case B3 -> "B3";
			case B2 -> "B2";
			case B1 -> "B1";
			case P1 -> "P1";
			case P2 -> "P2";
			case P3 -> "P3";
			default -> "N";
		};
	}
}