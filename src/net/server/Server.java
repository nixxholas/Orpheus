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

import client.GameCharacter;
import client.SkillFactory;
import client.command.external.CommandLoader;
import constants.ParanoiaConstants;
import constants.ServerConstants;
import gm.GMPacketCreator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.GamePacket;
import tools.DatabaseConnection;
import net.GameServerHandler;
import net.PacketProcessor;
import net.mina.GameCodecFactory;
import net.server.guild.Alliance;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import gm.server.GMServer;
import server.TimerManager;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import paranoia.BlacklistHandler;
import server.CashShop.CashItemFactory;
import server.ItemInfoProvider;
import server.MapleStocks;
import server.WorldRecommendation;
import tools.GameLogger;
import tools.PacketCreator;
import tools.Output;

public class Server implements Runnable {

	private IoAcceptor acceptor;
	private List<Map<Byte, String>> channels = new LinkedList<Map<Byte, String>>();
	private List<World> worlds = new ArrayList<World>();
	private Properties subnetInfo = new Properties();
	private static Server instance = null;
	private ArrayList<Map<Byte, AtomicInteger>> load = new ArrayList<Map<Byte, AtomicInteger>>();
	private List<WorldRecommendation> worldRecommendedList = new LinkedList<WorldRecommendation>();
	private Map<Integer, Guild> guilds = new LinkedHashMap<Integer, Guild>();
	private PlayerBuffStorage buffStorage = new PlayerBuffStorage();
	private Map<Integer, Alliance> alliances = new LinkedHashMap<Integer, Alliance>();
	private boolean online = false;
	private boolean gmServerEnabled = false;
	private boolean debugMode = false;

	public static Server getInstance() {
		if (instance == null) {
			instance = new Server();
		}
		return instance;
	}

	public boolean isOnline() {
		return online;
	}

	public List<WorldRecommendation> worldRecommendedList() {
		return worldRecommendedList;
	}

	public void removeChannel(byte worldId, byte channelId) {
		channels.remove(channelId);
		if (load.contains(worldId)) {
			load.get(worldId).remove(channelId);
		}
		World world = worlds.get(worldId);
		if (world != null) {
			world.removeChannel(channelId);
		}
	}

	public Channel getChannel(byte worldId, byte channelId) {
		return worlds.get(worldId).getChannel(channelId);
	}

	public List<Channel> getChannelsFromWorld(byte worldId) {
		return worlds.get(worldId).getChannels();
	}

	public List<Channel> getAllChannels() {
		List<Channel> list = new ArrayList<Channel>();
		for (World world : worlds) {
			for (Channel ch : world.getChannels()) {
				list.add(ch);
			}
		}

		return list;
	}

	public String getIP(byte worldId, byte channelId) {
		return channels.get(worldId).get(channelId);
	}

