package client;

public class WorldRateInfo extends RateInfo {

	private double bossDrop;
	
	public WorldRateInfo() {
		super();
		
		this.bossDrop = 1.0f;
	}
	
	public WorldRateInfo(double exp, double meso, double drop, double bossDrop) {
		super(exp, meso, drop);
		
		this.bossDrop = bossDrop;
	}
	
	public WorldRateInfo(WorldRateInfo other) {
		super(other);
		
		this.bossDrop = other.bossDrop;
	}
	
	public double bossDrop() {
		return this.bossDrop;
	}
	
	public void bossDrop(double value) {
		this.bossDrop = value;
	}
}
