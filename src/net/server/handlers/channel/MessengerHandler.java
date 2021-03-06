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
import net.AbstractPacketHandler;
import net.GamePacket;
import net.server.Messenger;
import net.server.MessengerCharacter;
import net.server.MessengerState;
import net.server.World;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MessengerHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte mode = reader.readByte();
		GameCharacter player = c.getPlayer();
		World world = c.getWorldServer();
		MessengerState state = player.getMessengerState();
		switch (mode) {
			case 0x00:
				if (!state.isActive()) {
					int messengerId = reader.readInt();
					if (messengerId == 0) {
						MessengerCharacter member = new MessengerCharacter(player);
						final Messenger messenger = world.createMessenger(member);
						state.accept(messenger, 0);
					} else {
						final Messenger messenger = world.getMessenger(messengerId);
						int position = messenger.getLowestPosition();
						MessengerCharacter member = new MessengerCharacter(player, position);
						if (messenger.getMembers().size() < 3) {
							state.accept(messenger, position);
							world.joinMessenger(messenger.getId(), member, player.getName(), member.getChannel());
						}
					}
				}
				break;
				
			case 0x02:
				if (state.isActive()) {
					MessengerCharacter member = new MessengerCharacter(player);
					world.leaveMessenger(state.getId(), member);
					state.reset();
				}
				break;
				
			case 0x03:
				if (state.isActive()) {
					if (state.hasCapacity()) {
						String input = reader.readMapleAsciiString();
						GameCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);
						if (target != null) {
							if (!target.getMessengerState().isActive()) {
								final GamePacket invite = PacketCreator.messengerInvite(c.getPlayer().getName(), state.getId());
								target.getClient().announce(invite);
								c.announce(PacketCreator.messengerNote(input, 4, 1));
							} else {
								c.announce(PacketCreator.messengerChat(player.getName() + " : " + input + " is already using Maple Messenger"));
							}
						} else {
							if (world.find(input) > -1) {
								world.messengerInvite(c.getPlayer().getName(), state.getId(), input, c.getChannelId());
							} else {
								c.announce(PacketCreator.messengerNote(input, 4, 0));
							}
						}
					} else {
						c.announce(PacketCreator.messengerChat(player.getName() + " : You cannot have more than 3 people in the Maple Messenger"));
					}
				}
				break;
				
			case 0x05:
				String targeted = reader.readMapleAsciiString();
				GameCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
				if (target != null) {
					if (target.getMessengerState().isActive()) {
						target.getClient().announce(PacketCreator.messengerNote(player.getName(), 5, 0));
					}
				} else {
					world.declineChat(targeted, player.getName());
				}
				break;
				
			case 0x06:
				if (state.isActive()) {
					String input = reader.readMapleAsciiString();
					world.messengerChat(state.getId(), input, player.getName());
				}
				break;
		}
	}
}
