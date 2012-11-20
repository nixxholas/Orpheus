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
import client.GameCharacter;
import constants.ExpTable;
import client.GameClient;
import client.InventoryType;
import client.Pet;
import client.autoban.AutobanManager;
import tools.Randomizer;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PetFoodHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		AutobanManager abm = chr.getAutobanManager();
		if (abm.getLastSpam(2) + 500 > System.currentTimeMillis()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		abm.spam(2);
		abm.setTimestamp(1, slea.readInt());
		if (chr.getNoPets() == 0) {
			c.getSession().write(PacketCreator.enableActions());
			return;
		}
		int previousFullness = 100;
		byte slot = 0;
		Pet[] pets = chr.getPets();
		for (byte i = 0; i < 3; i++) {
			if (pets[i] != null) {
				if (pets[i].getFullness() < previousFullness) {
					slot = i;
					previousFullness = pets[i].getFullness();
				}
			}
		}
		Pet pet = chr.getPet(slot);
		byte pos = (byte) slea.readShort();
		int itemId = slea.readInt();
		IItem use = chr.getInventory(InventoryType.USE).getItem(pos);
		if (use == null || (itemId / 10000) != 212 || use.getItemId() != itemId)
			return;
		boolean gainCloseness = false;
		if (Randomizer.nextInt(101) > 50) {
			gainCloseness = true;
		}
		if (pet.getFullness() < 100) {
			int newFullness = pet.getFullness() + 30;
			if (newFullness > 100)
				newFullness = 100;
			pet.setFullness(newFullness);
			if (gainCloseness && pet.getCloseness() < 30000) {
				int newCloseness = pet.getCloseness() + 1;
				if (newCloseness > 30000) {
					newCloseness = 30000;
				}
				pet.setCloseness(newCloseness);
				if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
					pet.setLevel((byte) (pet.getLevel() + 1));
					c.announce(PacketCreator.showOwnPetLevelUp(chr.getPetIndex(pet)));
					chr.getMap().broadcastMessage(PacketCreator.showPetLevelUp(c.getPlayer(), chr.getPetIndex(pet)));
				}
			}
			chr.getMap().broadcastMessage(PacketCreator.commandResponse(chr.getId(), slot, 1, true));
		} else {
			if (gainCloseness) {
				int newCloseness = pet.getCloseness() - 1;
				if (newCloseness < 0) {
					newCloseness = 0;
				}
				pet.setCloseness(newCloseness);
				if (pet.getLevel() > 1 && newCloseness < ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
					pet.setLevel((byte) (pet.getLevel() - 1));
				}
			}
			chr.getMap().broadcastMessage(PacketCreator.commandResponse(chr.getId(), slot, 0, false));
		}
		InventoryManipulator.removeFromSlot(c, InventoryType.USE, pos, (short) 1, false);
		IItem petz = chr.getInventory(InventoryType.CASH).getItem(pet.getPosition());
		c.announce(PacketCreator.updateSlot(petz));
		c.announce(PacketCreator.enableActions());
	}
}
