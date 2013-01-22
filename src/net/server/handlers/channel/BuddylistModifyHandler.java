/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss <aaron@deviant-core.net>
    				Patrick Huy <patrick.huy@frz.cc>
					Matthias Butz <matze@odinms.de>
					Jan Christian Meyer <vimes@odinms.de>

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
package net.server.handlers.channel;

import static client.BuddyList.BuddyOperation.ADDED;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import client.BuddyList;
import client.BuddylistEntry;
import client.SimpleCharacterInfo;
import client.GameCharacter;
import client.GameClient;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import tools.DatabaseCall;
import tools.DatabaseConnection;
import net.AbstractPacketHandler;
import net.server.World;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class BuddylistModifyHandler extends AbstractPacketHandler {

	private static class BuddyCapacityInfo extends SimpleCharacterInfo {
		public final int buddyCapacity;

		public BuddyCapacityInfo(int id, String name, int buddyCapacity) {
			super(id, name);
			this.buddyCapacity = buddyCapacity;
		}
	}

	private void nextPendingRequest(GameClient c) {
		SimpleCharacterInfo pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
		if (pendingBuddyRequest != null) {
			c.announce(PacketCreator.requestBuddylistAdd(pendingBuddyRequest.id, c.getPlayer().getId(), pendingBuddyRequest.name));
		}
	}

	private BuddyCapacityInfo getCharacterInfo(String name) throws SQLException {
		Connection connection = DatabaseConnection.getConnection();
		try (DatabaseCall call = DatabaseCall.query(getSelectCharacterByName(connection, name))) {
			
			ResultSet rs = call.resultSet();			
			if (rs.next()) {
				final BuddyCapacityInfo result = 
						new BuddyCapacityInfo(rs.getInt("id"), rs.getString("name"), rs.getInt("buddyCapacity"));
				return result;
			} else {
				return null;
			}
		}
	}

	private PreparedStatement getSelectCharacterByName(Connection connection, String name) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `id`, `name`, `buddyCapacity` FROM `characters` WHERE `name` LIKE ?");
		ps.setString(1, name);
		return ps;
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int mode = reader.readByte();
		GameCharacter player = c.getPlayer();
		BuddyList buddylist = player.getBuddylist();
		if (mode == 1) { // add
			String addName = reader.readMapleAsciiString();
			String group = reader.readMapleAsciiString();
			if (group.length() > 16 || addName.length() < 4 || addName.length() > 13) {
				return; // hax.
			}
			BuddylistEntry entry = buddylist.get(addName);
			if (entry != null && !entry.isVisible() && group.equals(entry.getGroup())) {
				c.announce(PacketCreator.serverNotice(1, "You already have \"" + entry.getName() + "\" on your Buddylist"));
			} else if (buddylist.isFull() && entry == null) {
				c.announce(PacketCreator.serverNotice(1, "Your buddylist is already full"));
			} else if (entry == null) {
				try {
					World world = c.getWorldServer();
					BuddyCapacityInfo charWithId = null;
					byte channel;
					GameCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
					if (otherChar != null) {
						channel = c.getChannelId();
						charWithId = new BuddyCapacityInfo(otherChar.getId(), otherChar.getName(), otherChar.getBuddylist().getCapacity());
					} else {
						channel = world.find(addName);
						charWithId = getCharacterInfo(addName);
					}
					if (charWithId != null) {
						BuddyAddResult buddyAddResult = null;
						if (channel != -1) {
							buddyAddResult = world.requestBuddyAdd(addName, c.getChannelId(), player.getId(), player.getName());
						} else {
							Connection connection = DatabaseConnection.getConnection();
							try (DatabaseCall call = DatabaseCall.query(getSelectPendingBuddyCount(connection, charWithId))) {
								
								ResultSet rs = call.resultSet();
								
								if (!rs.next()) {
									throw new RuntimeException("Result set expected");
								} else if (rs.getInt("buddyCount") >= charWithId.buddyCapacity) {
									buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
								}
							}
							
							try (DatabaseCall call = DatabaseCall.query(getSelectPendingBuddy(connection, player, charWithId))) {
								
								ResultSet rs = call.resultSet();
								
								if (rs.next()) {
									buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
								}
							}
						}
						
						if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
							c.announce(PacketCreator.serverNotice(1, "\"" + addName + "\"'s Buddylist is full"));
						} else {
							byte displayChannel = -1;
							int otherCid = charWithId.id;
							if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
								displayChannel = channel;
								notifyRemoteChannel(c, channel, otherCid, ADDED);
							} else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
								Connection connection = DatabaseConnection.getConnection();
								try (PreparedStatement ps = getInsertPendingBunny(connection, player, charWithId)) {
									ps.executeUpdate();
								}
							}
							buddylist.put(new BuddylistEntry(charWithId.name, group, otherCid, displayChannel, true));
							c.announce(PacketCreator.updateBuddylist(buddylist.getBuddies()));
						}
					} else {
						c.announce(PacketCreator.serverNotice(1, "A character called \"" + addName + "\" does not exist"));
					}
				} catch (SQLException e) {
				}
			} else {
				entry.changeGroup(group);
				c.announce(PacketCreator.updateBuddylist(buddylist.getBuddies()));
			}
		} else if (mode == 2) { // accept buddy
			int otherCid = reader.readInt();
			if (!buddylist.isFull()) {
				try {
					byte channel = c.getWorldServer().find(otherCid);// worldInterface.find(otherCid);
					String otherName = null;
					GameCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(otherCid);
					if (otherChar == null) {
						Connection connection = DatabaseConnection.getConnection();
						try (DatabaseCall call = DatabaseCall.query(getSelectCharacterNameById(connection, otherCid))) {
							
							ResultSet rs = call.resultSet();

							if (rs.next()) {
								otherName = rs.getString("name");
							}
						}
					} else {
						otherName = otherChar.getName();
					}
					if (otherName != null) {
						buddylist.put(new BuddylistEntry(otherName, "Default Group", otherCid, channel, true));
						c.announce(PacketCreator.updateBuddylist(buddylist.getBuddies()));
						notifyRemoteChannel(c, channel, otherCid, ADDED);
					}
				} catch (SQLException e) {
				}
			}
			nextPendingRequest(c);
		} else if (mode == 3) { // delete
			int otherCid = reader.readInt();
			if (buddylist.containsVisible(otherCid)) {
				notifyRemoteChannel(c, c.getWorldServer().find(otherCid), otherCid, BuddyOperation.DELETED);
			}
			buddylist.remove(otherCid);
			c.announce(PacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
			nextPendingRequest(c);
		}
	}

	private PreparedStatement getSelectCharacterNameById(Connection connection, int characterId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `name` FROM `characters` WHERE `id` = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private PreparedStatement getInsertPendingBunny(Connection connection, GameCharacter player, BuddyCapacityInfo info)
			throws SQLException {
		PreparedStatement ps = connection.prepareStatement("INSERT INTO `buddies` (`characterid`, `buddyid`, `pending`) VALUES (?, ?, 1)");
		ps.setInt(1, info.id);
		ps.setInt(2, player.getId());
		return ps;
	}

	private PreparedStatement getSelectPendingBuddy(Connection connection, GameCharacter player, BuddyCapacityInfo info)
			throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `pending` FROM `buddies` WHERE `characterid` = ? AND `buddyid` = ?");
		ps.setInt(1, info.id);
		ps.setInt(2, player.getId());
		return ps;
	}

	private PreparedStatement getSelectPendingBuddyCount(Connection connection, BuddyCapacityInfo info)
			throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) AS `buddyCount` FROM `buddies` WHERE `characterid` = ? AND `pending` = 0");
		ps.setInt(1, info.id);
		return ps;
	}

	private void notifyRemoteChannel(GameClient c, int remoteChannel, int targetId, BuddyOperation operation) {
		GameCharacter player = c.getPlayer();
		if (remoteChannel != -1) {
			c.getWorldServer().buddyChanged(targetId, player.getId(), player.getName(), c.getChannelId(), operation);
		}
	}
}
