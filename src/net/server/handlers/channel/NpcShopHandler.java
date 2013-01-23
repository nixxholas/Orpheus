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
import server.ItemInfoProvider;
import server.Shop;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class NpcShopHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte operation = reader.readByte();
		final Shop shop = c.getPlayer().getShop();
		switch (NpcShopOperation.fromByte(operation)) {
		case BUY:
			handleBuy(reader, c, shop);
			break;
			
		case SELL:
			handleSell(reader, c, shop);
			break;
			
		case RECHARGE:
			handleRecharge(reader, c, shop);
			break;
			
		case LEAVE:
			handleLeave(c);
			break;
			
		default:
			// TODO: disconnect!
			break;		
		}
	}

	private void handleBuy(SeekableLittleEndianAccessor reader, GameClient c, final Shop shop) {
		short slot = reader.readShort();
		int itemId = reader.readInt();
		short quantity = reader.readShort();
		shop.buy(c, slot, itemId, quantity);
	}
	
	private void handleSell(SeekableLittleEndianAccessor reader, GameClient c, final Shop shop) {
		short slot = reader.readShort();
		int itemId = reader.readInt();
		short quantity = reader.readShort();
		shop.sell(c, ItemInfoProvider.getInstance().getInventoryType(itemId), slot, quantity);
	}

	private void handleRecharge(SeekableLittleEndianAccessor reader, GameClient c, final Shop shop) {
		byte slot = (byte) reader.readShort();
		shop.recharge(c, slot);
	}
	
	private void handleLeave(GameClient c) {
		c.getPlayer().setShop(null);
	}
}
