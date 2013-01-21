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
package net.server.guild;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import client.GameCharacter;
import client.GameClient;
import java.util.LinkedList;

import tools.DatabaseCall;
import tools.DatabaseConnection;
import tools.Output;
import net.GamePacket;
import net.server.Channel;
import net.server.Server;
import tools.PacketCreator;

public class Guild {
	public final static int CREATE_GUILD_COST = 1500000;
	public final static int CHANGE_EMBLEM_COST = 5000000;

	private final List<GuildCharacter> members = new ArrayList<GuildCharacter>();
	private final Map<Byte, List<Integer>> notifications = new LinkedHashMap<Byte, List<Integer>>();

	// 1 = master, 2 = jr, 5 = lowest member
	private String rankTitles[] = new String[5]; 

	private String name, notice;
	private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor,
			signature, allianceId;
	private byte worldId;
	private boolean isDirty = true;
	
	private Guild(int id) {
		this.id = id;
	}
	
	public static Guild loadFromDb(GuildCharacter initiator) {
		int guildId = initiator.getGuildId();
		int worldId = initiator.getWorld();
		Connection con = DatabaseConnection.getConnection();
		try {			
			Guild result = new Guild(guildId);
			try (DatabaseCall guildCall = DatabaseCall.query(getSelectGuildCommand(con, guildId))) {
				ResultSet rs = guildCall.resultSet();
				if (!rs.first()) {
					return null;
				}
				
				result.name = rs.getString("name");
				result.gp = rs.getInt("GP");
				result.logo = rs.getInt("logo");
				result.logoColor = rs.getInt("logoColor");
				result.logoBG = rs.getInt("logoBG");
				result.logoBGColor = rs.getInt("logoBGColor");
				result.capacity = rs.getInt("capacity");
				for (int i = 1; i <= 5; i++) {
					result.rankTitles[i - 1] = rs.getString("rank" + i + "title");
				}
				result.leader = rs.getInt("leader");
				result.notice = rs.getString("notice");
				result.signature = rs.getInt("signature");
				result.allianceId = rs.getInt("allianceId");
			} 
			
			try (DatabaseCall membersCall = DatabaseCall.query(getSelectMembersCommand(con, guildId))) {
				ResultSet rs = membersCall.resultSet();
				if (!rs.first()) {
					return null;
				}
				do {
					result.members.add(new GuildCharacter(rs.getInt("id"), rs.getInt("level"), rs.getString("name"), (byte) -1, (byte) worldId, rs.getInt("job"), rs.getInt("guildrank"), guildId, false, rs.getInt("allianceRank")));
				} while (rs.next());
				result.setOnline(initiator.getId(), true, initiator.getChannel());
			} 
			
			return result;
		} catch (SQLException e) {
			Output.print("Unable to read guild information from database.\n" + e);
			return null;
		}	
	}
	
	private static PreparedStatement getSelectMembersCommand(
			Connection connection, int guildId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `id`, `name`, `level`, `job`, `guildrank`, `allianceRank` FROM `characters` WHERE `guildid` = ? ORDER BY `guildrank` ASC, `name` ASC");
			ps.setInt(1, guildId);
		return ps;
	}

	private static PreparedStatement getSelectGuildCommand(
			Connection connection, int guildId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT * FROM `guilds` WHERE `guildid` = ?");
		ps.setInt(1, guildId);
		return ps;
	}

	public void buildNotifications() {
		if (!isDirty) {
			return;
		}
		Set<Byte> chs = Server.getInstance().getChannelServer(worldId);
		if (notifications.keySet().size() != chs.size()) {
			notifications.clear();
			for (Byte ch : chs) {
				notifications.put(ch, new LinkedList<Integer>());
			}
		} else {
			for (List<Integer> l : notifications.values()) {
				l.clear();
			}
		}
		synchronized (members) {
			for (GuildCharacter member : members) {
				if (!member.isOnline()) {
					continue;
				}
				List<Integer> ch = notifications.get(member.getChannel());
				if (ch != null)
					ch.add(member.getId());
				// Unable to connect to Channel... error was here
			}
		}
		isDirty = false;
	}

