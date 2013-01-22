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
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class AfterLoginHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte c2 = reader.readByte();
		byte c3 = 5;
		if (reader.available() > 0) {
			c3 = reader.readByte();
		}
		if (c2 == 1 && c3 == 1) {
			if (c.getPin() == null) {
				c.announce(PacketCreator.registerPin());
			} else {
				c.announce(PacketCreator.requestPin());
			}
		} else if (c2 == 1 && c3 == 0) {
			String pin = reader.readMapleAsciiString();
			if (c.checkPin(pin)) {
				c.announce(PacketCreator.pinAccepted());
			} else {
				c.announce(PacketCreator.requestPinAfterFailure());
			}
		} else if (c2 == 2 && c3 == 0) {
			String pin = reader.readMapleAsciiString();
			if (c.checkPin(pin)) {
				c.announce(PacketCreator.registerPin());
			} else {
				c.announce(PacketCreator.requestPinAfterFailure());
			}
		} else if (c2 == 0 && c3 == 5) {
			c.updateLoginState(GameClient.State.NOT_LOGGED_IN);
		}
	}
}
