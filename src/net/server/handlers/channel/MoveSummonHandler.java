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

import java.awt.Point;
import java.util.Collection;
import java.util.List;
import client.GameCharacter;
import client.GameClient;
import server.maps.MapleSummon;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MoveSummonHandler extends AbstractMovementPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int oid = slea.readInt();
		Point startPos = new Point(slea.readShort(), slea.readShort());
		List<LifeMovementFragment> res = parseMovement(slea);
		GameCharacter player = c.getPlayer();
		Collection<MapleSummon> summons = player.getSummons().values();
		MapleSummon summon = null;
		for (MapleSummon sum : summons) {
			if (sum.getObjectId() == oid) {
				summon = sum;
				break;
			}
		}
		if (summon != null) {
			updatePosition(res, summon, 0);
			player.getMap().broadcastMessage(player, MaplePacketCreator.moveSummon(player.getId(), oid, startPos, res), summon.getPosition());
		}
	}
}
