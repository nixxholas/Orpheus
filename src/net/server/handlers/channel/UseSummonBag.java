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
import tools.Randomizer;
import net.AbstractMaplePacketHandler;
import server.InventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author AngelSL
 */
public final class UseSummonBag extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		// [4A 00][6C 4C F2 02][02 00][63 0B 20 00]
		if (!c.getPlayer().isAlive()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		slea.readInt();
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt();
		IItem toUse = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
		if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemId) {
			InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
			int[][] toSpawn = MapleItemInformationProvider.getInstance().getSummonMobs(itemId);
			for (int z = 0; z < toSpawn.length; z++) {
				int[] toSpawnChild = toSpawn[z];
				if (Randomizer.nextInt(101) <= toSpawnChild[1]) {
					c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(toSpawnChild[0]), c.getPlayer().getPosition());
				}
			}
		}
		c.announce(PacketCreator.enableActions());
	}
}
