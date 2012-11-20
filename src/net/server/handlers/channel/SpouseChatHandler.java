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

//import client.GameCharacter;
import client.GameClient;
//import client.command.CommandProcessor;
import net.AbstractPacketHandler;
//import tools.PacketCreator;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SpouseChatHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		Output.print(slea.toString());
		// slea.readMapleAsciiString();//recipient
		// String msg = slea.readMapleAsciiString();
		// if (!CommandProcessor.processCommand(c, msg))
		// if (c.getPlayer().isMarried()) {
		// GameCharacter wife =
		// c.getChannelServer().getPlayerStorage().getCharacterById(c.getPlayer().getPartnerId());
		// if (wife != null) {
		// wife.getClient().announce(PacketCreator.sendSpouseChat(c.getPlayer(),
		// msg));
		// c.announce(PacketCreator.sendSpouseChat(c.getPlayer(), msg));
		// } else
		// try {
		// if
		// (c.getChannelServer().getWorldInterface().isConnected(wife.getName()))
		// {
		// c.getChannelServer().getWorldInterface().sendSpouseChat(c.getPlayer().getName(),
		// wife.getName(), msg);
		// c.announce(PacketCreator.sendSpouseChat(c.getPlayer(), msg));
		// } else
		// c.getPlayer().message("You are either not married or your spouse is currently offline.");
		// } catch (Exception e) {
		// c.getPlayer().message("You are either not married or your spouse is currently offline.");
		// c.getChannelServer().reconnectWorld();
		// }
		// }
	}
}
