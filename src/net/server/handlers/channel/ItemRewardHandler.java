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
import java.util.List;
import net.AbstractPacketHandler;
import net.server.Server;
import server.InventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.RewardItem;
import tools.PacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Jay Estrella / Modified by kevintjuh93
 */
public final class ItemRewardHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		byte slot = (byte) slea.readShort();
		int itemId = slea.readInt(); // will load from xml I don't care.
		if (c.getPlayer().getInventory(InventoryType.USE).getItem(slot).getItemId() != itemId || c.getPlayer().getInventory(InventoryType.USE).countById(itemId) < 1)
			return;
		MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
		Pair<Integer, List<RewardItem>> rewards = ii.getItemReward(itemId);
		for (RewardItem reward : rewards.getRight()) {
			if (!InventoryManipulator.checkSpace(c, reward.itemid, reward.quantity, "")) {
				c.announce(PacketCreator.showInventoryFull());
				break;
			}
			if (Randomizer.nextInt(rewards.getLeft()) < reward.prob) {
				// Is it even possible to get an item with prob 1?
				if (ItemConstants.getInventoryType(reward.itemid) == InventoryType.EQUIP) {
					final IItem item = ii.getEquipById(reward.itemid);
					if (reward.period != -1) {
						item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
					}
					InventoryManipulator.addFromDrop(c, item, false);
				} else {
					InventoryManipulator.addById(c, reward.itemid, reward.quantity);
				}
				InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, false, false);
				if (reward.worldmsg != null) {
					String msg = reward.worldmsg;
					msg.replaceAll("/name", c.getPlayer().getName());
					msg.replaceAll("/item", ii.getName(reward.itemid));
					Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(6, msg));
				}
				break;
			}
		}
		c.announce(PacketCreator.enableActions());
	}
}
