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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.IItem;
import client.GameCharacter;
import client.InventoryType;
import constants.ItemConstants;
import java.util.ArrayList;
import tools.PacketCreator;
import tools.Output;

/**
 * 
 * @author Matze
 */
public class Trade {
	private Trade partner = null;
	private List<IItem> items = new ArrayList<IItem>();
	private List<IItem> exchangeItems;
	private int meso = 0;
	private int exchangeMeso;
	boolean locked = false;
	private GameCharacter chr;
	private byte number;

	public Trade(byte number, GameCharacter c) {
		chr = c;
		this.number = number;
	}

	private static int getFee(int meso) {
		final double tax;
		if (meso >= 100000000) {
			tax = 0.06;
		} else if (meso >= 25000000) {
			tax = 0.05;
		} else if (meso >= 10000000) {
			tax = 0.04;
		} else if (meso >= 5000000) {
			tax = 0.03;
		} else if (meso >= 1000000) {
			tax = 0.018;
		} else if (meso >= 100000) {
			tax = 0.008;
		} else {
			tax = 0.0;
		}
		int fee = (int) Math.round(tax * meso);
		return fee;
	}

	private void lock() {
		locked = true;
		partner.getChr().getClient().getSession().write(PacketCreator.getTradeConfirmation());
	}

	private void complete1() {
		exchangeItems = partner.getItems();
		exchangeMeso = partner.getMeso();
	}

	private void complete2() {
		items.clear();
		meso = 0;
		for (IItem item : exchangeItems) {
			if ((item.getFlag() & ItemConstants.KARMA) == ItemConstants.KARMA)
				item.setFlag((byte) (item.getFlag() ^ ItemConstants.KARMA)); // yay, reset trade flag.
			else if (item.getType() == IItem.ITEM && (item.getFlag() & ItemConstants.SPIKES) == ItemConstants.SPIKES)
				item.setFlag((byte) (item.getFlag() ^ ItemConstants.SPIKES));

			InventoryManipulator.addFromDrop(chr.getClient(), item, true);
		}
		if (exchangeMeso > 0) {
			chr.gainMeso(exchangeMeso - getFee(exchangeMeso), true, true, true);
		}
		exchangeMeso = 0;
		if (exchangeItems != null) {
			exchangeItems.clear();
		}
		chr.getClient().getSession().write(PacketCreator.getTradeCompletion(number));
	}

	private void cancel() {
		for (IItem item : items) {
			InventoryManipulator.addFromDrop(chr.getClient(), item, true);
		}
		if (meso > 0) {
			chr.gainMeso(meso, true, true, true);
		}
		meso = 0;
		if (items != null) {
			items.clear();
		}
		exchangeMeso = 0;
		if (exchangeItems != null) {
			exchangeItems.clear();
		}
		chr.getClient().getSession().write(PacketCreator.getTradeCancel(number));
	}

	private boolean isLocked() {
		return locked;
	}

	private int getMeso() {
		return meso;
	}

	public void setMeso(int meso) {
		if (locked) {
			throw new RuntimeException("Trade is locked.");
		}
		if (meso < 0) {
			Output.print("[MT] " + chr.getName() + " Trying to trade < 0 mesos");
			return;
		}
		if (chr.getMeso() >= meso) {
			chr.gainMeso(-meso, false, true, false);
			this.meso += meso;
			chr.getClient().getSession().write(PacketCreator.getTradeMesoSet((byte) 0, this.meso));
			if (partner != null) {
				partner.getChr().getClient().getSession().write(PacketCreator.getTradeMesoSet((byte) 1, this.meso));
			}
		} else {
		}
	}

	public void addItem(IItem item) {
		items.add(item);
		chr.getClient().getSession().write(PacketCreator.getTradeItemAdd((byte) 0, item));
		if (partner != null) {
			partner.getChr().getClient().getSession().write(PacketCreator.getTradeItemAdd((byte) 1, item));
		}
	}

	public void chat(String message) {
		chr.getClient().getSession().write(PacketCreator.getTradeChat(chr, message, true));
		if (partner != null) {
			partner.getChr().getClient().getSession().write(PacketCreator.getTradeChat(chr, message, false));
		}
	}

	public Trade getPartner() {
		return partner;
	}

	public void setPartner(Trade partner) {
		if (locked) {
			return;
		}
		this.partner = partner;
	}

