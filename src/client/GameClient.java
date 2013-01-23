/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss

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
package client;

import gm.server.GMServer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import javax.script.ScriptEngine;
import net.GamePacket;
import tools.DatabaseConnection;
import net.server.Channel;
import net.server.MessengerState;
import net.server.Server;
import net.server.MessengerCharacter;
import net.server.PartyCharacter;
import net.server.PartyOperation;
import net.server.World;
import net.server.guild.GuildCharacter;
import scripting.npc.NpcConversationManager;
import scripting.npc.NpcScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.Trade;
import server.TimerManager;
import server.maps.HiredMerchant;
import tools.DatabaseCall;
import tools.HashCreator;
import tools.AesCrypto;
import tools.PacketCreator;
import tools.HexTool;
import tools.Output;
import org.apache.mina.core.session.IoSession;
import constants.ServerConstants;
import server.Minigame;
import server.quest.Quest;
import tools.GameLogger;

public class GameClient {

	public static enum State {
		NOT_LOGGED_IN(0),
		TRANSITION(1),
		LOGGED_IN(2);
		
		private final int code;
		
		private State(int code) {
			this.code = code;
		}
		
		public int getCode() {
			return this.code;
		}
		
		public boolean is(State other) {
			return other != null && this.code == other.code;
		}
		
		public static State fromCode(int code) {
			switch(code) {
			case 0:
				return NOT_LOGGED_IN;
			case 1:
				return TRANSITION;
			case 2:
				return LOGGED_IN;
			default:
				return null;
			}
		}
	}
	
	private final class ClientTimeoutAction implements Runnable {
		private final long then;

		private ClientTimeoutAction(long pingTimestamp) {
			this.then = pingTimestamp;
		}

		@Override
		public void run() {
			if (lastPong < then) {
				final IoSession copy = getSession();
				if (copy != null && copy.isConnected()) {
					copy.close(true);
				}
			}
		}
	}

	public static final String CLIENT_KEY = "CLIENT";
	private AesCrypto send;
	private AesCrypto receive;
	private IoSession session;
	
	private int accountId = 1;
	private boolean loggedIn = false;
	private boolean serverTransition = false;
	private String accountName = null;
	private Calendar birthday = null;
	private byte characterSlots = 3;
	private int gmlevel;
	private String pin = null;
	private String pic = null;
	private byte gender = -1;

	private long lastPong;
	private Set<String> macs = new HashSet<String>();
	private byte loginAttempts = 0;
	private int pinAttempts = 0;
	private int picAttempts = 0;

	private byte worldId;
	private byte channelId = 1;
	private GameCharacter player;
	private Map<String, ScriptEngine> engines = new HashMap<String, ScriptEngine>();
	private ScheduledFuture<?> idleTask = null;

	public GameClient(AesCrypto send, AesCrypto receive, IoSession session) {
		this.send = send;
		this.receive = receive;
		this.session = session;
	}

	public synchronized AesCrypto getReceiveCrypto() {
		return receive;
	}

	public synchronized AesCrypto getSendCrypto() {
		return send;
	}

	public synchronized IoSession getSession() {
		return session;
	}

	public GameCharacter getPlayer() {
		return player;
	}

	public void setPlayer(GameCharacter player) {
		this.player = player;
	}

	public void sendCharList(int server) {
		this.session.write(PacketCreator.getCharList(this, server));
	}

	public List<GameCharacter> loadCharacters(int serverId) {
		List<GameCharacter> chars = new ArrayList<GameCharacter>(15);
		try {
			for (SimpleCharacterInfo cni : loadCharactersInternal(serverId)) {
				chars.add(GameCharacter.loadFromDb(cni.id, this, false));
			}
		} catch (Exception e) {
		}
		return chars;
	}

	public List<String> loadCharacterNames(int serverId) {
		List<String> chars = new ArrayList<String>(15);
		for (SimpleCharacterInfo cni : loadCharactersInternal(serverId)) {
			chars.add(cni.name);
		}
		return chars;
	}

	public String getFormattedCharacterList(int serverId) {
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for (SimpleCharacterInfo cni : loadCharactersInternal(serverId)) {
			sb.append("#L").append(n).append("#").append(cni.name).append("#l\r\n");
			n++;
		}
		return sb.toString();
	}
	
