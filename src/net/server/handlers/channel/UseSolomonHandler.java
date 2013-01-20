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
import client.GameClient;
import client.InventoryType;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author XoticStory; modified by kevintjuh93
 */
public final class UseSolomonHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		reader.readInt();
		byte slot = (byte) reader.readShort();
		int itemId = reader.readInt();
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		IItem slotItem = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
		int gachaexp = ii.getExpById(itemId);
		if (c.getPlayer().getInventory(InventoryType.USE).countById(itemId) <= 0 || slotItem.getItemId() != itemId || c.getPlayer().getLevel() > ii.getMaxLevelById(itemId)) {
			return;
		}
		if ((c.getPlayer().getGachaExp() + gachaexp) > Integer.MAX_VALUE) {
			return;
		}
		c.getPlayer().gainGachaExp(gachaexp);
		InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
		c.announce(PacketCreator.enableActions());
	}
}
