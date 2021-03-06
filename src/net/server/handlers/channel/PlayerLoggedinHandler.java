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

import client.BuddylistEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import client.GameCharacter;
import client.GameClient;
import client.Family;
import client.InventoryType;
import client.LinkedCharacterInfo;
import client.skills.SkillFactory;
import gm.server.GMServer;
import java.sql.SQLException;
import java.util.List;
import constants.ScriptableNpcConstants;
import constants.ServerConstants;
import tools.DatabaseConnection;
import net.AbstractPacketHandler;
import net.server.Channel;
import net.server.CharacterIdChannelPair;
import net.server.PartyCharacter;
import net.server.PartyOperation;
import net.server.PlayerBuffValueHolder;
import net.server.Server;
import net.server.World;
import net.server.guild.Alliance;
import net.server.guild.Guild;
import tools.PacketCreator;
import tools.GameLogger;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;

public final class PlayerLoggedinHandler extends AbstractPacketHandler {

	@Override
	public final boolean validateState(GameClient c) {
		return !c.isLoggedIn();
	}

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		final int characterId = reader.readInt();
		final Server server = Server.getInstance();
		GameCharacter player = c.getWorldServer().getPlayerStorage().getCharacterById(characterId);
		if (player == null) {
			try {
				player = GameCharacter.loadFromDb(characterId, c, true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			player.newClient(c);
		}
		if (player.isGM() && server.isGMServerEnabled()) {
			GMServer.getInstance().addInGame(player.getName(), c.getSession());
		}
		c.setPlayer(player);
		c.setAccountId(player.getAccountId());
		GameClient.State state = c.getLoginState();
		boolean allowLogin = true;
		Channel cserv = c.getChannelServer();

		synchronized (this) {
			if (state.is(GameClient.State.TRANSITION)) {
				for (String charName : c.loadCharacterNames(c.getWorldId())) {
					for (Channel ch : c.getWorldServer().getChannels()) {
						if (ch.isConnected(charName)) {
							StringBuilder sb = new StringBuilder();
							sb.append("[" + Output.now() + "] ").append(player.getName()).append(" failed to login.\r\n");
							sb.append("[" + Output.now() + "] The player ").append((c.getWorldServer().isConnected(charName)) ? "is" : "isn't").append(" connected to the world server.\r\n");
							sb.append("[" + Output.now() + "] The player has character ").append(charName).append(" connected.\r\n");
							GameLogger.print(GameLogger.ACCOUNT_STUCK, sb.toString());
							if (ServerConstants.AUTO_UNSTUCK_ACCOUNTS) {
								ch.removePlayer(ch.getPlayerStorage().getCharacterByName(charName)); // unstuck
							}
							allowLogin = false;
						}
					}
					break;
				}
			}

			if (!state.is(GameClient.State.TRANSITION) || !allowLogin) {
				c.setPlayer(null);
				c.announce(PacketCreator.getAfterLoginError(7));
				return;
			}
			c.updateLoginState(GameClient.State.NOT_LOGGED_IN);
		}
		cserv.addPlayer(player);
		List<PlayerBuffValueHolder> buffs = server.getPlayerBuffStorage().getBuffsFromStorage(characterId);
		if (buffs != null) {
			player.silentGiveBuffs(buffs);
		}
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		PreparedStatement pss = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT Mesos FROM dueypackages WHERE RecieverId = ? and Checked = 1");
			ps.setInt(1, player.getId());
			rs = ps.executeQuery();
			if (rs.next()) {
				try {
					pss = DatabaseConnection.getConnection().prepareStatement("UPDATE dueypackages SET Checked = 0 where RecieverId = ?");
					pss.setInt(1, player.getId());
					pss.executeUpdate();
					pss.close();
				} catch (SQLException e) {
				}
				c.announce(PacketCreator.sendDueyMSG((byte) 0x1B));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pss != null) {
					pss.close();
				}
				if (ps != null) {
					ps.close();
				}
			} catch (SQLException ex) {
				// ignore
			}
		}
		c.announce(PacketCreator.getCharInfo(player));
		if (!player.isHidden() && ServerConstants.HIDE_GMS_ON_LOGIN) {
			player.toggleHide(true);
		}
		player.sendKeymap();
		player.sendMacros();
		player.getMap().addPlayer(player);
		World world = server.getWorld(c.getWorldId());
		world.getPlayerStorage().addPlayer(player);
		server.getLoad(c.getWorldId()).get(c.getChannelId()).incrementAndGet();
		int buddyIds[] = player.getBuddylist().getBuddyIds();
		world.loggedOn(player.getName(), player.getId(), c.getChannelId(), buddyIds);
		for (CharacterIdChannelPair onlineBuddy : server.getWorld(c.getWorldId()).multiBuddyFind(player.getId(), buddyIds)) {
			BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
			ble.setChannel(onlineBuddy.getChannel());
			player.getBuddylist().put(ble);
		}
		c.announce(PacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
		c.announce(PacketCreator.loadFamily(player));
		if (player.getFamilyId() > 0) {
			Family f = world.getFamily(player.getFamilyId());
			if (f == null) {
				f = new Family(player.getId());
				world.addFamily(player.getFamilyId(), f);
			}
			player.setFamily(f);
			c.announce(PacketCreator.getFamilyInfo(f.getMember(player.getId())));
		}
		if (player.getGuildId() > 0) {
			Guild playerGuild = server.getGuild(player.getGuildId(), player.getGuildCharacter());
			if (playerGuild == null) {
				player.deleteGuild(player.getGuildId());
				player.resetGuildCharacter();
				player.setGuildId(0);
			} else {
				server.setGuildMemberOnline(player.getGuildCharacter(), true, c.getChannelId());
				c.announce(PacketCreator.showGuildInfo(player));
				int allianceId = player.getGuild().getAllianceId();
				if (allianceId > 0) {
					Alliance newAlliance = server.getAlliance(allianceId);
					if (newAlliance == null) {
						newAlliance = Alliance.loadAlliance(allianceId);
						if (newAlliance != null) {
							server.addAlliance(allianceId, newAlliance);
						} else {
							player.getGuild().setAllianceId(0);
						}
					}
					if (newAlliance != null) {
						c.announce(PacketCreator.getAllianceInfo(newAlliance));
						c.announce(PacketCreator.getGuildAlliances(newAlliance, c));
						server.allianceMessage(allianceId, PacketCreator.allianceMemberOnline(player, true), player.getId(), -1);
					}
				}
			}
		}
		player.showNote();
		if (player.getParty() != null) {
			PartyCharacter pchar = player.getPartyCharacter();
			pchar.setChannelId(c.getChannelId());
			pchar.setMapId(player.getMapId());
			pchar.setOnline(true);
			world.updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, pchar);
		}
		player.updatePartyMemberHP();
		/*
		 * Wrong packet, well at least it must popup when you open the buddy
		 * window. And when you open it, it doesn't send something to the
		 * server. So with this information I am assuming it's another packet.
		 * CharacterNameAndId pendingBuddyRequest =
		 * player.getBuddylist().pollPendingRequest(); if (pendingBuddyRequest
		 * != null) { player.getBuddylist().put(new
		 * BuddylistEntry(pendingBuddyRequest.getName(), "Default Group",
		 * pendingBuddyRequest.getId(), (byte) -1, false));
		 * c.announce(PacketCreator
		 * .requestBuddylistAdd(pendingBuddyRequest.getId(), player.getId(),
		 * pendingBuddyRequest.getName())); }
		 */
		if (player.getInventory(InventoryType.EQUIPPED).findById(1122017) != null) {
			player.equipPendantOfSpirit();
		}
		c.announce(PacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
		c.announce(PacketCreator.updateGender(player));
		player.checkMessenger();
		c.announce(PacketCreator.enableReport());
		
		final LinkedCharacterInfo info = player.getLinkedCharacter();
		final int level = info == null ? 0 : info.level;
		player.changeSkillLevel(SkillFactory.getSkill(10000000 * player.getJobType() + 12), (byte) (level / 10), 20, -1);
		player.checkBerserk();
		player.expirationTask();
		player.refreshRates();
		if (ServerConstants.MAKE_NPCS_SCRIPTABLE) {
			for (int i = 0; i < ScriptableNpcConstants.SCRIPTABLE_NPCS.length; i++) {
				c.announce(PacketCreator.setNpcScriptable(ScriptableNpcConstants.SCRIPTABLE_NPCS[i], ScriptableNpcConstants.SCRIPTABLE_NPCS_DESC[i]));
			}
		}
		if (ServerConstants.GREET_PLAYERS_ON_LOGIN && !player.isGM()) {
			Server.getInstance().broadcastMessage(c.getWorldId(), PacketCreator.serverNotice(6, "[Notice] " + player.getName() + " has logged in."));
		}
		if (ServerConstants.GREET_GMS_ON_LOGIN && player.isGM()) {
			Server.getInstance().broadcastMessage(c.getWorldId(), PacketCreator.serverNotice(6, "[Notice] " + player.getName() + " (" + player.getStaffRank() + ") has logged in."));
		}
	}
}
