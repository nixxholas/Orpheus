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
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class AfterLoginHandler extends AbstractMaplePacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		byte c2 = slea.readByte();
		byte c3 = 5;
		if (slea.available() > 0) {
			c3 = slea.readByte();
		}
		if (c2 == 1 && c3 == 1) {
			if (c.getPin() == null) {
				c.announce(MaplePacketCreator.registerPin());
			} else {
				c.announce(MaplePacketCreator.requestPin());
			}
		} else if (c2 == 1 && c3 == 0) {
			String pin = slea.readMapleAsciiString();
			if (c.checkPin(pin)) {
				c.announce(MaplePacketCreator.pinAccepted());
			} else {
				c.announce(MaplePacketCreator.requestPinAfterFailure());
			}
		} else if (c2 == 2 && c3 == 0) {
			String pin = slea.readMapleAsciiString();
			if (c.checkPin(pin)) {
				c.announce(MaplePacketCreator.registerPin());
			} else {
				c.announce(MaplePacketCreator.requestPinAfterFailure());
			}
		} else if (c2 == 0 && c3 == 5) {
			c.updateLoginState(GameClient.LOGIN_NOTLOGGEDIN);
		}
	}
}
