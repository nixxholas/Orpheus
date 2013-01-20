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

import client.IItem;
import client.GameClient;
import client.InventoryType;
import constants.ItemConstants;
import net.AbstractPacketHandler;
import net.server.Server;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.RewardInfo;
import server.RewardItem;
import tools.PacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Jay Estrella / Modified by kevintjuh93
 */
public final class ItemRewardHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte slot = (byte) reader.readShort();
		int itemId = reader.readInt(); // will load from xml I don't care.
		if (c.getPlayer().getInventory(InventoryType.USE).getItem(slot).getItemId() != itemId || c.getPlayer().getInventory(InventoryType.USE).countById(itemId) < 1) {
			return;
		}
		
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		RewardInfo rewards = ii.getItemReward(itemId);
		for (RewardItem reward : rewards.getRewardItems()) {
			if (!InventoryManipulator.checkSpace(c, reward.itemId, reward.quantity, "")) {
				c.announce(PacketCreator.showInventoryFull());
				break;
			}
			if (Randomizer.nextInt(rewards.total) < reward.probability) {
				// Is it even possible to get an item with prob 1?
				if (ItemConstants.getInventoryType(reward.itemId) == InventoryType.EQUIP) {
					final IItem item = ii.getEquipById(reward.itemId);
					if (reward.period != -1) {
						item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
					}
					InventoryManipulator.addFromDrop(c, item, false);
				} else {
					InventoryManipulator.addById(c, reward.itemId, reward.quantity);
				}
				InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, false, false);
				if (reward.notice != null) {
					String msg = reward.notice;
					msg.replaceAll("/name", c.getPlayer().getName());
					msg.replaceAll("/item", ii.getName(reward.itemId));
					Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(6, msg));
				}
				break;
			}
		}
		c.announce(PacketCreator.enableActions());
	}
}
