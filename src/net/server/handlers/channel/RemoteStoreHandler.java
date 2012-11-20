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
import net.server.Channel;
import net.server.Server;
import server.maps.HiredMerchant;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author kevintjuh93 :3
 */
public class RemoteStoreHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		HiredMerchant hm = getMerchant(c);
		if (chr.hasMerchant() && hm != null) {
			if (hm.getChannel() == chr.getClient().getChannel()) {
				hm.setOpen(false);
				hm.removeAllVisitors("");
				chr.setHiredMerchant(hm);
				chr.announce(MaplePacketCreator.getHiredMerchant(chr, hm, false));
			} else {
				c.announce(MaplePacketCreator.remoteChannelChange((byte) (hm.getChannel() - 1)));
			}
			return;
		} else {
			chr.dropMessage(1, "You don't have a Merchant open");
		}
		c.announce(MaplePacketCreator.enableActions());
	}

	public HiredMerchant getMerchant(GameClient c) {
		if (c.getPlayer().hasMerchant()) {
			for (Channel cserv : Server.getInstance().getChannelsFromWorld(c.getWorld())) {
				if (cserv.getHiredMerchants().get(c.getPlayer().getId()) != null) {
					return cserv.getHiredMerchants().get(c.getPlayer().getId());
				}
			}
		}
		return null;
	}
}
