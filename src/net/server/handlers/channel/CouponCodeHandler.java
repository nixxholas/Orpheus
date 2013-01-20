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

import java.sql.SQLException;
import client.GameClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Penguins (Acrylic)
 */
public final class CouponCodeHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		reader.skip(2);
		String code = reader.readMapleAsciiString();
		boolean validcode = false;
		int type = -1;
		int item = -1;
		validcode = getNXCodeValid(code.toUpperCase(), validcode);
		if (validcode) {
			type = getNXCode(code, "type");
			item = getNXCode(code, "item");
			if (type != 5) {
				try {
					Connection con = DatabaseConnection.getConnection();
					PreparedStatement ps = con.prepareStatement("UPDATE nxcode SET `valid` = 0 WHERE code = " + code);
					ps.executeUpdate();
					ps.close();
					ps = con.prepareStatement("UPDATE nxcode SET `user` = ? WHERE code = ?");
					ps.setString(1, c.getPlayer().getName());
					ps.setString(2, code);
					ps.executeUpdate();
					ps.close();
				} catch (SQLException e) {
				}
			}
			switch (type) {
				case 0:
				case 1:
				case 2:
					c.getPlayer().getCashShop().gainCash(type, item);
					break;
				case 3:
					c.getPlayer().getCashShop().gainCash(0, item);
					c.getPlayer().getCashShop().gainCash(2, (item / 5000));
					break;
				case 4:
					InventoryManipulator.addById(c, item, (short) 1, null, -1, -1);
					c.announce(PacketCreator.showCouponRedeemedItem(item));
					break;
				case 5:
					c.getPlayer().getCashShop().gainCash(0, item);
					break;
			}
			c.announce(PacketCreator.showCash(c.getPlayer()));
		} else {
			// c.announce(PacketCreator.wrongCouponCode());
		}
		c.announce(PacketCreator.enableCSUse());
	}

	private int getNXCode(String code, String type) {
		int item = -1;
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `" + type + "` FROM nxcode WHERE code = ?");
			ps.setString(1, code);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				item = rs.getInt(type);
			}
			rs.close();
			ps.close();
		} catch (SQLException ex) {
		}
		return item;
	}

	private boolean getNXCodeValid(String code, boolean validcode) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `valid` FROM nxcode WHERE code = ?");
			ps.setString(1, code);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				validcode = rs.getInt("valid") != 0;
			}
			rs.close();
			ps.close();
		} catch (SQLException ex) {
		}
		return validcode;
	}
}
