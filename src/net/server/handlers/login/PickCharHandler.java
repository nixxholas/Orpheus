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
package net.server.handlers.login;

import java.net.InetAddress;
import java.net.UnknownHostException;
import client.GameClient;
import net.server.Server;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PickCharHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int charId = reader.readInt();
		byte world = (byte) reader.readInt();// Wuuu? ):
		c.setWorldId(world);
		String macs = reader.readMapleAsciiString();
		c.updateMacs(macs);
		if (c.hasBannedMac()) {
			c.getSession().close(true);
			return;
		}
		try {
			c.setChannelId((byte) Randomizer.nextInt(Server.getInstance().getLoad(world).size()));
		} catch (Exception e) {
			c.setChannelId((byte) 1);
		}
		try {
			if (c.getIdleTask() != null) {
				c.getIdleTask().cancel(true);
			}
			c.updateLoginState(GameClient.LOGIN_SERVER_TRANSITION);
			String channelServerIP = GameClient.getChannelServerIPFromSubnet(c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0], c.getChannelId());
			if (channelServerIP.equals("0.0.0.0")) {
				String[] socket = Server.getInstance().getIP(c.getWorldId(), c.getChannelId()).split(":");
				c.announce(PacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
			} else {
				c.announce(PacketCreator.getServerIP(InetAddress.getByName(channelServerIP), Integer.parseInt(Server.getInstance().getIP(c.getWorldId(), c.getChannelId()).split(":")[1]), charId));
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
