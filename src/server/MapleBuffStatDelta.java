package server;

import client.BuffStat;

public final class MapleBuffStatDelta {
	public final BuffStat stat;
	public final int delta;
	
	public MapleBuffStatDelta(BuffStat stat, int delta) {
		this.stat = stat;
		this.delta = delta;
	}
}
