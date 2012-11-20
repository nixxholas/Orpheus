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
import net.AbstractMaplePacketHandler;
import server.Portal;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ChangeMapSpecialHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		
		slea.readByte();
		final String startwp = slea.readMapleAsciiString();
		slea.readShort();
		
		final GameCharacter player = c.getPlayer();
		final Portal portal = player.getMap().getPortal(startwp);
		if (player.portalDelay() > System.currentTimeMillis()
				|| player.getBlockedPortals().contains(portal.getScriptName())) {
			c.announce(PacketCreator.enableActions());
		} else if (portal != null) {
			portal.enterPortal(c);
		} 
	}
}