	public GameCharacter getChr() {
		return chr;
	}

	public List<IItem> getItems() {
		return new LinkedList<IItem>(items);
	}

	private boolean fitsInInventory() {
		ItemInfoProvider mii = ItemInfoProvider.getInstance();
		Map<InventoryType, Integer> neededSlots = new LinkedHashMap<InventoryType, Integer>();
		for (IItem item : exchangeItems) {
			InventoryType type = mii.getInventoryType(item.getItemId());
			if (neededSlots.get(type) == null) {
				neededSlots.put(type, 1);
			} else {
				neededSlots.put(type, neededSlots.get(type) + 1);
			}
		}
		for (Map.Entry<InventoryType, Integer> entry : neededSlots.entrySet()) {
			if (chr.getInventory(entry.getKey()).isFull(entry.getValue() - 1)) {
				return false;
			}
		}
		return true;
	}

	public static void completeTrade(GameCharacter c) {
		c.getTrade().lock();
		Trade local = c.getTrade();
		Trade partner = local.getPartner();
		if (partner.isLocked()) {
			local.complete1();
			partner.complete1();
			if (!local.fitsInInventory() || !partner.fitsInInventory()) {
				cancelTrade(c);
				c.message("There is not enough inventory space to complete the trade.");
				partner.getChr().message("There is not enough inventory space to complete the trade.");
				return;
			}
			if (local.getChr().getLevel() < 15) {
				if (local.getChr().getMesosTraded() + local.exchangeMeso > 1000000) {
					cancelTrade(c);
					local.getChr().getClient().getSession().write(PacketCreator.sendMesoLimit());
					return;
				} else {
					local.getChr().addMesosTraded(local.exchangeMeso);
				}
			} else if (c.getTrade().getChr().getLevel() < 15) {
				if (c.getMesosTraded() + c.getTrade().exchangeMeso > 1000000) {
					cancelTrade(c);
					c.getClient().getSession().write(PacketCreator.sendMesoLimit());
					return;
				} else {
					c.addMesosTraded(local.exchangeMeso);
				}
			}
			local.complete2();
			partner.complete2();
			partner.getChr().setTrade(null);
			c.setTrade(null);
		}
	}

	public static void cancelTrade(GameCharacter c) {
		c.getTrade().cancel();
		if (c.getTrade().getPartner() != null) {
			c.getTrade().getPartner().cancel();
			c.getTrade().getPartner().getChr().setTrade(null);
		}
		c.setTrade(null);
	}

	public static void startTrade(GameCharacter c) {
		if (c.getTrade() == null) {
			c.setTrade(new Trade((byte) 0, c));
			c.getClient().getSession().write(PacketCreator.getTradeStart(c.getClient(), c.getTrade(), (byte) 0));
		} else {
			c.message("You are already in a trade.");
		}
	}

	public static void inviteTrade(GameCharacter c1, GameCharacter c2) {
		if (c2.getTrade() == null) {
			c2.setTrade(new Trade((byte) 1, c2));
			c2.getTrade().setPartner(c1.getTrade());
			c1.getTrade().setPartner(c2.getTrade());
			c2.getClient().getSession().write(PacketCreator.getTradeInvite(c1));
		} else {
			c1.message("The other player is already trading with someone else.");
			cancelTrade(c1);
		}
	}

	public static void visitTrade(GameCharacter c1, GameCharacter c2) {
		if (c1.getTrade() != null && c1.getTrade().getPartner() == c2.getTrade() && c2.getTrade() != null && c2.getTrade().getPartner() == c1.getTrade()) {
			c2.getClient().getSession().write(PacketCreator.getTradePartnerAdd(c1));
			c1.getClient().getSession().write(PacketCreator.getTradeStart(c1.getClient(), c1.getTrade(), (byte) 1));
		} else {
			c1.message("The other player has already closed the trade.");
		}
	}

	public static void declineTrade(GameCharacter c) {
		Trade trade = c.getTrade();
		if (trade != null) {
			if (trade.getPartner() != null) {
				GameCharacter other = trade.getPartner().getChr();
				other.getTrade().cancel();
				other.setTrade(null);
				other.message(c.getName() + " has declined your trade request.");
			}
			trade.cancel();
			c.setTrade(null);
		}
	}
}