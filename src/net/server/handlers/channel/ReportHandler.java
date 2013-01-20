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

import client.GameCharacter;
import client.GameClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.AbstractPacketHandler;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author BubblesDev
 */
public final class ReportHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int type = reader.readByte(); // 01 = Conversation claim 00 = illegal
									// program
		String victim = reader.readMapleAsciiString();
		int reason = reader.readByte();
		String description = reader.readMapleAsciiString();
		if (type == 0) {
			if (c.getPlayer().getPossibleReports() > 0) {
				if (c.getPlayer().getMeso() > 299) {
					c.getPlayer().decreaseReports();
					c.getPlayer().gainMeso(-300, true);
				} else {
					c.announce(PacketCreator.reportResponse((byte) 4));
					return;
				}
			} else {
				c.announce(PacketCreator.reportResponse((byte) 2));
				return;
			}
			c.getChannelServer().broadcastGMPacket(PacketCreator.serverNotice(6, victim + " was reported for: " + description));
			addReport(c.getPlayer().getId(), GameCharacter.getIdByName(victim), 0, description, null);
		} else if (type == 1) {
			String chatlog = reader.readMapleAsciiString();
			if (chatlog == null) {
				return;
			}
			if (c.getPlayer().getPossibleReports() > 0) {
				if (c.getPlayer().getMeso() > 299) {
					c.getPlayer().decreaseReports();
					c.getPlayer().gainMeso(-300, true);
				} else {
					c.announce(PacketCreator.reportResponse((byte) 4));
					return;
				}
			}
			c.getChannelServer().broadcastGMPacket(PacketCreator.serverNotice(6, victim + " was reported for: " + description));
			addReport(c.getPlayer().getId(), GameCharacter.getIdByName(victim), reason, description, chatlog);
		} else {
			c.getChannelServer().broadcastGMPacket(PacketCreator.serverNotice(6, c.getPlayer().getName() + " is probably packet editing. Got unknown report type, which is impossible."));
		}
	}

	public void addReport(int reporterid, int victimid, int reason, String description, String chatlog) {
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("INSERT INTO reports (`reporterid`, `victimid`, `reason`, `description`, `chatlog`) VALUES (?, ?, ?, ?, ?)");
			ps.setInt(1, reporterid);
			ps.setInt(2, victimid);
			ps.setInt(3, reason);
			ps.setString(4, description);
			ps.setString(5, chatlog);
			ps.addBatch();
			ps.executeBatch();
			ps.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
