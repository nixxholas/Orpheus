package client;

public class RateInfo {
	private double exp;
	private double meso;
	private double drop;
	
	public RateInfo() {
		this.exp = 1.0;
		this.meso = 1.0;
		this.drop = 1.0;
	}
	
	public RateInfo(double exp, double meso, double drop) {
		this.exp = exp;
		this.meso = meso;
		this.drop = drop;
	}
	
	public RateInfo(RateInfo other) {
		this.exp = other.exp;
		this.meso = other.meso;
		this.drop = other.drop;
	}
	
	public double exp() {
		return exp;
	}
	
	public void exp(double value) {
		this.exp = value;
	}
	
	public double meso() {
		return meso;		
	}
	
	public void meso(double value) {
		this.meso = value;
	}
	
	public double drop() {
		return drop;
	}
	
	public void drop(double value) {
		this.drop = value;
	}
}
