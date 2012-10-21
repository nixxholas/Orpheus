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

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class NPCShopHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		byte bmode = slea.readByte();
		if (bmode == 0) { // mode 0 = buy :)
			short slot = slea.readShort();// slot
			int itemId = slea.readInt();
			short quantity = slea.readShort();
			c.getPlayer().getShop().buy(c, slot, itemId, quantity);
		} else if (bmode == 1) { // sell ;)
			short slot = slea.readShort();
			int itemId = slea.readInt();
			short quantity = slea.readShort();
			c.getPlayer().getShop().sell(c, MapleItemInformationProvider.getInstance().getInventoryType(itemId), slot, quantity);
		} else if (bmode == 2) { // recharge ;)
			byte slot = (byte) slea.readShort();
			c.getPlayer().getShop().recharge(c, slot);
		} else if (bmode == 3) // leaving :(
		{
			c.getPlayer().setShop(null);
		}
	}
}
