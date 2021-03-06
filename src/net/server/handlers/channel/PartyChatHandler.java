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

import paranoia.BlacklistHandler;
import constants.ParanoiaConstants;
import constants.ServerConstants;
import client.GameCharacter;
import client.GameClient;
import net.AbstractPacketHandler;
import net.server.Server;
import net.server.World;
import tools.GameLogger;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PartyChatHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter player = c.getPlayer();
		int type = reader.readByte(); // 0 for buddys, 1 for partys
		int numRecipients = reader.readByte();
		int recipients[] = new int[numRecipients];
		for (int i = 0; i < numRecipients; i++) {
			recipients[i] = reader.readInt();
		}
		String chattext = reader.readMapleAsciiString();
		World world = c.getWorldServer();
		if (chattext.length() > ServerConstants.MAX_CHAT_MESSAGE_LENGTH) {
			player.dropMessage("Your message is too long.");
			return; // packet editing, fucker.
		}
		if (type == 0) {
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.PARANOIA_CHAT_LOGGER && ParanoiaConstants.LOG_BUDDY_CHAT) {
				GameLogger.printFormatted(GameLogger.PARANOIA_CHAT, "[Buddy] [" + c.getPlayer().getName() + "] " + chattext);
			}
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOG_BLACKLIST_CHAT) {
				if (BlacklistHandler.isBlacklisted(c.getAccountId())) {
					BlacklistHandler.printBlacklistLog("[Buddy] [" + c.getPlayer().getName() + "] " + chattext, c.getAccountId());
				}
			}
			world.buddyChat(recipients, player.getId(), player.getName(), chattext);
		} else if (type == 1 && player.getParty() != null) {
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.PARANOIA_CHAT_LOGGER && ParanoiaConstants.LOG_PARTY_CHAT) {
				GameLogger.printFormatted(GameLogger.PARANOIA_CHAT, "[Party] [" + c.getPlayer().getName() + "] " + chattext);
			}
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOG_BLACKLIST_CHAT) {
				if (BlacklistHandler.isBlacklisted(c.getAccountId())) {
					BlacklistHandler.printBlacklistLog("[Party] [" + c.getPlayer().getName() + "] " + chattext, c.getAccountId());
				}
			}
			world.partyChat(player.getParty(), chattext, player.getName());
		} else if (type == 2 && player.getGuildId() > 0) {
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.PARANOIA_CHAT_LOGGER && ParanoiaConstants.LOG_GUILD_CHAT) {
				GameLogger.printFormatted(GameLogger.PARANOIA_CHAT, "[Guild] [" + c.getPlayer().getName() + "] " + chattext);
			}
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOG_BLACKLIST_CHAT) {
				if (BlacklistHandler.isBlacklisted(c.getAccountId())) {
					BlacklistHandler.printBlacklistLog("[Guild] [" + c.getPlayer().getName() + "] " + chattext, c.getAccountId());
				}
			}
			Server.getInstance().guildChat(player.getGuildId(), player.getName(), player.getId(), chattext);
		} else if (type == 3 && player.getGuild() != null) {
			int allianceId = player.getGuild().getAllianceId();
			if (allianceId > 0) {
				if (ServerConstants.USE_PARANOIA && ParanoiaConstants.PARANOIA_CHAT_LOGGER && ParanoiaConstants.LOG_ALLIANCE_CHAT) {
					GameLogger.printFormatted(GameLogger.PARANOIA_CHAT, "[Alliance] [" + c.getPlayer().getName() + "] " + chattext);
				}
				if (ServerConstants.USE_PARANOIA && ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOG_BLACKLIST_CHAT) {
					if (BlacklistHandler.isBlacklisted(c.getAccountId())) {
						BlacklistHandler.printBlacklistLog("[Alliance] [" + c.getPlayer().getName() + "] " + chattext, c.getAccountId());
					}
				}
				Server.getInstance().allianceMessage(allianceId, PacketCreator.multiChat(player.getName(), chattext, 3), player.getId(), -1);
			}
		}
	}
}
