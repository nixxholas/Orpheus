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

import java.util.List;
import client.GameCharacter;
import client.GameClient;
import server.movement.LifeMovementFragment;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MovePetHandler extends AbstractMovementPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int petId = reader.readInt();
		reader.readLong();
		// Point startPos = StreamUtil.readShortPoint(reader);
		List<LifeMovementFragment> res = parseMovement(reader);
		if (res.isEmpty()) {
			return;
		}
		GameCharacter player = c.getPlayer();
		byte slot = player.getPetIndex(petId);
		if (slot == -1) {
			return;
		}
		player.getPet(slot).updatePosition(res);
		player.getMap().broadcastMessage(player, PacketCreator.movePet(player.getId(), petId, slot, res), false);
	}
}
