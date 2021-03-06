/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import tools.DatabaseConnection;
import tools.PacketCreator;

public class BuddyList {
	public enum BuddyOperation {
		ADDED, 
		DELETED
	}

	public enum BuddyAddResult {
		BUDDYLIST_FULL, 
		ALREADY_ON_LIST, 
		OK
	}

	private Map<Integer, BuddylistEntry> buddies = new LinkedHashMap<Integer, BuddylistEntry>();
	private int capacity;
	private Deque<SimpleCharacterInfo> pendingRequests = new LinkedList<SimpleCharacterInfo>();

	public BuddyList(int capacity) {
		this.capacity = capacity;
	}

	public boolean contains(int characterId) {
		return buddies.containsKey(Integer.valueOf(characterId));
	}

	public boolean containsVisible(int characterId) {
		BuddylistEntry entry = buddies.get(characterId);
		if (entry == null) {
			return false;
		}
		return entry.isVisible();
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public BuddylistEntry get(int characterId) {
		return buddies.get(Integer.valueOf(characterId));
	}

	public BuddylistEntry get(String characterName) {
		String lowerCaseName = characterName.toLowerCase();
		for (BuddylistEntry entry : buddies.values()) {
			if (entry.getName().toLowerCase().equals(lowerCaseName)) {
				return entry;
			}
		}
		return null;
	}

	public void put(BuddylistEntry entry) {
		buddies.put(Integer.valueOf(entry.getCharacterId()), entry);
	}

	public void remove(int characterId) {
		buddies.remove(Integer.valueOf(characterId));
	}

	public Collection<BuddylistEntry> getBuddies() {
		return buddies.values();
	}

	public boolean isFull() {
		return buddies.size() >= capacity;
	}

	public int[] getBuddyIds() {
		int buddyIds[] = new int[buddies.size()];
		int i = 0;
		for (BuddylistEntry entry : buddies.values()) {
			buddyIds[i++] = entry.getCharacterId();
		}
		return buddyIds;
	}

	public void loadFromDb(int characterId) throws SQLException {
		final Connection connection = DatabaseConnection.getConnection();
		try (PreparedStatement ps = getSelectBuddiesByCharacterId(connection, characterId);
			ResultSet rs = ps.executeQuery();) {
		
			while (rs.next()) {
				if (rs.getInt("pending") == 1) {
					pendingRequests.push(new SimpleCharacterInfo(rs.getInt("buddyid"), rs.getString("buddyname")));
				} else {
					put(new BuddylistEntry(rs.getString("buddyname"), rs.getString("group"), rs.getInt("buddyid"), (byte) -1, true));
				}
			}
			
		}
		
		try (PreparedStatement ps = getSelectPendingBuddyRequests(connection, characterId)) {
			ps.executeUpdate();
		}
	}

	private PreparedStatement getSelectPendingBuddyRequests(final Connection connection, int characterId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("DELETE FROM `buddies` WHERE `pending` = 1 AND `characterid` = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private PreparedStatement getSelectBuddiesByCharacterId(final Connection connection, int characterId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT b.`buddyid`, b.`pending`, b.`group`, c.`name` AS `buddyname` FROM `buddies` as b, `characters` as c WHERE c.`id` = b.`buddyid` AND b.`characterid` = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	public SimpleCharacterInfo pollPendingRequest() {
		return pendingRequests.pollLast();
	}

	public void addBuddyRequest(GameClient c, int cidFrom, String nameFrom, byte channelFrom) {
		put(new BuddylistEntry(nameFrom, "Default Group", cidFrom, channelFrom, false));
		if (pendingRequests.isEmpty()) {
			c.getSession().write(PacketCreator.requestBuddylistAdd(cidFrom, c.getPlayer().getId(), nameFrom));
		} else {
			pendingRequests.push(new SimpleCharacterInfo(cidFrom, nameFrom));
		}
	}
}
