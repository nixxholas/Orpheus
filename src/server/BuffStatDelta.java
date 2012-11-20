package server;

import client.BuffStat;

public final class BuffStatDelta {
	public final BuffStat stat;
	public final int delta;
	
	public BuffStatDelta(BuffStat stat, int delta) {
		this.stat = stat;
		this.delta = delta;
	}
}
