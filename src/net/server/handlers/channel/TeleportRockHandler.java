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
import client.TeleportRockInfo;
import net.AbstractPacketHandler;
import server.maps.FieldLimit;
import server.maps.GameMap;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author kevintjuh93
 */
public final class TeleportRockHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter chr = c.getPlayer();
		byte type = reader.readByte();
		boolean vip = reader.readByte() == 1;
		final TeleportRockInfo info = chr.getTeleportRockInfo();
		if (type == 0x00) {
			int mapId = reader.readInt();
			if (vip) {
				info.deleteVip(mapId);
			} else {
				info.deleteRegular(mapId);
			}
			c.announce(PacketCreator.refreshTeleportRockMaps(chr, true, vip));
		} else if (type == 0x01) {
			final GameMap map = chr.getMap();
			if (!FieldLimit.CANNOTVIPROCK.check(map.getFieldLimit())) {
				if (vip){
					info.addVip(map.getId());
				} else {
					info.addRegular(map.getId());
				}
				c.announce(PacketCreator.refreshTeleportRockMaps(chr, false, vip));
			} else {
				chr.message("You may not save this map.");
			}
		}
	}
}