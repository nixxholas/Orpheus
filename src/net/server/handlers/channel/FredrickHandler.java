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
import client.ItemFactory;
import client.ItemInventoryEntry;
import client.GameCharacter;
import client.GameClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author kevintjuh93
 */
public class FredrickHandler extends AbstractPacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter chr = c.getPlayer();
		byte operation = reader.readByte();

		switch (operation) {
			case 0x19: // Will never come...
				// c.announce(PacketCreator.getFredrick((byte) 0x24));
				break;
			case 0x1A:
				List<ItemInventoryEntry> items;
				try {
					items = ItemFactory.MERCHANT.loadItems(chr.getId(), false);
					if (!check(chr, items)) {
						c.announce(PacketCreator.fredrickMessage((byte) 0x21));
						return;
					}

					chr.gainMeso(chr.getMerchantMeso(), false);
					chr.setMerchantMeso(0);
					if (deleteItems(chr)) {
						for (int i = 0; i < items.size(); i++) {
							InventoryManipulator.addFromDrop(c, items.get(i).item, false);
						}
						c.announce(PacketCreator.fredrickMessage((byte) 0x1E));
					} else {
						chr.message("An unknown error has occured.");
						return;
					}
					break;
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				break;
			case 0x1C: // Exit
				break;
			default:

		}
	}

	private static boolean check(GameCharacter chr, List<ItemInventoryEntry> entries) {
		if (chr.getMeso() + chr.getMerchantMeso() < 0) {
			return false;
		}
		for (ItemInventoryEntry entry : entries) {
			final IItem item = entry.item;
			if (!InventoryManipulator.checkSpace(chr.getClient(), item.getItemId(), item.getQuantity(), item.getOwner()))
				return false;
		}

		return true;
	}

	private static boolean deleteItems(GameCharacter chr) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE `type` = ? AND `characterid` = ?");

			ps.setInt(1, ItemFactory.MERCHANT.getValue());
			ps.setInt(2, chr.getId());
			ps.execute();
			ps.close();
			return true;
		} catch (SQLException e) {
			return false;
		}

	}
}
