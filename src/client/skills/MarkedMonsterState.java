package client.skills;

public class MarkedMonsterState {
	private int monsterId;
	
	public MarkedMonsterState() {
		this.monsterId = 0;
	}

	public int getMonster() {
		return monsterId;
	}
	
	public void setMonster(int id) {
		this.monsterId = id;
	}
}
