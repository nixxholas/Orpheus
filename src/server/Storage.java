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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.IItem;
import client.ItemFactory;
import client.ItemInventoryEntry;
import client.GameClient;
import client.InventoryType;
import tools.DatabaseConnection;
import tools.PacketCreator;

/**
 * 
 * @author Matze
 */
public class Storage {
	private int id;
	private List<IItem> items;
	private int meso;
	private byte slots;
	private Map<InventoryType, List<IItem>> typeItems = new HashMap<InventoryType, List<IItem>>();

	private Storage(int id, byte slots, int meso) {
		this.id = id;
		this.slots = slots;
		this.items = new LinkedList<IItem>();
		this.meso = meso;
	}

	private static Storage create(int id, byte world) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO storages (accountid, world, slots, meso) VALUES (?, ?, 4, 0)");
			ps.setInt(1, id);
			ps.setByte(2, world);
			ps.executeUpdate();
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return loadOrCreateFromDB(id, world);
	}

	public static Storage loadOrCreateFromDB(int id, byte world) {
		Storage ret = null;
		int storeId;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT storageid, slots, meso FROM storages WHERE accountid = ? AND world = ?");
			ps.setInt(1, id);
			ps.setByte(2, world);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return create(id, world);
			} else {
				storeId = rs.getInt("storageid");
				ret = new Storage(storeId, (byte) rs.getInt("slots"), rs.getInt("meso"));
				rs.close();
				ps.close();
				for (ItemInventoryEntry entry : ItemFactory.STORAGE.loadItems(ret.id, false))
					ret.items.add(entry.item);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return ret;
	}

	public byte getSlots() {
		return slots;
	}

	public boolean gainSlots(int slots) {
		slots += this.slots;

		if (slots <= 48) {
			this.slots = (byte) slots;
			return true;
		}

		return false;
	}

	public void setSlots(byte set) {
		this.slots = set;
	}

	public void saveToDB() {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?");
			ps.setInt(1, slots);
			ps.setInt(2, meso);
			ps.setInt(3, id);
			ps.executeUpdate();
			ps.close();
			List<ItemInventoryEntry> itemsWithType = new ArrayList<ItemInventoryEntry>();

			for (IItem item : items)
				itemsWithType.add(new ItemInventoryEntry(item, ItemInfoProvider.getInstance().getInventoryType(item.getItemId())));

			ItemFactory.STORAGE.saveItems(itemsWithType, id);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public IItem getItem(byte slot) {
		return items.get(slot);
	}

	public IItem takeOut(byte slot) {
		IItem ret = items.remove(slot);
		InventoryType type = ItemInfoProvider.getInstance().getInventoryType(ret.getItemId());
		typeItems.put(type, new ArrayList<IItem>(filterItems(type)));
		return ret;
	}

	public void store(IItem item) {
		items.add(item);
		InventoryType type = ItemInfoProvider.getInstance().getInventoryType(item.getItemId());
		typeItems.put(type, new ArrayList<IItem>(filterItems(type)));
	}

	public List<IItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	private List<IItem> filterItems(InventoryType type) {
		List<IItem> ret = new LinkedList<IItem>();
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		for (IItem item : items) {
			if (ii.getInventoryType(item.getItemId()) == type) {
				ret.add(item);
			}
		}
		return ret;
	}

	public byte getSlot(InventoryType type, byte slot) {
		byte ret = 0;
		for (IItem item : items) {
			if (item == typeItems.get(type).get(slot)) {
				return ret;
			}
			ret++;
		}
		return -1;
	}

	public void sendStorage(GameClient c, int npcId) {
		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		Collections.sort(items, new Comparator<IItem>() {
			public int compare(IItem o1, IItem o2) {
				if (ii.getInventoryType(o1.getItemId()).asByte() < ii.getInventoryType(o2.getItemId()).asByte()) {
					return -1;
				} else if (ii.getInventoryType(o1.getItemId()) == ii.getInventoryType(o2.getItemId())) {
					return 0;
				}
				return 1;
			}
		});
		for (InventoryType type : InventoryType.values()) {
			typeItems.put(type, new ArrayList<IItem>(items));
		}
		c.getSession().write(PacketCreator.getStorage(npcId, slots, items, meso));
	}

	public void sendStored(GameClient c, InventoryType type) {
		c.getSession().write(PacketCreator.storeStorage(slots, type, typeItems.get(type)));
	}

	public void sendTakenOut(GameClient c, InventoryType type) {
		c.getSession().write(PacketCreator.takeOutStorage(slots, type, typeItems.get(type)));
	}

	public int getMeso() {
		return meso;
	}

	public void setMeso(int meso) {
		if (meso < 0) {
			throw new RuntimeException();
		}
		this.meso = meso;
	}

	public void sendMeso(GameClient c) {
		c.getSession().write(PacketCreator.mesoStorage(slots, meso));
	}

	public boolean isFull() {
		return items.size() >= slots;
	}

	public void close() {
		typeItems.clear();
	}
}
