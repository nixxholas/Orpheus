package server;

import provider.MapleData;
import provider.MapleDataTool;

public final class RewardItem {

	public final int itemId, period;
	public final short probability, quantity;
	public final String effect, notice;

	public RewardItem(MapleData child) {
		this.itemId = MapleDataTool.getInt("item", child, 0);
		this.probability = (byte) MapleDataTool.getInt("prob", child, 0);
		this.quantity = (short) MapleDataTool.getInt("count", child, 0);
		this.effect = MapleDataTool.getString("Effect", child, "");
		this.notice = MapleDataTool.getString("worldMsg", child, null);
		this.period = MapleDataTool.getInt("period", child, -1);
	}
}