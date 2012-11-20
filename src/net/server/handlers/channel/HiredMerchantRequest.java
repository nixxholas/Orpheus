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

import client.ItemFactory;
import client.GameCharacter;
import java.sql.SQLException;
import java.util.Arrays;
import client.GameClient;
import net.AbstractMaplePacketHandler;
import server.maps.GameMapObjectType;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author XoticStory
 */
public final class HiredMerchantRequest extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		if (chr.getMap().getMapObjectsInRange(chr.getPosition(), 23000, Arrays.asList(GameMapObjectType.HIRED_MERCHANT)).isEmpty() && chr.getMapId() > 910000000 && chr.getMapId() < 910000023) {
			if (!chr.hasMerchant()) {
				try {
					if (ItemFactory.MERCHANT.loadItems(chr.getId(), false).isEmpty() && chr.getMerchantMeso() == 0) {
						c.announce(PacketCreator.hiredMerchantBox());
					} else {
						chr.announce(PacketCreator.retrieveFirstMessage());
					}
				} catch (SQLException ex) {
				}
			} else {
				chr.dropMessage(1, "You already have a store open.");
			}
		} else {
			chr.dropMessage(1, "You cannot open your hired merchant here.");
		}
	}
}
