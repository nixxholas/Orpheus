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

import client.GameCharacter;
import java.awt.Point;
import java.io.File;
import java.sql.PreparedStatement;
import client.GameClient;
import client.InventoryType;
import client.Pet;
import client.PetDataFactory;
import client.SkillFactory;
import java.sql.SQLException;
import tools.DatabaseConnection;
import net.AbstractPacketHandler;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.InventoryManipulator;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SpawnPetHandler extends AbstractPacketHandler {
	private static MapleDataProvider dataRoot = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Item.wz"));

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		slea.readInt();
		byte slot = slea.readByte();
		slea.readByte();
		boolean lead = slea.readByte() == 1;
		Pet pet = chr.getInventory(InventoryType.CASH).getItem(slot).getPet();
		if (pet == null)
			return;
		int petid = pet.getItemId();
		if (petid == 5000028 || petid == 5000047) // Handles Dragon AND Robos
		{
			if (chr.haveItem(petid + 1)) {
				chr.dropMessage(5, "You can't hatch your " + (petid == 5000028 ? "Dragon egg" : "Robo egg") + " if you already have a Baby " + (petid == 5000028 ? "Dragon." : "Robo."));
				c.getSession().write(PacketCreator.enableActions());
				return;
			} else {
				int evolveid = MapleDataTool.getInt("info/evol1", dataRoot.getData("Pet/" + petid + ".img"));
				int petId = Pet.createPet(evolveid);
				if (petId == -1) {
					return;
				}
				try {
					PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM pets WHERE `petid` = ?");
					ps.setInt(1, pet.getUniqueId());
					ps.executeUpdate();
					ps.close();
				} catch (SQLException ex) {
				}
				long expiration = chr.getInventory(InventoryType.CASH).getItem(slot).getExpiration();
				InventoryManipulator.removeById(c, InventoryType.CASH, petid, (short) 1, false, false);
				InventoryManipulator.addById(c, evolveid, (short) 1, null, petId, expiration);
				c.getSession().write(PacketCreator.enableActions());
				return;
			}
		}
		if (chr.getPetIndex(pet) != -1) {
			chr.unequipPet(pet, true);
		} else {
			if (chr.getSkillLevel(SkillFactory.getSkill(8)) == 0 && chr.getPet(0) != null) {
				chr.unequipPet(chr.getPet(0), false);
			}
			if (lead) {
				chr.shiftPetsRight();
			}
			Point pos = chr.getPosition();
			pos.y -= 12;
			pet.setPos(pos);
			pet.setFoothold(chr.getMap().getFootholds().findBelow(pet.getPos()).getId());
			pet.setStance(0);
			pet.setSummoned(true);
			pet.saveToDb();
			chr.addPet(pet);
			chr.getMap().broadcastMessage(c.getPlayer(), PacketCreator.showPet(c.getPlayer(), pet, false, false), true);
			c.announce(PacketCreator.petStatUpdate(c.getPlayer()));
			c.announce(PacketCreator.enableActions());
			chr.startFullnessSchedule(PetDataFactory.getHunger(pet.getItemId()), pet, chr.getPetIndex(pet));
		}
	}
}