	public String getCharacterName(int n, int serverId) {
		int k = 0;
		for (SimpleCharacterInfo cni : loadCharactersInternal(serverId)) {
			if (k == n) {
				return cni.name;
			}
			k++;
		}
		return null;
	}
	
	public int getCharacterId(int n, int serverId) {
		int k = 0;
		for (SimpleCharacterInfo cni : loadCharactersInternal(serverId)) {
			if (k == n) {
				return cni.id;
			}
			k++;
		}
		return -1;
	}
	
	public boolean isCharacterInGuild(int cid) {
		return (GameCharacter.getGuildIdById(cid) != 0);
	}
	
	private List<SimpleCharacterInfo> loadCharactersInternal(int serverId) {
		final Connection connection = DatabaseConnection.getConnection();

		List<SimpleCharacterInfo> chars = new ArrayList<SimpleCharacterInfo>(15);
		try (DatabaseCall call = DatabaseCall.query(getSelectCharacters(connection, serverId))) {

			ResultSet rs = call.resultSet();
			while (rs.next()) {
				chars.add(new SimpleCharacterInfo(rs.getInt("id"), rs.getString("name")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return chars;
	}

	private PreparedStatement getSelectCharacters(final Connection connection, int serverId) throws SQLException {
		PreparedStatement ps;
		if (ServerConstants.ENABLE_HARDCORE_MODE) {
			ps = connection.prepareStatement("SELECT `id`, `name` FROM `characters` WHERE `accountid` = ? AND `world` = ? AND `dead` != 1");
		} else {
			ps = connection.prepareStatement("SELECT `id`, `name` FROM `characters` WHERE `accountid` = ? AND `world` = ?");
		}
		ps.setInt(1, this.getAccountId());
		ps.setInt(2, serverId);
		return ps;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public boolean hasBannedIP() {
		boolean ret = false;

		final Connection connection = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM `ipbans` WHERE ? LIKE CONCAT(`ip`, '%')");
			ps.setString(1, session.getRemoteAddress().toString());
			ResultSet rs = ps.executeQuery();
			rs.next();
			if (rs.getInt(1) > 0) {
				ret = true;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
		}
		return ret;
	}

	public boolean hasBannedMac() {
		if (macs.isEmpty()) {
			return false;
		}
		boolean ret = false;
		int i = 0;
		try {
			StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM `macbans` WHERE `mac` IN (");
			for (i = 0; i < macs.size(); i++) {
				sql.append("?");
				if (i != macs.size() - 1) {
					sql.append(", ");
				}
			}
			sql.append(")");
			final Connection connection = DatabaseConnection.getConnection();
			PreparedStatement ps = connection.prepareStatement(sql.toString());
			i = 0;
			for (String mac : macs) {
				i++;
				ps.setString(i, mac);
			}
			ResultSet rs = ps.executeQuery();
			rs.next();
			if (rs.getInt(1) > 0) {
				ret = true;
			}
			rs.close();
			ps.close();
		} catch (Exception e) {
		}
		return ret;
	}

	private void loadMacsIfNescessary() throws SQLException {
		if (!this.macs.isEmpty()) {
			return;
		}

		final Connection connection = DatabaseConnection.getConnection();
		try (DatabaseCall call = DatabaseCall.query(getSelectMacsByAccountId(connection))) {

			ResultSet rs = call.resultSet();
			if (rs.next()) {
				for (String mac : rs.getString("macs").split(", ")) {
					if (!mac.equals("")) {
						macs.add(mac);
					}
				}
			}
		}
	}

	private PreparedStatement getSelectMacsByAccountId(final Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `macs` FROM `accounts` WHERE `id` = ?");
		ps.setInt(1, accountId);
		return ps;
	}

	public void banMacs() {
		Connection connection = DatabaseConnection.getConnection();
		try {
			loadMacsIfNescessary();
			List<String> filtered = new LinkedList<String>();
			PreparedStatement ps = connection.prepareStatement("SELECT `filter` FROM `macfilters`");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				filtered.add(rs.getString("filter"));
			}
			rs.close();
			ps.close();
			ps = connection.prepareStatement("INSERT INTO `macbans` (`mac`) VALUES (?)");
			for (String mac : macs) {
				boolean matched = false;
				for (String filter : filtered) {
					if (mac.matches(filter)) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					ps.setString(1, mac);
					ps.executeUpdate();
				}
			}
			ps.close();
		} catch (SQLException e) {
		}
	}

	public int finishLogin() {
		synchronized (GameClient.class) {
			final State state = getLoginState();
			if (state.is(State.NOT_LOGGED_IN)) {
				updateLoginState(State.LOGGED_IN);
			} else {
				loggedIn = false;
				return 7;
			}
		}
		return 0;
	}

	public void setPin(String pin) {
		this.pin = pin;

		final Connection connection = DatabaseConnection.getConnection();
		try (PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `pin` = ? WHERE `id` = ?")) {
			ps.setString(1, pin);
			ps.setInt(2, accountId);
			ps.executeUpdate();
		} catch (SQLException e) {
		}
	}

	public String getPin() {
		return pin;
	}

	public boolean checkPin(String other) {
		pinAttempts++;
		if (pinAttempts > 5) {
			getSession().close(true);
		}
		if (pin.equals(other)) {
			pinAttempts = 0;
			return true;
		}
		return false;
	}

	public void setPic(String pic) {
		this.pic = pic;

		final Connection connection = DatabaseConnection.getConnection();
		try (PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `pic` = ? WHERE `id` = ?")) {
			ps.setString(1, pic);
			ps.setInt(2, accountId);
			ps.executeUpdate();
		} catch (SQLException e) {
		}
	}

	public String getPic() {
		return pic;
	}

	public boolean checkPic(String other) {
		picAttempts++;
		if (picAttempts > 5) {
			getSession().close(true);
		}
		if (pic.equals(other)) {
			picAttempts = 0;
			return true;
		}
		return false;
	}

	public AuthResult login(String accountName, String password) {
		loginAttempts++;
		if (loginAttempts > 4) {
			getSession().close(true);
		}
		
		AuthResult loginok = AuthResult.NOT_REGISTERED;
		Connection connection = DatabaseConnection.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = connection.prepareStatement("SELECT `id`, `password`, `salt`, `gender`, `banned`, `gm`, `pin`, `pic`, `characterslots`, `tos` FROM `accounts` WHERE `name` = ?");
			ps.setString(1, accountName);
			rs = ps.executeQuery();
			if (rs.next()) {
				if (rs.getByte("banned") == 1) {
					return AuthResult.DELETED_OR_BLOCKED;
				}
				accountId = rs.getInt("id");
				gmlevel = rs.getInt("gm");
				pin = rs.getString("pin");
				pic = rs.getString("pic");
				gender = rs.getByte("gender");
				characterSlots = rs.getByte("characterslots");
				String passhash = rs.getString("password");
				String salt = rs.getString("salt");

				// we do not unban
				byte tos = rs.getByte("tos");
				ps.close();
				rs.close();
				final State loginState = getLoginState();
				if (!loginState.is(State.NOT_LOGGED_IN)) { 
					// already loggedin
					loggedIn = false;
					loginok = AuthResult.ALREADY_LOGGED_IN;
				} else if (password.equals(passhash) || checkHash(passhash, "SHA-1", password) || checkHash(passhash, "SHA-512", password + salt)) {
					if (tos == 0) {
						loginok = AuthResult.TERMS_OF_SERVICE;
					} else {
						loginok = AuthResult.SUCCESS;
					}
				} else {
					loggedIn = false;
					loginok = AuthResult.INCORRECT_PASSWORD;
				}
				
				if (loginok.is(AuthResult.SUCCESS)) {
					// We're going to change the hashing algorithm to SHA-512 with salt, so we can be secure! :3
					SecureRandom random = new SecureRandom();
					byte bytes[] = new byte[32]; // 32 bit salt (results may vary. depends on RNG algorithm)
					random.nextBytes(bytes);
					String saltNew = HexTool.toString(bytes).replace(" ", "").toLowerCase();
					String passhashNew = HashCreator.getHash("SHA-512", password + saltNew);
					ps = connection.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE `id` = ?");
					ps.setString(1, passhashNew);
					ps.setString(2, saltNew);
					ps.setInt(3, accountId);
					ps.executeUpdate();
					ps.close();
					rs.close();
				}
				
				ps = connection.prepareStatement("INSERT INTO `iplog` (`accountid`, `ip`) VALUES (?, ?)");
				ps.setInt(1, accountId);
				ps.setString(2, session.getRemoteAddress().toString());
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
				if (rs != null && !rs.isClosed()) {
					rs.close();
				}
			} catch (SQLException e) {
			}
		}

		if (loginok.is(AuthResult.SUCCESS)) {
			loginAttempts = 0;
		}
		return loginok;
	}

	public Calendar getTempBanCalendar() {
		Connection connection = DatabaseConnection.getConnection();

		final Calendar tempban = Calendar.getInstance();
		try (DatabaseCall call = DatabaseCall.query(getSelectTempbans(connection))) {
			ResultSet rs = call.resultSet();
			
			long blubb = rs.getLong("tempban");
			if (blubb == 0) { 
				// basically if timestamp in db is 0000-00-00
				return null;
			}
			final Calendar today = Calendar.getInstance();
			tempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
			if (today.getTimeInMillis() < tempban.getTimeInMillis()) {
				return tempban;
			}
			return null;
		} catch (SQLException e) {
		}
		return null; // why oh why!?!
	}

	private PreparedStatement getSelectTempbans(Connection connection) throws SQLException {
		PreparedStatement ps;
		ps = connection.prepareStatement("SELECT `tempban` FROM `accounts` WHERE `id` = ?");
		ps.setInt(1, accountId);
		return ps;
	}

	public static long dottedQuadToLong(String dottedQuad) throws RuntimeException {
		String[] quads = dottedQuad.split("\\.");
		if (quads.length != 4) {
			throw new RuntimeException("Invalid IP Address format.");
		}
		long ipAddress = 0;
		for (int i = 0; i < 4; i++) {
			int quad = Integer.parseInt(quads[i]);
			ipAddress += (long) (quad % 256) * (long) Math.pow(256, (double) (4 - i));
		}
		return ipAddress;
	}

	public static String getChannelServerIPFromSubnet(String clientIPAddress, byte channel) {
		long ipAddress = dottedQuadToLong(clientIPAddress);
		Properties subnetInfo = Server.getInstance().getSubnetInfo();
		if (subnetInfo.contains("net.login.subnetcount")) {
			int subnetCount = Integer.parseInt(subnetInfo.getProperty("net.login.subnetcount"));
			for (int i = 0; i < subnetCount; i++) {
				String[] connectionInfo = subnetInfo.getProperty("net.login.subnet." + i).split(":");
				long subnet = dottedQuadToLong(connectionInfo[0]);
				long channelIP = dottedQuadToLong(connectionInfo[1]);
				byte channelNumber = Byte.parseByte(connectionInfo[2]);
				if (((ipAddress & subnet) == (channelIP & subnet)) && (channel == channelNumber)) {
					return connectionInfo[1];
				}
			}
		}
		return "0.0.0.0";
	}

	public void updateMacs(String macData) {
		macs.addAll(Arrays.asList(macData.split(", ")));
		StringBuilder newMacData = new StringBuilder();
		Iterator<String> iter = macs.iterator();
		while (iter.hasNext()) {
			String cur = iter.next();
			newMacData.append(cur);
			if (iter.hasNext()) {
				newMacData.append(", ");
			}
		}

		final Connection connection = DatabaseConnection.getConnection();
		try (PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `macs` = ? WHERE `id` = ?")) {
			ps.setString(1, newMacData.toString());
			ps.setInt(2, accountId);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getAccountId() {
		return accountId;
	}

	public void setAccountId(int id) {
		this.accountId = id;
	}

	public void updateLoginState(State newState) {
		Connection connection = DatabaseConnection.getConnection();
		try (PreparedStatement ps = getUpdateLoginState(connection, newState)) {
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (newState.is(State.NOT_LOGGED_IN)) {
			loggedIn = false;
			serverTransition = false;
		} else {
			serverTransition = newState.is(State.TRANSITION);
			loggedIn = !serverTransition;
		}
	}

	private PreparedStatement getUpdateLoginState(Connection connection, State newState) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `loggedin` = ?, `lastlogin` = CURRENT_TIMESTAMP() WHERE `id` = ?");
		ps.setInt(1, newState.getCode());
		ps.setInt(2, this.accountId);
		return ps;
	}

	public State getLoginState() {
		Connection connection = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = connection.prepareStatement("SELECT `loggedin`, `lastlogin`, UNIX_TIMESTAMP(`birthday`) AS `birthday` FROM `accounts` WHERE `id` = ?");
			ps.setInt(1, getAccountId());
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				throw new RuntimeException("getLoginState - MapleClient");
			}
			birthday = Calendar.getInstance();
			long blubb = rs.getLong("birthday");
			if (blubb > 0) {
				birthday.setTimeInMillis(blubb * 1000);
			}
			State state = State.fromCode(rs.getInt("loggedin"));
			if (state.is(State.TRANSITION)) {
				if (rs.getTimestamp("lastlogin").getTime() + 30000 < System.currentTimeMillis()) {
					state = State.NOT_LOGGED_IN;
					updateLoginState(State.NOT_LOGGED_IN);
				}
			} else if (state.is(State.LOGGED_IN) && player == null) {
                state = State.NOT_LOGGED_IN;
                updateLoginState(State.NOT_LOGGED_IN);
            }  
			rs.close();
			ps.close();
			if (state.is(State.LOGGED_IN)) {
				loggedIn = true;
			} else if (state.is(State.TRANSITION)) {
				ps = connection.prepareStatement("UPDATE `accounts` SET `loggedin` = 0 WHERE `id` = ?");
				ps.setInt(1, getAccountId());
				ps.executeUpdate();
				ps.close();
			} else {
				loggedIn = false;
			}
			
			return state;
		} catch (SQLException e) {
			loggedIn = false;
			e.printStackTrace();
			throw new RuntimeException("login state");
		}
	}
	
	public void setBirthday(String month, String day, String year) {
		birthday.set(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
	}

	public boolean checkBirthDate(Calendar date) {
		return 
				date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) 
				&& date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) 
				&& date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
	}

	private void removePlayer() {
		try {
			// This boots them out of channel too.
			getWorldServer().removePlayer(player);
			
			Server.getInstance().getLoad(worldId).get(channelId).decrementAndGet();
			if (player.getMap() != null) {
				player.getMap().removePlayer(player);
			}
			if (player.getTrade() != null) {
				Trade.cancelTrade(player);
			}
			if (gmlevel > 0) {
				GMServer.getInstance().removeInGame(player.getName());
			}
			player.cancelAllBuffs(true);
			if (player.getEventInstance() != null) {
				player.getEventInstance().playerDisconnected(player);
			}
			player.cancelAllDebuffs();
		} catch (final Throwable t) {
			GameLogger.print(GameLogger.ACCOUNT_STUCK, t);
		}
	}

	public final void disconnect() {
		// once per GameClient instance
		try {
			if (player != null && isLoggedIn()) {
				removePlayer();
				player.saveToDb(true);

				World worlda = getWorldServer();
				player.saveCooldowns();
				player.unequipPendantOfSpirit();
				Minigame game = player.getActiveMinigame();
				if (game != null) {
					player.setActiveMinigame(null);
					if (game.isOwner(player)) {
						player.getMap().broadcastMessage(PacketCreator.removeCharBox(player));
						game.broadcastToVisitor(PacketCreator.getMiniGameClose());
					} else {
						game.removeVisitor(player);
					}
				}

				HiredMerchant merchant = player.getHiredMerchant();
				if (merchant != null) {
					if (merchant.isOwner(player)) {
						merchant.setOpen(true);
					} else {
						merchant.removeVisitor(player);
					}
					try {
						merchant.saveItems(false);
					} catch (SQLException ex) {
						Output.print("An error occurred while saving Hired Merchant items.");
					}
				}
				final MessengerState state = player.getMessengerState();
				if (state.isActive()) {
					MessengerCharacter messengerplayer = new MessengerCharacter(player);
					worlda.leaveMessenger(state.getId(), messengerplayer);
					state.reset();
				}
				NpcScriptManager npcsm = NpcScriptManager.getInstance();
				if (npcsm != null) {
					npcsm.dispose(this);
				}
				if (!player.isAlive()) {
					player.setHp(50, true);
				}

				for (QuestStatus status : player.getStartedQuests()) {
					Quest quest = status.getQuest();
					if (quest.getTimeLimit() > 0) {
						QuestStatus newStatus = new QuestStatus(quest, QuestCompletionState.NOT_STARTED);
						newStatus.setForfeited(player.getQuest(quest).getForfeited() + 1);
						player.updateQuest(newStatus);
					}
				}
				if (player.getParty() != null) {
					PartyCharacter chrp = player.getPartyCharacter();
					chrp.setOnline(false);
					worlda.updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, chrp);
				}
				if (!this.serverTransition && isLoggedIn()) {
					worlda.loggedOff(player.getName(), player.getId(), channelId, player.getBuddylist().getBuddyIds());
				} else {
					worlda.loggedOn(player.getName(), player.getId(), channelId, player.getBuddylist().getBuddyIds());
				}
				if (player.getGuildId() > 0) {
					Server.getInstance().setGuildMemberOnline(player.getGuildCharacter(), false, (byte) -1);
					int allianceId = player.getGuild().getAllianceId();
					if (allianceId > 0) {
						Server.getInstance().allianceMessage(allianceId, PacketCreator.allianceMemberOnline(player, false), player.getId(), -1);
					}
				}
			}
		} finally {
			player = null;
			session.close(true);
		}
		if (!this.serverTransition) {
			this.updateLoginState(State.NOT_LOGGED_IN);
		}
	}

	public final void empty() {
		if (this.player != null) {
			this.player.getMount().empty();
			this.player.empty();
		}
		
		this.player = null;
		this.session = null;
		this.engines.clear();
		this.engines = null;
		this.send = null;
		this.receive = null;
		this.channelId = -1;
	}

	public byte getChannelId() {
		return channelId;
	}

	public Channel getChannelServer() {
		return Server.getInstance().getChannel(worldId, channelId);
	}

	public World getWorldServer() {
		return Server.getInstance().getWorld(worldId);
	}

	public Channel getChannelServer(byte channelId) {
		return Server.getInstance().getChannel(worldId, channelId);
	}

	public boolean deleteCharacter(int characterId) {
		Connection connection = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = connection.prepareStatement("SELECT `id`, `guildid`, `guildrank`, `name`, `allianceRank` FROM `characters` WHERE `id` = ? AND `accountid` = ?");
			ps.setInt(1, characterId);
			ps.setInt(2, accountId);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return false;
			}
			if (rs.getInt("guildid") > 0) {
				try {
					Server.getInstance().deleteGuildCharacter(new GuildCharacter(characterId, 0, rs.getString("name"), (byte) -1, (byte) -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank")));
				} catch (Exception re) {
					rs.close();
					ps.close();
					return false;
				}
			}
			rs.close();
			ps = connection.prepareStatement("DELETE FROM `wishlists` WHERE `charid` = ?");
			ps.setInt(1, characterId);
			ps.executeUpdate();
			ps = connection.prepareStatement("DELETE FROM `characters` WHERE `id` = ?");
			ps.setInt(1, characterId);
			ps.executeUpdate();
			String[] toDel = {"famelog", "inventoryitems", "keymap", "queststatus", "savedlocations", "skillmacros", "skills", "eventstats"};
			for (String s : toDel) {
				ps = connection.prepareStatement("DELETE FROM `" + s + "` WHERE `characterid` = ?");
				ps.setInt(1, characterId);
				ps.executeUpdate();
			}

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String name) {
		this.accountName = name;
	}

	public void setChannelId(byte channel) {
		this.channelId = channel;
	}

	public byte getWorldId() {
		return worldId;
	}

	public void setWorldId(byte world) {
		this.worldId = world;
	}

	public void pongReceived() {
		lastPong = System.currentTimeMillis();
	}

	public void sendPing() {
		final long ping = System.currentTimeMillis();
		announce(PacketCreator.getPing());
		TimerManager.getInstance().schedule(new ClientTimeoutAction(ping), 15000);
	}

	public Set<String> getMacs() {
		return Collections.unmodifiableSet(macs);
	}

	public int getGmLevel() {
		return this.gmlevel;
	}

	public void setScriptEngine(String name, ScriptEngine e) {
		engines.put(name, e);
	}

	public ScriptEngine getScriptEngine(String name) {
		return engines.get(name);
	}

	public void removeScriptEngine(String name) {
		engines.remove(name);
	}

	public ScheduledFuture<?> getIdleTask() {
		return idleTask;
	}

	public void setIdleTask(ScheduledFuture<?> idleTask) {
		this.idleTask = idleTask;
	}

	public NpcConversationManager getConversationManager() {
		return NpcScriptManager.getInstance().getConversationManager(this);
	}

	public QuestActionManager getQuestManager() {
		return QuestScriptManager.getInstance().getQuestManager(this);
	}

	public boolean acceptTermsOfService() {
		boolean disconnect = false;
		if (accountName == null) {
			return true;
		}

		final Connection connection = DatabaseConnection.getConnection();
		try (DatabaseCall call = DatabaseCall.query(getSelectTosById(connection))) {
			
			ResultSet rs = call.resultSet();
			if (rs.next()) {
				if (rs.getByte("tos") == 1) {
					disconnect = true;
				}
			}
		} catch (SQLException e) {
		}
		
		try (PreparedStatement ps = getUpdateTosById(connection)) {
			ps.executeUpdate();
		} catch (SQLException e) {
		}

		return disconnect;
	}

	private PreparedStatement getUpdateTosById(final Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `tos` = 1 WHERE `id` = ?");
		ps.setInt(1, accountId);
		return ps;
	}

	private PreparedStatement getSelectTosById(final Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `tos` FROM `accounts` WHERE `id` = ?");
		ps.setInt(1, accountId);
		return ps;
	}

	public static boolean checkHash(String hash, String type, String password) {
		try {
			MessageDigest digester = MessageDigest.getInstance(type);
			digester.update(password.getBytes("UTF-8"), 0, password.length());
			return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase().equals(hash);
		} catch (Exception e) {
			throw new RuntimeException("Encoding the string failed", e);
		}
	}

	public short getCharacterSlots() {
		return characterSlots;
	}

	public boolean gainCharacterSlot() {
		if (characterSlots < 15) {
			Connection con = DatabaseConnection.getConnection();
			try {
				PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `characterslots` = ? WHERE `id` = ?");
				ps.setInt(1, this.characterSlots += 1);
				ps.setInt(2, accountId);
				ps.executeUpdate();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	public final byte getGReason() {
		final Connection connection = DatabaseConnection.getConnection();
		try (DatabaseCall call = DatabaseCall.query(getSelectBanReason(connection))) {
			
			ResultSet rs = call.resultSet();
			if (rs.next()) {
				return rs.getByte("greason");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private PreparedStatement getSelectBanReason(final Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `greason` FROM `accounts` WHERE `id` = ?");
		ps.setInt(1, accountId);
		return ps;
	}

	public byte getGender() {
		return gender;
	}

	public void setGender(byte value) {
		this.gender = value;
		Connection con = DatabaseConnection.getConnection();
		try (PreparedStatement ps = getUpdateAccountGender(con);){			
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
		}
	}

	private PreparedStatement getUpdateAccountGender(Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `gender` = ? WHERE `id` = ?");
		ps.setByte(1, gender);
		ps.setInt(2, accountId);
		return ps;
	}

	public void announce(GamePacket packet) {
		session.write(packet);
	}

	public void saveLastKnownIP() {
		final String sockAddr = getSession().getRemoteAddress().toString();
		final Connection con = DatabaseConnection.getConnection();

		try (PreparedStatement ps = getUpdateLastKnownIP(con, sockAddr);) {
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			GameLogger.print(GameLogger.EXCEPTION_CAUGHT, e);
		}
	}

	private PreparedStatement getUpdateLastKnownIP(Connection connection, String sockAddr) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `lastknownip` = ? WHERE `name` = ?");
		ps.setString(1, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
		ps.setString(2, accountName);
		return ps;
	}
	
	public boolean isGM() {
		return (gmlevel >= 2);
	}
	
	public boolean isDeveloper() {
		return (gmlevel >= 4);
	}
	
	public static String getAccountNameById(int id) {
		final Connection con = DatabaseConnection.getConnection();
		try (PreparedStatement ps = getSelectAccountById(con, id);
			ResultSet rs = ps.executeQuery();) {			
			if (rs.next()) {
				String name = rs.getString("name");
				return name;
			}
		} catch (Exception e) {
		}
		return null;
	}

	private static PreparedStatement getSelectAccountById(Connection connection, int id) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `name` FROM `accounts` WHERE `id` = ?");
		ps.setInt(1, id);
		return ps;
	}
}
