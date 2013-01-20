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

import java.util.List;
import client.IEquip;
import client.IItem;
import client.Item;
import client.GameClient;
import client.SkillFactory;
import client.Inventory;
import client.InventoryType;
import client.IEquip.ScrollResult;
import client.ISkill;
import net.AbstractPacketHandler;
import server.ItemInfoProvider;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 * @author Frz
 */
public final class ScrollHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		reader.readInt(); // whatever...
		byte slot = (byte) reader.readShort();
		byte dst = (byte) reader.readShort();
		byte ws = (byte) reader.readShort();
		boolean whiteScroll = false; // white scroll being used?
		boolean legendarySpirit = false; // legendary spirit skill
		if ((ws & 2) == 2) {
			whiteScroll = true;
		}
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		IEquip toScroll = (IEquip) c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem(dst);
		ISkill LegendarySpirit = SkillFactory.getSkill(1003);
		if (c.getPlayer().getSkillLevel(LegendarySpirit) > 0 && dst >= 0) {
			legendarySpirit = true;
			toScroll = (IEquip) c.getPlayer().getInventory(InventoryType.EQUIP).getItem(dst);
		}
		byte oldLevel = toScroll.getLevel();
		if (((IEquip) toScroll).getUpgradeSlots() < 1) {
			c.announce(PacketCreator.getInventoryFull());
			return;
		}
		Inventory useInventory = c.getPlayer().getInventory(InventoryType.USE);
		IItem scroll = useInventory.getItem(slot);
		IItem wscroll = null;
		List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
		if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
			c.announce(PacketCreator.getInventoryFull());
			return;
		}
		if (whiteScroll) {
			wscroll = useInventory.findById(2340000);
			if (wscroll == null || wscroll.getItemId() != 2340000) {
				whiteScroll = false;
			}
		}
		if (scroll.getItemId() != 2049100 && !isCleanSlate(scroll.getItemId())) {
			if (!canScroll(scroll.getItemId(), toScroll.getItemId())) {
				return;
			}
		}
		if (scroll.getQuantity() < 1) {
			return;
		}
		IEquip scrolled = (IEquip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll);
		ScrollResult scrollSuccess = IEquip.ScrollResult.FAIL; // fail
		if (scrolled == null) {
			scrollSuccess = IEquip.ScrollResult.CURSE;
		} else if (scrolled.getLevel() > oldLevel || (isCleanSlate(scroll.getItemId()) && scrolled.getLevel() == oldLevel + 1)) {
			scrollSuccess = IEquip.ScrollResult.SUCCESS;
		}
		useInventory.removeItem(scroll.getSlot(), (short) 1, false);
		if (whiteScroll) {
			useInventory.removeItem(wscroll.getSlot(), (short) 1, false);
			if (wscroll.getQuantity() < 1) {
				c.announce(PacketCreator.clearInventoryItem(InventoryType.USE, wscroll.getSlot(), false));
			} else {
				c.announce(PacketCreator.updateInventorySlot(InventoryType.USE, (Item) wscroll));
			}
		}
		if (scrollSuccess == IEquip.ScrollResult.CURSE) {
			c.announce(PacketCreator.scrolledItem(scroll, toScroll, true));
			if (dst < 0) {
				c.getPlayer().getInventory(InventoryType.EQUIPPED).removeItem(toScroll.getSlot());
			} else {
				c.getPlayer().getInventory(InventoryType.EQUIP).removeItem(toScroll.getSlot());
			}
		} else {
			c.announce(PacketCreator.scrolledItem(scroll, scrolled, false));
		}
		c.getPlayer().getMap().broadcastMessage(PacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, legendarySpirit));
		if (dst < 0 && (scrollSuccess == IEquip.ScrollResult.SUCCESS || scrollSuccess == IEquip.ScrollResult.CURSE)) {
			c.getPlayer().equipChanged();
		}
	}

	private boolean isCleanSlate(int scrollId) {
		return scrollId > 2048999 && scrollId < 2049004;
	}

	public boolean canScroll(int scrollid, int itemid) {
		return (scrollid / 100) % 100 == (itemid / 10000) % 100;
	}
}
