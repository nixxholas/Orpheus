package client;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FameStats {
	private static final int DAY = 3600000 * 24;
	private static final int DAYS_IN_PERIOD = 30;
	private static final int ROLLOVER_PERIOD = DAY * DAYS_IN_PERIOD;
	
	private FameEntry latest;
	private List<FameEntry> recent;

	private class FameEntry {
		public final int characterId;
		public final long timestamp;
		
		public FameEntry(int characterId, long timestamp) {
			this.timestamp = timestamp;
			this.characterId = characterId;
		}
	}
	
	public FameStats(ResultSet rs) throws SQLException {
		this.recent = new ArrayList<FameEntry>(DAYS_IN_PERIOD + 1);
		
		while (rs.next()) {
			final int target = rs.getInt("characterid_to");
			final long timestamp = rs.getTimestamp("when").getTime();
			
			final FameEntry entry = new FameEntry(target, timestamp);
			if (this.latest == null || this.latest.timestamp < timestamp) {
				this.latest = entry;
			}
			
			this.recent.add(entry);
		}	
	}
	
	public FameStatus canGiveFame(int targetId) {
		// NOTE: Hey, play fair.
//		if (gmLevel > 0) {
//			return FameStatus.OK;
//		} 
		
		if (this.latest == null || isWithin(this.latest.timestamp, DAY)) {
			return FameStatus.NOT_TODAY;
		} 
		
		for (FameEntry entry : this.recent) {
			if (entry.characterId == targetId && isWithin(entry.timestamp, ROLLOVER_PERIOD)) {
				return FameStatus.NOT_THIS_MONTH;
			}
		}

		return FameStatus.OK;
	}
	
	private static boolean isWithin(long timestamp, long period) {
		return timestamp >= System.currentTimeMillis() - period;
	}
	
	public void addEntry(int targetId, long timestamp) {
		this.addEntry(targetId, timestamp, true);
	}
	
	public void addEntry(int targetId, long timestamp, boolean cleanUpOld) {
		final FameEntry newEntry = new FameEntry(targetId, timestamp);
		this.recent.add(newEntry);
		
		if (this.latest.timestamp < timestamp) {
			this.latest = newEntry;
		}
		
		if (!cleanUpOld) {
			return;
		}

		final List<FameEntry> stillRecent = new ArrayList<FameEntry>(DAYS_IN_PERIOD);
		for (FameEntry recentEntry : this.recent) {
			if (isWithin(recentEntry.timestamp, ROLLOVER_PERIOD)) {
				stillRecent.add(recentEntry);
			}
		}
		
		this.recent.clear();
		this.recent = stillRecent;		
	}
}
