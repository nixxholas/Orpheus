package client;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DojoState {
	private int points, stage, energy;
	private boolean hasFinishedTutorial, hasParty;
	private long finishTimestamp;
	
	public DojoState() {
	}
	
	public DojoState(ResultSet rs) throws SQLException {
		this.points = rs.getInt("dojoPoints");
		this.stage = rs.getInt("lastDojoStage");
		this.hasFinishedTutorial = rs.getInt("finishedDojoTutorial") == 1;
	}
	
	public boolean hasFinishedTutorial() {
		return hasFinishedTutorial;
	}
	
	public int getEnergy() {
		return energy;
	}

	public boolean hasParty() {
		return hasParty;
	}

	public int getPoints() {
		return points;
	}

	public int getStage() {
		return stage;
	}
	
	public long getFinishTimestamp() {
		return this.finishTimestamp;
	}
	
	public int addDojoPointsByMap(int mapId) {
		int increase = 0;
		if (this.points < 17000) {
			// TODO: bash head into desk because of this line:
			increase = 1 + ((mapId - 1) / 100 % 100) / 6;
			if (!this.hasParty) {
				increase++;
			}
			this.points += increase;
		}
		return increase;
	}
	
	public void setStage(int stage) {
		this.stage = stage;
	}
	
	public void setEnergy(int x) {
		this.energy = x;
	}

	public void setDojoParty(boolean hasParty) {
		this.hasParty = hasParty;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public void startRun(int mapId) {
		int stage = (mapId / 100) % 100;
		this.finishTimestamp = System.currentTimeMillis() + (stage > 36 ? 15 : stage / 6 + 5) * 60000;
	}
	
	public void setFinishedDojoTutorial() {
		this.hasFinishedTutorial = true;
	}
}
