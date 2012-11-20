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
package net.server.handlers.login;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import constants.ServerConstants;
import client.GameCharacter;
import client.GameClient;
import tools.DatabaseConnection;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ViewCharHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		try {
			PreparedStatement ps;
			if (ServerConstants.ENABLE_HARDCORE_MODE) {
				ps = DatabaseConnection.getConnection().prepareStatement("SELECT world, id FROM characters WHERE accountid = ? AND dead != 1");
			} else {
				ps = DatabaseConnection.getConnection().prepareStatement("SELECT world, id FROM characters WHERE accountid = ?");
			}
			ps.setInt(1, c.getAccID());
			short charsNum = 0;
			List<Byte> worlds = new ArrayList<Byte>();
			List<GameCharacter> chars = new ArrayList<GameCharacter>();
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				byte cworld = rs.getByte("world");
				boolean inside = false;
				for (int w : worlds) {
					if (w == cworld) {
						inside = true;
					}
				}
				if (!inside) {
					worlds.add(cworld);
				}
				GameCharacter chr = GameCharacter.loadCharFromDB(rs.getInt("id"), c, false);
				chars.add(chr);
				charsNum++;
			}
			rs.close();
			ps.close();
			int unk = charsNum + 3 - charsNum % 3;
			c.announce(PacketCreator.showAllCharacter(charsNum, unk));
			for (byte w : worlds) {
				List<GameCharacter> chrsinworld = new ArrayList<GameCharacter>();
				for (GameCharacter chr : chars) {
					if (chr.getWorld() == w) {
						chrsinworld.add(chr);
					}
				}
				c.announce(PacketCreator.showAllCharacterInfo(w, chrsinworld));
			}
		} catch (Exception e) {
		}
	}
}
