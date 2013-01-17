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

import client.IItem;
import client.Item;
import client.GameCharacter;
import client.GameClient;
import client.Inventory;
import client.InventoryType;
import client.Job;
import client.SkinColor;
import net.AbstractPacketHandler;
import server.ItemInfoProvider;
import tools.PacketCreator;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CreateCharHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		String name = slea.readMapleAsciiString();
		if (!GameCharacter.canCreateChar(name)) {
			return;
		}
		GameCharacter newchar = GameCharacter.getDefault(c);
		newchar.setWorld(c.getWorld());
		int job = slea.readInt();
		int face = slea.readInt();
		newchar.setFace(face);
		newchar.setHair(slea.readInt() + slea.readInt());
		int skincolor = slea.readInt();
		if (skincolor > 3) {
			return;
		}
		newchar.setSkinColor(SkinColor.getById(skincolor));
		int top = slea.readInt();
		int bottom = slea.readInt();
		int shoes = slea.readInt();
		int weapon = slea.readInt();
		newchar.setGender(slea.readByte());
		newchar.setName(name);
		if (!newchar.isGM()) {
			if (job == 0) { 
				// Knights of Cygnus
				newchar.setJob(Job.NOBLESSE);
				newchar.getInventory(InventoryType.ETC).addItem(new Item(4161047, (byte) 0, (short) 1));
			} else if (job == 1) { 
				// Adventurer
				newchar.getInventory(InventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1));
			} else if (job == 2) { 
				// Aran
				newchar.setJob(Job.LEGEND);
				newchar.getInventory(InventoryType.ETC).addItem(new Item(4161048, (byte) 0, (short) 1));
			} else {
				// Muhaha
				c.disconnect(); 
				
				// Should probably ban for packet editing.
				Output.print("[CHAR CREATION] A new job ID has been found: " + job); 
				return;
			}
		}
		// CHECK FOR EQUIPS
		Inventory equip = newchar.getInventory(InventoryType.EQUIPPED);
		if (newchar.isGM()) {
			IItem eq_hat = ItemInfoProvider.getInstance().getEquipById(1002140);
			eq_hat.setPosition((byte) -1);
			equip.addFromDB(eq_hat);
			top = 1042003;
			bottom = 1062007;
			weapon = 1322013;
		}
		IItem eq_top = ItemInfoProvider.getInstance().getEquipById(top);
		eq_top.setPosition((byte) -5);
		equip.addFromDB(eq_top);
		IItem eq_bottom = ItemInfoProvider.getInstance().getEquipById(bottom);
		eq_bottom.setPosition((byte) -6);
		equip.addFromDB(eq_bottom);
		IItem eq_shoes = ItemInfoProvider.getInstance().getEquipById(shoes);
		eq_shoes.setPosition((byte) -7);
		equip.addFromDB(eq_shoes);
		IItem eq_weapon = ItemInfoProvider.getInstance().getEquipById(weapon);
		eq_weapon.setPosition((byte) -11);
		equip.addFromDB(eq_weapon.copy());
		newchar.saveToDB(false);
		c.announce(PacketCreator.addNewCharEntry(newchar));
	}
}