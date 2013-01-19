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
import tools.GameLogger;
import tools.PacketCreator;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;
import client.GameClient;
import client.command.AdminCommands;
import client.command.DeveloperCommands;
import client.command.DonorCommands;
import client.command.EnumeratedCommands;
import client.command.GMCommands;
import client.command.PlayerCommands;
import client.command.SupportCommands;
import client.command.external.CommandLoader;

/**
 * @author Aaron Weiss
 */
public final class GeneralChatHandler extends net.AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		String s = slea.readMapleAsciiString();
		GameCharacter chr = c.getPlayer();
		char heading = s.charAt(0);
		/*
		 * WARNING: Daemons below!
		 * The following code is messy, and hard to follow.
		 * This is in an attempt to make the client.command classes
		 * easier to understand and work with. I apologize for the mess!
		 * 
		 * As of May 6th, the daemons should be a little cleaner.
		 * 
		 * As of May 27th, daemons are messier because of CommandLoader and extern commands.
		 */
		if (heading == '/' || heading == '!' || heading == '@') {
			String[] sp = s.split(" ");
			sp[0] = sp[0].toLowerCase().substring(1);
			if (ServerConstants.USE_EXTERNAL_COMMAND_LOADER) {
				if (!CommandLoader.isInitialized()) {
					try {
						Output.print("Loading commands.");
						long startTime = System.currentTimeMillis();
						CommandLoader.getInstance().load(ServerConstants.COMMAND_JAR_PATH);
						Output.print("Loading completed in " + ((System.currentTimeMillis() - startTime)) + "ms.");
					} catch (Exception e) {
						Output.print("Failed to load commands.");
						GameLogger.print(GameLogger.EXCEPTION_CAUGHT, e);
					}
				} else if (CommandLoader.isInitialized() && CommandLoader.getInstance().getCommandProcessor() != null) {
					CommandLoader.getInstance().getCommandProcessor().execute(c, sp, heading);
				} else {
					Output.print("External CommandLoader is broken.");
				}
			} else if (heading == '@' || heading == '/') {
				boolean commandExecuted = true;
				switch (chr.gmLevel()) {
					case 5:
					case 4:
					case 3: 
					case 2:
					case 1:
						commandExecuted = DonorCommands.execute(c, sp, heading);
						if (commandExecuted) break;
					case 0:
						commandExecuted = PlayerCommands.execute(c, sp, heading);
						if (commandExecuted) break;
					default:
						EnumeratedCommands.execute(c, sp, heading);
						break;
				}
			} else {
				boolean commandExecuted = false;
				switch (chr.gmLevel()) {
					case 5:
						commandExecuted = AdminCommands.execute(c, sp, heading);
						if (commandExecuted) break;
					case 4:
						DeveloperCommands.setSLEA(slea);
						commandExecuted = DeveloperCommands.execute(c, sp, heading);
						if (commandExecuted) break;
					case 3:
						commandExecuted = GMCommands.execute(c, sp, heading);
						if (commandExecuted) break;
					case 2:
						commandExecuted = SupportCommands.execute(c, sp, heading);
						if (commandExecuted) break;
					default:
						EnumeratedCommands.execute(c, sp, heading);
						break;
				}
			}
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOG_BLACKLIST_COMMAND) {
				if (BlacklistHandler.isBlacklisted(c.getAccID())) {
					BlacklistHandler.printBlacklistLog("[" + c.getPlayer().getName() + "] Used " + heading + sp[0] + ((sp.length > 1) ? " with parameters: " + Output.joinStringFrom(sp, 1) : "."), c.getAccID());
				}
			}
		} else {
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOG_BLACKLIST_CHAT) {
				if (BlacklistHandler.isBlacklisted(c.getAccID())) {
					BlacklistHandler.printBlacklistLog("[General] [" + c.getPlayer().getName() + "] " + s, c.getAccID());
				}
			}
			if (ServerConstants.USE_PARANOIA && ParanoiaConstants.PARANOIA_CHAT_LOGGER && ParanoiaConstants.LOG_GENERAL_CHAT) {
				GameLogger.printFormatted(GameLogger.PARANOIA_CHAT, "[General] [" + c.getPlayer().getName() + "] " + s);
			}
			if (!chr.isHidden()) {
				if (s.length() <= ServerConstants.MAX_CHAT_MESSAGE_LENGTH) {
					chr.getMap().broadcastMessage(PacketCreator.getChatText(chr.getId(), s, (chr.isGM() && chr.getGmText()), slea.readByte()));
				} else {
					chr.dropMessage("Your message was too long.");
				}
			} else {
				chr.getMap().broadcastGMMessage(PacketCreator.getChatText(chr.getId(), s, (chr.isGM() && chr.getGmText()), slea.readByte()));
			}
		}
	}
}
