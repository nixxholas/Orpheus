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
import client.InventoryType;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class ItemMoveHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		reader.skip(4);
		final InventoryType type = InventoryType.fromByte(reader.readByte());
		final byte sourceSlot = (byte) reader.readShort();
		final byte targetSlot = (byte) reader.readShort();
		final short quantity = reader.readShort();
		if (sourceSlot < 0 && targetSlot > 0) {
			InventoryManipulator.unequip(c, sourceSlot, targetSlot);
		} else if (targetSlot < 0) {
			InventoryManipulator.equip(c, sourceSlot, targetSlot);
		} else if (targetSlot == 0) {
			InventoryManipulator.drop(c, type, sourceSlot, quantity);
		} else {
			InventoryManipulator.move(c, type, sourceSlot, targetSlot);
		}
	}
}