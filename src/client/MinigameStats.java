package client;

import tools.data.output.PacketWriter;

public class MinigameStats {
	private int winCount;
	private int tieCount;
	private int lossCount;
	private int points;
	
	public MinigameStats() {
		this.winCount = 0;
		this.lossCount = 0;
		this.tieCount = 0;
		this.points = 2000;
	}
	
	public MinigameStats(int wins, int losses, int ties, int points) {
		this.winCount = wins;
		this.lossCount = losses;
		this.tieCount = ties;
		this.points = points;
	}
	
	public void countWin() {
		this.winCount ++;
	}
		
	public void countTie() {
		this.tieCount ++;
	}
	
	public void countLoss() {
		this.lossCount ++;
	}
	
	public int getWins() {
		return this.winCount;
	}
	
	public int getTies() {
		return this.tieCount;
	}
	
	public int getLosses() {
		return this.lossCount;
	}
	
	public int getPoints() {
		return this.points;
	}
	
	public void serialize(PacketWriter writer) {
		writer.writeInt(this.winCount);
		writer.writeInt(this.tieCount);
		writer.writeInt(this.lossCount);
		writer.writeInt(this.points);
	}
	
	public static void processOwnerWin(MinigameStats ownerStats, MinigameStats visitorStats) {
		ownerStats.countWin();
		visitorStats.countLoss();
	}
	
	public static void processOwnerLoss(MinigameStats ownerStats, MinigameStats visitorStats) {
		ownerStats.countLoss();
		visitorStats.countWin();
	}
	
	public static void processTie(MinigameStats ownerStats, MinigameStats visitorStats) {
		ownerStats.countTie();
		visitorStats.countTie();
	}
}