	public void writeToDB(boolean disbanding) {
		try {
			Connection con = DatabaseConnection.getConnection();
			if (!disbanding) {
				StringBuilder builder = new StringBuilder();
				builder.append("UPDATE `guilds` SET `GP` = ?, `logo` = ?, `logoColor` = ?, `logoBG` = ?, `logoBGColor` = ?, ");
				for (int i = 0; i < 5; i++) {
					builder.append("rank").append(i + 1).append("title = ?, ");
				}
				builder.append("`capacity` = ?, `notice` = ? WHERE `guildid` = ?");
				PreparedStatement ps = con.prepareStatement(builder.toString());
				ps.setInt(1, gp);
				ps.setInt(2, logo);
				ps.setInt(3, logoColor);
				ps.setInt(4, logoBG);
				ps.setInt(5, logoBGColor);
				for (int i = 6; i < 11; i++) {
					ps.setString(i, rankTitles[i - 6]);
				}
				ps.setInt(11, capacity);
				ps.setString(12, notice);
				ps.setInt(13, this.id);
				ps.execute();
				ps.close();
			} else {
				PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET `guildid` = 0, `guildrank` = 5 WHERE `guildid` = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				ps = con.prepareStatement("DELETE FROM `guilds` WHERE `guildid` = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				this.broadcast(PacketCreator.guildDisband(this.id));
			}
		} catch (SQLException se) {
		}
	}

	public int getId() {
		return id;
	}

	public int getLeaderId() {
		return leader;
	}

	public int getGP() {
		return gp;
	}

	public int getLogo() {
		return logo;
	}

	public void setLogo(int l) {
		logo = l;
	}

	public int getLogoColor() {
		return logoColor;
	}

	public void setLogoColor(int c) {
		logoColor = c;
	}

	public int getLogoBG() {
		return logoBG;
	}

	public void setLogoBG(int bg) {
		logoBG = bg;
	}

	public int getLogoBGColor() {
		return logoBGColor;
	}

	public void setLogoBGColor(int c) {
		logoBGColor = c;
	}

	public String getNotice() {
		if (notice == null) {
			return "";
		}
		return notice;
	}

	public String getName() {
		return name;
	}

	public java.util.Collection<GuildCharacter> getMembers() {
		return java.util.Collections.unmodifiableCollection(members);
	}

	public int getCapacity() {
		return capacity;
	}

	public int getSignature() {
		return signature;
	}

	public void broadcast(GamePacket packet) {
		broadcast(packet, -1, GuildOperation.NONE);
	}

	public void broadcast(GamePacket packet, int exception) {
		broadcast(packet, exception, GuildOperation.NONE);
	}

	public void broadcast(GamePacket packet, int exceptionId, GuildOperation operation) {
		synchronized (notifications) {
			if (isDirty) {
				buildNotifications();
			}
			try {
				for (Byte b : Server.getInstance().getChannelServer(worldId)) {
					if (notifications.get(b).size() > 0) {
						if (operation == GuildOperation.DISBAND) {
							Server.getInstance().getWorld(worldId).setGuildAndRank(notifications.get(b), 0, 5, exceptionId);
						} else if (operation == GuildOperation.EMBELMCHANGE) {
							Server.getInstance().getWorld(worldId).changeEmblem(this.id, notifications.get(b), new GuildSummary(this));
						} else {
							Server.getInstance().getWorld(worldId).sendPacket(notifications.get(b), packet, exceptionId);
						}
					}
				}
			} catch (Exception re) {
				Output.print("Failed to contact channels for broadcast."); // fu?
			}
		}
	}

	public void guildMessage(GamePacket serverNotice) {
		for (GuildCharacter member : members) {
			for (Channel channel : Server.getInstance().getChannelsFromWorld(worldId)) {
				if (channel.getPlayerStorage().getCharacterById(member.getId()) != null) {
					channel.getPlayerStorage().getCharacterById(member.getId()).getClient().getSession().write(serverNotice);
					break;
				}
			}
		}
	}

	public final void setOnline(int characterId, boolean online, byte channel) {
		boolean broadcast = true;
		for (GuildCharacter member : members) {
			if (member.getId() == characterId) {
				if (member.isOnline() && online) {
					broadcast = false;
				}
				member.setOnline(online);
				member.setChannelId(channel);
				break;
			}
		}
		if (broadcast) {
			this.broadcast(PacketCreator.guildMemberOnline(id, characterId, online), characterId);
		}
		isDirty = true;
	}

	public void guildChat(String name, int characterId, String message) {
		this.broadcast(PacketCreator.multiChat(name, message, 2), characterId);
	}

	public String getRankTitle(int rank) {
		return rankTitles[rank - 1];
	}

	public static int createGuild(int leaderId, String name) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT `guildid` FROM `guilds` WHERE `name` = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.first()) {
				ps.close();
				rs.close();
				return 0;
			}
			ps.close();
			rs.close();
			ps = con.prepareStatement("INSERT INTO `guilds` (`leader`, `name`, `signature`) VALUES (?, ?, ?)");
			ps.setInt(1, leaderId);
			ps.setString(2, name);
			ps.setInt(3, (int) System.currentTimeMillis());
			ps.execute();
			ps.close();
			ps = con.prepareStatement("SELECT `guildid` FROM `guilds` WHERE `leader` = ?");
			ps.setInt(1, leaderId);
			rs = ps.executeQuery();
			rs.first();
			int guildid = rs.getInt("guildid");
			rs.close();
			ps.close();
			return guildid;
		} catch (Exception e) {
			return 0;
		}
	}

	public int addGuildMember(GuildCharacter member) {
		synchronized (members) {
			if (members.size() >= capacity) {
				return 0;
			}
			for (int i = members.size() - 1; i >= 0; i--) {
				if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(member.getName()) < 0) {
					members.add(i + 1, member);
					isDirty = true;
					break;
				}
			}
		}
		this.broadcast(PacketCreator.newGuildMember(member));
		return 1;
	}

	public void leaveGuild(GuildCharacter member) {
		this.broadcast(PacketCreator.memberLeft(member, false));
		synchronized (members) {
			members.remove(member);
			isDirty = true;
		}
	}

	public void expelMember(GuildCharacter initiator, String name, int characterId) {
		synchronized (members) {
			java.util.Iterator<GuildCharacter> itr = members.iterator();
			GuildCharacter member;
			while (itr.hasNext()) {
				member = itr.next();
				if (member.getId() == characterId && initiator.getGuildRank() < member.getGuildRank()) {
					this.broadcast(PacketCreator.memberLeft(member, true));
					itr.remove();
					isDirty = true;
					try {
						if (member.isOnline()) {
							Server.getInstance().getWorld(member.getWorld()).setGuildAndRank(characterId, 0, 5);
						} else {
							try {
								PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `notes` (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
								ps.setString(1, member.getName());
								ps.setString(2, initiator.getName());
								ps.setString(3, "You have been expelled from the guild.");
								ps.setLong(4, System.currentTimeMillis());
								ps.executeUpdate();
								ps.close();
							} catch (SQLException e) {
								Output.print("Failed to expel a member from a guild.\n" + e);
							}
							Server.getInstance().getWorld(member.getWorld()).setOfflineGuildStatus((short) 0, (byte) 5, characterId);
						}
					} catch (Exception re) {
						re.printStackTrace();
						return;
					}
					return;
				}
			}
			Output.print("Unable to find guild member with name " + name + " and id " + characterId);
		}
	}

	public void changeRank(int characterId, int newRank) {
		for (GuildCharacter member : members) {
			if (characterId == member.getId()) {
				try {
					if (member.isOnline()) {
						Server.getInstance().getWorld(member.getWorld()).setGuildAndRank(characterId, this.id, newRank);
					} else {
						Server.getInstance().getWorld(member.getWorld()).setOfflineGuildStatus((short) this.id, (byte) newRank, characterId);
					}
				} catch (Exception re) {
					re.printStackTrace();
					return;
				}
				member.setGuildRank(newRank);
				this.broadcast(PacketCreator.changeRank(member));
				return;
			}
		}
	}

	public void setGuildNotice(String notice) {
		this.notice = notice;
		writeToDB(false);
		this.broadcast(PacketCreator.guildNotice(this.id, notice));
	}

	public void memberLevelJobUpdate(GuildCharacter gc) {
		for (GuildCharacter member : members) {
			if (gc.equals(member)) {
				member.setJobId(gc.getJobId());
				member.setLevel(gc.getLevel());
				this.broadcast(PacketCreator.guildMemberLevelJobUpdate(gc));
				break;
			}
		}
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GuildCharacter)) {
			return false;
		}
		GuildCharacter o = (GuildCharacter) other;
		return (o.getId() == id && o.getName().equals(name));
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
		hash = 89 * hash + this.id;
		return hash;
	}

	public void changeRankTitle(String[] ranks) {
		for (int i = 0; i < 5; i++) {
			rankTitles[i] = ranks[i];
		}
		this.broadcast(PacketCreator.rankTitleChange(this.id, ranks));
		this.writeToDB(false);
	}

	public void disbandGuild() {
		this.writeToDB(true);
		this.broadcast(null, -1, GuildOperation.DISBAND);
	}

	public void setGuildEmblem(short bg, byte bgcolor, short logo, byte logocolor) {
		this.logoBG = bg;
		this.logoBGColor = bgcolor;
		this.logo = logo;
		this.logoColor = logocolor;
		this.writeToDB(false);
		this.broadcast(null, -1, GuildOperation.EMBELMCHANGE);
	}

	public GuildCharacter getMember(int characterId) {
		for (GuildCharacter mgc : members) {
			if (mgc.getId() == characterId) {
				return mgc;
			}
		}
		return null;
	}

	public boolean increaseCapacity() {
		if (capacity > 99) {
			return false;
		}
		capacity += 5;
		this.writeToDB(false);
		this.broadcast(PacketCreator.guildCapacityChange(this.id, this.capacity));
		return true;
	}

	public void gainGP(int amount) {
		this.gp += amount;
		this.writeToDB(false);
		this.guildMessage(PacketCreator.updateGP(this.id, this.gp));
	}

	public static GuildInviteResponse sendInvite(GameClient c, String targetName) {
		GameCharacter character = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
		if (character == null) {
			return GuildInviteResponse.NOT_IN_CHANNEL;
		}
		if (character.getGuildId() > 0) {
			return GuildInviteResponse.ALREADY_IN_GUILD;
		}
		character.getClient().getSession().write(PacketCreator.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName()));
		return null;
	}

	public static void displayGuildRanks(GameClient c, int npcId) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `name`, `GP`, `logoBG`, `logoBGColor`, " + "`logo`, `logoColor` FROM `guilds` ORDER BY `GP` DESC LIMIT 50");
			ResultSet rs = ps.executeQuery();
			c.getSession().write(PacketCreator.showGuildRanks(npcId, rs));
			ps.close();
			rs.close();
		} catch (SQLException e) {
			Output.print("Failed to display guild ranks.\n" + e);
		}
	}

	public int getAllianceId() {
		return allianceId;
	}

	public void setAllianceId(int allianceId) {
		this.allianceId = allianceId;
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `guilds` SET `allianceId` = ? WHERE `guildid` = ?");
			ps.setInt(1, allianceId);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
		}
	}

	public int getIncreaseGuildCost(int size) {
		return 500000 * (size - 6) / 6;
	}
}
