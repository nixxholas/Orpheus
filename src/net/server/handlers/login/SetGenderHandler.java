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
import server.TimerManager;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author kevintjuh93
 */
public class SetGenderHandler extends AbstractPacketHandler {
	
	@Override
	public void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte type = reader.readByte(); // ?
		
		if (type == 0x01 && c.getGender() == 10) { 
			// Packet shouldn't come if Gender isn't 10.
			c.setGender(reader.readByte());
			c.announce(PacketCreator.getAuthSuccess(c));
			final GameClient client = c;
			c.setIdleTask(TimerManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					client.getSession().close(true);
				}
			}, 600000));
		}
	}

}
