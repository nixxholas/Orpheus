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
import client.MapleInventoryType;
import client.MaplePet;
import client.PetCommand;
import client.PetDataFactory;
import tools.Randomizer;
import net.AbstractMaplePacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PetCommandHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		int petId = slea.readInt();
		byte petIndex = chr.getPetIndex(petId);
		MaplePet pet = null;
		if (petIndex == -1) {
			return;
		} else {
			pet = chr.getPet(petIndex);
		}
		slea.readInt();
		slea.readByte();
		byte command = slea.readByte();
		PetCommand petCommand = PetDataFactory.getPetCommand(pet.getItemId(), (int) command);
		if (petCommand == null) {
			return;
		}
		boolean success = false;
		if (Randomizer.nextInt(101) <= petCommand.getProbability()) {
			success = true;
			if (pet.getCloseness() < 30000) {
				int newCloseness = pet.getCloseness() + petCommand.getIncrease();
				if (newCloseness > 30000) {
					newCloseness = 30000;
				}
				pet.setCloseness(newCloseness);
				if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
					pet.setLevel((byte) (pet.getLevel() + 1));
					c.announce(PacketCreator.showOwnPetLevelUp(chr.getPetIndex(pet)));
					chr.getMap().broadcastMessage(PacketCreator.showPetLevelUp(c.getPlayer(), chr.getPetIndex(pet)));
				}
				IItem petz = chr.getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
				c.announce(PacketCreator.updateSlot(petz));
			}
		}
		chr.getMap().broadcastMessage(c.getPlayer(), PacketCreator.commandResponse(chr.getId(), petIndex, command, success), true);
	}
}
