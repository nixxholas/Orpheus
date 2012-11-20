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

	public void removeChannel(byte worldid, byte channel) {
		channels.remove(channel);
		if (load.contains(worldid)) {
			load.get(worldid).remove(channel);
		}
		World world = worlds.get(worldid);
		if (world != null) {
			world.removeChannel(channel);
		}
	}

	public Channel getChannel(byte world, byte channel) {
		return worlds.get(world).getChannel(channel);
	}

	public List<Channel> getChannelsFromWorld(byte world) {
		return worlds.get(world).getChannels();
	}

	public List<Channel> getAllChannels() {
		List<Channel> channelz = new ArrayList<Channel>();
		for (World world : worlds) {
			for (Channel ch : world.getChannels()) {
				channelz.add(ch);
			}
		}

		return channelz;
	}

	public String getIP(byte world, byte channel) {
		return channels.get(world).get(channel);
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
		tMan.register(tMan.purge(), 300000);// Purging ftw...
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
		CashItemFactory.getSpecialCashItems();// just load who cares o.o
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
		try {
			worlds.clear();
			worlds = null;
			channels.clear();
			channels = null;
			worldRecommendedList.clear();
			worldRecommendedList = null;
			load.clear();
			load = null;
			TimerManager.getInstance().stop();
			acceptor.unbind();
			Output.print("Server is now offline.");
		} catch (NullPointerException e) {
			// We're already off. Let's get out of here...
		}
		System.exit(0);// BOEIEND :D
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

	public boolean addGuildtoAlliance(int aId, int guildId) {
		Alliance alliance = alliances.get(aId);
		if (alliance != null) {
			alliance.addGuild(guildId);
			return true;
		}
		return false;
	}

	public boolean removeGuildFromAlliance(int aId, int guildId) {
		Alliance alliance = alliances.get(aId);
		if (alliance != null) {
			alliance.removeGuild(guildId);
			return true;
		}
		return false;
	}

	public boolean setAllianceRanks(int aId, String[] ranks) {
		Alliance alliance = alliances.get(aId);
		if (alliance != null) {
			alliance.setRankTitle(ranks);
			return true;
		}
		return false;
	}

	public boolean setAllianceNotice(int aId, String notice) {
		Alliance alliance = alliances.get(aId);
		if (alliance != null) {
			alliance.setNotice(notice);
			return true;
		}
		return false;
	}

	public boolean increaseAllianceCapacity(int aId, int inc) {
		Alliance alliance = alliances.get(aId);
		if (alliance != null) {
			alliance.increaseCapacity(inc);
			return true;
		}
		return false;
	}

	public Set<Byte> getChannelServer(byte world) {
		return new HashSet<Byte>(channels.get(world).keySet());
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

	public Guild getGuild(int id, GuildCharacter mgc) {
		synchronized (guilds) {
			if (guilds.get(id) != null) {
				return guilds.get(id);
			}
			if (mgc == null) {
				return null;
			}
			Guild g = new Guild(mgc);
			if (g.getId() == -1) {
				return null;
			}
			guilds.put(id, g);
			return g;
		}
	}

	public void clearGuilds() {// remake
		synchronized (guilds) {
			guilds.clear();
		}
		// for (List<Channel> world : worlds.values()) {
		// reloadGuildCharacters();

	}

	public void setGuildMemberOnline(GuildCharacter mgc, boolean bOnline, byte channel) {
		Guild g = getGuild(mgc.getGuildId(), mgc);
		g.setOnline(mgc.getId(), bOnline, channel);
	}

	public int addGuildMember(GuildCharacter mgc) {
		Guild g = guilds.get(mgc.getGuildId());
		if (g != null) {
			return g.addGuildMember(mgc);
		}
		return 0;
	}

	public boolean setGuildAllianceId(int gId, int aId) {
		Guild guild = guilds.get(gId);
		if (guild != null) {
			guild.setAllianceId(aId);
			return true;
		}
		return false;
	}

	public void leaveGuild(GuildCharacter mgc) {
		Guild g = guilds.get(mgc.getGuildId());
		if (g != null) {
			g.leaveGuild(mgc);
		}
	}

	public void guildChat(int gid, String name, int cid, String msg) {
		Guild g = guilds.get(gid);
		if (g != null) {
			g.guildChat(name, cid, msg);
		}
	}

	public void changeRank(int gid, int cid, int newRank) {
		Guild g = guilds.get(gid);
		if (g != null) {
			g.changeRank(cid, newRank);
		}
	}

	public void expelMember(GuildCharacter initiator, String name, int cid) {
		Guild g = guilds.get(initiator.getGuildId());
		if (g != null) {
			g.expelMember(initiator, name, cid);
		}
	}

	public void setGuildNotice(int gid, String notice) {
		Guild g = guilds.get(gid);
		if (g != null) {
			g.setGuildNotice(notice);
		}
	}

	public void memberLevelJobUpdate(GuildCharacter mgc) {
		Guild g = guilds.get(mgc.getGuildId());
		if (g != null) {
			g.memberLevelJobUpdate(mgc);
		}
	}

	public void changeRankTitle(int gid, String[] ranks) {
		Guild g = guilds.get(gid);
		if (g != null) {
			g.changeRankTitle(ranks);
		}
	}

	public void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
		Guild g = guilds.get(gid);
		if (g != null) {
			g.setGuildEmblem(bg, bgcolor, logo, logocolor);
		}
	}

	public void disbandGuild(int gid) {
		synchronized (guilds) {
			Guild g = guilds.get(gid);
			g.disbandGuild();
			guilds.remove(gid);
		}
	}

	public boolean increaseGuildCapacity(int gid) {
		Guild g = guilds.get(gid);
		if (g != null) {
			return g.increaseCapacity();
		}
		return false;
	}

	public void gainGP(int gid, int amount) {
		Guild g = guilds.get(gid);
		if (g != null) {
			g.gainGP(amount);
		}
	}

	public PlayerBuffStorage getPlayerBuffStorage() {
		return buffStorage;
	}

	public void deleteGuildCharacter(GuildCharacter mgc) {
		setGuildMemberOnline(mgc, false, (byte) -1);
		if (mgc.getGuildRank() > 1) {
			leaveGuild(mgc);
		} else {
			disbandGuild(mgc.getGuildId());
		}
	}

	public void reloadGuildCharacters(byte world) {
		World worlda = getWorld(world);
		for (GameCharacter character : worlda.getPlayerStorage().getAllCharacters()) {
			if (character.getGuildId() > 0) {
				setGuildMemberOnline(character.getMGC(), true, worlda.getId());
				memberLevelJobUpdate(character.getMGC());
			}
		}
		worlda.reloadGuildSummary();
	}

	public void broadcastMessage(byte world, GamePacket packet) {
		for (Channel ch : getChannelsFromWorld(world)) {
			ch.broadcastPacket(packet);
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

	public final Runnable shutdown(final boolean restart) { // only once :D
		return new Runnable() {

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
						instance.finalize();// FUU I CAN AND IT'S FREE
					} catch (Throwable ex) {
					}
					instance = null;
					System.gc();
					getInstance().run();// DID I DO EVERYTHING?! D:
				}
			}
		};
	}
}