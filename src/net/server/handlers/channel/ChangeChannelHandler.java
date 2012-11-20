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

import client.MapleBuffStat;
import client.GameCharacter;
import java.net.InetAddress;
import client.GameClient;
import client.MapleInventoryType;
import java.io.IOException;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import server.MapleTrade;
import server.maps.FieldLimit;
import server.maps.HiredMerchant;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class ChangeChannelHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		byte channel = (byte) (slea.readByte() + 1);
		GameCharacter chr = c.getPlayer();
		Server server = Server.getInstance();
		if (chr.isBanned()) {
			c.disconnect();
			return;
		}
		if (!chr.isAlive() || FieldLimit.CHANGECHANNEL.check(chr.getMap().getFieldLimit())) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		String[] socket = Server.getInstance().getIP(c.getWorld(), channel).split(":");
		if (chr.getTrade() != null) {
			MapleTrade.cancelTrade(c.getPlayer());
		}

		HiredMerchant merchant = chr.getHiredMerchant();
		if (merchant != null) {
			if (merchant.isOwner(c.getPlayer())) {
				merchant.setOpen(true);
			} else {
				merchant.removeVisitor(c.getPlayer());
			}
		}
		server.getPlayerBuffStorage().addBuffsToStorage(chr.getId(), chr.getAllBuffs());
		chr.cancelBuffEffects();
		chr.cancelMagicDoor();
		chr.saveCooldowns();
		// Canceling mounts? Noty
		if (chr.getBuffedValue(MapleBuffStat.PUPPET) != null) {
			chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
		}
		if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {
			chr.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
		}
		chr.getInventory(MapleInventoryType.EQUIPPED).checked(false); // test
		chr.getMap().removePlayer(chr);
		chr.getClient().getChannelServer().removePlayer(chr);
		chr.saveToDB(true);
		server.getLoad(c.getWorld()).get(c.getChannel()).decrementAndGet();
		chr.getClient().updateLoginState(GameClient.LOGIN_SERVER_TRANSITION);
		try {
			c.announce(PacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
		} catch (IOException e) {
		}
	}
}