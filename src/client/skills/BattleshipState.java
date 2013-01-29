package client.skills;

public class BattleshipState {
	private int hp = 0;

	public BattleshipState() {
		this.hp = 0;
	}
	
	public int getHp() {
		return this.hp;
	}
	
	public void setHp(int hp) {
		this.hp = hp;
	}
	
	public void resetHp(int skillLevel, int characterLevel) {
		this.hp = 4000 * skillLevel + ((characterLevel - 120) * 2000);
	}
	
	public int decreaseHp(int amount) {
		int newHp = this.hp - amount;
		this.hp = newHp;
		return newHp;
	}
}
