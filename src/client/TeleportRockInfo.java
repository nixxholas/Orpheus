package client;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeleportRockInfo {
	
	private final List<Integer> regular = new ArrayList<Integer>(5);
	private final List<Integer> vip = new ArrayList<Integer>(10);
	
	public TeleportRockInfo() {
	}
	
	public TeleportRockInfo(ResultSet rs) throws SQLException {
		while (rs.next()) {
			int mapId = rs.getInt("mapid");
			if (rs.getInt("vip") == 1) {
				this.vip.add(mapId);
			} else {
				this.regular.add(mapId);
			}
		}
	}
	
	public List<Integer> getRegularMaps() {
		return Collections.unmodifiableList(this.regular);
	}

	public List<Integer> getVipMaps() {
		return Collections.unmodifiableList(this.vip);
	}

	public void addRegular(int mapId) {
		if (this.regular.size() >= 5) {
			return;
		}
		this.regular.add(Integer.valueOf(mapId));
	}
	
	public void deleteRegular(int mapId) {
		this.regular.remove(Integer.valueOf(mapId));
	}

	public void addVip(int mapId) {
		if (this.vip.size() >= 10) {
			return;
		}
		
		this.vip.add(Integer.valueOf(mapId));
	}
	
	public void deleteVip(int mapId) {
		this.vip.remove(Integer.valueOf(mapId));
	}
}
