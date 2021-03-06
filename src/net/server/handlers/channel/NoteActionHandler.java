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

import java.sql.PreparedStatement;
import client.GameClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.DatabaseConnection;
import tools.data.input.SeekableLittleEndianAccessor;
import net.AbstractPacketHandler;
import tools.PacketCreator;

public final class NoteActionHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int action = reader.readByte();
		if (action == 0 && c.getPlayer().getCashShop().getAvailableNotes() > 0) {
			String charname = reader.readMapleAsciiString();
			String message = reader.readMapleAsciiString();
			try {
				if (c.getPlayer().getCashShop().isOpened())
					c.announce(PacketCreator.showCashInventory(c));

				c.getPlayer().sendNote(charname, message, (byte) 1);
				c.getPlayer().getCashShop().decreaseNotes();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else if (action == 1) {
			int num = reader.readByte();
			reader.readByte();
			reader.readByte();
			int fame = 0;
			for (int i = 0; i < num; i++) {
				int id = reader.readInt();
				reader.readByte(); // Fame, but we read it from the database :)
				PreparedStatement ps;
				try {
					ps = DatabaseConnection.getConnection().prepareStatement("SELECT `fame` FROM notes WHERE id=? AND deleted=0");
					ps.setInt(1, id);
					ResultSet rs = ps.executeQuery();
					if (rs.next())
						fame += rs.getInt("fame");
					rs.close();

					ps = DatabaseConnection.getConnection().prepareStatement("UPDATE notes SET `deleted` = 1 WHERE id = ?");
					ps.setInt(1, id);
					ps.executeUpdate();
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (fame > 0) {
				c.getPlayer().gainFame(fame);
				c.announce(PacketCreator.getShowFameGain(fame));
			}
		}
	}
}
