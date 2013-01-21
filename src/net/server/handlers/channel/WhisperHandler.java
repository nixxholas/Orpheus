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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import paranoia.BlacklistHandler;
import constants.ParanoiaConstants;
import constants.ServerConstants;
import net.AbstractPacketHandler;
import net.server.World;
import tools.DatabaseConnection;
import tools.GameLogger;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class WhisperHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte mode = reader.readByte();
		if (mode == 6) { // whisper
			String recipient = reader.readMapleAsciiString();
			String text = reader.readMapleAsciiString();
			GameCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
			if (player != null) {
				if (ServerConstants.USE_PARANOIA && ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOG_BLACKLIST_CHAT) {
					if (BlacklistHandler.isBlacklisted(c.getAccountId())) {
						BlacklistHandler.printBlacklistLog("[Whisper] [" + c.getPlayer().getName() + " > " + recipient + "] " + text, c.getAccountId());
					}
				}
				if (ServerConstants.USE_PARANOIA && ParanoiaConstants.PARANOIA_CHAT_LOGGER && ParanoiaConstants.LOG_WHISPERS) {
					GameLogger.printFormatted(GameLogger.PARANOIA_CHAT, "[Whisper] [" + c.getPlayer().getName() + " > " + recipient + "] " + text);
				}
				if (text.length() <= ServerConstants.MAX_CHAT_MESSAGE_LENGTH) {
					player.getClient().announce(PacketCreator.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
					c.announce(PacketCreator.getWhisperReply(recipient, (byte) 1));
				} else {
					player.dropMessage("Your message was too long.");
				}
			} else {// not found
				World world = c.getWorldServer();
				if (world.isConnected(recipient)) {
					world.whisper(c.getPlayer().getName(), recipient, c.getChannel(), text);
					c.announce(PacketCreator.getWhisperReply(recipient, (byte) 1));
				} else {
					c.announce(PacketCreator.getWhisperReply(recipient, (byte) 0));
				}
			}
		} else if (mode == 5) { // - /find
			String recipient = reader.readMapleAsciiString();
			GameCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
			if (victim != null && c.getPlayer().getGmLevel() >= victim.getGmLevel()) {
				if (victim.getCashShop().isOpened()) {
					c.announce(PacketCreator.getFindReply(victim.getName(), -1, 2));
					// } else if (victim.inMTS()) {
					// c.announce(PacketCreator.getFindReply(victim.getName(),
					// -1, 0));
				} else {
					c.announce(PacketCreator.getFindReply(victim.getName(), victim.getMap().getId(), 1));
				}
			} else { // not found
				try {
					PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT gm FROM characters WHERE name = ?");
					ps.setString(1, recipient);
					ResultSet rs = ps.executeQuery();
					if (rs.next()) {
						if (rs.getInt("gm") > c.getPlayer().getGmLevel()) {
							c.announce(PacketCreator.getWhisperReply(recipient, (byte) 0));
							return;
						}
					}
					rs.close();
					ps.close();
					byte channel = (byte) (c.getWorldServer().find(recipient) - 1);
					if (channel > -1) {
						c.announce(PacketCreator.getFindReply(recipient, channel, 3));
					} else {
						c.announce(PacketCreator.getWhisperReply(recipient, (byte) 0));
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else if (mode == 0x44) {
			// Buddy find?
		}
	}
}
