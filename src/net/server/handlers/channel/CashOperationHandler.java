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

import client.IEquip;
import client.IItem;
import client.GameCharacter;
import client.GameClient;
import client.Inventory;
import client.InventoryType;
import client.Ring;
import client.RingCreationInfo;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import net.AbstractPacketHandler;
import server.CashShop;
import server.CashShop.CashItem;
import server.CashShop.CashItemFactory;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import tools.PacketCreator;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CashOperationHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter chr = c.getPlayer();
		CashShop cs = chr.getCashShop();
		if (!cs.isOpened()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		final int action = reader.readByte();
		if (action == 0x03 || action == 0x1E) {
			reader.readByte();
			final int useNX = reader.readInt();
			final int snCS = reader.readInt();
			CashItem cItem = CashItemFactory.getItem(snCS);
			if (cItem == null || !cItem.isOnSale() || cs.getCash(useNX) < cItem.getPrice()) {
				return;
			}
			if (action == 0x03) { 
				// Item
				IItem item = cItem.toItem();
				cs.addToInventory(item);
				c.announce(PacketCreator.showBoughtCashItem(item, c.getAccountId()));
			} else { 
				// Package
				List<IItem> cashPackage = CashItemFactory.getPackage(cItem.getItemId());
				for (IItem item : cashPackage) {
					cs.addToInventory(item);
				}
				c.announce(PacketCreator.showBoughtCashPackage(cashPackage, c.getAccountId()));
			}
			cs.gainCash(useNX, -cItem.getPrice());
			c.announce(PacketCreator.showCash(chr));
		} else if (action == 0x04) {
			// TODO: check for gender
			int birthday = reader.readInt();
			CashItem cItem = CashItemFactory.getItem(reader.readInt());
			Map<String, String> recipient = GameCharacter.getCharacterFromDatabase(reader.readMapleAsciiString());
			String message = reader.readMapleAsciiString();
			if (!canBuy(cItem, cs.getCash(4)) || message.length() < 1 || message.length() > 73) {
				return;
			}
			if (!checkBirthday(c, birthday)) {
				c.announce(PacketCreator.showCashShopMessage((byte) 0xC4));
				return;
			} else if (recipient == null) {
				c.announce(PacketCreator.showCashShopMessage((byte) 0xA9));
				return;
			} else if (recipient.get("accountid").equals(String.valueOf(c.getAccountId()))) {
				c.announce(PacketCreator.showCashShopMessage((byte) 0xA8));
				return;
			}
			cs.gift(Integer.parseInt(recipient.get("id")), chr.getName(), message, cItem.getSN());
			c.announce(PacketCreator.showGiftSucceed(recipient.get("name"), cItem));
			cs.gainCash(4, -cItem.getPrice());
			c.announce(PacketCreator.showCash(chr));
			try {
				// last byte - fame or not
				chr.sendNote(recipient.get("name"), chr.getName() + " has sent you a gift! Go check out the Cash Shop.", (byte) 0); 
			} catch (SQLException ex) {
			}
			GameCharacter receiver = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient.get("name"));
			if (receiver != null)
				receiver.showNote();
		} else if (action == 0x05) { 
			// Modify wish list
			cs.clearWishList();
			for (byte i = 0; i < 10; i++) {
				int sn = reader.readInt();
				CashItem cItem = CashItemFactory.getItem(sn);
				if (cItem != null && cItem.isOnSale() && sn != 0) {
					cs.addToWishList(sn);
				}
			}
			c.announce(PacketCreator.showWishList(chr, true));
		} else if (action == 0x06) { 
			// Increase Inventory Slots
			reader.skip(1);
			int cash = reader.readInt();
			byte mode = reader.readByte();
			if (mode == 0) {
				byte type = reader.readByte();
				if (cs.getCash(cash) < 4000) {
					return;
				}
				if (chr.gainSlots(type, 4, false)) {
					c.announce(PacketCreator.showBoughtInventorySlots(type, chr.getSlots(type)));
					cs.gainCash(cash, -4000);
					c.announce(PacketCreator.showCash(chr));
				}
			} else {
				CashItem cItem = CashItemFactory.getItem(reader.readInt());
				int type = (cItem.getItemId() - 9110000) / 1000;
				if (!canBuy(cItem, cs.getCash(cash))) {
					return;
				}
				if (chr.gainSlots(type, 8, false)) {
					c.announce(PacketCreator.showBoughtInventorySlots(type, chr.getSlots(type)));
					cs.gainCash(cash, -cItem.getPrice());
					c.announce(PacketCreator.showCash(chr));
				}
			}
		} else if (action == 0x07) { 
			// Increase Storage Slots
			reader.skip(1);
			int cash = reader.readInt();
			byte mode = reader.readByte();
			if (mode == 0) {
				if (cs.getCash(cash) < 4000) {
					return;
				}
				if (chr.getStorage().gainSlots(4)) {
					c.announce(PacketCreator.showBoughtStorageSlots(chr.getStorage().getSlots()));
					cs.gainCash(cash, -4000);
					c.announce(PacketCreator.showCash(chr));
				}
			} else {
				CashItem cItem = CashItemFactory.getItem(reader.readInt());

				if (!canBuy(cItem, cs.getCash(cash))) {
					return;
				}
				if (chr.getStorage().gainSlots(8)) {
					c.announce(PacketCreator.showBoughtStorageSlots(chr.getStorage().getSlots()));
					cs.gainCash(cash, -cItem.getPrice());
					c.announce(PacketCreator.showCash(chr));
				}
			}
		} else if (action == 0x08) { 
			// Increase Character Slots
			reader.skip(1);
			int cash = reader.readInt();
			CashItem cItem = CashItemFactory.getItem(reader.readInt());

			if (!canBuy(cItem, cs.getCash(cash)))
				return;

			if (c.gainCharacterSlot()) {
				c.announce(PacketCreator.showBoughtCharacterSlot(c.getCharacterSlots()));
				cs.gainCash(cash, -cItem.getPrice());
				c.announce(PacketCreator.showCash(chr));
			}
		} else if (action == 0x0D) { 
			// Take from Cash Inventory
			IItem item = cs.findByCashId(reader.readInt());
			if (item == null) {
				return;
			}
			if (chr.getInventory(ItemInfoProvider.getInstance().getInventoryType(item.getItemId())).addItem(item) != -1) {
				cs.removeFromInventory(item);
				c.announce(PacketCreator.takeFromCashInventory(item));
			}
		} else if (action == 0x0E) { 
			// Put into Cash Inventory
			int cashId = reader.readInt();
			reader.skip(4);
			Inventory mi = chr.getInventory(InventoryType.fromByte(reader.readByte()));
			IItem item = mi.findByCashId(cashId);
			if (item == null) {
				return;
			}
			cs.addToInventory(item);
			mi.removeSlot(item.getSlot());
			c.announce(PacketCreator.putIntoCashInventory(item, c.getAccountId()));
		} else if (action == 0x1D) { 
			// crush ring (action 28)
			if (checkBirthday(c, reader.readInt())) {
				int toCharge = reader.readInt();
				int SN = reader.readInt();
				String recipient = reader.readMapleAsciiString();
				String text = reader.readMapleAsciiString();
				CashItem ring = CashItemFactory.getItem(SN);
				GameCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
				if (partner == null) {
					chr.getClient().announce(PacketCreator.serverNotice(1, "The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel."));
				} else {
					if (partner.getGender() == chr.getGender()) {
						chr.dropMessage("You and your partner are the same gender, please buy a friendship ring.");
						return;
					}
					IEquip item = (IEquip) ring.toItem();
					final RingCreationInfo info = getRingCreationInfo(ring, chr, partner);
					int ringid = Ring.createRing(info);
					item.setRingId(ringid);
					cs.addToInventory(item);
					c.announce(PacketCreator.showBoughtCashItem(item, c.getAccountId()));
					cs.gift(partner.getId(), chr.getName(), text, item.getSN(), (ringid + 1));
					cs.gainCash(toCharge, -ring.getPrice());
					chr.getRingsInfo().addCrushRing(Ring.loadFromDb(ringid));
					try {
						chr.sendNote(partner.getName(), text, (byte) 1);
					} catch (SQLException ex) {
					}
					partner.showNote();
				}
			} else {
				chr.dropMessage("The birthday you entered was incorrect.");
			}
			c.announce(PacketCreator.showCash(c.getPlayer()));
		} else if (action == 0x20) { 
			// everything is 1 meso...
			int itemId = CashItemFactory.getItem(reader.readInt()).getItemId();
			if (chr.getMeso() > 0) {
				if (itemId == 4031180 || itemId == 4031192 || itemId == 4031191) {
					chr.gainMeso(-1, false);
					InventoryManipulator.addById(c, itemId, (short) 1);
					c.announce(PacketCreator.showBoughtQuestItem(itemId));
				}
			}
			c.announce(PacketCreator.showCash(c.getPlayer()));
		} else if (action == 0x23) {
			// Friendship :3
			if (checkBirthday(c, reader.readInt())) {
				int payment = reader.readByte();
				reader.skip(3); // 0s
				int snID = reader.readInt();
				CashItem ring = CashItemFactory.getItem(snID);
				String sentTo = reader.readMapleAsciiString();
				int available = reader.readShort() - 1;
				String text = reader.readAsciiString(available);
				reader.readByte();
				GameCharacter partner = c.getChannelServer().getPlayerStorage().getCharacterByName(sentTo);
				if (partner == null) {
					chr.dropMessage("The partner you specified cannot be found.\r\nPlease make sure your partner is online and in the same channel.");
				} else {
					IEquip item = (IEquip) ring.toItem();
					int ringid = Ring.createRing(getRingCreationInfo(ring, chr, partner));
					item.setRingId(ringid);
					cs.addToInventory(item);
					c.announce(PacketCreator.showBoughtCashItem(item, c.getAccountId()));
					cs.gift(partner.getId(), chr.getName(), text, item.getSN(), (ringid + 1));
					cs.gainCash(payment, -ring.getPrice());
					chr.getRingsInfo().addFriendshipRing(Ring.loadFromDb(ringid));
					try {
						chr.sendNote(partner.getName(), text, (byte) 1);
					} catch (SQLException ex) {
					}
					partner.showNote();
				}
			} else {
				chr.dropMessage("The birthday you entered was incorrect.");
			}
			c.announce(PacketCreator.showCash(c.getPlayer()));
		} else {
			Output.print(reader.toString());
		}
	}

	private RingCreationInfo getRingCreationInfo(
			CashItem ringItem,
			GameCharacter partner1, 
			GameCharacter partner2) {
		
		return new RingCreationInfo(ringItem.getItemId(), partner1.getId(), partner1.getName(), partner2.getId(), partner2.getName());
	}

	private boolean checkBirthday(GameClient c, int dateNumber) {
		int year = dateNumber / 10000;
		int month = (dateNumber - year * 10000) / 100;
		int day = dateNumber - year * 10000 - month * 100;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(year, month - 1, day);
		return c.checkBirthDate(cal);
	}

	public boolean canBuy(CashItem item, int cash) {
		return item != null && item.isOnSale() && item.getPrice() <= cash;
	}
}
