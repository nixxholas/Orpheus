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

	public void setGuildAndRank(int characterId, int guildId, int rank) {
		GameCharacter player = getPlayerStorage().getCharacterById(characterId);
		if (player == null) {
			return;
		}
		boolean bDifferentGuild;
		if (guildId == -1 && rank == -1) {
			bDifferentGuild = true;
		} else {
			bDifferentGuild = guildId != player.getGuildId();
			player.setGuildId(guildId);
			player.setGuildRank(rank);
			player.saveGuildStatus();
		}
		if (bDifferentGuild) {
			player.getMap().broadcastMessage(player, PacketCreator.removePlayerFromMap(characterId), false);
			player.getMap().broadcastMessage(player, PacketCreator.spawnPlayerMapObject(player), false);
		}
	}

	public void changeEmblem(int guildId, List<Integer> affectedPlayers, GuildSummary summary) {
		updateGuildSummary(guildId, summary);
		sendPacket(affectedPlayers, PacketCreator.guildEmblemChange(guildId, summary.getEmblem()), -1);
		setGuildAndRank(affectedPlayers, -1, -1, -1); // respawn player
	}

	public void sendPacket(List<Integer> targetIds, GamePacket packet, int exception) {
		for (int id : targetIds) {
			if (id == exception) {
				continue;
			}
			
			final GameCharacter character = getPlayerStorage().getCharacterById(id);
			if (character != null) {
				character.getClient().getSession().write(packet);
			}
		}
	}

	public Party createParty(PartyCharacter initiator) {
		int partyId = runningPartyId.getAndIncrement();
		Party party = new Party(partyId, initiator);
		parties.put(party.getId(), party);
		return party;
	}

	public Party getParty(int partyId) {
		return parties.get(partyId);
	}

	public Party disbandParty(int partyId) {
		return parties.remove(partyId);
	}

	public void updateParty(Party party, PartyOperation operation, PartyCharacter target) {
		for (PartyCharacter member : party.getMembers()) {
			GameCharacter character = getPlayerStorage().getCharacterByName(member.getName());
			if (character != null) {
				if (operation == PartyOperation.DISBAND) {
					character.setParty(null);
					character.setPartyCharacter(null);
				} else {
					character.setParty(party);
					character.setPartyCharacter(member);
				}
				character.getClient().getSession().write(PacketCreator.updateParty(character.getClient().getChannel(), party, operation, target));
			}
		}
		
		switch (operation) {
			case LEAVE:
			case EXPEL:
				GameCharacter character = getPlayerStorage().getCharacterByName(target.getName());
				if (character != null) {
					character.getClient().getSession().write(PacketCreator.updateParty(character.getClient().getChannel(), party, operation, target));
					character.setParty(null);
					character.setPartyCharacter(null);
				}
			default:
				return;
		}
	}

	public void updateParty(int partyId, PartyOperation operation, PartyCharacter target) {
		Party party = getParty(partyId);
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
				disbandParty(partyId);
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
		GameCharacter character = getPlayerStorage().getCharacterByName(name);
		if (character != null) {
			channel = character.getClient().getChannel();
		}
		return channel;
	}

	public byte find(int id) {
		byte channel = -1;
		GameCharacter character = getPlayerStorage().getCharacterById(id);
		if (character != null) {
			channel = character.getClient().getChannel();
		}
		return channel;
	}

	public void partyChat(Party party, String message, String sender) {
		for (PartyCharacter partychar : party.getMembers()) {
			if (!(partychar.getName().equals(sender))) {
				GameCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
				if (chr != null) {
					chr.getClient().getSession().write(PacketCreator.multiChat(sender, message, 1));
				}
			}
		}
	}

	public void buddyChat(int[] recipientCharacterIds, int senderCharacterId, String sender, String message) {
		PlayerStorage playerStorage = getPlayerStorage();
		for (int recepientId : recipientCharacterIds) {
			GameCharacter chr = playerStorage.getCharacterById(recepientId);
			if (chr != null) {
				if (chr.getBuddylist().containsVisible(senderCharacterId)) {
					chr.getClient().getSession().write(PacketCreator.multiChat(sender, message, 0));
				}
			}
		}
	}

	public CharacterIdChannelPair[] multiBuddyFind(int senderId, int[] characterIds) {
		List<CharacterIdChannelPair> foundsChars = new ArrayList<CharacterIdChannelPair>(characterIds.length);
		for (Channel channel : getChannels()) {
			for (int characterId : channel.multiBuddyFind(senderId, characterIds)) {
				foundsChars.add(new CharacterIdChannelPair(characterId, channel.getId()));
			}
		}
		return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
	}

	public Messenger getMessenger(int messengerId) {
		return messengers.get(messengerId);
	}

	public void leaveMessenger(int messengerId, MessengerCharacter target) {
		Messenger messenger = getMessenger(messengerId);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		int position = messenger.getPositionByName(target.getName());
		messenger.removeMember(target);
		removeMessengerPlayer(messenger, position);
	}

	public void messengerInvite(String senderName, int messengerId, String targetName, byte sourceChannelId) {
		if (isConnected(targetName)) {
			final GameCharacter target = getPlayerStorage().getCharacterByName(targetName);
			MessengerState state = target.getMessengerState();
			if (!state.isActive()) {
				target.getClient().getSession().write(PacketCreator.messengerInvite(senderName, messengerId));
				GameCharacter from = getChannel(sourceChannelId).getPlayerStorage().getCharacterByName(senderName);
				from.getClient().getSession().write(PacketCreator.messengerNote(targetName, 4, 1));
			} else {
				GameCharacter from = getChannel(sourceChannelId).getPlayerStorage().getCharacterByName(senderName);
				from.getClient().getSession().write(PacketCreator.messengerChat(senderName + " : " + targetName + " is already using Maple Messenger"));
			}
		}
	}

	public void addMessengerPlayer(Messenger messenger, String senderName, byte sourceChannelId, int position) {
		for (MessengerCharacter messengerchar : messenger.getMembers()) {
			if (!(messengerchar.getName().equals(senderName))) {
				GameCharacter chr = getPlayerStorage().getCharacterByName(messengerchar.getName());
				if (chr != null) {
					GameCharacter from = getChannel(sourceChannelId).getPlayerStorage().getCharacterByName(senderName);
					chr.getClient().getSession().write(PacketCreator.addMessengerPlayer(senderName, from, position, (byte) (sourceChannelId - 1)));
					from.getClient().getSession().write(PacketCreator.addMessengerPlayer(chr.getName(), chr, messengerchar.getPosition(), (byte) (messengerchar.getChannel() - 1)));
				}
			} else if ((messengerchar.getName().equals(senderName))) {
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

	public void messengerChat(int messengerId, String message, String senderName) {
		final Messenger messenger = getMessenger(messengerId);
		for (MessengerCharacter member : messenger.getMembers()) {
			if (!(member.getName().equals(senderName))) {
				GameCharacter chr = getPlayerStorage().getCharacterByName(member.getName());
				if (chr != null) {
					chr.getClient().getSession().write(PacketCreator.messengerChat(message));
				}
			}
		}
	}

	public void declineChat(String targetName, String senderName) {
		if (isConnected(targetName)) {
			GameCharacter character = getPlayerStorage().getCharacterByName(targetName);
			if (character != null && character.getMessengerState() != null) {
				character.getClient().getSession().write(PacketCreator.messengerNote(senderName, 5, 0));
			}
		}
	}

	public void updateMessenger(int messengerId, String senderName, byte sourceChannelId) {
		Messenger messenger = getMessenger(messengerId);
		int position = messenger.getPositionByName(senderName);
		updateMessenger(messenger, senderName, position, sourceChannelId);
	}

	public void updateMessenger(Messenger messenger, String senderName, int position, byte sourceChannelId) {
		for (MessengerCharacter member : messenger.getMembers()) {
			Channel channel = getChannel(sourceChannelId);
			if (!(member.getName().equals(senderName))) {
				GameCharacter character = channel.getPlayerStorage().getCharacterByName(member.getName());
				if (character != null) {
					character.getClient().getSession().write(PacketCreator.updateMessengerPlayer(senderName, getChannel(sourceChannelId).getPlayerStorage().getCharacterByName(senderName), position, (byte) (sourceChannelId - 1)));
				}
			}
		}
	}

	public void silentLeaveMessenger(int messengerId, MessengerCharacter target) {
		Messenger messenger = getMessenger(messengerId);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.silentRemoveMember(target);
	}

	public void joinMessenger(int messengerId, MessengerCharacter target, String senderName, byte sourceChannelId) {
		Messenger messenger = getMessenger(messengerId);
		if (messenger == null) {
			throw new IllegalArgumentException("No messenger with the specified messengerid exists");
		}
		messenger.addMember(target);
		addMessengerPlayer(messenger, senderName, sourceChannelId, target.getPosition());
	}

	public void silentJoinMessenger(int messengerId, MessengerCharacter target, int position) {
		Messenger messenger = getMessenger(messengerId);
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

	public boolean isConnected(String characterName) {
		return getPlayerStorage().getCharacterByName(characterName) != null;
	}

	public void whisper(String senderName, String targetName, byte sourceChannelId, String message) {
		if (isConnected(targetName)) {
			getPlayerStorage().getCharacterByName(targetName).getClient().getSession().write(PacketCreator.getWhisper(senderName, sourceChannelId, message));
		}
	}

	public BuddyAddResult requestBuddyAdd(String addName, byte sourceChannelId, int senderId, String senderName) {
		GameCharacter addChar = getPlayerStorage().getCharacterByName(addName);
		if (addChar != null) {
			BuddyList buddylist = addChar.getBuddylist();
			if (buddylist.isFull()) {
				return BuddyAddResult.BUDDYLIST_FULL;
			}
			if (!buddylist.contains(senderId)) {
				buddylist.addBuddyRequest(addChar.getClient(), senderId, senderName, sourceChannelId);
			} else if (buddylist.containsVisible(senderId)) {
				return BuddyAddResult.ALREADY_ON_LIST;
			}
		}
		return BuddyAddResult.OK;
	}

	public void buddyChanged(int characterId, int senderId, String characterName, byte channelId, BuddyOperation operation) {
		GameCharacter addChar = getPlayerStorage().getCharacterById(characterId);
		if (addChar != null) {
			BuddyList buddylist = addChar.getBuddylist();
			switch (operation) {
				case ADDED:
					if (buddylist.contains(senderId)) {
						buddylist.put(new BuddylistEntry(characterName, "Default Group", senderId, channelId, true));
						addChar.getClient().getSession().write(PacketCreator.updateBuddyChannel(senderId, (byte) (channelId - 1)));
					}
					break;
					
				case DELETED:
					if (buddylist.contains(senderId)) {
						buddylist.put(new BuddylistEntry(characterName, "Default Group", senderId, (byte) -1, buddylist.get(senderId).isVisible()));
						addChar.getClient().getSession().write(PacketCreator.updateBuddyChannel(senderId, (byte) -1));
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
