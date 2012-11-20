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
package net.server;

import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.GameCharacter;
import client.Family;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.GamePacket;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import net.server.guild.GuildSummary;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Output;

/**
 * 
 * @author kevintjuh93
 */
public class World {

	private byte id, flag;
	private int exprate, droprate, mesorate, bossdroprate;
	private String eventmsg;
	private List<Channel> channels = new ArrayList<Channel>();
	private Map<Integer, Party> parties = new HashMap<Integer, Party>();
	private AtomicInteger runningPartyId = new AtomicInteger();
	private Map<Integer, Messenger> messengers = new HashMap<Integer, Messenger>();
	private AtomicInteger runningMessengerId = new AtomicInteger();
	private Map<Integer, Family> families = new LinkedHashMap<Integer, Family>();
	private Map<Integer, GuildSummary> gsStore = new HashMap<Integer, GuildSummary>();
	private PlayerStorage players = new PlayerStorage();

	public World(byte world, byte flag, String eventmsg, int exprate, int droprate, int mesorate, int bossdroprate) {
		this.id = world;
		this.flag = flag;
		this.eventmsg = eventmsg;
		this.exprate = exprate;
		this.droprate = droprate;
		this.mesorate = mesorate;
		this.bossdroprate = bossdroprate;
		runningPartyId.set(1);
		runningMessengerId.set(1);
	}

	public List<Channel> getChannels() {
		return channels;
	}

	public Channel getChannel(byte channel) {
		return channels.get(channel - 1);
	}

	public void addChannel(Channel channel) {
		channels.add(channel);
	}

	public void removeChannel(byte channel) {
		channels.remove(channel);
	}

	public void setFlag(byte b) {
		this.flag = b;
	}

	public byte getFlag() {
		return flag;
	}

	public String getEventMessage() {
		return eventmsg;
	}

	public int getExpRate() {
		return exprate;
	}

	public void setExpRate(int exp) {
		this.exprate = exp;
	}

	public int getDropRate() {
		return droprate;
	}

	public void setDropRate(int drop) {
		this.droprate = drop;
	}

	public int getMesoRate() {
		return mesorate;
	}

	public void setMesoRate(int meso) {
		this.mesorate = meso;
	}

	public int getBossDropRate() {
		return bossdroprate;
	}

	public PlayerStorage getPlayerStorage() {
		return players;
	}

	public void removePlayer(GameCharacter chr) {
		channels.get(chr.getClient().getChannel() - 1).removePlayer(chr);
		players.removePlayer(chr.getId());
	}

	public byte getId() {
		return id;
	}

	public void addFamily(int id, Family f) {
		synchronized (families) {
			if (!families.containsKey(id)) {
				families.put(id, f);
			}
		}
	}

	public Family getFamily(int id) {
		synchronized (families) {
			if (families.containsKey(id)) {
				return families.get(id);
			}
			return null;
		}
	}

	public Guild getGuild(GuildCharacter mgc) {
		int gid = mgc.getGuildId();
		Guild g = null;
		g = Server.getInstance().getGuild(gid, mgc);
		if (gsStore.get(gid) == null) {
			gsStore.put(gid, new GuildSummary(g));
		}
		return g;
	}

	public GuildSummary getGuildSummary(int gid) {
		if (gsStore.containsKey(gid)) {
			return gsStore.get(gid);
		} else {
			Guild g = Server.getInstance().getGuild(gid, null);
			if (g != null) {
				gsStore.put(gid, new GuildSummary(g));
			}
			return gsStore.get(gid);
		}
	}

	public void updateGuildSummary(int gid, GuildSummary mgs) {
		gsStore.put(gid, mgs);
	}

	public void reloadGuildSummary() {
		Guild g;
		Server server = Server.getInstance();
		for (int i : gsStore.keySet()) {
			g = server.getGuild(i, null);
			if (g != null) {
				gsStore.put(i, new GuildSummary(g));
			} else {
				gsStore.remove(i);
			}
		}
	}

