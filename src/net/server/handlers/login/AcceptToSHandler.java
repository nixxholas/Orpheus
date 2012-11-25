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

/**
 * 
 * @author kevintjuh93
 */
public final class AcceptToSHandler extends AbstractPacketHandler {

	@Override
	public boolean validateState(GameClient c) {
		return !c.isLoggedIn();
	}

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		if (slea.available() == 0 || slea.readByte() != 1 || c.acceptTermsOfService()) {
			// Client dc's but just because I am cool I do this (:
			c.disconnect();
			return;
		}
		if (c.finishLogin() == 0) {
			c.announce(PacketCreator.getAuthSuccess(c));
		} else {
			// shouldn't happen XD
			c.announce(PacketCreator.getLoginFailed(9));
		}
	}
}
