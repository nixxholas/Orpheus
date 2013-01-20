package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RewardInfo {
	
	public final int total;
	private final List<RewardItem> items;
	
	public RewardInfo(List<RewardItem> items) {
		
		int total = 0;
		this.items = new ArrayList<RewardItem>(items.size());
		for (RewardItem item : items) {
			this.items.add(item);
			total += item.probability;
		}
		
		this.total = total;
	}
	
	public List<RewardItem> getRewardItems() {
		return Collections.unmodifiableList(this.items);
	}
}