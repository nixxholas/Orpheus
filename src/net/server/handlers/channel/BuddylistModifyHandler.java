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
import client.CharacterNameAndId;
import client.GameCharacter;
import client.GameClient;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import tools.DatabaseConnection;
import net.AbstractMaplePacketHandler;
import net.server.World;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class BuddylistModifyHandler extends AbstractMaplePacketHandler {

	private static class CharacterIdNameBuddyCapacity extends CharacterNameAndId {
		public final int buddyCapacity;

		public CharacterIdNameBuddyCapacity(int id, String name, int buddyCapacity) {
			super(id, name);
			this.buddyCapacity = buddyCapacity;
		}
	}

	private void nextPendingRequest(GameClient c) {
		CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
		if (pendingBuddyRequest != null) {
			c.announce(PacketCreator.requestBuddylistAdd(pendingBuddyRequest.id, c.getPlayer().getId(), pendingBuddyRequest.name));
		}
	}

	private CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(String name) throws SQLException {
		Connection con = DatabaseConnection.getConnection();
		try(PreparedStatement ps = getSelectCharacterByName(name, con);
				ResultSet rs = ps.executeQuery();) {
			
			CharacterIdNameBuddyCapacity ret = null;
			if (rs.next()) {
				ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("buddyCapacity"));
				return ret;
			} else {
				return null;
			}
		}
	}

	private PreparedStatement getSelectCharacterByName(String name,
			Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?");
		ps.setString(1, name);
		return ps;
	}

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int mode = slea.readByte();
		GameCharacter player = c.getPlayer();
		BuddyList buddylist = player.getBuddylist();
		if (mode == 1) { // add
			String addName = slea.readMapleAsciiString();
			String group = slea.readMapleAsciiString();
			if (group.length() > 16 || addName.length() < 4 || addName.length() > 13) {
				return; // hax.
			}
			BuddylistEntry ble = buddylist.get(addName);
			if (ble != null && !ble.isVisible() && group.equals(ble.getGroup())) {
				c.announce(PacketCreator.serverNotice(1, "You already have \"" + ble.getName() + "\" on your Buddylist"));
			} else if (buddylist.isFull() && ble == null) {
				c.announce(PacketCreator.serverNotice(1, "Your buddylist is already full"));
			} else if (ble == null) {
				try {
					World world = c.getWorldServer();
					CharacterIdNameBuddyCapacity charWithId = null;
					byte channel;
					GameCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
					if (otherChar != null) {
						channel = c.getChannel();
						charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(), otherChar.getBuddylist().getCapacity());
					} else {
						channel = world.find(addName);
						charWithId = getCharacterIdAndNameFromDatabase(addName);
					}
					if (charWithId != null) {
						BuddyAddResult buddyAddResult = null;
						if (channel != -1) {
							buddyAddResult = world.requestBuddyAdd(addName, c.getChannel(), player.getId(), player.getName());
						} else {
							Connection con = DatabaseConnection.getConnection();
							try(PreparedStatement ps = getSelectPendingBuddyCount(con, charWithId);
									ResultSet rs = ps.executeQuery();) {
							
								if (!rs.next()) {
									throw new RuntimeException("Result set expected");
								} else if (rs.getInt("buddyCount") >= charWithId.buddyCapacity) {
									buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
								}
							}
							
							try (PreparedStatement ps = getSelectPendingBuddy(con, player, charWithId);
									ResultSet rs = ps.executeQuery();) {
								
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
								Connection con = DatabaseConnection.getConnection();
								try (PreparedStatement ps = getInsertPendingBunny(con, player, charWithId)) {
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
				ble.changeGroup(group);
				c.announce(PacketCreator.updateBuddylist(buddylist.getBuddies()));
			}
		} else if (mode == 2) { // accept buddy
			int otherCid = slea.readInt();
			if (!buddylist.isFull()) {
				try {
					byte channel = c.getWorldServer().find(otherCid);// worldInterface.find(otherCid);
					String otherName = null;
					GameCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(otherCid);
					if (otherChar == null) {
						Connection con = DatabaseConnection.getConnection();
						try (PreparedStatement ps = getSelectCharacterNameById(con, otherCid);
								ResultSet rs = ps.executeQuery();) {

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
			int otherCid = slea.readInt();
			if (buddylist.containsVisible(otherCid)) {
				notifyRemoteChannel(c, c.getWorldServer().find(otherCid), otherCid, BuddyOperation.DELETED);
			}
			buddylist.remove(otherCid);
			c.announce(PacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
			nextPendingRequest(c);
		}
	}

	private PreparedStatement getSelectCharacterNameById(Connection con,
			int otherCid) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?");
		ps.setInt(1, otherCid);
		return ps;
	}

	private PreparedStatement getInsertPendingBunny(Connection con,
			GameCharacter player, CharacterIdNameBuddyCapacity charWithId)
			throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 1)");
		ps.setInt(1, charWithId.id);
		ps.setInt(2, player.getId());
		return ps;
	}

	private PreparedStatement getSelectPendingBuddy(Connection con,
			GameCharacter player, CharacterIdNameBuddyCapacity charWithId)
			throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
		ps.setInt(1, charWithId.id);
		ps.setInt(2, player.getId());
		return ps;
	}

	private PreparedStatement getSelectPendingBuddyCount(
			Connection con, CharacterIdNameBuddyCapacity charWithId)
			throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
		ps.setInt(1, charWithId.id);
		return ps;
	}

	private void notifyRemoteChannel(GameClient c, int remoteChannel, int otherCid, BuddyOperation operation) {
		GameCharacter player = c.getPlayer();
		if (remoteChannel != -1) {
			c.getWorldServer().buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation);
		}
	}
}
