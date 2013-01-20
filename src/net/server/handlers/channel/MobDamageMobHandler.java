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
import net.AbstractPacketHandler;
import server.maps.GameMap;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Jay Estrella
 */
public final class MobDamageMobHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int from = reader.readInt();
		reader.readInt();
		int to = reader.readInt();
		reader.readByte();
		int dmg = reader.readInt();
		GameMap map = c.getPlayer().getMap();
		if (map.getMonsterByOid(from) != null && map.getMonsterByOid(to) != null) {
			map.damageMonster(c.getPlayer(), map.getMonsterByOid(to), dmg);
		}
	}
}
