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
import net.AbstractMaplePacketHandler;
import net.server.Server;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Flav
 */
public class EnterCashShopHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		try {
			GameCharacter player = c.getPlayer();

			if (player.getCashShop().isOpened())
				return;

			Server.getInstance().getPlayerBuffStorage().addBuffsToStorage(player.getId(), player.getAllBuffs());
			player.cancelBuffEffects();
			player.cancelExpirationTask();
			c.announce(PacketCreator.openCashShop(c, false));
			player.saveToDB(true);
			player.getCashShop().open(true);
			player.getMap().removePlayer(player);
			c.announce(PacketCreator.showCashInventory(c));
			c.announce(PacketCreator.showGifts(player.getCashShop().loadGifts()));
			c.announce(PacketCreator.showWishList(player, false));
			c.announce(PacketCreator.showCash(player));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
