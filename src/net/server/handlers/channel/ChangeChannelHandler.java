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

import client.BuffStat;
import client.GameCharacter;
import java.net.InetAddress;
import client.GameClient;
import client.InventoryType;
import java.io.IOException;
import net.AbstractPacketHandler;
import net.server.Server;
import server.Trade;
import server.maps.FieldLimit;
import server.maps.HiredMerchant;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class ChangeChannelHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte channel = (byte) (reader.readByte() + 1);
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
			Trade.cancelTrade(c.getPlayer());
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
		if (chr.getBuffedValue(BuffStat.PUPPET) != null) {
			chr.cancelEffectFromBuffStat(BuffStat.PUPPET);
		}
		if (chr.getBuffedValue(BuffStat.COMBO) != null) {
			chr.cancelEffectFromBuffStat(BuffStat.COMBO);
		}
		chr.getInventory(InventoryType.EQUIPPED).checked(false); // test
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