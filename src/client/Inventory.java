/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss

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
package client;

import constants.ItemConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Matze
 */
public class Inventory implements Iterable<IItem> {
	
	private Map<Byte, IItem> items = new LinkedHashMap<Byte, IItem>();
	private byte capacity;
	private InventoryType type;
	private boolean checked = false;

	public Inventory(InventoryType type, byte capacity) {
		this.items = new LinkedHashMap<Byte, IItem>();
		this.type = type;
		this.capacity = capacity;
	}

	public byte getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = (byte) capacity;
	}

	public IItem findById(int itemId) {
		for (IItem item : items.values()) {
			if (item.getItemId() == itemId) {
				return item;
			}
		}
		return null;
	}

	public int countById(int itemId) {
		int possesed = 0;
		for (IItem item : items.values()) {
			if (item.getItemId() == itemId) {
				possesed += item.getQuantity();
			}
		}
		return possesed;
	}

	public List<IItem> listById(int itemId) {
		List<IItem> ret = new ArrayList<IItem>();
		for (IItem item : items.values()) {
			if (item.getItemId() == itemId) {
				ret.add(item);
			}
		}
		if (ret.size() > 1) {
			Collections.sort(ret);
		}
		return ret;
	}

	public Collection<IItem> list() {
		return items.values();
	}

	public byte addItem(IItem item) {
		byte slotId = getNextFreeSlot();
		if (slotId < 0) {
			return -1;
		}
		items.put(slotId, item);
		item.setSlot(slotId);
		return slotId;
	}

	public void addFromDB(IItem item) {
		if (item.getSlot() < 0 && !this.type.equals(InventoryType.EQUIPPED)) {
			throw new RuntimeException("Item with negative position in non-equipped inventory?");
		}
		items.put(item.getSlot(), item);
	}

	public void move(byte sourceSlot, byte targetSlot, short slotMax) {
		Item source = (Item) items.get(sourceSlot);
		Item target = (Item) items.get(targetSlot);
		if (source == null) {
			throw new RuntimeException("Trying to move empty slot");
		}
		if (target == null) {
			source.setSlot(targetSlot);
			items.put(targetSlot, source);
			items.remove(sourceSlot);
		} else if (target.getItemId() == source.getItemId() && !ItemConstants.isRechargable(source.getItemId())) {
			if (type.asByte() == InventoryType.EQUIP.asByte()) {
				swap(target, source);
			}
			if (source.getQuantity() + target.getQuantity() > slotMax) {
				short rest = (short) ((source.getQuantity() + target.getQuantity()) - slotMax);
				source.setQuantity(rest);
				target.setQuantity(slotMax);
			} else {
				target.setQuantity((short) (source.getQuantity() + target.getQuantity()));
				items.remove(sourceSlot);
			}
		} else {
			swap(target, source);
		}
	}

	private void swap(IItem source, IItem target) {
		items.remove(source.getSlot());
		items.remove(target.getSlot());
		byte swapPos = source.getSlot();
		source.setSlot(target.getSlot());
		target.setSlot(swapPos);
		items.put(source.getSlot(), source);
		items.put(target.getSlot(), target);
	}

	public IItem getItem(byte slot) {
		return items.get(slot);
	}

	public void removeItem(byte slot) {
		removeItem(slot, (short) 1, false);
	}

	public void removeItem(byte slot, short quantity, boolean allowZero) {
		IItem item = items.get(slot);
		if (item == null) {
			// TODO: is it ok not to throw an exception here?
			return;
		}
		item.setQuantity((short) (item.getQuantity() - quantity));
		if (item.getQuantity() < 0) {
			item.setQuantity((short) 0);
		}
		if (item.getQuantity() == 0 && !allowZero) {
			removeSlot(slot);
		}
	}

	public void removeSlot(byte slot) {
		items.remove(slot);
	}

	public boolean isFull() {
		return items.size() >= capacity;
	}

	public boolean isFull(int margin) {
		return items.size() + margin >= capacity;
	}

	public byte getNextFreeSlot() {
		if (isFull()) {
			return -1;
		}
		for (byte i = 1; i <= capacity; i++) {
			if (!items.keySet().contains(i)) {
				return i;
			}
		}
		return -1;
	}

	public byte getNumFreeSlot() {
		if (isFull()) {
			return 0;
		}
		byte free = 0;
		for (byte i = 1; i <= capacity; i++) {
			if (!items.keySet().contains(i)) {
				free++;
			}
		}
		return free;
	}

	public InventoryType getType() {
		return type;
	}

	@Override
	public Iterator<IItem> iterator() {
		return Collections.unmodifiableCollection(items.values()).iterator();
	}

	public Collection<Inventory> allInventories() {
		return Collections.singletonList(this);
	}

	public IItem findByCashId(int cashId) {
		boolean isRing = false;
		IEquip equip = null;
		for (IItem item : items.values()) {
			if (item.getType() == IItem.EQUIP) {
				equip = (IEquip) item;
				isRing = equip.getRingId() > -1;
			}
			if ((item.getPetId() > -1 ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId()) == cashId)
				return item;
		}

		return null;
	}

	public boolean checked() {
		return checked;
	}

	public void checked(boolean yes) {
		checked = yes;
	}
}