package client;

public final class StatDelta {
	public final Stat stat;
	public final int delta;
	
	public StatDelta(Stat stat, int delta) {
		this.stat = stat;
		this.delta = delta;
	}
}