	public void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) {
		for (int cid : cids) {
			if (cid != exception) {
				setGuildAndRank(cid, guildid, rank);
			}
		}
	}

	public void setOfflineGuildStatus(int guildid, byte guildrank, int cid) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
			ps.setInt(1, guildid);
			ps.setInt(2, guildrank);
			ps.setInt(3, cid);
			ps.execute();
			ps.close();
			ps = null;
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	public void setGuildAndRank(int cid, int guildid, int rank) {
		GameCharacter player = getPlayerStorage().getCharacterById(cid);
		if (player == null) {
			return;
		}
		boolean bDifferentGuild;
		if (guildid == -1 && rank == -1) {
			bDifferentGuild = true;
		} else {
			bDifferentGuild = guildid != player.getGuildId();
			player.setGuildId(guildid);
			player.setGuildRank(rank);
			player.saveGuildStatus();
		}
		if (bDifferentGuild) {
			player.getMap().broadcastMessage(player, PacketCreator.removePlayerFromMap(cid), false);
			player.getMap().broadcastMessage(player, PacketCreator.spawnPlayerMapobject(player), false);
		}
	}

	public void changeEmblem(int gid, List<Integer> affectedPlayers, GuildSummary mgs) {
		updateGuildSummary(gid, mgs);
		sendPacket(affectedPlayers, PacketCreator.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
		setGuildAndRank(affectedPlayers, -1, -1, -1); // respawn player
	}

	public void sendPacket(List<Integer> targetIds, GamePacket packet, int exception) {
		GameCharacter c;
		for (int i : targetIds) {
			if (i == exception) {
				continue;
			}
			c = getPlayerStorage().getCharacterById(i);
			if (c != null) {
				c.getClient().getSession().write(packet);
			}
		}
	}

	public Party createParty(PartyCharacter chrfor) {
		int partyid = runningPartyId.getAndIncrement();
		Party party = new Party(partyid, chrfor);
		parties.put(party.getId(), party);
		return party;
	}

	public Party getParty(int partyid) {
		return parties.get(partyid);
	}

	public Party disbandParty(int partyid) {
		return parties.remove(partyid);
	}

	public void updateParty(Party party, PartyOperation operation, PartyCharacter target) {
		for (PartyCharacter partychar : party.getMembers()) {
			GameCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
			if (chr != null) {
				if (operation == PartyOperation.DISBAND) {
					chr.setParty(null);
					chr.setMPC(null);
				} else {
					chr.setParty(party);
					chr.setMPC(partychar);
				}
				chr.getClient().getSession().write(PacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
			}
		}
		switch (operation) {
			case LEAVE:
			case EXPEL:
				GameCharacter chr = getPlayerStorage().getCharacterByName(target.getName());
				if (chr != null) {
					chr.getClient().getSession().write(PacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
					chr.setParty(null);
					chr.setMPC(null);
				}
			default:
				return;
		}
	}

	public void updateParty(int partyid, PartyOperation operation, PartyCharacter target) {
		Party party = getParty(partyid);
		if (party == null) {
			throw new IllegalArgumentException("no party with the specified partyid exists");
		}
		switch (operation) {
			case JOIN:
				party.addMember(target);
				break;
			case EXPEL:
			case LEAVE:
				party.removeMember(target);
				break;
			case DISBAND:
				disbandParty(partyid);
				break;
			case SILENT_UPDATE:
			case LOG_ONOFF:
				party.updateMember(target);
				break;
			case CHANGE_LEADER:
				party.setLeader(target);
				break;
			default:
				Output.print("Unhandled updateParty operation " + operation.name() + ".");
		}
		updateParty(party, operation, target);
	}

	public byte find(String name) {
		byte channel = -1;
		GameCharacter chr = getPlayerStorage().getCharacterByName(name);
		if (chr != null) {
			channel = chr.getClient().getChannel();
		}
		return channel;
	}

	public byte find(int id) {
		byte channel = -1;
		GameCharacter chr = getPlayerStorage().getCharacterById(id);
		if (chr != null) {
			channel = chr.getClient().getChannel();
		}
		return channel;
	}

	public void partyChat(Party party, String chattext, String namefrom) {
		for (PartyCharacter partychar : party.getMembers()) {
			if (!(partychar.getName().equals(namefrom))) {
				GameCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
				if (chr != null) {
					chr.getClient().getSession().write(PacketCreator.multiChat(namefrom, chattext, 1));
				}
			}
		}
	}

	public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) {
		PlayerStorage playerStorage = getPlayerStorage();
		for (int characterId : recipientCharacterIds) {
			GameCharacter chr = playerStorage.getCharacterById(characterId);
			if (chr != null) {
				if (chr.getBuddylist().containsVisible(cidFrom)) {
					chr.getClient().getSession().write(PacketCreator.multiChat(nameFrom, chattext, 0));
				}
			}
		}
	}

	public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
		List<CharacterIdChannelPair> foundsChars = new ArrayList<CharacterIdChannelPair>(characterIds.length);
		for (Channel ch : getChannels()) {
			for (int charid : ch.multiBuddyFind(charIdFrom, characterIds)) {
				foundsChars.add(new CharacterIdChannelPair(charid, ch.getId()));
			}
		}
		return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
	}

	public Messenger getMessenger(int messengerid) {
		return messengers.get(messengerid);
	}

	public void leaveMessenger(int messengerid, MessengerCharacter target) {
		Messenger messenger = getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		int position = messenger.getPositionByName(target.getName());
		messenger.removeMember(target);
		removeMessengerPlayer(messenger, position);
	}

	public void messengerInvite(String sender, int messengerid, String target, byte fromchannel) {
		if (isConnected(target)) {
			Messenger messenger = getPlayerStorage().getCharacterByName(target).getMessenger();
			if (messenger == null) {
				getPlayerStorage().getCharacterByName(target).getClient().getSession().write(PacketCreator.messengerInvite(sender, messengerid));
				GameCharacter from = getChannel(fromchannel).getPlayerStorage().getCharacterByName(sender);
				from.getClient().getSession().write(PacketCreator.messengerNote(target, 4, 1));
			} else {
				GameCharacter from = getChannel(fromchannel).getPlayerStorage().getCharacterByName(sender);
				from.getClient().getSession().write(PacketCreator.messengerChat(sender + " : " + target + " is already using Maple Messenger"));
			}
		}
	}

	public void addMessengerPlayer(Messenger messenger, String namefrom, byte fromchannel, int position) {
		for (MessengerCharacter messengerchar : messenger.getMembers()) {
			if (!(messengerchar.getName().equals(namefrom))) {
				GameCharacter chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					GameCharacter from = getChannel(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
					chr.getClient().getSession().write(PacketCreator.addMessengerPlayer(namefrom, from, position, (byte) (fromchannel - 1)));
					from.getClient().getSession().write(PacketCreator.addMessengerPlayer(chr.getName(), chr, messengerchar.getPosition(), (byte) (messengerchar.getChannel() - 1)));
				}
			} else if ((messengerchar.getName().equals(namefrom))) {
				GameCharacter chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().getSession().write(PacketCreator.joinMessenger(messengerchar.getPosition()));
				}
			}
		}
	}

	public void removeMessengerPlayer(Messenger messenger, int position) {
		for (MessengerCharacter messengerchar : messenger.getMembers()) {
			GameCharacter chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
			if (chr != null) {
				chr.getClient().getSession().write(PacketCreator.removeMessengerPlayer(position));
			}
		}
	}

	public void messengerChat(Messenger messenger, String chattext, String namefrom) {
		for (MessengerCharacter messengerchar : messenger.getMembers()) {
			if (!(messengerchar.getName().equals(namefrom))) {
				GameCharacter chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().getSession().write(PacketCreator.messengerChat(chattext));
				}
			}
		}
	}

	public void declineChat(String target, String namefrom) {
		if (isConnected(target)) {
			GameCharacter chr = getPlayerStorage().getCharacterByName(target);
			if (chr != null && chr.getMessenger() != null) {
				chr.getClient().getSession().write(PacketCreator.messengerNote(namefrom, 5, 0));
			}
		}
	}

	public void updateMessenger(int messengerid, String namefrom, byte fromchannel) {
		Messenger messenger = getMessenger(messengerid);
		int position = messenger.getPositionByName(namefrom);
		updateMessenger(messenger, namefrom, position, fromchannel);
	}

	public void updateMessenger(Messenger messenger, String namefrom, int position, byte fromchannel) {
		for (MessengerCharacter messengerchar : messenger.getMembers()) {
			Channel ch = getChannel(fromchannel);
			if (!(messengerchar.getName().equals(namefrom))) {
				GameCharacter chr = ch.getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					chr.getClient().getSession().write(PacketCreator.updateMessengerPlayer(namefrom, getChannel(fromchannel).getPlayerStorage().getCharacterByName(namefrom), position, (byte) (fromchannel - 1)));
				}
			}
		}
	}

	public void silentLeaveMessenger(int messengerid, MessengerCharacter target) {
		Messenger messenger = getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.silentRemoveMember(target);
	}

	public void joinMessenger(int messengerid, MessengerCharacter target, String from, byte fromchannel) {
		Messenger messenger = getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.addMember(target);
		addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
	}

	public void silentJoinMessenger(int messengerid, MessengerCharacter target, int position) {
		Messenger messenger = getMessenger(messengerid);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.silentAddMember(target, position);
	}

	public Messenger createMessenger(MessengerCharacter chrfor) {
		int messengerid = runningMessengerId.getAndIncrement();
		Messenger messenger = new Messenger(messengerid, chrfor);
		messengers.put(messenger.getId(), messenger);
		return messenger;
	}

	public boolean isConnected(String charName) {
		return getPlayerStorage().getCharacterByName(charName) != null;
	}

	public void whisper(String sender, String target, byte channel, String message) {
		if (isConnected(target)) {
			getPlayerStorage().getCharacterByName(target).getClient().getSession().write(PacketCreator.getWhisper(sender, channel, message));
		}
	}

	public BuddyAddResult requestBuddyAdd(String addName, byte channelFrom, int cidFrom, String nameFrom) {
		GameCharacter addChar = getPlayerStorage().getCharacterByName(addName);
		if (addChar != null) {
			BuddyList buddylist = addChar.getBuddylist();
			if (buddylist.isFull()) {
				return BuddyAddResult.BUDDYLIST_FULL;
			}
			if (!buddylist.contains(cidFrom)) {
				buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom);
			} else if (buddylist.containsVisible(cidFrom)) {
				return BuddyAddResult.ALREADY_ON_LIST;
			}
		}
		return BuddyAddResult.OK;
	}

	public void buddyChanged(int cid, int cidFrom, String name, byte channel, BuddyOperation operation) {
		GameCharacter addChar = getPlayerStorage().getCharacterById(cid);
		if (addChar != null) {
			BuddyList buddylist = addChar.getBuddylist();
			switch (operation) {
				case ADDED:
					if (buddylist.contains(cidFrom)) {
						buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, channel, true));
						addChar.getClient().getSession().write(PacketCreator.updateBuddyChannel(cidFrom, (byte) (channel - 1)));
					}
					break;
				case DELETED:
					if (buddylist.contains(cidFrom)) {
						buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, (byte) -1, buddylist.get(cidFrom).isVisible()));
						addChar.getClient().getSession().write(PacketCreator.updateBuddyChannel(cidFrom, (byte) -1));
					}
					break;
			}
		}
	}

	public void loggedOff(String name, int characterId, byte channel, int[] buddies) {
		updateBuddies(characterId, channel, buddies, true);
	}

	public void loggedOn(String name, int characterId, byte channel, int buddies[]) {
		updateBuddies(characterId, channel, buddies, false);
	}

	private void updateBuddies(int characterId, byte channel, int[] buddies, boolean offline) {
		PlayerStorage playerStorage = getPlayerStorage();
		for (int buddy : buddies) {
			GameCharacter chr = playerStorage.getCharacterById(buddy);
			if (chr != null) {
				BuddylistEntry ble = chr.getBuddylist().get(characterId);
				if (ble != null && ble.isVisible()) {
					byte mcChannel;
					if (offline) {
						ble.setChannel((byte) -1);
						mcChannel = -1;
					} else {
						ble.setChannel(channel);
						mcChannel = (byte) (channel - 1);
					}
					chr.getBuddylist().put(ble);
					chr.getClient().getSession().write(PacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
				}
			}
		}
	}

	public void setServerMessage(String msg) {
		for (Channel ch : channels) {
			ch.setServerMessage(msg);
		}
	}

	public void broadcastPacket(GamePacket data) {
		for (GameCharacter chr : players.getAllCharacters()) {
			chr.announce(data);
		}
	}

	public final void shutdown() {
		for (Channel ch : getChannels()) {
			ch.shutdown();
		}
		players.disconnectAll();
	}
}
