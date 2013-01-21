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
import net.server.Party;
import net.server.PartyCharacter;
import net.server.PartyOperation;
import net.server.World;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PartyOperationHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int operation = reader.readByte();
		GameCharacter player = c.getPlayer();
		World world = c.getWorldServer();
		Party party = player.getParty();
		PartyCharacter partyplayer = player.getPartyCharacter();
		switch (operation) {
			case 1: { // create
				if (player.getParty() == null) {
					partyplayer = new PartyCharacter(player);
					party = world.createParty(partyplayer);
					player.setParty(party);
					player.setPartyCharacter(partyplayer);
					c.announce(PacketCreator.partyCreated());
				} else {
					c.announce(PacketCreator.serverNotice(5, "You can't create a party as you are already in one."));
				}
				break;
			}
			case 2: {
				if (party != null && partyplayer != null) {
					if (partyplayer.equals(party.getLeader())) {
						world.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
						if (player.getEventInstance() != null) {
							player.getEventInstance().disbandParty();
						}
					} else {
						world.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
						if (player.getEventInstance() != null) {
							player.getEventInstance().leftParty(player);
						}
					}
					player.setParty(null);
				}
				break;
			}
			case 3: {// join
				int partyid = reader.readInt();
				if (c.getPlayer().getParty() == null) {
					party = world.getParty(partyid);
					if (party != null) {
						if (party.getMembers().size() < 6) {
							partyplayer = new PartyCharacter(player);
							world.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
							player.receivePartyMemberHP();
							player.updatePartyMemberHP();
						} else {
							c.announce(PacketCreator.partyStatusMessage(17));
						}
					} else {
						c.announce(PacketCreator.serverNotice(5, "The person you have invited to the party is already in one."));
					}
				} else {
					c.announce(PacketCreator.serverNotice(5, "You can't join the party as you are already in one."));
				}
				break;
			}
			case 4: {// invite
				String name = reader.readMapleAsciiString();
				GameCharacter invited = world.getPlayerStorage().getCharacterByName(name);
				if (invited != null) {
					if (invited.getParty() == null) {
						if (party.getMembers().size() < 6) {
							invited.getClient().announce(PacketCreator.partyInvite(player));
						} else {
							c.announce(PacketCreator.partyStatusMessage(16));
						}
					} else {
						c.announce(PacketCreator.partyStatusMessage(17));
					}
				} else {
					c.announce(PacketCreator.partyStatusMessage(19));
				}
				break;
			}
			case 5: { // expel
				int cid = reader.readInt();
				if (partyplayer.equals(party.getLeader())) {
					PartyCharacter expelled = party.getMemberById(cid);
					if (expelled != null) {
						world.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
						if (player.getEventInstance() != null) {
							if (expelled.isOnline()) {
								player.getEventInstance().disbandParty();
							}
						}
					}
				}
				break;
			}

			case 6: {
				int newLeader = reader.readInt();
				PartyCharacter newLeadr = party.getMemberById(newLeader);
				party.setLeader(newLeadr);
				world.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newLeadr);
				break;
			}
		}
	}
}