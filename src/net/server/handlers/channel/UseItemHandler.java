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
import client.MapleDisease;
import client.InventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 */
public final class UseItemHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		if (!c.getPlayer().isAlive()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
		slea.readInt();
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt();
		IItem toUse = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
		if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId) {
			if (itemId == 2022178 || itemId == 2022433 || itemId == 2050004) {
				c.getPlayer().dispelDebuffs();
				remove(c, slot);
				return;
			} else if (itemId == 2050003) {
				c.getPlayer().dispelDebuff(MapleDisease.SEAL);
				remove(c, slot);
				return;
			}
			if (isTownScroll(itemId)) {
				if (ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer())) {
					remove(c, slot);
				}
				c.announce(PacketCreator.enableActions());
				return;
			}
			remove(c, slot);
			ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
			c.getPlayer().checkBerserk();
		}
	}

	private void remove(GameClient c, byte slot) {
		MapleInventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
		c.announce(PacketCreator.enableActions());
	}

	private boolean isTownScroll(int itemId) {
		return itemId >= 2030000 && itemId < 2030021;
	}
}
