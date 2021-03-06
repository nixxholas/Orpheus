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
import net.server.Server;
import client.GameClient;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegisterPicHandler extends AbstractPacketHandler {

	private static Logger log = LoggerFactory.getLogger(RegisterPicHandler.class);

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		reader.readByte();
		int charId = reader.readInt();
		String macs = reader.readMapleAsciiString();
		c.updateMacs(macs);
		if (c.hasBannedMac()) {
			c.getSession().close(true);
			return;
		}
		reader.readMapleAsciiString();
		String pic = reader.readMapleAsciiString();
		if (c.getPic() == null || c.getPic().equals("")) {
			c.setPic(pic);
			try {
				if (c.getIdleTask() != null) {
					c.getIdleTask().cancel(true);
				}
				c.updateLoginState(GameClient.State.TRANSITION);
				String channelServerIP = GameClient.getChannelServerIPFromSubnet(c.getSession().getRemoteAddress().toString().replace("/", "").split(":")[0], c.getChannelId());
				if (channelServerIP.equals("0.0.0.0")) {
					String[] socket = Server.getInstance().getIP(c.getWorldId(), c.getChannelId()).split(":");
					c.announce(PacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), charId));
				} else {
					String[] socket = Server.getInstance().getIP(c.getWorldId(), c.getChannelId()).split(":");
					c.announce(PacketCreator.getServerIP(InetAddress.getByName(channelServerIP), Integer.parseInt(socket[1]), charId));
				}
			} catch (UnknownHostException e) {
				log.error("Host not found", e);
			}
		} else {
			c.getSession().close(true);
		}
	}
}