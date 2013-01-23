package client;

public class StockEntry {
	public final int count;
	public final int value;
	public final int change;
	
	public StockEntry(int count, int value, int change) {
		this.count = count;
		this.value = value;
		this.change = change;
	}
}
