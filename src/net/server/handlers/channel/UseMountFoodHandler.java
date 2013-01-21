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

import client.GameCharacter;
import client.GameClient;
import client.InventoryType;
import client.Mount;
import constants.ExpTable;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author PurpleMadness
 */
public final class UseMountFoodHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		reader.skip(6);
		final int itemId = reader.readInt();
	
		final GameCharacter player = c.getPlayer();
		if (player.getInventory(InventoryType.USE).findById(itemId) == null) {
			// TODO: Why was this called if they don't have the item? Hax?
			return;
		}
		
		final Mount mount = player.getMount();
		if (mount != null && mount.getFatigue() > 0) {
			mount.setFatigue(Math.max(mount.getFatigue() - 30, 0));
			mount.setExp(2 * mount.getLevel() + 6 + mount.getExp());
			int level = mount.getLevel();
			boolean levelUp = mount.getExp() >= ExpTable.getMountExpNeededForLevel(level) && level < 31;
			if (levelUp) {
				mount.setLevel(level + 1);
			}
			player.getMap().broadcastMessage(PacketCreator.updateMount(player.getId(), mount, levelUp));
			InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, true, false);
		}
	}
}