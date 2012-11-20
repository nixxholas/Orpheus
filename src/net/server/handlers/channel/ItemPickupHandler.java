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
import net.server.MaplePartyCharacter;
import client.GameClient;
import client.autoban.AutobanFactory;
import java.awt.Point;
import net.AbstractMaplePacketHandler;
import scripting.item.ItemScriptManager;
import server.InventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.scriptedItem;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class ItemPickupHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(final SeekableLittleEndianAccessor slea, final GameClient c) {
		slea.readInt(); // Timestamp
		slea.readByte();
		Point cpos = slea.readPos();
		int oid = slea.readInt();
		GameCharacter chr = c.getPlayer();
		MapleMapObject ob = chr.getMap().getMapObject(oid);
		if (chr.getInventory(MapleItemInformationProvider.getInstance().getInventoryType(ob.getObjectId())).getNextFreeSlot() > -1) {
			if (chr.getMapId() > 209000000 && chr.getMapId() < 209000016) {// happyville
																			// trees
				MapleMapItem mapitem = (MapleMapItem) ob;
				if (mapitem.getDropper().getObjectId() == c.getPlayer().getObjectId()) {
					if (InventoryManipulator.addFromDrop(c, mapitem.getItem(), false)) {
						chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
						chr.getMap().removeMapObject(ob);
					} else {
						c.announce(PacketCreator.enableActions());
						return;
					}
					mapitem.setPickedUp(true);
				} else {
					c.announce(PacketCreator.getInventoryFull());
					c.announce(PacketCreator.getShowInventoryFull());
					return;
				}
				c.announce(PacketCreator.enableActions());
				return;
			}
			try {
				ob.hashCode();
			} catch (NullPointerException e) {
				c.announce(PacketCreator.getInventoryFull());
				c.announce(PacketCreator.getShowInventoryFull());
				return;
			}
			if (ob instanceof MapleMapItem) {
				MapleMapItem mapitem = (MapleMapItem) ob;
				synchronized (mapitem) {
					if (mapitem.getQuest() > 0 && !chr.needQuestItem(mapitem.getQuest(), mapitem.getItemId())) {
						c.announce(PacketCreator.showItemUnavailable());
						c.announce(PacketCreator.enableActions());
						return;
					}
					if (mapitem.isPickedUp()) {
						c.announce(PacketCreator.getInventoryFull());
						c.announce(PacketCreator.getShowInventoryFull());
						return;
					}
					final double Distance = cpos.distanceSq(mapitem.getPosition());
					if (Distance > 2500) {
						AutobanFactory.SHORT_ITEM_VAC.autoban(chr, cpos.toString() + Distance);
					} else if (chr.getPosition().distanceSq(mapitem.getPosition()) > 90000.0) {
						AutobanFactory.ITEM_VAC.autoban(chr, cpos.toString() + Distance);
					}
					if (mapitem.getMeso() > 0) {
						if (chr.getParty() != null) {
							int mesosamm = mapitem.getMeso();
							if (mesosamm > 50000 * chr.getMesoRate()) {
								return;
							}
							int partynum = 0;
							for (MaplePartyCharacter partymem : chr.getParty().getMembers()) {
								if (partymem.isOnline() && partymem.getMapId() == chr.getMap().getId() && partymem.getChannel() == c.getChannel()) {
									partynum++;
								}
							}
							for (MaplePartyCharacter partymem : chr.getParty().getMembers()) {
								if (partymem.isOnline() && partymem.getMapId() == chr.getMap().getId()) {
									GameCharacter somecharacter = c.getChannelServer().getPlayerStorage().getCharacterById(partymem.getId());
									if (somecharacter != null) {
										somecharacter.gainMeso(mesosamm / partynum, true, true, false);
									}
								}
							}
						} else {
							chr.gainMeso(mapitem.getMeso(), true, true, false);
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
							if (!InventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
								c.announce(PacketCreator.enableActions());
								return;
							}
						}
					} else if (useItem(c, mapitem.getItem().getItemId())) {
						if (mapitem.getItem().getItemId() / 10000 == 238) {
							chr.getMonsterBook().addCard(c, mapitem.getItem().getItemId());
						}
					} else if (InventoryManipulator.addFromDrop(c, mapitem.getItem(), true)) {
					} else if (mapitem.getItem().getItemId() == 4031868) {
						chr.getMap().broadcastMessage(PacketCreator.updateAriantPQRanking(chr.getName(), chr.getItemQuantity(4031868, false), false));
					} else {
						c.announce(PacketCreator.enableActions());
						return;
					}
					mapitem.setPickedUp(true);
					chr.getMap().broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
					chr.getMap().removeMapObject(ob);
				}
			}
		}
		c.announce(PacketCreator.enableActions());
	}

	static boolean useItem(final GameClient c, final int id) {
		if (id / 1000000 == 2) {
			MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
			if (ii.isConsumeOnPickup(id)) {
				if (id > 2022430 && id < 2022434) {
					for (GameCharacter player : c.getPlayer().getMap().getCharacters()) {
						if (player.getParty() == c.getPlayer().getParty()) {
							ii.getItemEffect(id).applyTo(player);
						}
					}
				} else {
					ii.getItemEffect(id).applyTo(c.getPlayer());
				}
				return true;
			}
		}
		return false;
	}
}
