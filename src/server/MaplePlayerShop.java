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
package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import client.IItem;
import client.GameCharacter;
import client.GameClient;
import net.GamePacket;
import net.SendOpcode;
import server.maps.AbstractMapleMapObject;
import server.maps.MapleMapObjectType;
import tools.PacketCreator;
import tools.data.output.PacketWriter;

/**
 * 
 * @author Matze
 */
public class MaplePlayerShop extends AbstractMapleMapObject {
	private GameCharacter owner;
	private GameCharacter[] visitors = new GameCharacter[3];
	private List<MaplePlayerShopItem> items = new ArrayList<MaplePlayerShopItem>();
	private GameCharacter[] slot = {null, null, null};
	private String description;
	private int boughtnumber = 0;
	private List<String> bannedList = new ArrayList<String>();

	public MaplePlayerShop(GameCharacter owner, String description) {
		this.setPosition(owner.getPosition());
		this.owner = owner;
		this.description = description;
	}

	public boolean hasFreeSlot() {
		return visitors[0] == null || visitors[1] == null || visitors[2] == null;
	}

	public boolean isOwner(GameCharacter c) {
		return owner.equals(c);
	}

	public void addVisitor(GameCharacter visitor) {
		for (int i = 0; i < 3; i++) {
			if (visitors[i] == null) {
				visitors[i] = visitor;
				if (this.getSlot(0) == null) {
					this.setSlot(visitor, 0);
					this.broadcast(PacketCreator.getPlayerShopNewVisitor(visitor, 1));
				} else if (this.getSlot(1) == null) {
					this.setSlot(visitor, 1);
					this.broadcast(PacketCreator.getPlayerShopNewVisitor(visitor, 2));
				} else if (this.getSlot(2) == null) {
					this.setSlot(visitor, 2);
					this.broadcast(PacketCreator.getPlayerShopNewVisitor(visitor, 3));
					visitor.getMap().broadcastMessage(PacketCreator.addCharBox(this.getOwner(), 1));
				}
				break;
			}
		}
	}

	public void removeVisitor(GameCharacter visitor) {
		if (visitor == owner) {
			owner.getMap().removeMapObject(this);
			owner.setPlayerShop(null);
		}
		for (int i = 0; i < 3; i++) {
			if (visitors[i] != null && visitors[i].getId() == visitor.getId()) {
				int slot_ = visitor.getSlot();
				visitors[i] = null;
				this.setSlot(null, i);
				visitor.setSlot(-1);
				this.broadcast(PacketCreator.getPlayerShopRemoveVisitor(slot_ + 1));
				return;
			}
		}
	}

	public boolean isVisitor(GameCharacter visitor) {
		return visitors[0] == visitor || visitors[1] == visitor || visitors[2] == visitor;
	}

	public void addItem(MaplePlayerShopItem item) {
		items.add(item);
	}

	public void removeItem(int item) {
		items.remove(item);
	}

	/**
	 * no warnings for now o.op
	 * 
	 * @param c
	 * @param item
	 * @param quantity
	 */
	public void buy(GameClient c, int item, short quantity) {
		if (isVisitor(c.getPlayer())) {
			MaplePlayerShopItem pItem = items.get(item);
			IItem newItem = pItem.getItem().copy();
			newItem.setQuantity(newItem.getQuantity());
			if (quantity < 1 || pItem.getBundles() < 1 || newItem.getQuantity() > pItem.getBundles() || !pItem.isExist()) {
				return;
			} else if (newItem.getType() == 1 && newItem.getQuantity() > 1) {
				return;
			}
			synchronized (c.getPlayer()) {
				if (c.getPlayer().getMeso() >= (long) pItem.getPrice() * quantity) {
					if (MapleInventoryManipulator.addFromDrop(c, newItem, false)) {
						c.getPlayer().gainMeso(-pItem.getPrice() * quantity, true);
						owner.gainMeso(pItem.getPrice() * quantity, true);
						pItem.setBundles((short) (pItem.getBundles() - quantity));
						if (pItem.getBundles() < 1) {
							pItem.setDoesExist(false);
							if (++boughtnumber == items.size()) {
								owner.setPlayerShop(null);
								owner.getMap().broadcastMessage(PacketCreator.removeCharBox(owner));
								this.removeVisitors();
								owner.dropMessage(1, "Your items are sold out, and therefore your shop is closed.");
							}
						}
					} else {
						c.getPlayer().dropMessage(1, "Your inventory is full. Please clean a slot before buying this item.");
					}
				}
			}
		}
	}

	public void broadcastToVisitors(GamePacket packet) {
		for (int i = 0; i < 3; i++) {
			if (visitors[i] != null) {
				visitors[i].getClient().getSession().write(packet);
			}
		}
	}

	public void removeVisitors() {
		try {
			for (int i = 0; i < 3; i++) {
				if (visitors[i] != null) {
					visitors[i].getClient().getSession().write(PacketCreator.shopErrorMessage(10, 1));
					removeVisitor(visitors[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (owner != null) {
			removeVisitor(getOwner());
		}
	}

	public static GamePacket shopErrorMessage(int error, int type) {
		PacketWriter w = new PacketWriter();
		w.writeShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.write(0x0A);
		w.write(type);
		w.write(error);
		return w.getPacket();
	}

	public void broadcast(GamePacket packet) {
		if (owner.getClient() != null && owner.getClient().getSession() != null) {
			owner.getClient().getSession().write(packet);
		}
		broadcastToVisitors(packet);
	}

	public void chat(GameClient c, String chat) {
		byte s = 0;
		for (GameCharacter visitor : getVisitors()) {
			s++;
			if (visitor != null) {
				if (visitor.getName().equalsIgnoreCase(c.getPlayer().getName())) {
					break;
				}
			} else if (s == 3) {
				s = 0;
			}
		}
		broadcast(PacketCreator.getPlayerShopChat(c.getPlayer(), chat, s));
	}

	public void sendShop(GameClient c) {
		c.getSession().write(PacketCreator.getPlayerShop(c, this, isOwner(c.getPlayer())));
	}

	public GameCharacter getOwner() {
		return owner;
	}

	public GameCharacter[] getVisitors() {
		return visitors;
	}

	public GameCharacter getSlot(int s) {
		return slot[s];
	}

	private void setSlot(GameCharacter person, int s) {
		slot[s] = person;
		if (person != null) {
			person.setSlot(s);
		}
	}

	public List<MaplePlayerShopItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@SuppressWarnings("unused")
	public void banPlayer(String name) {
		if (!bannedList.contains(name)) {
			bannedList.add(name);
		}
		for (int i = 0; i < 3; i++) {
			if (visitors[i] != null && visitors[i].getName().equals(name)) {
				visitors[i].getClient().getSession().write(PacketCreator.shopErrorMessage(5, 1));
				removeVisitor(visitors[i]);
			}
			return;
		}
	}

	public boolean isBanned(String name) {
		return bannedList.contains(name);
	}

	@Override
	public void sendDestroyData(GameClient client) {
		client.getSession().write(PacketCreator.removeCharBox(this.getOwner()));
	}

	@Override
	public void sendSpawnData(GameClient client) {
		client.getSession().write(PacketCreator.addCharBox(this.getOwner(), 4));
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.SHOP;
	}
}