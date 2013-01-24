package client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RingsInfo {
	private List<Ring> crushRings = new ArrayList<Ring>();
	private List<Ring> friendshipRings = new ArrayList<Ring>();
	private Ring marriageRing;
	
	public void addCrushRing(Ring r) {
		crushRings.add(r);
	}

	public void addFriendshipRing(Ring r) {
		friendshipRings.add(r);
	}

	public List<Ring> getFriendshipRings() {
		Collections.sort(friendshipRings);
		return friendshipRings;
	}

	public List<Ring> getCrushRings() {
		Collections.sort(crushRings);
		return crushRings;
	}

	public Ring getMarriageRing() {
		return marriageRing;
	}
	
	public void setMarriageRing(Ring ring) {
		this.marriageRing = ring;
	}
	
	public Ring getRingById(int id) {
		for (Ring ring : getCrushRings()) {
			if (ring.getRingId() == id) {
				return ring;
			}
		}
		for (Ring ring : getFriendshipRings()) {
			if (ring.getRingId() == id) {
				return ring;
			}
		}
		if (getMarriageRing().getRingId() == id) {
			return getMarriageRing();
		}

		return null;
	}
}
