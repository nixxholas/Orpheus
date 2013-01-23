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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import client.IItem;
import client.Item;
import client.GameClient;
import client.InventoryType;
import client.Pet;
import constants.ItemConstants;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Output;

/**
 * 
 * @author Matze
 */
public class Shop {
	private static final Set<Integer> rechargeableItems = new LinkedHashSet<Integer>();
	private int id;
	private int npcId;
	private List<ShopItem> items;
	private int tokenvalue = 1000000000;
	private int token = 4000313;

	static {
		for (int i = 2070000; i < 2070017; i++) {
			rechargeableItems.add(i);
		}
		rechargeableItems.add(2331000);// Blaze Capsule
		rechargeableItems.add(2332000);// Glaze Capsule
		rechargeableItems.add(2070018);
		rechargeableItems.remove(2070014); // doesn't exist
		for (int i = 2330000; i <= 2330005; i++) {
			rechargeableItems.add(i);
		}
	}

	private Shop(int id, int npcId) {
		this.id = id;
		this.npcId = npcId;
		items = new ArrayList<ShopItem>();
	}

	private void addItem(ShopItem item) {
		items.add(item);
	}

	public void sendShop(GameClient c) {
		c.getPlayer().setShop(this);
		c.getSession().write(PacketCreator.getNpcShop(c, getNpcId(), items));
	}

