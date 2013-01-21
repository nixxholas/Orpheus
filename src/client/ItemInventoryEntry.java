package client;

public final class ItemInventoryEntry {
	
	public final IItem item;
	public final InventoryType type;
	
	public ItemInventoryEntry(IItem item, InventoryType type) {
		this.item = item;
		this.type = type;
	}
}
