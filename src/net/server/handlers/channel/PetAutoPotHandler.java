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
import server.MapleStatEffect;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PetAutoPotHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		if (!c.getPlayer().isAlive()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		slea.readByte();
		slea.readLong();
		slea.readInt();
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt();
		IItem toUse = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
		if (toUse != null && toUse.getQuantity() > 0) {
			if (toUse.getItemId() != itemId) {
				c.announce(PacketCreator.enableActions());
				return;
			}
			InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
			MapleStatEffect stat = ItemInfoProvider.getInstance().getItemEffect(toUse.getItemId());
			stat.applyTo(c.getPlayer());
			if (stat.getMp() > 0) {
				c.announce(PacketCreator.sendAutoMpPot(itemId));
			}
			if (stat.getHp() > 0) {
				c.announce(PacketCreator.sendAutoHpPot(itemId));
			}
		}
	}
}
