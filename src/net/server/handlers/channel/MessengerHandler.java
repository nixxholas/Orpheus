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
import net.server.MapleMessenger;
import net.server.MapleMessengerCharacter;
import net.server.World;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MessengerHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		String input;
		byte mode = slea.readByte();
		GameCharacter player = c.getPlayer();
		World world = c.getWorldServer();
		MapleMessenger messenger = player.getMessenger();
		switch (mode) {
			case 0x00:
				if (messenger == null) {
					int messengerid = slea.readInt();
					if (messengerid == 0) {
						MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
						messenger = world.createMessenger(messengerplayer);
						player.setMessenger(messenger);
						player.setMessengerPosition(0);
					} else {
						messenger = world.getMessenger(messengerid);
						int position = messenger.getLowestPosition();
						MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player, position);
						if (messenger.getMembers().size() < 3) {
							player.setMessenger(messenger);
							player.setMessengerPosition(position);
							world.joinMessenger(messenger.getId(), messengerplayer, player.getName(), messengerplayer.getChannel());
						}
					}
				}
				break;
			case 0x02:
				if (messenger != null) {
					MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
					world.leaveMessenger(messenger.getId(), messengerplayer);
					player.setMessenger(null);
					player.setMessengerPosition(4);
				}
				break;
			case 0x03:
				if (messenger.getMembers().size() < 3) {
					input = slea.readMapleAsciiString();
					GameCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);
					if (target != null) {
						if (target.getMessenger() == null) {
							target.getClient().announce(PacketCreator.messengerInvite(c.getPlayer().getName(), messenger.getId()));
							c.announce(PacketCreator.messengerNote(input, 4, 1));
						} else {
							c.announce(PacketCreator.messengerChat(player.getName() + " : " + input + " is already using Maple Messenger"));
						}
					} else {
						if (world.find(input) > -1) {
							world.messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getChannel());
						} else {
							c.announce(PacketCreator.messengerNote(input, 4, 0));
						}
					}
				} else {
					c.announce(PacketCreator.messengerChat(player.getName() + " : You cannot have more than 3 people in the Maple Messenger"));
				}
				break;
			case 0x05:
				String targeted = slea.readMapleAsciiString();
				GameCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
				if (target != null) {
					if (target.getMessenger() != null) {
						target.getClient().announce(PacketCreator.messengerNote(player.getName(), 5, 0));
					}
				} else {
					world.declineChat(targeted, player.getName());
				}
				break;
			case 0x06:
				if (messenger != null) {
					MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(player);
					input = slea.readMapleAsciiString();
					world.messengerChat(messenger, input, messengerplayer.getName());
				}
				break;
		}
	}
}
