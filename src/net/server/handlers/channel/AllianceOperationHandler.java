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
import net.SendOpcode;
import net.server.Server;
import net.server.guild.Alliance;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.PacketWriter;

/**
 * 
 * @author XoticStory
 */
public final class AllianceOperationHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		Alliance alliance = null;
		if (c.getPlayer().getGuild() != null && c.getPlayer().getGuild().getAllianceId() > 0) {
			alliance = Server.getInstance().getAlliance(c.getPlayer().getGuild().getAllianceId());
		}
		if (alliance == null) {
			c.getPlayer().dropMessage("You are not in an alliance.");
			c.announce(PacketCreator.enableActions());
			return;
		} else if (c.getPlayer().getMGC().getAllianceRank() > 2 || !alliance.getGuilds().contains(c.getPlayer().getGuildId())) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		switch (reader.readByte()) {
			case 0x01:
				Server.getInstance().allianceMessage(alliance.getId(), sendShowInfo(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId()), -1, -1);
				break;
			case 0x02: { 
				// Leave Alliance
				if (c.getPlayer().getGuild().getAllianceId() == 0 || c.getPlayer().getGuildId() < 1 || c.getPlayer().getGuildRank() != 1) {
					return;
				}
				Server.getInstance().allianceMessage(alliance.getId(), sendChangeGuild(c.getPlayer().getGuildId(), c.getPlayer().getId(), c.getPlayer().getGuildId(), 2), -1, -1);
				break;
			}
			case 0x03: 
				// send alliance invite
				String charName = reader.readMapleAsciiString();
				byte channel = c.getWorldServer().find(charName);
				if (channel == -1) {
					c.getPlayer().dropMessage("The player is not online.");
				} else {
					GameCharacter victim = Server.getInstance().getChannel(c.getWorld(), channel).getPlayerStorage().getCharacterByName(charName);
					if (victim.getGuildId() == 0) {
						c.getPlayer().dropMessage("The person you are trying to invite does not have a guild.");
					} else if (victim.getGuildRank() != 1) {
						c.getPlayer().dropMessage("The player is not the leader of his/her guild.");
					} else {
						Server.getInstance().allianceMessage(alliance.getId(), sendInvitation(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId(), reader.readMapleAsciiString()), -1, -1);
					}
				}
				break;
			case 0x04: {
				int guildid = reader.readInt();
				// reader.readMapleAsciiString();//guild name
				if (c.getPlayer().getGuild().getAllianceId() != 0 || c.getPlayer().getGuildRank() != 1 || c.getPlayer().getGuildId() < 1) {
					return;
				}
				Server.getInstance().allianceMessage(alliance.getId(), sendChangeGuild(guildid, c.getPlayer().getId(), c.getPlayer().getGuildId(), 0), -1, -1);
				break;
			}
			case 0x06: { 
				// Expel Guild
				int guildid = reader.readInt();
				int allianceid = reader.readInt();
				if (c.getPlayer().getGuild().getAllianceId() == 0 || c.getPlayer().getGuild().getAllianceId() != allianceid) {
					return;
				}
				Server.getInstance().allianceMessage(alliance.getId(), sendChangeGuild(allianceid, c.getPlayer().getId(), guildid, 1), -1, -1);
				break;
			}
			case 0x07: { 
				// Change Alliance Leader
				if (c.getPlayer().getGuild().getAllianceId() == 0 || c.getPlayer().getGuildId() < 1) {
					return;
				}
				Server.getInstance().allianceMessage(alliance.getId(), sendChangeLeader(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId(), reader.readInt()), -1, -1);
				break;
			}
			case 0x08:
				String ranks[] = new String[5];
				for (int i = 0; i < 5; i++) {
					ranks[i] = reader.readMapleAsciiString();
				}
				Server.getInstance().setAllianceRanks(alliance.getId(), ranks);
				Server.getInstance().allianceMessage(alliance.getId(), PacketCreator.changeAllianceRankTitle(alliance.getId(), ranks), -1, -1);
				break;
			case 0x09: {
				int int1 = reader.readInt();
				byte byte1 = reader.readByte();
				Server.getInstance().allianceMessage(alliance.getId(), sendChangeRank(c.getPlayer().getGuild().getAllianceId(), c.getPlayer().getId(), int1, byte1), -1, -1);
				break;
			}
			case 0x0A:
				String notice = reader.readMapleAsciiString();
				Server.getInstance().setAllianceNotice(alliance.getId(), notice);
				Server.getInstance().allianceMessage(alliance.getId(), PacketCreator.allianceNotice(alliance.getId(), notice), -1, -1);
				break;
			default:
				c.getPlayer().dropMessage("Feature not available");
		}
		alliance.saveToDB();
	}

	private static GamePacket sendShowInfo(int allianceId, int playerId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x02);
		w.writeInt(allianceId);
		w.writeInt(playerId);
		return w.getPacket();
	}

	private static GamePacket sendInvitation(int allianceId, int playerId, final String guildName) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x05);
		w.writeInt(allianceId);
		w.writeInt(playerId);
		w.writeLengthString(guildName);
		return w.getPacket();
	}

	private static GamePacket sendChangeGuild(int allianceId, int playerId, int guildId, int option) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x07);
		w.writeInt(allianceId);
		w.writeInt(guildId);
		w.writeInt(playerId);
		w.writeAsByte(option);
		return w.getPacket();
	}

	private static GamePacket sendChangeLeader(int allianceId, int playerId, int victim) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x08);
		w.writeInt(allianceId);
		w.writeInt(playerId);
		w.writeInt(victim);
		return w.getPacket();
	}

	private static GamePacket sendChangeRank(int allianceId, int playerId, int int1, byte byte1) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x09);
		w.writeInt(allianceId);
		w.writeInt(playerId);
		w.writeInt(int1);
		w.writeInt(byte1);
		return w.getPacket();
	}
}
