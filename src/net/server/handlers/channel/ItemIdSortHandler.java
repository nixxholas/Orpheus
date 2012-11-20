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

import client.IItem;
import client.Item;
import client.GameCharacter;
import client.GameClient;
import client.MapleInventory;
import client.MapleInventoryType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author BubblesDev
 */
public final class ItemIdSortHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		chr.getAutobanManager().setTimestamp(4, slea.readInt());
		byte inv = slea.readByte();
		if (inv < 0 || inv > 5) {
			c.disconnect();
			return;
		}
		MapleInventory Inv = chr.getInventory(MapleInventoryType.fromByte(inv));
		ArrayList<Item> itemarray = new ArrayList<Item>();
		for (Iterator<IItem> it = Inv.iterator(); it.hasNext();) {
			Item item = (Item) it.next();
			itemarray.add((Item) item.copy());
		}
		Collections.sort(itemarray);
		for (IItem item : itemarray) {
			MapleInventoryManipulator.removeById(c, MapleInventoryType.fromByte(inv), item.getItemId(), item.getQuantity(), false, false);
		}
		for (IItem i : itemarray) {
			MapleInventoryManipulator.addFromDrop(c, i, false);
		}
		c.announce(PacketCreator.finishedSort2(inv));
	}
}
