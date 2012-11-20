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

import client.GameClient;
import constants.ServerConstants;
import net.AbstractPacketHandler;
import net.server.Server;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ServerStatusRequestHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		byte world = (byte) slea.readShort();// Wuuu? ):
		int status;
		int num = 0;
		for (byte load : Server.getInstance().getLoad(world).keySet()) {
			num += load;
		}
		if (num >= ServerConstants.CHANNEL_LOAD) {
			status = 2;
		} else if (num >= ServerConstants.CHANNEL_LOAD * .8) { // More than 80
																// percent o___o
			status = 1;
		} else {
			status = 0;
		}
		c.announce(PacketCreator.getServerStatus(status));
	}
}
