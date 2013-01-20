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
		switch (operation) {
		case 0:
			return BUY;
		case 1:
			return SELL;
		case 2:
			return RECHARGE;
		case 3:
			return LEAVE;
		default:
			return null;
		}
	}
}