	public void buy(GameClient c, short slot, int itemId, short quantity) {
		ShopItem item = findBySlot(slot);
		if (item != null) {
			if (item.getItemId() != itemId) {
				Output.print("Wrong slot number in shop " + id);
				return;
			}
		} else
			return;

		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		if (item != null && item.getPrice() > 0) {
			if (c.getPlayer().getMeso() >= (long) item.getPrice() * quantity) {
				if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
					if (!ItemConstants.isRechargable(itemId)) { // Pets can't be
																// bought from
																// shops
						InventoryManipulator.addById(c, itemId, quantity);
						c.getPlayer().gainMeso(-(item.getPrice() * quantity), false);
					} else {
						short slotMax = ii.getSlotMax(c, item.getItemId());
						quantity = slotMax;
						InventoryManipulator.addById(c, itemId, quantity);
						c.getPlayer().gainMeso(-item.getPrice(), false);
					}
					c.getSession().write(PacketCreator.shopTransaction((byte) 0));
				} else
					c.getSession().write(PacketCreator.shopTransaction((byte) 3));

			} else
				c.getSession().write(PacketCreator.shopTransaction((byte) 2));

		} else if (item != null && item.getPitch() > 0) {
			if (c.getPlayer().getInventory(InventoryType.ETC).countById(4310000) >= (long) item.getPitch() * quantity) {
				if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
					if (!ItemConstants.isRechargable(itemId)) {
						InventoryManipulator.addById(c, itemId, quantity);
						InventoryManipulator.removeById(c, InventoryType.ETC, 4310000, item.getPitch() * quantity, false, false);
					} else {
						short slotMax = ii.getSlotMax(c, item.getItemId());
						quantity = slotMax;
						InventoryManipulator.addById(c, itemId, quantity);
						InventoryManipulator.removeById(c, InventoryType.ETC, 4310000, item.getPitch() * quantity, false, false);
					}
					c.getSession().write(PacketCreator.shopTransaction((byte) 0));
				} else
					c.getSession().write(PacketCreator.shopTransaction((byte) 3));
			}

		} else if (c.getPlayer().getInventory(InventoryType.CASH).countById(token) != 0) {
			int amount = c.getPlayer().getInventory(InventoryType.CASH).countById(token);
			int value = amount * tokenvalue;
			int cost = item.getPrice() * quantity;
			if (c.getPlayer().getMeso() + value >= cost) {
				int cardreduce = value - cost;
				int diff = cardreduce + c.getPlayer().getMeso();
				if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
					if (ItemConstants.isPet(itemId)) {
						int petId = Pet.createPet(itemId);
						InventoryManipulator.addById(c, itemId, quantity, null, petId, -1);
					} else {
						InventoryManipulator.addById(c, itemId, quantity);
					}
					c.getPlayer().gainMeso(diff, false);
				} else {
					c.getSession().write(PacketCreator.shopTransaction((byte) 3));
				}
				c.getSession().write(PacketCreator.shopTransaction((byte) 0));
			} else
				c.getSession().write(PacketCreator.shopTransaction((byte) 2));
		}
	}

	public void sell(GameClient c, InventoryType type, short slot, short quantity) {
		if (quantity == 0xFFFF || quantity == 0) {
			quantity = 1;
		}
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		IItem item = c.getPlayer().getInventory(type).getItem((byte) slot);
		if (ItemConstants.isRechargable(item.getItemId())) {
			quantity = item.getQuantity();
		}
		if (quantity < 0) {
			return;
		}
		short iQuant = item.getQuantity();
		if (iQuant == 0xFFFF) {
			iQuant = 1;
		}
		if (quantity <= iQuant && iQuant > 0) {
			InventoryManipulator.removeFromSlot(c, type, (byte) slot, quantity, false);
			double price;
			if (ItemConstants.isRechargable(item.getItemId())) {
				price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(c, item.getItemId());
			} else {
				price = ii.getPrice(item.getItemId());
			}
			int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
			if (price != -1 && recvMesos > 0) {
				c.getPlayer().gainMeso(recvMesos, false);
			}
			c.getSession().write(PacketCreator.shopTransaction((byte) 0x8));
		}
	}

	public void recharge(GameClient c, byte slot) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		IItem item = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
		if (item == null || !ItemConstants.isRechargable(item.getItemId())) {
			return;
		}
		short slotMax = ii.getSlotMax(c, item.getItemId());
		if (item.getQuantity() < 0) {
			return;
		}
		if (item.getQuantity() < slotMax) {
			int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
			if (c.getPlayer().getMeso() >= price) {
				item.setQuantity(slotMax);
				c.getSession().write(PacketCreator.updateInventorySlot(InventoryType.USE, (Item) item));
				c.getPlayer().gainMeso(-price, false, true, false);
				c.getSession().write(PacketCreator.shopTransaction((byte) 0x8));
			} else {
				c.getSession().write(PacketCreator.serverNotice(1, "You do not have enough mesos."));
				c.getSession().write(PacketCreator.enableActions());
			}
		}
	}

	private ShopItem findBySlot(short slot) {
		return items.get(slot);
	}

	public static Shop createFromDB(int id, boolean isShopId) {
		Shop ret = null;
		int shopId;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps;
			if (isShopId) {
				ps = con.prepareStatement("SELECT * FROM shops WHERE shopid = ?");
			} else {
				ps = con.prepareStatement("SELECT * FROM shops WHERE npcid = ?");
			}
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				shopId = rs.getInt("shopid");
				ret = new Shop(shopId, rs.getInt("npcid"));
				rs.close();
				ps.close();
			} else {
				rs.close();
				ps.close();
				return null;
			}
			ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
			ps.setInt(1, shopId);
			rs = ps.executeQuery();
			List<Integer> recharges = new ArrayList<Integer>(rechargeableItems);
			while (rs.next()) {
				if (ItemConstants.isRechargable(rs.getInt("itemid"))) {
					ShopItem starItem = new ShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("pitch"));
					ret.addItem(starItem);
					if (rechargeableItems.contains(starItem.getItemId())) {
						recharges.remove(Integer.valueOf(starItem.getItemId()));
					}
				} else {
					ret.addItem(new ShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("pitch")));
				}
			}
			for (Integer recharge : recharges) {
				ret.addItem(new ShopItem((short) 1000, recharge.intValue(), 0, 0));
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public int getNpcId() {
		return npcId;
	}

	public int getId() {
		return id;
	}
}
