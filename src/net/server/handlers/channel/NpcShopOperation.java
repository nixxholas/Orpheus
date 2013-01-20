package net.server.handlers.channel;

public enum NpcShopOperation {
	BUY(0),
	SELL(1),
	RECHARGE(2),
	LEAVE(3);
	
	private byte type;
	private NpcShopOperation(int type) {
		this.type = (byte) type;
	}
	
	public static NpcShopOperation fromByte(byte operation) {
		for (NpcShopOperation item : NpcShopOperation.values()) {
			if (item.type == operation) {
				return item;
			}
		}
		
		return null;
	}
}