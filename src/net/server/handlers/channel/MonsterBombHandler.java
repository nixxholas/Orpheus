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
import net.AbstractMaplePacketHandler;
import server.life.MapleMonster;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MonsterBombHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int oid = slea.readInt();
		MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(oid);
		if (!c.getPlayer().isAlive() || monster == null) {
			return;
		}
		if (monster.getId() == 8500003 || monster.getId() == 8500004) {
			monster.getMap().broadcastMessage(PacketCreator.killMonster(monster.getObjectId(), 4));
			c.getPlayer().getMap().removeMapObject(oid);
		}
	}
}
