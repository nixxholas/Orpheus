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

import client.GameClient;
import client.GameCharacter;
import net.AbstractPacketHandler;
import net.GamePacket;
import net.SendOpcode;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.PacketWriter;

/**
 * 
 * @author Moogra
 */
public final class FamilyUseHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
		final int type = reader.readInt();
		GameCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(reader.readMapleAsciiString());
		if (type == 0 || type == 1) {
			victim = c.getChannelServer().getPlayerStorage().getCharacterByName(reader.readMapleAsciiString());
			if (victim != null) {
				if (type == 0) {
					c.getPlayer().changeMap(victim.getMap(), victim.getMap().getPortal(0));
				} else {
					victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().getPortal(0));
				}
			} else {
				return;
			}
		} else {
			int erate = type == 3 ? 150 : (type == 4 || type == 6 || type == 8 || type == 10 ? 200 : 100);
			int drate = type == 2 ? 150 : (type == 4 || type == 5 || type == 7 || type == 9 ? 200 : 100);
			if (type > 8) {
			} else {
				c.announce(useRep(drate == 100 ? 2 : (erate == 100 ? 3 : 4), type, erate, drate, ((type > 5 || type == 4) ? 2 : 1) * 15 * 60 * 1000));
			}
		}
		c.getPlayer().getFamily().getMember(c.getPlayer().getId()).gainReputation(repCost[type]);
	}

	/**
	 * [65 00][02][08 00 00 00][C8 00 00 00][00 00 00 00][00][40 77 1B 00]
	 */
	private static GamePacket useRep(int mode, int type, int erate, int drate, int time) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(0x60);// noty
		w.writeAsByte(mode);
		w.writeInt(type);
		if (mode < 4) {
			w.writeInt(erate);
			w.writeInt(drate);
		}
		w.writeAsByte(0);
		w.writeInt(time);
		return w.getPacket();
	}

	// 20 00
	// 00 00 00 00
	// 00 00 00 00 00 00 00 00
	// 80 01
	// 00 00 28 00
	// 8C 93 3E 00
	// 40 0D
	// 03 00 14 00
	// 8C 93 3E 00
	// 40 0D 03 00 00 00 00 00 02
	@SuppressWarnings("unused")
	private static GamePacket giveBuff() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_BUFF.getValue());
		w.writeInt(0);
		w.writeLong(0);

		return null;
	}
}
