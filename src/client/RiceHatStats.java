package client;

public class RiceHatStats {

	private int givenRiceCakes;
	private boolean gottenRiceHat;

	public int getGivenRiceCakes() {
		return givenRiceCakes;
	}

	public void increaseGivenRiceCakes(int amount) {
		this.givenRiceCakes += amount;
	}

	public boolean getGottenRiceHat() {
		return gottenRiceHat;
	}

	public void setGottenRiceHat(boolean b) {
		this.gottenRiceHat = b;
	}
	
}
