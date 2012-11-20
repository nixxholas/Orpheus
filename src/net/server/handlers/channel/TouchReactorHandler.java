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
import scripting.reactor.ReactorScriptManager;
import server.maps.Reactor;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Generic
 */
public final class TouchReactorHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int oid = slea.readInt();
		Reactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
		if (reactor != null) {
			if (slea.readByte() != 0) {
				ReactorScriptManager.getInstance().touch(c, reactor);
			} else {
				ReactorScriptManager.getInstance().untouch(c, reactor);
			}
		}
	}
}