	@Override
	public void run() {
		long loadingStartTime = System.currentTimeMillis();
		if (ServerConstants.CLEAR_ERROR_LOGS_ON_BOOT) {
			if (new File(GameLogger.ACCOUNT_STUCK).exists()) GameLogger.clearLog(GameLogger.ACCOUNT_STUCK);
			if (new File(GameLogger.EXCEPTION_CAUGHT).exists()) GameLogger.clearLog(GameLogger.EXCEPTION_CAUGHT);
		}
		if (ParanoiaConstants.CLEAR_LOGS_ON_STARTUP) {
			if (ParanoiaConstants.PARANOIA_CONSOLE_LOGGER) GameLogger.clearLog(GameLogger.PARANOIA_CONSOLE);
			if (ParanoiaConstants.PARANOIA_CHAT_LOGGER) GameLogger.clearLog(GameLogger.PARANOIA_CHAT);
			if (ParanoiaConstants.PARANOIA_COMMAND_LOGGER) GameLogger.clearLog(GameLogger.PARANOIA_COMMAND);
		}
		Properties p = new Properties();
		try {
			p.load(new FileInputStream("orpheus.ini"));
			Output.print("Configuration loaded.");
		} catch (Exception e) {
			Output.print("Missing configuration file, please run mksrv script to generate one.");
			System.exit(0);
		}
		if (!ServerConstants.DB_USE_COMPILED_VALUES) { 
			DatabaseConnection.update("jdbc:mysql://" + p.getProperty("mysql_host") + ":" + p.getProperty("mysql_port") + "/Orpheus?autoReconnect=true", p.getProperty("mysql_user"), p.getProperty("mysql_pass"));
		}
		Runtime.getRuntime().addShutdownHook(new Thread(shutdown(false)));
		DatabaseConnection.getConnection();
		Output.print("Database connection established.");
		IoBuffer.setUseDirectBuffer(false);
		IoBuffer.setAllocator(new SimpleBufferAllocator());
		acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new GameCodecFactory()));
		TimerManager tMan = TimerManager.getInstance();
		tMan.start();
		tMan.register(tMan.purge(), 300000); // Purging ftw...
		boolean bindRankings = true;
		String[] events = ServerConstants.EVENTS.split(" ");
		for (int i = 0; i < events.length; i++) {
			if (events[i].equalsIgnoreCase("rankings")) {
				bindRankings = false;
			}
		}
		if (bindRankings) tMan.register(new RankingWorker(), ServerConstants.RANKING_INTERVAL);
		try {
			gmServerEnabled = Boolean.parseBoolean(p.getProperty("gmserver"));
			debugMode = Boolean.parseBoolean(p.getProperty("debug"));
			for (byte i = 0; i < Byte.parseByte(p.getProperty("worlds")); i++) {
				long startTime = System.currentTimeMillis();
				Output.print("Server is loading world" + i + ".");
				World world = new World(i, Byte.parseByte(p.getProperty("flag" + i)), p.getProperty("eventmessage" + i), Byte.parseByte(p.getProperty("exprate" + i)), Byte.parseByte(p.getProperty("droprate" + i)), Byte.parseByte(p.getProperty("mesorate" + i)), Byte.parseByte(p.getProperty("bossdroprate" + i)));// ohlol

				worldRecommendedList.add(new WorldRecommendation(i, p.getProperty("recommendmessage" + i)));
				worlds.add(world);
				channels.add(new LinkedHashMap<Byte, String>());
				load.add(new LinkedHashMap<Byte, AtomicInteger>());
				for (byte j = 0; j < Byte.parseByte(p.getProperty("channels" + i)); j++) {
					byte channelid = (byte) (j + 1);
					Channel channel = new Channel(i, channelid);
					world.addChannel(channel);
					channels.get(i).put(channelid, channel.getIP());
					load.get(i).put(channelid, new AtomicInteger());
				}
				world.setServerMessage(p.getProperty("servermessage" + i));
				Output.print("Loading completed in " + ((System.currentTimeMillis() - startTime)) + "ms.");
			}
		} catch (Exception e) {
			Output.print("Corrupted configuration file, please run mksrv script to generate a new one.");
			e.printStackTrace();
			System.exit(0);
		}
		acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
		acceptor.setHandler(new GameServerHandler(PacketProcessor.getProcessor()));
		try {
			acceptor.bind(new InetSocketAddress(8484));
		} catch (IOException ex) {
		}
		Output.print("Login Server: Listening on port 8484.");
		long startTime; // Used to time loading phases.
		Output.print("Loading skills.");
		startTime = System.currentTimeMillis();
		SkillFactory.loadAllSkills();
		Output.print("Loading completed in " + ((System.currentTimeMillis() - startTime)) + "ms.");
		Output.print("Loading items.");
		startTime = System.currentTimeMillis();
		CashItemFactory.getSpecialCashItems(); // just load who cares o.o
		ItemInfoProvider.getInstance().getAllItems();
		Output.print("Loading completed in " + ((System.currentTimeMillis() - startTime)) + "ms.");
		if (isGMServerEnabled()) {
			GMServer.getInstance();
		}
		if (ParanoiaConstants.ENABLE_BLACKLISTING && ParanoiaConstants.LOAD_BLACKLIST_ON_STARTUP) {
			Output.print("Loading Paranoia blacklist.");
			startTime = System.currentTimeMillis();
			BlacklistHandler.getBlacklistedAccountIds();
			Output.print("Loading completed in " + ((System.currentTimeMillis() - startTime)) + "ms.");
		}
		if (ServerConstants.LOAD_STOCKS_ON_BOOT && ServerConstants.USE_MAPLE_STOCKS) {
			Output.print("Loading MapleStocks.");
			startTime = System.currentTimeMillis();
			MapleStocks.getInstance(false).loadAll();
			Output.print("Loading completed in " + ((System.currentTimeMillis() - startTime)) + "ms.");
			
		}
		if (ServerConstants.LOAD_COMMANDS_ON_BOOT && ServerConstants.USE_EXTERNAL_COMMAND_LOADER) {
			try {
				Output.print("Loading commands.");
				startTime = System.currentTimeMillis();
				CommandLoader.getInstance().load(ServerConstants.COMMAND_JAR_PATH);
				Output.print("Loading completed in " + ((System.currentTimeMillis() - startTime)) + "ms.");
			} catch (Exception e) {
				Output.print("Failed to load commands.");
				GameLogger.print(GameLogger.EXCEPTION_CAUGHT, e);
			}
		}
		Output.print("Server is now online! (Took " + ((System.currentTimeMillis() - loadingStartTime)) + "ms)");
		online = true;
	}

	public void shutdown() {
		clearSafely(this.worlds);
		worlds = null;
		
		clearSafely(this.channels);
		channels = null;

		clearSafely(this.worldRecommendedList);
		worldRecommendedList = null;

		clearSafely(this.load);
		load = null;

		TimerManager.getInstance().stop();
		acceptor.unbind();
		
		Output.print("Server is now offline.");
		
		// BOEIEND :D
		System.exit(0);
	}
	
	private static <T> void clearSafely(List<T> list) {
		if (list != null) {
			list.clear();
		}
	}

	public static void main(String args[]) {
		Server.getInstance().run();
	}
	
	public boolean isGMServerEnabled() {
		return gmServerEnabled;
	}
	
	public boolean isDebugging() {
		return debugMode;
	}

	public Properties getSubnetInfo() {
		return subnetInfo;
	}

	public Map<Byte, AtomicInteger> getLoad(byte i) {
		return load.get(i);
	}

	public List<Map<Byte, AtomicInteger>> getLoad() {
		return load;
	}

	public Alliance getAlliance(int id) {
		synchronized (alliances) {
			if (alliances.containsKey(id)) {
				return alliances.get(id);
			}
			return null;
		}
	}

	public void addAlliance(int id, Alliance alliance) {
		synchronized (alliances) {
			if (!alliances.containsKey(id)) {
				alliances.put(id, alliance);
			}
		}
	}

	public void disbandAlliance(int id) {
		synchronized (alliances) {
			Alliance alliance = alliances.get(id);
			if (alliance != null) {
				for (Integer gid : alliance.getGuilds()) {
					guilds.get(gid).setAllianceId(0);
				}
				alliances.remove(id);
			}
		}
	}

	public void allianceMessage(int id, GamePacket packet, int exception, int guildex) {
		Alliance alliance = alliances.get(id);
		if (alliance != null) {
			for (Integer gid : alliance.getGuilds()) {
				if (guildex == gid) {
					continue;
				}
				Guild guild = guilds.get(gid);
				if (guild != null) {
					guild.broadcast(packet, exception);
				}
			}
		}
	}

	public boolean addGuildtoAlliance(int allianceId, int guildId) {
		Alliance alliance = alliances.get(allianceId);
		if (alliance != null) {
			alliance.addGuild(guildId);
			return true;
		}
		return false;
	}

	public boolean removeGuildFromAlliance(int allianceId, int guildId) {
		Alliance alliance = alliances.get(allianceId);
		if (alliance != null) {
			alliance.removeGuild(guildId);
			return true;
		}
		return false;
	}

	public boolean setAllianceRanks(int allianceId, String[] ranks) {
		Alliance alliance = alliances.get(allianceId);
		if (alliance != null) {
			alliance.setRankTitle(ranks);
			return true;
		}
		return false;
	}

	public boolean setAllianceNotice(int allianceId, String notice) {
		Alliance alliance = alliances.get(allianceId);
		if (alliance != null) {
			alliance.setNotice(notice);
			return true;
		}
		return false;
	}

	public boolean increaseAllianceCapacity(int allianceId, int increase) {
		Alliance alliance = alliances.get(allianceId);
		if (alliance != null) {
			alliance.increaseCapacity(increase);
			return true;
		}
		return false;
	}

	public Set<Byte> getChannelServer(byte worldId) {
		return new HashSet<Byte>(channels.get(worldId).keySet());
	}

	public byte getHighestChannelId() {
		byte highest = 0;
		for (Byte channel : channels.get(0).keySet()) {
			if (channel != null && channel.intValue() > highest) {
				highest = channel.byteValue();
			}
		}
		return highest;
	}

	public int createGuild(int leaderId, String name) {
		return Guild.createGuild(leaderId, name);
	}

	public Guild getGuild(int id, GuildCharacter member) {
		synchronized (guilds) {
			if (guilds.get(id) != null) {
				return guilds.get(id);
			}
			if (member == null) {
				return null;
			}
			Guild guild = Guild.loadFromDb(member);
			if (guild == null) {
				return null;
			}
			guilds.put(id, guild);
			return guild;
		}
	}

	public void clearGuilds() {
		// remake
		synchronized (guilds) {
			guilds.clear();
		}
		// for (List<Channel> world : worlds.values()) {
		// reloadGuildCharacters();
	}

	public void setGuildMemberOnline(GuildCharacter member, boolean isOnline, byte channelId) {
		Guild guild = getGuild(member.getGuildId(), member);
		guild.setOnline(member.getId(), isOnline, channelId);
	}

	public int addGuildMember(GuildCharacter member) {
		Guild guild = guilds.get(member.getGuildId());
		if (guild != null) {
			return guild.addGuildMember(member);
		}
		return 0;
	}

	public boolean setGuildAllianceId(int guildId, int allianceId) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.setAllianceId(allianceId);
			return true;
		}
		return false;
	}

	public void leaveGuild(GuildCharacter member) {
		Guild guild = guilds.get(member.getGuildId());
		if (guild != null) {
			guild.leaveGuild(member);
		}
	}

	public void guildChat(int guildId, String name, int characterId, String message) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.guildChat(name, characterId, message);
		}
	}

	public void changeRank(int guildId, int characterId, int newRank) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.changeRank(characterId, newRank);
		}
	}

	public void expelMember(GuildCharacter initiator, String name, int characterId) {
		Guild guild = guilds.get(initiator.getGuildId());
		if (guild != null) {
			guild.expelMember(initiator, name, characterId);
		}
	}

	public void setGuildNotice(int guildId, String notice) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.setGuildNotice(notice);
		}
	}

	public void memberLevelJobUpdate(GuildCharacter member) {
		Guild guild = guilds.get(member.getGuildId());
		if (guild != null) {
			guild.memberLevelJobUpdate(member);
		}
	}

	public void changeRankTitle(int guildId, String[] ranks) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.changeRankTitle(ranks);
		}
	}

	public void setGuildEmblem(int guildId, short bg, byte bgcolor, short logo, byte logocolor) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.setGuildEmblem(bg, bgcolor, logo, logocolor);
		}
	}

	public void disbandGuild(int guildId) {
		synchronized (guilds) {
			Guild guild = guilds.get(guildId);
			guild.disbandGuild();
			guilds.remove(guildId);
		}
	}

	public boolean increaseGuildCapacity(int guildId) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			return guild.increaseCapacity();
		}
		return false;
	}

	public void gainGP(int guildId, int amount) {
		Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.gainGP(amount);
		}
	}

	public PlayerBuffStorage getPlayerBuffStorage() {
		return buffStorage;
	}

	public void deleteGuildCharacter(GuildCharacter member) {
		setGuildMemberOnline(member, false, (byte) -1);
		if (member.getGuildRank() > 1) {
			leaveGuild(member);
		} else {
			disbandGuild(member.getGuildId());
		}
	}

	public void reloadGuildCharacters(byte worldId) {
		World world = getWorld(worldId);
		for (GameCharacter character : world.getPlayerStorage().getAllCharacters()) {
			if (character.getGuildId() > 0) {
				setGuildMemberOnline(character.getGuildCharacter(), true, world.getId());
				memberLevelJobUpdate(character.getGuildCharacter());
			}
		}
		world.reloadGuildSummary();
	}

	public void broadcastMessage(byte worldId, GamePacket packet) {
		for (Channel channel : getChannelsFromWorld(worldId)) {
			channel.broadcastPacket(packet);
		}
	}

	public World getWorld(int id) {
		return worlds.get(id);
	}

	public List<World> getWorlds() {
		return worlds;
	}

	public void gmChat(String message, String exclude) {
		GMServer server = GMServer.getInstance();
		server.broadcastInGame(PacketCreator.serverNotice(6, message));
		server.broadcastOutGame(GMPacketCreator.chat(message), exclude);
	}

	public final Runnable shutdown(final boolean restart) { 
		// only once :D
		return new ShutdownAction(restart);
	}
	
	private final class ShutdownAction implements Runnable {
		private final boolean restart;

		private ShutdownAction(boolean restart) {
			this.restart = restart;
		}

		@Override
		public void run() {
			Output.printNewLine();
			Output.print("The server is now " + (restart ? "restarting." : "shutting down."));
			for (World w : getWorlds()) {
				w.shutdown();
			}
			for (World w : getWorlds()) {
				while (w.getPlayerStorage().getAllCharacters().size() > 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						// Well, shit.
						w.getPlayerStorage().disconnectAll(); // try to save us.
					}
				}
			}
			for (Channel ch : getAllChannels()) {
				while (ch.getConnectedClients() > 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						// Well, shit.
						ch.getPlayerStorage().disconnectAll(); // try to save us.
					}
				}
			}

			TimerManager.getInstance().purge();
			TimerManager.getInstance().stop();

			for (Channel ch : getAllChannels()) {
				while (!ch.finishedShutdown()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
						// Well, damn.
						ch.shutdown(); // try to save us.
					}
				}
			}
			worlds.clear();
			worlds = null;
			channels.clear();
			channels = null;
			worldRecommendedList.clear();
			worldRecommendedList = null;
			load.clear();
			load = null;
			Output.print("Server is now offline.");
			acceptor.unbind();
			acceptor = null;
			if (!restart) {
				shutdown();
				System.exit(0);
			} else {
				Output.print("\r\nThe server is now restarting.");
				try {
					// FUU I CAN AND IT'S FREE
					instance.finalize();
				} catch (Throwable ex) {
				}
				instance = null;
				System.gc();
				getInstance().run(); 
				// DID I DO EVERYTHING?! D:
			}
		}
	}
}