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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import client.GameCharacter;
import client.GameClient;
import tools.DatabaseConnection;
import tools.Output;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class BBSOperationHandler extends AbstractPacketHandler {

	private String correctLength(String in, int maxSize) {
		return in.length() > maxSize ? in.substring(0, maxSize) : in;
	}

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		if (c.getPlayer().getGuildId() < 1) {
			return;
		}
		byte mode = reader.readByte();
		int localthreadid = 0;
		switch (mode) {
			case 0:
				boolean bEdit = reader.readByte() == 1;
				if (bEdit) {
					localthreadid = reader.readInt();
				}
				boolean bNotice = reader.readByte() == 1;
				String title = correctLength(reader.readMapleAsciiString(), 25);
				String text = correctLength(reader.readMapleAsciiString(), 600);
				int icon = reader.readInt();
				if (icon >= 0x64 && icon <= 0x6a) {
					if (c.getPlayer().getItemQuantity(5290000 + icon - 0x64, false) > 0) {
						return;
					}
				} else if (icon < 0 || icon > 3) {
					return;
				}
				if (!bEdit) {
					newBBSThread(c, title, text, icon, bNotice);
				} else {
					editBBSThread(c, title, text, icon, localthreadid);
				}
				break;
			case 1:
				localthreadid = reader.readInt();
				deleteBBSThread(c, localthreadid);
				break;
			case 2:
				int start = reader.readInt();
				listBBSThreads(c, start * 10);
				break;
			case 3: // list thread + reply, followed by id (int)
				localthreadid = reader.readInt();
				displayThread(c, localthreadid);
				break;
			case 4: // reply
				localthreadid = reader.readInt();
				text = correctLength(reader.readMapleAsciiString(), 25);
				newBBSReply(c, localthreadid, text);
				break;
			case 5: // delete reply
				localthreadid = reader.readInt(); // we don't use this
				int replyid = reader.readInt();
				deleteBBSReply(c, replyid);
				break;
			default:
				Output.print("Unhandled BBS mode: " + reader.toString());
		}
	}

	private static void listBBSThreads(GameClient c, int start) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC");
			ps.setInt(1, c.getPlayer().getGuildId());
			ResultSet rs = ps.executeQuery();
			c.announce(PacketCreator.BBSThreadList(rs, start));
			rs.close();
			ps.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private static void newBBSReply(GameClient c, int localThreadId, String text) {
		if (c.getPlayer().getGuildId() <= 0) {
			return;
		}
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
			ps.setInt(1, c.getPlayer().getGuildId());
			ps.setInt(2, localThreadId);
			ResultSet threadRS = ps.executeQuery();
			if (!threadRS.next()) {
				threadRS.close();
				ps.close();
				return;
			}
			int threadid = threadRS.getInt("threadid");
			threadRS.close();
			ps.close();
			ps = con.prepareStatement("INSERT INTO bbs_replies " + "(`threadid`, `postercid`, `timestamp`, `content`) VALUES " + "(?, ?, ?, ?)");
			ps.setInt(1, threadid);
			ps.setInt(2, c.getPlayer().getId());
			ps.setLong(3, System.currentTimeMillis());
			ps.setString(4, text);
			ps.execute();
			ps.close();
			ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount + 1 WHERE threadid = ?");
			ps.setInt(1, threadid);
			ps.execute();
			ps.close();
			displayThread(c, localThreadId);
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private static void editBBSThread(GameClient client, String title, String text, int icon, int localThreadId) {
		GameCharacter c = client.getPlayer();
		if (c.getGuildId() < 1) {
			return;
		}
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE bbs_threads SET `name` = ?, `timestamp` = ?, " + "`icon` = ?, " + "`startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)");
			ps.setString(1, title);
			ps.setLong(2, System.currentTimeMillis());
			ps.setInt(3, icon);
			ps.setString(4, text);
			ps.setInt(5, c.getGuildId());
			ps.setInt(6, localThreadId);
			ps.setInt(7, c.getId());
			ps.setBoolean(8, c.getGuildRank() < 3);
			ps.execute();
			ps.close();
			displayThread(client, localThreadId);
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private static void newBBSThread(GameClient client, String title, String text, int icon, boolean isNotice) {
		GameCharacter c = client.getPlayer();
		if (c.getGuildId() <= 0) {
			return;
		}
		int nextId = 0;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps;
			if (!isNotice) {
				ps = con.prepareStatement("SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?");
				ps.setInt(1, c.getGuildId());
				ResultSet rs = ps.executeQuery();
				rs.next();
				nextId = rs.getInt("lastLocalId") + 1;
				rs.close();
				ps.close();
			}
			ps = con.prepareStatement("INSERT INTO bbs_threads " + "(`postercid`, `name`, `timestamp`, `icon`, `startpost`, " + "`guildid`, `localthreadid`) " + "VALUES(?, ?, ?, ?, ?, ?, ?)");
			ps.setInt(1, c.getId());
			ps.setString(2, title);
			ps.setLong(3, System.currentTimeMillis());
			ps.setInt(4, icon);
			ps.setString(5, text);
			ps.setInt(6, c.getGuildId());
			ps.setInt(7, nextId);
			ps.execute();
			ps.close();
			displayThread(client, nextId);
		} catch (SQLException se) {
			se.printStackTrace();
		}

	}

	public static void deleteBBSThread(GameClient client, int localThreadId) {
		GameCharacter player = client.getPlayer();
		if (player.getGuildId() <= 0) {
			return;
		}
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
			ps.setInt(1, player.getGuildId());
			ps.setInt(2, localThreadId);
			ResultSet threadRS = ps.executeQuery();
			if (!threadRS.next()) {
				threadRS.close();
				ps.close();
				return;
			}
			if (player.getId() != threadRS.getInt("postercid") && player.getGuildRank() > 2) {
				threadRS.close();
				ps.close();
				return;
			}
			int threadid = threadRS.getInt("threadid");
			ps.close();
			ps = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?");
			ps.setInt(1, threadid);
			ps.execute();
			ps.close();
			ps = con.prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?");
			ps.setInt(1, threadid);
			ps.execute();
			threadRS.close();
			ps.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	public static void deleteBBSReply(GameClient client, int replyId) {
		GameCharacter player = client.getPlayer();
		if (player.getGuildId() <= 0) {
			return;
		}
		int threadid;
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?");
			ps.setInt(1, replyId);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return;
			}
			if (player.getId() != rs.getInt("postercid") && player.getGuildRank() > 2) {
				rs.close();
				ps.close();
				return;
			}
			threadid = rs.getInt("threadid");
			rs.close();
			ps.close();
			ps = con.prepareStatement("DELETE FROM bbs_replies WHERE replyid = ?");
			ps.setInt(1, replyId);
			ps.execute();
			ps.close();
			ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount - 1 WHERE threadid = ?");
			ps.setInt(1, threadid);
			ps.execute();
			ps.close();
			displayThread(client, threadid, false);
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	public static void displayThread(GameClient client, int threadId) {
		displayThread(client, threadId, true);
	}

	public static void displayThread(GameClient client, int threadId, boolean isThreadIdLocal) {
		GameCharacter player = client.getPlayer();
		if (player.getGuildId() <= 0) {
			return;
		}
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? AND " + (isThreadIdLocal ? "local" : "") + "threadid = ?");
			ps.setInt(1, player.getGuildId());
			ps.setInt(2, threadId);
			ResultSet threadRS = ps.executeQuery();
			if (!threadRS.next()) {
				threadRS.close();
				ps.close();
				return;
			}
			ResultSet repliesRS = null;
			PreparedStatement ps2 = null;
			if (threadRS.getInt("replycount") >= 0) {
				ps2 = con.prepareStatement("SELECT * FROM bbs_replies WHERE threadid = ?");
				ps2.setInt(1, !isThreadIdLocal ? threadId : threadRS.getInt("threadid"));
				repliesRS = ps2.executeQuery();
			}
			client.announce(PacketCreator.showThread(isThreadIdLocal ? threadId : threadRS.getInt("localthreadid"), threadRS, repliesRS));
			repliesRS.close();
			ps.close();
			if (ps2 != null) {
				ps2.close();
			}
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (RuntimeException re) {// btw we get this everytime for some
										// reason, but replies work!
			re.printStackTrace();
			Output.print("The number of reply rows does not match the replycount in thread.");
		}
	}
}
