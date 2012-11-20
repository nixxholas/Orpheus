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
import client.GameClient;
import client.MaplePet;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.InventoryType;
import net.server.MaplePartyCharacter;
import scripting.item.ItemScriptManager;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.scriptedItem;

/**
 * @author TheRamon
 */
public final class PetLootHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		MaplePet pet = chr.getPet(chr.getPetIndex(slea.readInt()));// why would
																	// it be an
																	// int...?
		if (!pet.isSummoned())
			return;

		slea.skip(13);
		int oid = slea.readInt();
		MapleMapObject ob = chr.getMap().getMapObject(oid);
		if (ob == null || pet == null) {
			c.announce(PacketCreator.getInventoryFull());
			return;
		}
		if (ob instanceof MapleMapItem) {
			MapleMapItem mapitem = (MapleMapItem) ob;
			synchronized (mapitem) {
				if (!chr.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) {
					c.announce(PacketCreator.showItemUnavailable());
					c.announce(PacketCreator.enableActions());
					return;
				}
				if (mapitem.isPickedUp()) {
					c.announce(PacketCreator.getInventoryFull());
					return;
				}
				if (mapitem.getDropper() == c.getPlayer()) {
					return;
				}
				if (mapitem.getMeso() > 0) {
					if (chr.getParty() != null) {
						int mesosamm = mapitem.getMeso();
						if (mesosamm > 50000 * chr.getMesoRate())
							return;
						int partynum = 0;
						for (MaplePartyCharacter partymem : chr.getParty().getMembers()) {
							if (partymem.isOnline() && partymem.getMapId() == chr.getMap().getId() && partymem.getChannel() == c.getChannel()) {
								partynum++;
							}
						}
						for (MaplePartyCharacter partymem : chr.getParty().getMembers()) {
							if (partymem.isOnline() && partymem.getMapId() == chr.getMap().getId()) {
								GameCharacter somecharacter = c.getChannelServer().getPlayerStorage().getCharacterById(partymem.getId());
								if (somecharacter != null)
									somecharacter.gainMeso(mesosamm / partynum, true, true, false);
							}
						}
						chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), true, chr.getPetIndex(pet)), mapitem.getPosition());
						chr.getMap().removeMapObject(ob);
					} else if (chr.getInventory(InventoryType.EQUIPPED).findById(1812000) != null) {
						chr.gainMeso(mapitem.getMeso(), true, true, false);
						chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), true, chr.getPetIndex(pet)), mapitem.getPosition());
						chr.getMap().removeMapObject(ob);
					} else {
						mapitem.setPickedUp(false);
						c.announce(PacketCreator.enableActions());
						return;
					}
				} else if (ItemPickupHandler.useItem(c, mapitem.getItem().getItemId())) {
					if (mapitem.getItem().getItemId() / 10000 == 238) {
						chr.getMonsterBook().addCard(c, mapitem.getItem().getItemId());
					}
					mapitem.setPickedUp(true);
					chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), true, chr.getPetIndex(pet)), mapitem.getPosition());
					chr.getMap().removeMapObject(ob);
				} else if (mapitem.getItem().getItemId() / 100 == 50000) {
					if (chr.getInventory(InventoryType.EQUIPPED).findById(1812007) != null) {
						for (int i : chr.getExcluded()) {
							if (mapitem.getItem().getItemId() == i) {
								return;
							}
						}
					} else if (MapleInventoryManipulator.addById(c, mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), null, -1, mapitem.getItem().getExpiration())) {
						chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), true, chr.getPetIndex(pet)), mapitem.getPosition());
						chr.getMap().removeMapObject(ob);
					} else {
						return;
					}
				} else if (mapitem.getItem().getItemId() / 10000 == 243) {
					MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
					scriptedItem info = ii.getScriptedItemInfo(mapitem.getItem().getItemId());
					if (info.runOnPickup()) {
						ItemScriptManager ism = ItemScriptManager.getInstance();
						String scriptName = info.getScript();
						if (ism.scriptExists(scriptName))
							ism.getItemScript(c, scriptName);

					} else {
						MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true);
					}
					chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), true, chr.getPetIndex(pet)), mapitem.getPosition());
					chr.getMap().removeMapObject(ob);
				} else if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
					chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), true, chr.getPetIndex(pet)), mapitem.getPosition());
					chr.getMap().removeMapObject(ob);
				} else {
					return;
				}
				mapitem.setPickedUp(true);
			}
		}
		// c.announce(PacketCreator.enableActions());
	}
}
