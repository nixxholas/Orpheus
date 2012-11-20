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
import client.Stat;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class GiveFameHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter target = (GameCharacter) c.getPlayer().getMap().getMapObject(slea.readInt());
		int mode = slea.readByte();
		int famechange = 2 * mode - 1;
		GameCharacter player = c.getPlayer();
		if ((target == player || player.getLevel() < 15)) {
			return;
		}
		switch (player.canGiveFame(target)) {
			case OK:
				if (Math.abs(target.getFame() + famechange) < 30001) {
					target.addFame(famechange);
					target.updateSingleStat(Stat.FAME, target.getFame());
				}
				if (!player.isGM()) {
					player.hasGivenFame(target);
				}
				c.announce(PacketCreator.giveFameResponse(mode, target.getName(), target.getFame()));
				target.getClient().announce(PacketCreator.receiveFame(mode, player.getName()));
				break;
			case NOT_TODAY:
				c.announce(PacketCreator.giveFameErrorResponse(3));
				break;
			case NOT_THIS_MONTH:
				c.announce(PacketCreator.giveFameErrorResponse(4));
				break;
		}
	}
}
