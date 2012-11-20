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

import java.awt.Point;
import java.util.Iterator;
import java.util.List;
import client.Equip;
import client.IItem;
import client.Item;
import client.BuffStat;
import client.GameCharacter;
import client.GameClient;
import client.InventoryType;
import constants.ItemConstants;
import constants.ServerConstants;
import tools.PacketCreator;
import tools.Output;

/**
 * 
 * @author Matze
 */
public class InventoryManipulator {
	public static boolean addRing(GameCharacter chr, int itemId, int ringId) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		InventoryType type = ii.getInventoryType(itemId);
		IItem nEquip = ii.getEquipById(itemId, ringId);
		byte newSlot = chr.getInventory(type).addItem(nEquip);
		if (newSlot == -1) {
			return false;
		}
		chr.getClient().announce(PacketCreator.addInventorySlot(type, nEquip));
		return true;
	}

	public static boolean addById(GameClient c, int itemId, short quantity) {
		return addById(c, itemId, quantity, null, -1, -1);
	}

	public static boolean addById(GameClient c, int itemId, short quantity, long expiration) {
		return addById(c, itemId, quantity, null, -1, (byte) 0, expiration);
	}

	public static boolean addById(GameClient c, int itemId, short quantity, String owner, int petid) {
		return addById(c, itemId, quantity, owner, petid, -1);
	}

	public static boolean addById(GameClient c, int itemId, short quantity, String owner, int petid, long expiration) {
		return addById(c, itemId, quantity, owner, petid, (byte) 0, expiration);
	}

	public static boolean addById(GameClient c, int itemId, short quantity, String owner, int petid, byte flag, long expiration) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		InventoryType type = ii.getInventoryType(itemId);
		if (!type.equals(InventoryType.EQUIP)) {
			short slotMax = ii.getSlotMax(c, itemId);
			List<IItem> existing = c.getPlayer().getInventory(type).listById(itemId);
			if (!ItemConstants.isRechargable(itemId)) {
				if (existing.size() > 0) { // first update all existing slots to
											// slotMax
					Iterator<IItem> i = existing.iterator();
					while (quantity > 0) {
						if (i.hasNext()) {
							Item eItem = (Item) i.next();
							short oldQ = eItem.getQuantity();
							if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null)) {
								short newQ = (short) Math.min(oldQ + quantity, slotMax);
								quantity -= (newQ - oldQ);
								eItem.setQuantity(newQ);
								eItem.setExpiration(expiration);
								c.announce(PacketCreator.updateInventorySlot(type, eItem));
							}
						} else {
							break;
						}
					}
				}
				while (quantity > 0 || ItemConstants.isRechargable(itemId)) {
					short newQ = (short) Math.min(quantity, slotMax);
					if (newQ != 0) {
						quantity -= newQ;
						Item nItem = new Item(itemId, (byte) 0, newQ, petid);
						nItem.setFlag(flag);
						nItem.setExpiration(expiration);
						byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
						if (newSlot == -1) {
							c.announce(PacketCreator.getInventoryFull());
							c.announce(PacketCreator.getShowInventoryFull());
							return false;
						}
						if (owner != null) {
							nItem.setOwner(owner);
						}
						c.announce(PacketCreator.addInventorySlot(type, nItem));
						if ((ItemConstants.isRechargable(itemId)) && quantity == 0) {
							break;
						}
					} else {
						c.announce(PacketCreator.enableActions());
						return false;
					}
				}
			} else {
				Item nItem = new Item(itemId, (byte) 0, quantity, petid);
				nItem.setFlag(flag);
				nItem.setExpiration(expiration);
				byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
				if (newSlot == -1) {
					c.announce(PacketCreator.getInventoryFull());
					c.announce(PacketCreator.getShowInventoryFull());
					return false;
				}
				c.announce(PacketCreator.addInventorySlot(type, nItem));
				c.announce(PacketCreator.enableActions());
			}
		} else if (quantity == 1) {
			IItem nEquip = ii.getEquipById(itemId);
			nEquip.setFlag(flag);
			nEquip.setExpiration(expiration);
			if (owner != null) {
				nEquip.setOwner(owner);
			}
			byte newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
			if (newSlot == -1) {
				c.announce(PacketCreator.getInventoryFull());
				c.announce(PacketCreator.getShowInventoryFull());
				return false;
			}
			c.announce(PacketCreator.addInventorySlot(type, nEquip));
		} else {
			throw new RuntimeException("Trying to create equip with non-one quantity");
		}
		return true;
	}

	public static boolean addFromDrop(GameClient c, IItem item, boolean show) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		InventoryType type = ii.getInventoryType(item.getItemId());
		if (ii.isPickupRestricted(item.getItemId()) && c.getPlayer().getItemQuantity(item.getItemId(), true) > 0) {
			c.announce(PacketCreator.getInventoryFull());
			c.announce(PacketCreator.showItemUnavailable());
			return false;
		}
		short quantity = item.getQuantity();
		if (!type.equals(InventoryType.EQUIP)) {
			short slotMax = ii.getSlotMax(c, item.getItemId());
			List<IItem> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
			if (!ItemConstants.isRechargable(item.getItemId())) {
				if (existing.size() > 0) { // first update all existing slots to
											// slotMax
					Iterator<IItem> i = existing.iterator();
					while (quantity > 0) {
						if (i.hasNext()) {
							Item eItem = (Item) i.next();
							short oldQ = eItem.getQuantity();
							if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner())) {
								short newQ = (short) Math.min(oldQ + quantity, slotMax);
								quantity -= (newQ - oldQ);
								eItem.setQuantity(newQ);
								c.announce(PacketCreator.updateInventorySlot(type, eItem, true));
							}
						} else {
							break;
						}
					}
				}
				while (quantity > 0 || ItemConstants.isRechargable(item.getItemId())) {
					short newQ = (short) Math.min(quantity, slotMax);
					quantity -= newQ;
					Item nItem = new Item(item.getItemId(), (byte) 0, newQ);
					nItem.setExpiration(item.getExpiration());
					nItem.setOwner(item.getOwner());
					byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
					if (newSlot == -1) {
						c.announce(PacketCreator.getInventoryFull());
						c.announce(PacketCreator.getShowInventoryFull());
						item.setQuantity((short) (quantity + newQ));
						return false;
					}
					c.announce(PacketCreator.addInventorySlot(type, nItem, true));
					if ((ItemConstants.isRechargable(item.getItemId())) && quantity == 0) {
						break;
					}
				}
			} else {
				Item nItem = new Item(item.getItemId(), (byte) 0, quantity);
				byte newSlot = c.getPlayer().getInventory(type).addItem(nItem);
				if (newSlot == -1) {
					c.announce(PacketCreator.getInventoryFull());
					c.announce(PacketCreator.getShowInventoryFull());
					return false;
				}
				c.announce(PacketCreator.addInventorySlot(type, nItem));
				c.announce(PacketCreator.enableActions());
			}
		} else if (quantity == 1) {
			byte newSlot = c.getPlayer().getInventory(type).addItem(item);
			if (newSlot == -1) {
				c.announce(PacketCreator.getInventoryFull());
				c.announce(PacketCreator.getShowInventoryFull());
				return false;
			}
			c.announce(PacketCreator.addInventorySlot(type, item, true));
		} else {
			return false;
		}
		if (show) {
			c.announce(PacketCreator.getShowItemGain(item.getItemId(), item.getQuantity()));
		}
		return true;
	}

	public static boolean checkSpace(GameClient c, int itemid, int quantity, String owner) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		InventoryType type = ii.getInventoryType(itemid);
		if (!type.equals(InventoryType.EQUIP)) {
			short slotMax = ii.getSlotMax(c, itemid);
			List<IItem> existing = c.getPlayer().getInventory(type).listById(itemid);
			if (!ItemConstants.isRechargable(itemid)) {
				if (existing.size() > 0) // first update all existing slots to
											// slotMax
				{
					for (IItem eItem : existing) {
						short oldQ = eItem.getQuantity();
						if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
							short newQ = (short) Math.min(oldQ + quantity, slotMax);
							quantity -= (newQ - oldQ);
						}
						if (quantity <= 0) {
							break;
						}
					}
				}
			}
			final int numSlotsNeeded;
			if (slotMax > 0) {
				numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
			} else if (ItemConstants.isRechargable(itemid)) {
				numSlotsNeeded = 1;
			} else {
				numSlotsNeeded = 1;
				Output.print("checkSpace error");
			}
			return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
		} else {
			return !c.getPlayer().getInventory(type).isFull();
		}
	}

	public static void removeFromSlot(GameClient c, InventoryType type, byte slot, short quantity, boolean fromDrop) {
		removeFromSlot(c, type, slot, quantity, fromDrop, false);
	}

	public static void removeFromSlot(GameClient c, InventoryType type, byte slot, short quantity, boolean fromDrop, boolean consume) {
		IItem item = c.getPlayer().getInventory(type).getItem(slot);
		boolean allowZero = consume && ItemConstants.isRechargable(item.getItemId());
		c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
		if (item.getQuantity() == 0 && !allowZero) {
			c.announce(PacketCreator.clearInventoryItem(type, item.getPosition(), fromDrop));
		} else {
			c.announce(PacketCreator.updateInventorySlot(type, (Item) item, fromDrop));
		}
	}

	public static void removeById(GameClient c, InventoryType type, int itemId, int quantity, boolean fromDrop, boolean consume) {
		List<IItem> items = c.getPlayer().getInventory(type).listById(itemId);
		int remremove = quantity;
		for (IItem item : items) {
			if (remremove <= item.getQuantity()) {
				removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume);
				remremove = 0;
				break;
			} else {
				remremove -= item.getQuantity();
				removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume);
			}
		}
		if (remremove > 0) {
			throw new RuntimeException("[h4x] Not enough items available (" + itemId + ", " + (quantity - remremove) + "/" + quantity + ")");
		}
	}

	public static void move(GameClient c, InventoryType type, byte src, byte dst) {
		if (src < 0 || dst < 0) {
			return;
		}
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		IItem source = c.getPlayer().getInventory(type).getItem(src);
		IItem initialTarget = c.getPlayer().getInventory(type).getItem(dst);
		if (source == null) {
			return;
		}
		short olddstQ = -1;
		if (initialTarget != null) {
			olddstQ = initialTarget.getQuantity();
		}
		short oldsrcQ = source.getQuantity();
		short slotMax = ii.getSlotMax(c, source.getItemId());
		c.getPlayer().getInventory(type).move(src, dst, slotMax);
		if (!type.equals(InventoryType.EQUIP) && initialTarget != null && initialTarget.getItemId() == source.getItemId() && !ItemConstants.isRechargable(source.getItemId())) {
			if ((olddstQ + oldsrcQ) > slotMax) {
				c.announce(PacketCreator.moveAndMergeWithRestInventoryItem(type, src, dst, (short) ((olddstQ + oldsrcQ) - slotMax), slotMax));
			} else {
				c.announce(PacketCreator.moveAndMergeInventoryItem(type, src, dst, ((Item) c.getPlayer().getInventory(type).getItem(dst)).getQuantity()));
			}
		} else {
			c.announce(PacketCreator.moveInventoryItem(type, src, dst));
		}
	}

	public static void equip(GameClient c, byte src, byte dst) {
		Equip source = (Equip) c.getPlayer().getInventory(InventoryType.EQUIP).getItem(src);
		Equip target = (Equip) c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem(dst);
		if (source == null || !ItemInfoProvider.getInstance().canWearEquipment(c.getPlayer(), source)) {
			c.announce(PacketCreator.enableActions());
			return;
		} else if ((((source.getItemId() >= 1902000 && source.getItemId() <= 1902002) || source.getItemId() == 1912000) && c.getPlayer().isCygnus()) || ((source.getItemId() >= 1902005 && source.getItemId() <= 1902007) || source.getItemId() == 1912005) && !c.getPlayer().isCygnus()) {// Adventurer
																																																																							// taming
																																																																							// equipment
			return;
		}
		if (ItemInfoProvider.getInstance().isUntradeableOnEquip(source.getItemId())) {
			source.setFlag((byte) ItemConstants.UNTRADEABLE);
		}
		if (source.getRingId() > -1) {
			c.getPlayer().getRingById(source.getRingId()).equip();
		}
		if (dst == -6) { // unequip the overall
			IItem top = c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem((byte) -5);
			if (top != null && isOverall(top.getItemId())) {
				if (c.getPlayer().getInventory(InventoryType.EQUIP).isFull()) {
					c.announce(PacketCreator.getInventoryFull());
					c.announce(PacketCreator.getShowInventoryFull());
					return;
				}
				unequip(c, (byte) -5, c.getPlayer().getInventory(InventoryType.EQUIP).getNextFreeSlot());
			}
		} else if (dst == -5) {
			final IItem bottom = c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem((byte) -6);
			if (bottom != null && isOverall(source.getItemId())) {
				if (c.getPlayer().getInventory(InventoryType.EQUIP).isFull()) {
					c.announce(PacketCreator.getInventoryFull());
					c.announce(PacketCreator.getShowInventoryFull());
					return;
				}
				unequip(c, (byte) -6, c.getPlayer().getInventory(InventoryType.EQUIP).getNextFreeSlot());
			}
		} else if (dst == -10) {// check if weapon is two-handed
			IItem weapon = c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem((byte) -11);
			if (weapon != null && ItemInfoProvider.getInstance().isTwoHanded(weapon.getItemId())) {
				if (c.getPlayer().getInventory(InventoryType.EQUIP).isFull()) {
					c.announce(PacketCreator.getInventoryFull());
					c.announce(PacketCreator.getShowInventoryFull());
					return;
				}
				unequip(c, (byte) -11, c.getPlayer().getInventory(InventoryType.EQUIP).getNextFreeSlot());
			}
		} else if (dst == -11) {
			IItem shield = c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem((byte) -10);
			if (shield != null && ItemInfoProvider.getInstance().isTwoHanded(source.getItemId())) {
				if (c.getPlayer().getInventory(InventoryType.EQUIP).isFull()) {
					c.announce(PacketCreator.getInventoryFull());
					c.announce(PacketCreator.getShowInventoryFull());
					return;
				}
				unequip(c, (byte) -10, c.getPlayer().getInventory(InventoryType.EQUIP).getNextFreeSlot());
			}
		}
		if (dst == -18) {
			if (c.getPlayer().getMount() != null) {
				c.getPlayer().getMount().setItemId(source.getItemId());
			}
		}
		if (source.getItemId() == 1122017) {
			c.getPlayer().equipPendantOfSpirit();
		}
		// 1112413, 1112414, 1112405 (Lilin's Ring)
		source = (Equip) c.getPlayer().getInventory(InventoryType.EQUIP).getItem(src);
		target = (Equip) c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem(dst);
		c.getPlayer().getInventory(InventoryType.EQUIP).removeSlot(src);
		if (target != null) {
			c.getPlayer().getInventory(InventoryType.EQUIPPED).removeSlot(dst);
		}
		source.setPosition(dst);
		c.getPlayer().getInventory(InventoryType.EQUIPPED).addFromDB(source);
		if (target != null) {
			target.setPosition(src);
			c.getPlayer().getInventory(InventoryType.EQUIP).addFromDB(target);
		}
		if (c.getPlayer().getBuffedValue(BuffStat.BOOSTER) != null && isWeapon(source.getItemId())) {
			c.getPlayer().cancelBuffStats(BuffStat.BOOSTER);
		}
		c.announce(PacketCreator.moveInventoryItem(InventoryType.EQUIP, src, dst, (byte) 2));
		c.getPlayer().forceUpdateItem(InventoryType.EQUIPPED, source);
		c.getPlayer().equipChanged();
	}

	public static void unequip(GameClient c, byte src, byte dst) {
		Equip source = (Equip) c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem(src);
		Equip target = (Equip) c.getPlayer().getInventory(InventoryType.EQUIP).getItem(dst);
		if (dst < 0) {
			Output.print("Unequipping to negative slot.");
		}
		if (source == null) {
			return;
		}
		if (target != null && src <= 0) {
			c.announce(PacketCreator.getInventoryFull());
			return;
		}
		if (source.getItemId() == 1122017) {
			c.getPlayer().unequipPendantOfSpirit();
		}
		if (source.getRingId() > -1) {
			c.getPlayer().getRingById(source.getRingId()).unequip();
		}
		c.getPlayer().getInventory(InventoryType.EQUIPPED).removeSlot(src);
		if (target != null) {
			c.getPlayer().getInventory(InventoryType.EQUIP).removeSlot(dst);
		}
		source.setPosition(dst);
		c.getPlayer().getInventory(InventoryType.EQUIP).addFromDB(source);
		if (target != null) {
			target.setPosition(src);
			c.getPlayer().getInventory(InventoryType.EQUIPPED).addFromDB(target);
		}
		c.announce(PacketCreator.moveInventoryItem(InventoryType.EQUIP, src, dst, (byte) 1));
		c.getPlayer().equipChanged();
	}

	public static void drop(GameClient c, InventoryType type, byte src, short quantity) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		if (src < 0) {
			type = InventoryType.EQUIPPED;
		}
		IItem source = c.getPlayer().getInventory(type).getItem(src);
		int itemId = source.getItemId();
		if (itemId >= 5000000 && itemId <= 5000100) {
			return;
		}
		if (type == InventoryType.EQUIPPED && itemId == 1122017) {
			c.getPlayer().unequipPendantOfSpirit();
		}
		if (c.getPlayer().getItemEffect() == itemId && source.getQuantity() == 1) {
			c.getPlayer().setItemEffect(0);
			c.getPlayer().getMap().broadcastMessage(PacketCreator.itemEffect(c.getPlayer().getId(), 0));
		} else if (itemId == 5370000 || itemId == 5370001) {
			if (c.getPlayer().getItemQuantity(itemId, false) == 1) {
				c.getPlayer().setChalkboard(null);
			}
		}
		if (c.getPlayer().getItemQuantity(itemId, true) < quantity || quantity < 0 || source == null || quantity == 0 && !ItemConstants.isRechargable(itemId)) {
			return;
		}
		Point dropPos = new Point(c.getPlayer().getPosition());
		if (quantity < source.getQuantity() && !ItemConstants.isRechargable(itemId)) {
			IItem target = source.copy();
			target.setQuantity(quantity);
			source.setQuantity((short) (source.getQuantity() - quantity));
			c.announce(PacketCreator.dropInventoryItemUpdate(type, source));
			boolean weddingRing = source.getItemId() == 1112803 || source.getItemId() == 1112806 || source.getItemId() == 1112807 || source.getItemId() == 1112809;
			if (weddingRing) {
				c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
			} else if (c.getPlayer().getMap().getEverlast()) {
				if ((ii.isDropRestricted(target.getItemId()) && !ServerConstants.DROP_UNTRADEABLE_ITEMS) || ItemInfoProvider.getInstance().isCash(target.getItemId())) {
					c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
				} else {
					c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, false);
				}
			} else if ((ii.isDropRestricted(target.getItemId()) && !ServerConstants.DROP_UNTRADEABLE_ITEMS) || ItemInfoProvider.getInstance().isCash(target.getItemId())) {
				c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
			} else {
				c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
			}
		} else {
			c.getPlayer().getInventory(type).removeSlot(src);
			c.announce(PacketCreator.dropInventoryItem((src < 0 ? InventoryType.EQUIP : type), src));
			if (src < 0) {
				c.getPlayer().equipChanged();
			}
			if (c.getPlayer().getMap().getEverlast()) {
				if ((ii.isDropRestricted(itemId) && !ServerConstants.DROP_UNTRADEABLE_ITEMS)) {
					c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
				} else {
					c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, false);
				}
			} else if ((ii.isDropRestricted(itemId) && !ServerConstants.DROP_UNTRADEABLE_ITEMS)) {
				c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
			} else {
				c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
			}
		}
	}

	private static boolean isOverall(int itemId) {
		return itemId / 10000 == 105;
	}

	private static boolean isWeapon(int itemId) {
		return itemId >= 1302000 && itemId < 1492024;
	}
}
