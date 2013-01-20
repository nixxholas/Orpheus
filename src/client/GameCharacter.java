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

import java.awt.Point;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import net.GamePacket;
import net.server.Channel;
import net.server.Messenger;
import net.server.MessengerCharacter;
import net.server.Party;
import net.server.PartyCharacter;
import net.server.PartyOperation;
import net.server.PlayerBuffValueHolder;
import net.server.PlayerCoolDownValueHolder;
import net.server.PlayerDiseaseValueHolder;
import net.server.Server;
import net.server.World;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import scripting.event.EventInstanceManager;
import server.CashShop;
import server.BuffStatDelta;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.Minigame;
import server.PlayerShop;
import server.Portal;
import server.Shop;
import server.StatEffect;
import server.MapleStocks;
import server.Storage;
import server.Trade;
import server.TimerManager;
import server.events.MapleEvents;
import server.events.RescueGaga;
import server.events.gm.MapleFitness;
import server.events.gm.MapleOla;
import server.life.Monster;
import server.life.MobSkill;
import server.maps.AbstractAnimatedGameMapObject;
import server.maps.HiredMerchant;
import server.maps.Door;
import server.maps.GameMap;
import server.maps.GameMapEffect;
import server.maps.GameMapFactory;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.maps.Summon;
import server.maps.PlayerNPCs;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import server.partyquest.MonsterCarnival;
import server.partyquest.MonsterCarnivalParty;
import server.partyquest.PartyQuest;
import server.quest.Quest;
import tools.DatabaseConnection;
import tools.PacketCreator;
import tools.Output;
import tools.Pair;
import tools.Randomizer;
import client.autoban.AutobanManager;
import constants.ExpTable;
import constants.ItemConstants;
import constants.ServerConstants;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.FPArchMage;
import constants.skills.GM;
import constants.skills.Hermit;
import constants.skills.ILArchMage;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.Swordsman;
import constants.skills.ThunderBreaker;

public class GameCharacter extends AbstractAnimatedGameMapObject {

	private byte worldId;
	private int accountId, id;
	private String name;
	private int rank, rankMove, jobRank, jobRankMove;
	private int level, str, dex, luk, int_, hp, maxhp, mp, maxmp;
	private int hpMpApUsed;
	private int hair;
	private int face;
	private int remainingAp, remainingSp;
	private int fame;
	private int initialSpawnPoint;
	private int mapId;
	private int gender;
	private int chair;
	private int itemEffect;
	private int guildId, guildRank, allianceRank;
	private int messengerposition = 4;
	private int slots = 0;
	private int energybar;
	private int rebirths;
	private int gmLevel;
	private boolean whitetext = true;
	private Family family;
	private int familyId;
	private int bookCover;
	private int markedMonster = 0;
	private int battleshipHp = 0;
	private int mesosTraded = 0;
	private int possibleReports = 10;
	private int vanquisherStage, vanquisherKills;
	private int allowWarpToId;
	private int expRate = 1, mesoRate = 1, dropRate = 1;
	private MinigameStats omokStats, matchingCardStats;
	private DojoState dojoState = new DojoState();
	private int married;
	private long lastUsedCashItem, lastHealed;
	private transient int localMaxHp, localMaxMp, localStr, localDex, localLuk, localInt;
	private transient int magic, watk;
	private boolean hidden, canDoor = true, Berserk, hasMerchant;
	private int linkedLevel = 0;
	private String linkedName = null;
	private String chalktext;
	private MtsState mtsState = new MtsState();
	private AtomicInteger exp = new AtomicInteger();
	private AtomicInteger gachaexp = new AtomicInteger();
	private AtomicInteger meso = new AtomicInteger();
	private int merchantmeso;
	private BuddyList buddylist;
	private EventInstanceManager eventInstance = null;
	private HiredMerchant hiredMerchant = null;
	private GameClient client;
	private GuildCharacter mgc = null;
	private PartyCharacter mpc = null;
	private Inventory[] inventory;
	private Job job = Job.BEGINNER;
	private GameMap map; // Make a Dojo pq instance
	private Messenger messenger = null;
	private Minigame activeMinigame;
	private Mount mount;
	private Party party;
	private Pet[] pets = new Pet[3];
	private PlayerShop playerShop = null;
	private Shop shop = null;
	private SkinColor skinColor = SkinColor.NORMAL;
	private Storage storage = null;
	private Trade trade = null;
	private SavedLocation[] savedLocations;
	private SkillMacro[] skillMacros = new SkillMacro[5];
	private FameStats fameStats;
	private Map<Quest, QuestStatus> quests;
	private Set<Monster> controlled = new LinkedHashSet<Monster>();
	private Map<Integer, String> entered = new LinkedHashMap<Integer, String>();
	private Set<GameMapObject> visibleMapObjects = new LinkedHashSet<GameMapObject>();
	private Map<ISkill, SkillEntry> skills = new LinkedHashMap<ISkill, SkillEntry>();
	private EnumMap<BuffStat, BuffStatValueHolder> effects = new EnumMap<BuffStat, BuffStatValueHolder>(BuffStat.class);
	private Map<Integer, KeyBinding> keymap = new LinkedHashMap<Integer, KeyBinding>();
	private Map<Integer, Summon> summons = new LinkedHashMap<Integer, Summon>();
	private Map<Integer, CooldownValueHolder> coolDowns = new LinkedHashMap<Integer, CooldownValueHolder>(50);
	private EnumMap<Disease, DiseaseValueHolder> diseases = new EnumMap<Disease, DiseaseValueHolder>(Disease.class);
	private List<Door> doors = new ArrayList<Door>();
	private ScheduledFuture<?> dragonBloodSchedule;
	private ScheduledFuture<?> mapTimeLimitTask = null;
	private ScheduledFuture<?>[] fullnessSchedule = new ScheduledFuture<?>[3];
	private ScheduledFuture<?> hpDecreaseTask;
	private ScheduledFuture<?> beholderHealingSchedule, beholderBuffSchedule, berserkSchedule;
	private ScheduledFuture<?> expirationSchedule;
	private ScheduledFuture<?> recoverySchedule;
	private List<ScheduledFuture<?>> timers = new ArrayList<ScheduledFuture<?>>();
	// private NumberFormat nf = new DecimalFormat("#,###,###,###");
	private ArrayList<Integer> excluded = new ArrayList<Integer>();
	private MonsterBook monsterbook;
	private List<Ring> crushRings = new ArrayList<Ring>();
	private List<Ring> friendshipRings = new ArrayList<Ring>();
	private Ring marriageRing;
	private static String[] ariantRoomLeader = new String[3];
	private static int[] ariantRoomSlot = new int[3];
	private CashShop cashshop;
	private long portaldelay = 0, lastcombo = 0;
	private short combocounter = 0;
	private List<String> blockedPortals = new ArrayList<String>();
	public ArrayList<String> area_data = new ArrayList<String>();
	private AutobanManager autoban;
	private MapleStockPortfolio stockPortfolio;
	private boolean isbanned = false;
	private ScheduledFuture<?> pendantSchedule = null; // 1122017
	private byte pendantExp = 0, lastmobcount = 0;
	private TeleportRockInfo teleportRocks = new TeleportRockInfo();
	private Map<String, MapleEvents> events = new LinkedHashMap<String, MapleEvents>();
	private PartyQuest partyQuest = null;
	private boolean hardcore = false;
	private boolean dead = false;

	private GameCharacter() {
		setStance(0);
		inventory = new Inventory[InventoryType.values().length];
		savedLocations = new SavedLocation[SavedLocationType.values().length];

		for (InventoryType type : InventoryType.values()) {
			byte b = 24;
			if (type == InventoryType.CASH) {
				b = 96;
			}
			inventory[type.ordinal()] = new Inventory(type, (byte) b);
		}
		for (int i = 0; i < SavedLocationType.values().length; i++) {
			savedLocations[i] = null;
		}
		quests = new LinkedHashMap<Quest, QuestStatus>();
		setPosition(new Point(0, 0));
	}

	public static GameCharacter getDefault(GameClient c) {
		GameCharacter ret = new GameCharacter();
		ret.client = c;
		ret.gmLevel = c.gmLevel();
		ret.hp = 50;
		ret.maxhp = 50;
		ret.mp = 5;
		ret.maxmp = 5;
		ret.str = 12;
		ret.dex = 5;
		ret.int_ = 4;
		ret.luk = 4;
		ret.map = null;
		ret.job = Job.BEGINNER;
		ret.level = 1;
		ret.accountId = c.getAccountId();
		ret.buddylist = new BuddyList(20);
		ret.mount = null;
		ret.getInventory(InventoryType.EQUIP).setSlotLimit(24);
		ret.getInventory(InventoryType.USE).setSlotLimit(24);
		ret.getInventory(InventoryType.SETUP).setSlotLimit(24);
		ret.getInventory(InventoryType.ETC).setSlotLimit(24);
		int[] key = {18, 65, 2, 23, 3, 4, 5, 6, 16, 17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56, 59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41, 39};
		int[] type = {4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6, 5, 4, 5, 4, 4, 4, 4, 4};
		int[] action = {0, 106, 10, 1, 12, 13, 18, 24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53, 100, 101, 102, 103, 104, 105, 54, 22, 52, 21, 25, 26, 23, 27};
		for (int i = 0; i < key.length; i++) {
			ret.keymap.put(key[i], new KeyBinding(type[i], action[i]));
		}

		if (ret.isGM()) {
			ret.job = Job.SUPERGM;
			ret.level = 200;
			// int[] gmskills = {9001000, 9001001, 9001000, 9101000, 9101001,
			// 9101002, 9101003, 9101004, 9101005, 9101006, 9101007, 9101008};
		}
		return ret;
	}

	public void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
		if (this.coolDowns.containsKey(Integer.valueOf(skillId))) {
			this.coolDowns.remove(skillId);
		}
		this.coolDowns.put(Integer.valueOf(skillId), new CooldownValueHolder(skillId, startTime, length, timer));
	}

	public DojoState getDojoState() {
		return this.dojoState;
	}
	
	public void addCrushRing(Ring r) {
		crushRings.add(r);
	}

	public Ring getRingById(int id) {
		for (Ring ring : getCrushRings()) {
			if (ring.getRingId() == id) {
				return ring;
			}
		}
		for (Ring ring : getFriendshipRings()) {
			if (ring.getRingId() == id) {
				return ring;
			}
		}
		if (getMarriageRing().getRingId() == id) {
			return getMarriageRing();
		}

		return null;
	}

	public void addDoor(Door door) {
		doors.add(door);
	}

	public void addExcluded(int x) {
		excluded.add(x);
	}

	public void addFame(int famechange) {
		this.fame += famechange;
	}

	public void addFriendshipRing(Ring r) {
		friendshipRings.add(r);
	}

	public void addHP(int delta) {
		setHp(hp + delta);
		updateSingleStat(Stat.HP, hp);
	}

	public void addMesosTraded(int gain) {
		this.mesosTraded += gain;
	}

	public void addMP(int delta) {
		setMp(mp + delta);
		updateSingleStat(Stat.MP, mp);
	}

	public void addMPHP(int hpDiff, int mpDiff) {
		setHp(hp + hpDiff);
		setMp(mp + mpDiff);
		updateSingleStat(Stat.HP, getHp());
		updateSingleStat(Stat.MP, getMp());
	}

	public void addPet(Pet pet) {
		for (int i = 0; i < 3; i++) {
			if (pets[i] == null) {
				pets[i] = pet;
				return;
			}
		}
	}

	public void addStat(int type, int up) {
		if (type == 1) {
			this.str += up;
			updateSingleStat(Stat.STR, str);
		} else if (type == 2) {
			this.dex += up;
			updateSingleStat(Stat.DEX, dex);
		} else if (type == 3) {
			this.int_ += up;
			updateSingleStat(Stat.INT, int_);
		} else if (type == 4) {
			this.luk += up;
			updateSingleStat(Stat.LUK, luk);
		}
	}

	public int addHP(GameClient c) {
		GameCharacter player = c.getPlayer();
		Job jobtype = player.getJob();
		int MaxHP = player.getMaxHp();
		if (player.getHpMpApUsed() > 9999 || MaxHP >= 30000) {
			return MaxHP;
		}
		if (jobtype.isA(Job.BEGINNER)) {
			MaxHP += 8;
		} else if (jobtype.isA(Job.WARRIOR) || jobtype.isA(Job.DAWNWARRIOR1)) {
			if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(10000000) : SkillFactory.getSkill(1000001)) > 0) {
				MaxHP += 20;
			} else {
				MaxHP += 8;
			}
		} else if (jobtype.isA(Job.MAGICIAN) || jobtype.isA(Job.BLAZEWIZARD1)) {
			MaxHP += 6;
		} else if (jobtype.isA(Job.BOWMAN) || jobtype.isA(Job.WINDARCHER1)) {
			MaxHP += 8;
		} else if (jobtype.isA(Job.THIEF) || jobtype.isA(Job.NIGHTWALKER1)) {
			MaxHP += 8;
		} else if (jobtype.isA(Job.PIRATE) || jobtype.isA(Job.THUNDERBREAKER1)) {
			if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(15100000) : SkillFactory.getSkill(5100000)) > 0) {
				MaxHP += 18;
			} else {
				MaxHP += 8;
			}
		}
		return MaxHP;
	}

	public int addMP(GameClient c) {
		GameCharacter player = c.getPlayer();
		int MaxMP = player.getMaxMp();
		if (player.getHpMpApUsed() > 9999 || player.getMaxMp() >= 30000) {
			return MaxMP;
		}
		if (player.getJob().isA(Job.BEGINNER) || player.getJob().isA(Job.NOBLESSE) || player.getJob().isA(Job.LEGEND)) {
			MaxMP += 6;
		} else if (player.getJob().isA(Job.WARRIOR) || player.getJob().isA(Job.DAWNWARRIOR1) || player.getJob().isA(Job.ARAN1)) {
			MaxMP += 2;
		} else if (player.getJob().isA(Job.MAGICIAN) || player.getJob().isA(Job.BLAZEWIZARD1)) {
			if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(12000000) : SkillFactory.getSkill(2000001)) > 0) {
				MaxMP += 18;
			} else {
				MaxMP += 14;
			}

		} else if (player.getJob().isA(Job.BOWMAN) || player.getJob().isA(Job.THIEF)) {
			MaxMP += 10;
		} else if (player.getJob().isA(Job.PIRATE)) {
			MaxMP += 14;
		}

		return MaxMP;
	}

	public void addSummon(int id, Summon summon) {
		summons.put(id, summon);
	}

	public void addVisibleMapObject(GameMapObject mo) {
		visibleMapObjects.add(mo);
	}

	public void ban(String reason) {
		Connection con = DatabaseConnection.getConnection();
		try (PreparedStatement ps = getUpdateBanData(con, reason)) {
			ps.executeUpdate();
		} catch (SQLException ex) {
			Output.print("An error has occurred while writing ban data.");
			ex.printStackTrace();		
		}
	}

	private PreparedStatement getUpdateBanData(Connection connection,
			String reason) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `banned` = 1, `banreason` = ? WHERE `id` = ?");
		ps.setString(1, reason);
		ps.setInt(2, accountId);
		return ps;
	}

	public static boolean ban(String id, String reason, boolean accountId) {
		PreparedStatement ps = null;
		try {
			Connection con = DatabaseConnection.getConnection();
			if (id.matches("/[0-9]{1,3}\\..*")) {
				ps = con.prepareStatement("INSERT INTO `ipbans` VALUES (DEFAULT, ?)");
				ps.setString(1, id);
				ps.executeUpdate();
				ps.close();
				return true;
			}
			if (accountId) {
				ps = con.prepareStatement("SELECT `id` FROM `accounts` WHERE `name` = ?");
			} else {
				ps = con.prepareStatement("SELECT `accountid` FROM `characters` WHERE `name` = ?");
			}

			boolean ret = false;
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				PreparedStatement psb = DatabaseConnection.getConnection().prepareStatement("UPDATE `accounts` SET `banned` = 1, `banreason` = ? WHERE `id` = ?");
				psb.setString(1, reason);
				psb.setInt(2, rs.getInt(1));
				psb.executeUpdate();
				psb.close();
				ret = true;
			}
			rs.close();
			ps.close();
			return ret;
		} catch (SQLException ex) {
		} finally {
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException e) {
			}
		}
		return false;
	}

	public int calculateMaxBaseDamage(int watk) {
		int maxbasedamage;
		if (watk == 0) {
			maxbasedamage = 1;
		} else {
			IItem weapon_item = getInventory(InventoryType.EQUIPPED).getItem((byte) -11);
			if (weapon_item != null) {
				WeaponType weapon = ItemInfoProvider.getInstance().getWeaponType(weapon_item.getItemId());
				int mainstat;
				int secondarystat;
				if (weapon == WeaponType.BOW || weapon == WeaponType.CROSSBOW) {
					mainstat = localDex;
					secondarystat = localStr;
				} else if ((getJob().isA(Job.THIEF) || getJob().isA(Job.NIGHTWALKER1)) && (weapon == WeaponType.CLAW || weapon == WeaponType.DAGGER)) {
					mainstat = localLuk;
					secondarystat = localDex + localStr;
				} else {
					mainstat = localStr;
					secondarystat = localDex;
				}
				maxbasedamage = (int) (((weapon.getMaxDamageMultiplier() * mainstat + secondarystat) / 100.0) * watk) + 10;
			} else {
				maxbasedamage = 0;
			}
		}
		return maxbasedamage;
	}

	public void cancelAllBuffs(boolean disconnect) {
		if (disconnect) {
			effects.clear();
		} else {
			for (BuffStatValueHolder mbsvh : new ArrayList<BuffStatValueHolder>(effects.values())) {
				cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	public void cancelBuffStats(BuffStat stat) {
		List<BuffStat> buffStatList = Arrays.asList(stat);
		deregisterBuffStats(buffStatList);
		cancelPlayerBuffs(buffStatList);
	}

	public void setCombo(short count) {
		if (combocounter > 30000) {
			combocounter = 30000;
			return;
		} else {
			combocounter = count;
		}
		announce(PacketCreator.showCombo(combocounter));
	}

	public void setLastCombo(long time) {
		;
		lastcombo = time;
	}

	public short getCombo() {
		return combocounter;
	}

	public long getLastCombo() {
		return lastcombo;
	}

	public int getLastMobCount() { // Used for skills that have mobCount at 1.
									// (a/b)
		return lastmobcount;
	}

	public void setLastMobCount(byte count) {
		lastmobcount = count;
	}

	public void newClient(GameClient c) {
		c.setAccountName(this.client.getAccountName());// No null's for
														// accountName
		this.client = c;
		Portal portal = map.findClosestSpawnpoint(getPosition());
		if (portal == null) {
			portal = map.getPortal(0);
		}
		this.setPosition(portal.getPosition());
		this.initialSpawnPoint = portal.getId();
		this.map = c.getChannelServer().getMapFactory().getMap(getMapId());
	}

	public void cancelBuffEffects() {
		for (BuffStatValueHolder mbsvh : effects.values()) {
			mbsvh.schedule.cancel(false);
		}
		this.effects.clear();
	}

	private final class DojoForceWarpAction implements Runnable {
		private final boolean rightmap;

		private DojoForceWarpAction(boolean rightmap) {
			this.rightmap = rightmap;
		}

		@Override
		public void run() {
			if (rightmap) {
				final GameMap targetMap = client.getChannelServer().getMapFactory().getMap(925020000);
				client.getPlayer().changeMap(targetMap);
			}
		}
	}

	private final class PendantHourlyAction implements Runnable {
		@Override
		public void run() {
			if (pendantExp < 3) {
				pendantExp++;
				message("Pendant of the Spirit has been equipped for " + pendantExp + " hour(s), you will now receive " + pendantExp + "0% bonus exp.");
			} else {
				pendantSchedule.cancel(false);
			}
		}
	}

	private final class QuestExpirationAction implements Runnable {
		private final Quest quest;

		private QuestExpirationAction(Quest quest) {
			this.quest = quest;
		}

		@Override
		public void run() {
			announce(PacketCreator.questExpire(quest.getId()));
			QuestStatus newStatus = new QuestStatus(quest, QuestStatus.Status.NOT_STARTED);
			newStatus.setForfeited(getQuest(quest).getForfeited() + 1);
			updateQuest(newStatus);
		}
	}

	private final class MapEffectAction implements Runnable {
		private final GameMapEffect mapEffect;

		private MapEffectAction(GameMapEffect mapEffect) {
			this.mapEffect = mapEffect;
		}

		@Override
		public void run() {
			getClient().announce(mapEffect.makeDestroyData());
		}
	}

	private final class FullnessScheduleAction implements Runnable {
		private final Pet pet;
		private final int decrease;

		private FullnessScheduleAction(Pet pet, int decrease) {
			this.pet = pet;
			this.decrease = decrease;
		}

		@Override
		public void run() {
			int newFullness = pet.getFullness() - decrease;
			if (newFullness <= 5) {
				pet.setFullness(15);
				pet.saveToDb();
				unequipPet(pet, true);
			} else {
				pet.setFullness(newFullness);
				pet.saveToDb();
				IItem petz = getInventory(InventoryType.CASH).getItem(pet.getSlot());
				client.announce(PacketCreator.updateSlot(petz));
			}
		}
	}

	public static class CancelCooldownAction implements Runnable {

		private int skillId;
		private WeakReference<GameCharacter> target;

		public CancelCooldownAction(GameCharacter target, int skillId) {
			this.target = new WeakReference<GameCharacter>(target);
			this.skillId = skillId;
		}

		@Override
		public void run() {
			GameCharacter realTarget = target.get();
			if (realTarget != null) {
				realTarget.removeCooldown(skillId);
				realTarget.client.announce(PacketCreator.skillCooldown(skillId, 0));
			}
		}
	}

	public void cancelEffect(int itemId) {
		cancelEffect(ItemInfoProvider.getInstance().getItemEffect(itemId), false, -1);
	}

	public void cancelEffect(StatEffect effect, boolean overwrite, long startTime) {
		List<BuffStat> buffstats;
		if (!overwrite) {
			buffstats = getBuffStats(effect, startTime);
		} else {
			List<BuffStatDelta> statups = effect.getStatups();
			buffstats = new ArrayList<BuffStat>(statups.size());
			for (BuffStatDelta statup : statups) {
				buffstats.add(statup.stat);
			}
		}
		deregisterBuffStats(buffstats);
		if (effect.isMagicDoor()) {
			if (!getDoors().isEmpty()) {
				Door door = getDoors().iterator().next();
				for (GameCharacter chr : door.getTarget().getCharacters()) {
					door.sendDestroyData(chr.client);
				}
				for (GameCharacter chr : door.getTown().getCharacters()) {
					door.sendDestroyData(chr.client);
				}
				for (Door destroyDoor : getDoors()) {
					door.getTarget().removeMapObject(destroyDoor);
					door.getTown().removeMapObject(destroyDoor);
				}
				clearDoors();
				silentPartyUpdate();
			}
		}
		if (effect.getSourceId() == Spearman.HYPER_BODY || effect.getSourceId() == GM.HYPER_BODY || effect.getSourceId() == SuperGM.HYPER_BODY) {
			List<StatDelta> statup = new ArrayList<StatDelta>(4);
			statup.add(new StatDelta(Stat.HP, Math.min(hp, maxhp)));
			statup.add(new StatDelta(Stat.MP, Math.min(mp, maxmp)));
			statup.add(new StatDelta(Stat.MAXHP, maxhp));
			statup.add(new StatDelta(Stat.MAXMP, maxmp));
			client.announce(PacketCreator.updatePlayerStats(statup));
		}
		if (effect.isMonsterRiding()) {
			if (effect.getSourceId() != Corsair.BATTLE_SHIP) {
				this.getMount().cancelSchedule();
				this.getMount().setActive(false);
			}
		}
		if (!overwrite) {
			cancelPlayerBuffs(buffstats);
		}
	}

	public void cancelEffectFromBuffStat(BuffStat stat) {
		BuffStatValueHolder effect = effects.get(stat);
		if (effect != null) {
			cancelEffect(effect.effect, false, -1);
		}
	}

	public void setHidden(boolean hidden) {
		if (isGM()) {
			this.hidden = hidden;
		}
	}
	
	public void toggleHide(boolean login) {
		if (isGM()) {
			if (isHidden()) {
				this.hidden = false;
				announce(PacketCreator.getGMEffect(0x10, (byte) 0));
				getMap().broadcastNONGMMessage(this, PacketCreator.spawnPlayerMapObject(this), false);
				updatePartyMemberHP();
			} else {
				this.hidden = true;
				announce(PacketCreator.getGMEffect(0x10, (byte) 1));
				if (!login) {
					getMap().broadcastNONGMMessage(this, PacketCreator.removePlayerFromMap(getId()), false);
				}
			}
			announce(PacketCreator.enableActions());
		}
	}

	private void cancelFullnessSchedule(int petSlot) {
		if (fullnessSchedule[petSlot] != null) {
			fullnessSchedule[petSlot].cancel(false);
		}
	}

	public void cancelMagicDoor() {
		for (BuffStatValueHolder mbsvh : new ArrayList<BuffStatValueHolder>(effects.values())) {
			if (mbsvh.effect.isMagicDoor()) {
				cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	public void cancelMapTimeLimitTask() {
		if (mapTimeLimitTask != null) {
			mapTimeLimitTask.cancel(false);
		}
	}

	private void cancelPlayerBuffs(List<BuffStat> buffstats) {
		if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
			recalcLocalStats();
			enforceMaxHpMp();
			client.announce(PacketCreator.cancelBuff(buffstats));
			if (buffstats.size() > 0) {
				getMap().broadcastMessage(this, PacketCreator.cancelForeignBuff(getId(), buffstats), false);
			}
		}
	}

	public static boolean canCreateChar(String name) {
		if (name.length() < 4 || name.length() > 12) {
			return false;
		}

		if (isInUse(name)) {
			return false;
		}

		return getIdByName(name) < 0 && !name.toLowerCase().contains("gm") && Pattern.compile("[a-zA-Z0-9_-]{3,12}").matcher(name).matches();
	}

	public boolean canDoor() {
		return canDoor;
	}

	public void changeJob(Job newJob) {
		this.job = newJob;
		this.remainingSp++;
		if (newJob.getId() % 10 == 2) {
			this.remainingSp += 2;
		}
		if (newJob.getId() % 10 > 1) {
			this.remainingAp += 5;
		}
		int job_ = job.getId() % 1000; // lame temp "fix"
		if (job_ == 100) {
			maxhp += Randomizer.rand(200, 250);
		} else if (job_ == 200) {
			maxmp += Randomizer.rand(100, 150);
		} else if (job_ % 100 == 0) {
			maxhp += Randomizer.rand(100, 150);
			maxhp += Randomizer.rand(25, 50);
		} else if (job_ > 0 && job_ < 200) {
			maxhp += Randomizer.rand(300, 350);
		} else if (job_ < 300) {
			maxmp += Randomizer.rand(450, 500);
		} // handle KoC here (undone)
		else if (job_ > 0 && job_ != 1000) {
			maxhp += Randomizer.rand(300, 350);
			maxmp += Randomizer.rand(150, 200);
		}
		if (maxhp >= 30000) {
			maxhp = 30000;
		}
		if (maxmp >= 30000) {
			maxmp = 30000;
		}
		if (!isGM()) {
			for (byte i = 1; i < 5; i++) {
				gainSlots(i, 4, true);
			}
		}
		List<StatDelta> statup = new ArrayList<StatDelta>(5);
		statup.add(new StatDelta(Stat.MAXHP, maxhp));
		statup.add(new StatDelta(Stat.MAXMP, maxmp));
		statup.add(new StatDelta(Stat.AVAILABLEAP, remainingAp));
		statup.add(new StatDelta(Stat.AVAILABLESP, remainingSp));
		statup.add(new StatDelta(Stat.JOB, job.getId()));
		recalcLocalStats();
		client.announce(PacketCreator.updatePlayerStats(statup));
		silentPartyUpdate();
		if (this.guildId > 0) {
			getGuild().broadcast(PacketCreator.jobMessage(0, job.getId(), name), this.getId());
		}
		guildUpdate();
		getMap().broadcastMessage(this, PacketCreator.showForeignEffect(getId(), 8), false);
	}

	public void changeKeybinding(int key, KeyBinding keybinding) {
		if (keybinding.getType() != 0) {
			keymap.put(Integer.valueOf(key), keybinding);
		} else {
			keymap.remove(Integer.valueOf(key));
		}
	}

	public void changeMap(int map) {
		changeMap(map, 0);
	}

	public void changeMap(int map, int portal) {
		GameMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
		changeMap(warpMap, warpMap.getPortal(portal));
	}

	public void changeMap(int map, String portal) {
		GameMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
		changeMap(warpMap, warpMap.getPortal(portal));
	}

	public void changeMap(int map, Portal portal) {
		GameMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
		changeMap(warpMap, portal);
	}

	public void changeMap(GameMap to) {
		changeMap(to, to.getPortal(0));
	}

	public void changeMap(final GameMap to, final Portal pto) {
		changeMapInternal(to, pto.getPosition(), PacketCreator.getWarpToMap(to, pto.getId(), this));
	}

	public void changeMap(final GameMap to, final Point pos) {
		// Position :O (LEFT)
		changeMapInternal(to, pos, PacketCreator.getWarpToMap(to, 0x80, this));
	}

	public void changeMapBanish(int mapid, String portal, String msg) {
		dropMessage(5, msg);
		GameMap map_ = client.getChannelServer().getMapFactory().getMap(mapid);
		changeMap(map_, map_.getPortal(portal));
	}

	private void changeMapInternal(final GameMap to, final Point pos, GamePacket warpPacket) {
		warpPacket.setOnSend(new Runnable() {

			@Override
			public void run() {
				map.removePlayer(GameCharacter.this);
				if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
					map = to;
					setPosition(pos);
					map.addPlayer(GameCharacter.this);
					if (party != null) {
						mpc.setMapId(to.getId());
						silentPartyUpdate();
						client.announce(PacketCreator.updateParty(client.getChannel(), party, PartyOperation.SILENT_UPDATE, null));
						updatePartyMemberHP();
					}
					if (getMap().getHPDec() > 0) {
						hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {

							@Override
							public void run() {
								doHurtHp();
							}
						}, 10000);
					}
				}
			}
		});
		client.announce(warpPacket);
	}

	public void changeSkillLevel(ISkill skill, byte newLevel, int newMasterlevel, long expiration) {
		if (newLevel > -1) {
			skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
			this.client.announce(PacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, expiration));
		} else {
			skills.remove(skill);
			// Shouldn't use expiration anymore :)
			this.client.announce(PacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, -1)); 
			try {
				Connection con = DatabaseConnection.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM skills WHERE skillid = ? AND characterid = ?");
				ps.setInt(1, skill.getId());
				ps.setInt(2, id);
				ps.execute();
				ps.close();
			} catch (SQLException ex) {
				Output.print("Unable to delete skill from database.\r\n" + ex);
			}
		}
	}

	public void checkBerserk() {
		if (berserkSchedule != null) {
			berserkSchedule.cancel(false);
		}
		final GameCharacter chr = this;
		if (job.equals(Job.DARKKNIGHT)) {
			ISkill BerserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
			final int skilllevel = getSkillLevel(BerserkX);
			if (skilllevel > 0) {
				Berserk = chr.getHp() * 100 / chr.getMaxHp() < BerserkX.getEffect(skilllevel).getX();
				berserkSchedule = TimerManager.getInstance().register(new Runnable() {

					@Override
					public void run() {
						client.announce(PacketCreator.showOwnBerserk(skilllevel, Berserk));
						getMap().broadcastMessage(GameCharacter.this, PacketCreator.showBerserk(getId(), skilllevel, Berserk), false);
					}
				}, 5000, 3000);
			}
		}
	}

	public void checkMessenger() {
		if (messenger != null && messengerposition < 4 && messengerposition > -1) {
			World worldz = Server.getInstance().getWorld(worldId);
			worldz.silentJoinMessenger(messenger.getId(), new MessengerCharacter(this, messengerposition), messengerposition);
			worldz.updateMessenger(getMessenger().getId(), name, client.getChannel());
		}
	}

	public void checkMonsterAggro(Monster monster) {
		if (!monster.isControllerHasAggro()) {
			if (monster.getController() == this) {
				monster.setControllerHasAggro(true);
			} else {
				monster.switchController(this, true);
			}
		}
	}

	public void clearDoors() {
		doors.clear();
	}

	public void clearSavedLocation(SavedLocationType type) {
		savedLocations[type.ordinal()] = null;
	}

	public void controlMonster(Monster monster, boolean aggro) {
		monster.setController(this);
		controlled.add(monster);
		client.announce(PacketCreator.controlMonster(monster, false, aggro));
	}

	public int countItem(int itemid) {
		return inventory[ItemInfoProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
	}

	public void decreaseBattleshipHp(int decrease) {
		this.battleshipHp -= decrease;
		if (battleshipHp <= 0) {
			this.battleshipHp = 0;
			ISkill battleship = SkillFactory.getSkill(Corsair.BATTLE_SHIP);
			int cooldown = battleship.getEffect(getSkillLevel(battleship)).getCooldown();
			announce(PacketCreator.skillCooldown(Corsair.BATTLE_SHIP, cooldown));
			addCooldown(Corsair.BATTLE_SHIP, System.currentTimeMillis(), cooldown, TimerManager.getInstance().schedule(new CancelCooldownAction(this, Corsair.BATTLE_SHIP), cooldown * 1000));
			removeCooldown(5221999);
			cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
		} else {
			announce(PacketCreator.skillCooldown(5221999, battleshipHp / 10)); // :D
			addCooldown(5221999, 0, battleshipHp, null);
		}
	}

	public void decreaseReports() {
		this.possibleReports--;
	}

	public void deleteGuild(int guildId) {
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE `characters` SET `guildid` = 0, `guildrank` = 5 WHERE `guildid` = ?");
			ps.setInt(1, guildId);
			ps.execute();
			ps.close();
			ps = con.prepareStatement("DELETE FROM `guilds` WHERE `guildid` = ?");
			ps.setInt(1, id);
			ps.execute();
			ps.close();
		} catch (SQLException ex) {
			Output.print("Unable to delete guild from database.\r\n" + ex);
		}
	}

	private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, id);
		ps.executeUpdate();
		ps.close();
	}

	private void deregisterBuffStats(List<BuffStat> stats) {
		synchronized (stats) {
			List<BuffStatValueHolder> effectsToCancel = new ArrayList<BuffStatValueHolder>(stats.size());
			for (BuffStat stat : stats) {
				BuffStatValueHolder mbsvh = effects.get(stat);
				if (mbsvh != null) {
					effects.remove(stat);
					boolean addMbsvh = true;
					for (BuffStatValueHolder contained : effectsToCancel) {
						if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
							addMbsvh = false;
						}
					}
					if (addMbsvh) {
						effectsToCancel.add(mbsvh);
					}
					if (stat == BuffStat.RECOVERY) {
						if (recoverySchedule != null) {
							recoverySchedule.cancel(false);
							recoverySchedule = null;
						}
					} else if (stat == BuffStat.SUMMON || stat == BuffStat.PUPPET) {
						int summonId = mbsvh.effect.getSourceId();
						Summon summon = summons.get(summonId);
						if (summon != null) {
							getMap().broadcastMessage(PacketCreator.removeSummon(summon, true), summon.getPosition());
							getMap().removeMapObject(summon);
							removeVisibleMapObject(summon);
							summons.remove(summonId);
						}
						if (summon.getSkill() == DarkKnight.BEHOLDER) {
							if (beholderHealingSchedule != null) {
								beholderHealingSchedule.cancel(false);
								beholderHealingSchedule = null;
							}
							if (beholderBuffSchedule != null) {
								beholderBuffSchedule.cancel(false);
								beholderBuffSchedule = null;
							}
						}
					} else if (stat == BuffStat.DRAGONBLOOD) {
						dragonBloodSchedule.cancel(false);
						dragonBloodSchedule = null;
					}
				}
			}
			for (BuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
				if (cancelEffectCancelTasks.schedule != null) {
					cancelEffectCancelTasks.schedule.cancel(false);
				}
			}
		}
	}

	public void disableDoor() {
		canDoor = false;
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				canDoor = true;
			}
		}, 5000);
	}

	public void disbandGuild() {
		if (guildId < 1 || guildRank != 1) {
			return;
		}
		try {
			Server.getInstance().disbandGuild(guildId);
		} catch (Exception e) {
		}
	}

	public void dispel() {
		for (BuffStatValueHolder mbsvh : new ArrayList<BuffStatValueHolder>(effects.values())) {
			if (mbsvh.effect.isSkill()) {
				cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	public final List<PlayerDiseaseValueHolder> getAllDiseases() {
		final List<PlayerDiseaseValueHolder> ret = new ArrayList<PlayerDiseaseValueHolder>(5);

		DiseaseValueHolder vh;
		for (Entry<Disease, DiseaseValueHolder> disease : diseases.entrySet()) {
			vh = disease.getValue();
			ret.add(new PlayerDiseaseValueHolder(disease.getKey(), vh.startTime, vh.length));
		}
		return ret;
	}

	public final boolean hasDisease(final Disease dis) {
		for (final Disease disease : diseases.keySet()) {
			if (disease == dis) {
				return true;
			}
		}
		return false;
	}

	public void giveDebuff(final Disease disease, MobSkill skill) {
		final List<DiseaseEntry> debuff = Collections.singletonList(new DiseaseEntry(disease, skill.getX()));

		if (!hasDisease(disease) && diseases.size() < 2) {
			if (!(disease == Disease.SEDUCE || disease == Disease.STUN)) {
				if (isActiveBuffedValue(2321005)) {
					return;
				}
			}
			TimerManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					dispelDebuff(disease);
				}
			}, skill.getDuration());

			diseases.put(disease, new DiseaseValueHolder(System.currentTimeMillis(), skill.getDuration()));
			client.getSession().write(PacketCreator.giveDebuff(debuff, skill));
			map.broadcastMessage(this, PacketCreator.giveForeignDebuff(id, debuff, skill), false);
		}
	}

	public void dispelDebuff(Disease debuff) {
		if (hasDisease(debuff)) {
			long mask = debuff.getValue();
			announce(PacketCreator.cancelDebuff(mask));
			map.broadcastMessage(this, PacketCreator.cancelForeignDebuff(id, mask), false);

			diseases.remove(debuff);
		}
	}

	public void dispelDebuffs() {
		dispelDebuff(Disease.CURSE);
		dispelDebuff(Disease.DARKNESS);
		dispelDebuff(Disease.POISON);
		dispelDebuff(Disease.SEAL);
		dispelDebuff(Disease.WEAKEN);
	}

	public void cancelAllDebuffs() {
		diseases.clear();
	}

	public void dispelSkill(int skillid) {
		LinkedList<BuffStatValueHolder> allBuffs = new LinkedList<BuffStatValueHolder>(effects.values());
		for (BuffStatValueHolder mbsvh : allBuffs) {
			if (skillid == 0) {
				if (mbsvh.effect.isSkill() && (mbsvh.effect.getSourceId() % 10000000 == 1004 || dispelSkills(mbsvh.effect.getSourceId()))) {
					cancelEffect(mbsvh.effect, false, mbsvh.startTime);
				}
			} else if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
				cancelEffect(mbsvh.effect, false, mbsvh.startTime);
			}
		}
	}

	private boolean dispelSkills(int skillid) {
		switch (skillid) {
			case DarkKnight.BEHOLDER:
			case FPArchMage.ELQUINES:
			case ILArchMage.IFRIT:
			case Priest.SUMMON_DRAGON:
			case Bishop.BAHAMUT:
			case Ranger.PUPPET:
			case Ranger.SILVER_HAWK:
			case Sniper.PUPPET:
			case Sniper.GOLDEN_EAGLE:
			case Hermit.SHADOW_PARTNER:
				return true;
			default:
				return false;
		}
	}

	public void doHurtHp() {
		if (this.getInventory(InventoryType.EQUIPPED).findById(getMap().getHPDecProtect()) != null) {
			return;
		}
		addHP(-getMap().getHPDec());
		hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				doHurtHp();
			}
		}, 10000);
	}

	public void dropMessage(String message) {
		dropMessage(0, message);
	}

	public void dropMessage(int type, String message) {
		client.announce(PacketCreator.serverNotice(type, message));
	}

	public List<ScheduledFuture<?>> getTimers() {
		return timers;
	}

	private void enforceMaxHpMp() {
		List<StatDelta> stats = new ArrayList<StatDelta>(2);
		if (getMp() > getCurrentMaxMp()) {
			setMp(getMp());
			stats.add(new StatDelta(Stat.MP, getMp()));
		}
		if (getHp() > getCurrentMaxHp()) {
			setHp(getHp());
			stats.add(new StatDelta(Stat.HP, getHp()));
		}
		if (stats.size() > 0) {
			client.announce(PacketCreator.updatePlayerStats(stats));
		}
	}

	public void enteredScript(String script, int mapid) {
		if (!entered.containsKey(mapid)) {
			entered.put(mapid, script);
		}
	}

	public void enterHardcore() {
		hardcore = true;
		saveToDB(true);
	}

	public void equipChanged() {
		getMap().broadcastMessage(this, PacketCreator.updateCharLook(this), false);
		recalcLocalStats();
		enforceMaxHpMp();
		// saveToDB(true);
		if (getMessenger() != null) {
			Server.getInstance().getWorld(worldId).updateMessenger(getMessenger(), getName(), getWorldId(), client.getChannel());
		}
	}

	public void cancelExpirationTask() {
		if (expirationSchedule != null) {
			expirationSchedule.cancel(false);
			expirationSchedule = null;
		}
	}

	public void expirationTask() {
		if (expirationSchedule == null) {
			expirationSchedule = TimerManager.getInstance().register(new Runnable() {

				@Override
				public void run() {
					long expiration, currenttime = System.currentTimeMillis();
					Set<ISkill> keys = getSkills().keySet();
					for (Iterator<ISkill> i = keys.iterator(); i.hasNext();) {
						ISkill key = i.next();
						SkillEntry skill = getSkills().get(key);
						if (skill.expiration != -1 && skill.expiration < currenttime) {
							changeSkillLevel(key, (byte) -1, 0, -1);
						}
					}

					List<IItem> toberemove = new ArrayList<IItem>();
					for (Inventory inv : inventory) {
						for (IItem item : inv.list()) {
							expiration = item.getExpiration();
							if (expiration != -1 && (expiration < currenttime) && ((item.getFlag() & ItemConstants.LOCK) == ItemConstants.LOCK)) {
								byte aids = item.getFlag();
								aids &= ~(ItemConstants.LOCK);
								// Probably need a check, else people can make expiring items into permanent items...
								item.setFlag(aids); 
								item.setExpiration(-1);
								forceUpdateItem(inv.getType(), item); // TEST :3
							} else if (expiration != -1 && expiration < currenttime) {
								client.announce(PacketCreator.itemExpired(item.getItemId()));
								toberemove.add(item);
							}
						}
						for (IItem item : toberemove) {
							InventoryManipulator.removeFromSlot(client, inv.getType(), item.getSlot(), item.getQuantity(), true);
						}
						toberemove.clear();
					}
					// announce(PacketCreator.enableActions());
					// saveToDB(true);
				}
			}, 60000);
		}
	}

	public void forceUpdateItem(InventoryType type, IItem item) {
		client.announce(PacketCreator.clearInventoryItem(type, item.getSlot(), false));
		client.announce(PacketCreator.addInventorySlot(type, item, false));
	}

	public void gainGachaExp() {
		int expgain = 0;
		int currentgexp = gachaexp.get();
		if ((currentgexp + exp.get()) >= ExpTable.getExpNeededForLevel(level)) {
			expgain += ExpTable.getExpNeededForLevel(level) - exp.get();
			int nextneed = ExpTable.getExpNeededForLevel(level + 1);
			if ((currentgexp - expgain) >= nextneed) {
				expgain += nextneed;
			}
			this.gachaexp.set(currentgexp - expgain);
		} else {
			expgain = this.gachaexp.getAndSet(0);
		}
		gainExp(expgain, false, false);
		updateSingleStat(Stat.GACHAEXP, this.gachaexp.get());
	}

	public void gainGachaExp(int gain) {
		updateSingleStat(Stat.GACHAEXP, gachaexp.addAndGet(gain));
	}

	public void gainExp(int gain, boolean show, boolean inChat) {
		gainExp(gain, show, inChat, true);
	}

	public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
		int equip = (gain / 10) * pendantExp;
		int total = gain + equip;

		if (level < getMaxLevel()) {
			if ((long) this.exp.get() + (long) total > (long) Integer.MAX_VALUE) {
				int gainFirst = ExpTable.getExpNeededForLevel(level) - this.exp.get();
				total -= gainFirst + 1;
				this.gainExp(gainFirst + 1, false, inChat, white);
			}
			updateSingleStat(Stat.EXP, this.exp.addAndGet(total));
			if (show && gain != 0) {
				client.announce(PacketCreator.getShowExpGain(gain, equip, inChat, white));
			}
			while (level < getMaxLevel() && exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                levelUp(true);
            }
			if (exp.get() >= ExpTable.getExpNeededForLevel(level)) {
				levelUp(true);
				int need = ExpTable.getExpNeededForLevel(level);
				if (exp.get() >= need) {
					setExp(need - 1);
					updateSingleStat(Stat.EXP, need);
				}
			}
		}
	}

	public void gainFame(int delta) {
		this.addFame(delta);
		this.updateSingleStat(Stat.FAME, this.fame);
	}

	public void gainMeso(int gain, boolean show) {
		gainMeso(gain, show, false, false);
	}

	public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
		if (meso.get() + gain < 0) {
			client.announce(PacketCreator.enableActions());
			return;
		}
		updateSingleStat(Stat.MESO, meso.addAndGet(gain), enableActions);
		if (show) {
			client.announce(PacketCreator.getShowMesoGain(gain, inChat));
		}
	}

	public void genericGuildMessage(int code) {
		this.client.announce(PacketCreator.genericGuildMessage((byte) code));
	}

	public int getAccountId() {
		return accountId;
	}

	public List<PlayerBuffValueHolder> getAllBuffs() {
		List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
		for (BuffStatValueHolder mbsvh : effects.values()) {
			ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
		}
		return ret;
	}

	public List<PlayerCoolDownValueHolder> getAllCooldowns() {
		List<PlayerCoolDownValueHolder> ret = new ArrayList<PlayerCoolDownValueHolder>();
		for (CooldownValueHolder mcdvh : coolDowns.values()) {
			ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
		}
		return ret;
	}

	public int getAllianceRank() {
		return this.allianceRank;
	}

	public int getAllowWarpToId() {
		return allowWarpToId;
	}

	public static String getAriantRoomLeaderName(int room) {
		return ariantRoomLeader[room];
	}

	public static int getAriantSlotsRoom(int room) {
		return ariantRoomSlot[room];
	}

	public int getBattleshipHp() {
		return battleshipHp;
	}

	public BuddyList getBuddylist() {
		return buddylist;
	}

	public static Map<String, String> getCharacterFromDatabase(String name) {
		Map<String, String> character = new LinkedHashMap<String, String>();

		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `id`, `accountid`, `name` FROM `characters` WHERE `name` = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();

			if (!rs.next()) {
				rs.close();
				ps.close();
				return null;
			}

			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				character.put(rs.getMetaData().getColumnLabel(i), rs.getString(i));
			}

			rs.close();
			ps.close();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

		return character;
	}

	public static boolean isInUse(String name) {
		return getCharacterFromDatabase(name) != null;
	}

	public Long getBuffedStarttime(BuffStat effect) {
		BuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return Long.valueOf(mbsvh.startTime);
	}

	public Integer getBuffedValue(BuffStat effect) {
		BuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return Integer.valueOf(mbsvh.value);
	}

	public int getBuffSource(BuffStat stat) {
		BuffStatValueHolder mbsvh = effects.get(stat);
		if (mbsvh == null) {
			return -1;
		}
		return mbsvh.effect.getSourceId();
	}

	private List<BuffStat> getBuffStats(StatEffect effect, long startTime) {
		List<BuffStat> stats = new ArrayList<BuffStat>();
		for (Entry<BuffStat, BuffStatValueHolder> stateffect : effects.entrySet()) {
			if (stateffect.getValue().effect.sameSource(effect) && (startTime == -1 || startTime == stateffect.getValue().startTime)) {
				stats.add(stateffect.getKey());
			}
		}
		return stats;
	}

	public int getChair() {
		return chair;
	}

	public String getChalkboard() {
		return this.chalktext;
	}

	public GameClient getClient() {
		return client;
	}

	public final List<QuestStatus> getCompletedQuests() {
		List<QuestStatus> ret = new LinkedList<QuestStatus>();
		for (QuestStatus q : quests.values()) {
			if (q.getStatus().equals(QuestStatus.Status.COMPLETED)) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public Collection<Monster> getControlledMonsters() {
		return Collections.unmodifiableCollection(controlled);
	}

	public List<Ring> getCrushRings() {
		Collections.sort(crushRings);
		return crushRings;
	}

	public int getCurrentMaxHp() {
		return localMaxHp;
	}

	public int getCurrentMaxMp() {
		return localMaxMp;
	}

	public int getDex() {
		return dex;
	}
	
	public int getRebirths() {
		return rebirths;
	}

	public List<Door> getDoors() {
		return new ArrayList<Door>(doors);
	}

	public int getDropRate() {
		return dropRate;
	}

	public int getEnergyBar() {
		return energybar;
	}

	public EventInstanceManager getEventInstance() {
		return eventInstance;
	}

	public ArrayList<Integer> getExcluded() {
		return excluded;
	}

	public int getExp() {
		return exp.get();
	}

	public int getGachaExp() {
		return gachaexp.get();
	}

	public int getExpRate() {
		return expRate;
	}

	public int getFace() {
		return face;
	}

	public int getFame() {
		return fame;
	}

	public Family getFamily() {
		return family;
	}

	public void setFamily(Family f) {
		this.family = f;
	}

	public int getFamilyId() {
		return familyId;
	}

	public List<Ring> getFriendshipRings() {
		Collections.sort(friendshipRings);
		return friendshipRings;
	}

	public int getGender() {
		return gender;
	}

	public boolean isMale() {
		return getGender() == 0;
	}

	public Guild getGuild() {
		try {
			return Server.getInstance().getGuild(getGuildId(), null);
		} catch (Exception ex) {
			return null;
		}
	}

	public int getGuildId() {
		return guildId;
	}
	
	public static int getGuildIdById(int cid) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `guildid` FROM `characters` WHERE `id` = ?");
			ps.setInt(1, cid);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return 0;
			}
			int guildid = rs.getInt("guildid");
			rs.close();
			ps.close();
			return guildid;
		} catch (Exception e) {
			return 0;
		}
	}

	public int getGuildRank() {
		return guildRank;
	}

	public int getHair() {
		return hair;
	}

	public HiredMerchant getHiredMerchant() {
		return hiredMerchant;
	}

	public int getHp() {
		return hp;
	}

	public int getHpMpApUsed() {
		return hpMpApUsed;
	}

	public int getId() {
		return id;
	}

	public static int getIdByName(String name) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `id` FROM `characters` WHERE `name` = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return -1;
			}
			int id = rs.getInt("id");
			rs.close();
			ps.close();
			return id;
		} catch (Exception e) {
		}
		return -1;
	}

	public static String getNameById(int id) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM characters WHERE id = ?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return null;
			}
			String name = rs.getString("name");
			rs.close();
			ps.close();
			return name;
		} catch (Exception e) {
		}
		return null;
	}

	public int getInitialSpawnpoint() {
		return initialSpawnPoint;
	}

	public int getInt() {
		return int_;
	}

	public Inventory getInventory(InventoryType type) {
		return inventory[type.ordinal()];
	}

	public int getItemEffect() {
		return itemEffect;
	}

	public int getItemQuantity(int itemid, boolean checkEquipped) {
		int possesed = inventory[ItemInfoProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
		if (checkEquipped) {
			possesed += inventory[InventoryType.EQUIPPED.ordinal()].countById(itemid);
		}
		return possesed;
	}

	public Job getJob() {
		return job;
	}

	public int getJobRank() {
		return jobRank;
	}

	public int getJobRankMove() {
		return jobRankMove;
	}

	public int getJobType() {
		return job.getId() / 1000;
	}

	public Map<Integer, KeyBinding> getKeymap() {
		return keymap;
	}

	public long getLastHealed() {
		return lastHealed;
	}

	public long getLastUsedCashItem() {
		return lastUsedCashItem;
	}

	public int getLevel() {
		return level;
	}

	public int getLuk() {
		return luk;
	}

	public int getFh() {
		if (getMap().getFootholds().findBelow(this.getPosition()) == null) {
			return 0;
		} else {
			return getMap().getFootholds().findBelow(this.getPosition()).getId();
		}
	}

	public GameMap getMap() {
		return map;
	}

	public int getMapId() {
		if (map != null) {
			return map.getId();
		}
		return mapId;
	}

	public int getMarkedMonster() {
		return markedMonster;
	}

	public Ring getMarriageRing() {
		return marriageRing;
	}

	public int getMarried() {
		return married;
	}

	public int getMasterLevel(ISkill skill) {
		if (skills.get(skill) == null) {
			return 0;
		}
		return skills.get(skill).masterlevel;
	}

	public int getMaxHp() {
		return maxhp;
	}

	public int getMaxLevel() {
		return isCygnus() ? 120 : 200;
	}

	public int getMaxMp() {
		return maxmp;
	}

	public int getMeso() {
		return meso.get();
	}

	public int getMerchantMeso() {
		return merchantmeso;
	}

	public int getMesoRate() {
		return mesoRate;
	}

	public int getMesosTraded() {
		return mesosTraded;
	}

	public int getMessengerPosition() {
		return messengerposition;
	}

	public GuildCharacter getMGC() {
		return mgc;
	}

	public PartyCharacter getMPC() {
		// if (mpc == null) mpc = new PartyCharacter(this);
		return mpc;
	}

	public void setMPC(PartyCharacter mpc) {
		this.mpc = mpc;
	}

	public Minigame getActiveMinigame() {
		return activeMinigame;
	}

	public MinigameStats getOmokStats() {
		return this.omokStats;
	}
	
	public MinigameStats getMatchingCardStats() {
		return this.matchingCardStats;
	}

	public MonsterBook getMonsterBook() {
		return monsterbook;
	}

	public int getMonsterBookCover() {
		return bookCover;
	}

	public Mount getMount() {
		return mount;
	}

	public int getMp() {
		return mp;
	}

	public Messenger getMessenger() {
		return messenger;
	}

	public String getName() {
		return name;
	}

	public int getNextEmptyPetIndex() {
		for (int i = 0; i < 3; i++) {
			if (pets[i] == null) {
				return i;
			}
		}
		return 3;
	}

	public int getNoPets() {
		int ret = 0;
		for (int i = 0; i < 3; i++) {
			if (pets[i] != null) {
				ret++;
			}
		}
		return ret;
	}

	public int getNumControlledMonsters() {
		return controlled.size();
	}

	public Party getParty() {
		return party;
	}

	public int getPartyId() {
		return (party != null ? party.getId() : -1);
	}

	public PlayerShop getPlayerShop() {
		return playerShop;
	}

	public Pet[] getPets() {
		return pets;
	}

	public Pet getPet(int index) {
		return pets[index];
	}

	public byte getPetIndex(int petId) {
		for (byte i = 0; i < 3; i++) {
			if (pets[i] != null) {
				if (pets[i].getUniqueId() == petId) {
					return i;
				}
			}
		}
		return -1;
	}

	public byte getPetIndex(Pet pet) {
		for (byte i = 0; i < 3; i++) {
			if (pets[i] != null) {
				if (pets[i].getUniqueId() == pet.getUniqueId()) {
					return i;
				}
			}
		}
		return -1;
	}

	public int getPossibleReports() {
		return possibleReports;
	}

	public final byte getQuestStatus(final int quest) {
		for (final QuestStatus q : quests.values()) {
			if (q.getQuest().getId() == quest) {
				return (byte) q.getStatus().getId();
			}
		}
		return 0;
	}

	public QuestStatus getQuest(Quest quest) {
		if (!quests.containsKey(quest)) {
			return new QuestStatus(quest, QuestStatus.Status.NOT_STARTED);
		}
		return quests.get(quest);
	}

	public boolean needQuestItem(int questid, int itemid) {
		if (questid <= 0) {
			return true; // For non quest items :3
		}
		Quest quest = Quest.getInstance(questid);
		return getInventory(ItemConstants.getInventoryType(itemid)).countById(itemid) < quest.getItemAmountNeeded(itemid);
	}

	public int getRank() {
		return rank;
	}

	public int getRankMove() {
		return rankMove;
	}

	public int getRemainingAp() {
		return remainingAp;
	}

	public int getRemainingSp() {
		return remainingSp;
	}

	public int getSavedLocation(String type) {
		SavedLocation sl = savedLocations[SavedLocationType.fromString(type).ordinal()];
		if (sl == null) {
			return 102000000;
		}
		if (!SavedLocationType.fromString(type).equals(SavedLocationType.WORLDTOUR)) {
			clearSavedLocation(SavedLocationType.fromString(type));
		}
		return sl.mapId;
	}

	public Shop getShop() {
		return shop;
	}

	public Map<ISkill, SkillEntry> getSkills() {
		return Collections.unmodifiableMap(skills);
	}

	public int getSkillLevel(int skill) {
		SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
		if (ret == null) {
			return 0;
		}
		return ret.skillevel;
	}

	public byte getSkillLevel(ISkill skill) {
		if (skills.get(skill) == null) {
			return 0;
		}
		return skills.get(skill).skillevel;
	}

	public long getSkillExpiration(int skill) {
		SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
		if (ret == null) {
			return -1;
		}
		return ret.expiration;
	}

	public long getSkillExpiration(ISkill skill) {
		if (skills.get(skill) == null) {
			return -1;
		}
		return skills.get(skill).expiration;
	}

	public SkinColor getSkinColor() {
		return skinColor;
	}

	public int getSlot() {
		return slots;
	}

	public String getStaffRank() {
		return MapleRank.getById(gmLevel).toString();
	}

	public final List<QuestStatus> getStartedQuests() {
		List<QuestStatus> ret = new LinkedList<QuestStatus>();
		for (QuestStatus q : quests.values()) {
			if (q.getStatus().equals(QuestStatus.Status.STARTED)) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public final int getStartedQuestsSize() {
		int i = 0;
		for (QuestStatus q : quests.values()) {
			if (q.getStatus().equals(QuestStatus.Status.STARTED)) {
				if (q.getQuest().getInfoNumber() > 0) {
					i++;
				}
				i++;
			}
		}
		return i;
	}

	public StatEffect getStatForBuff(BuffStat effect) {
		BuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return null;
		}
		return mbsvh.effect;
	}
	
	public MapleStockPortfolio getStockPortfolio() {
		return stockPortfolio;
	}
	
	public boolean buyStock(MapleStock ms, int amount) {
		if (MapleStocks.getInstance().getTotalSold(ms.getTicker()) + amount <= ms.getCount()) {
			int price = ms.getValue() * amount;
			if (meso.get() - price < 0) {
				return false;
			}
			this.gainMeso(-price, false);
			if (stockPortfolio.hasStock(ms)) {
				return stockPortfolio.update(new Pair<String, Integer>(ms.getTicker(), amount));
			} else {
				return stockPortfolio.add(new Pair<String, Integer>(ms.getTicker(), amount));
			}
		}
		return false;
	}
	
	public boolean sellStock(MapleStock ms, int amount) {
		if (stockPortfolio.hasStock(ms, amount)) {
			int price = ms.getValue() * amount;
			if (meso.get() + price > Integer.MAX_VALUE || meso.get() + price < 0) {
				return false;
			}
			this.gainMeso(price, false);
			return stockPortfolio.remove(ms, amount);
		}
		return false;
	}
	
	public boolean hasStock(String ticker) {
		return this.hasStock(ticker, 1);
	}
	
	public boolean hasStock(String ticker, int amount) {
		return stockPortfolio.hasStock(MapleStocks.getInstance().getStock(ticker), amount);
	}
	
	public Storage getStorage() {
		return storage;
	}

	public int getStr() {
		return str;
	}

	public Map<Integer, Summon> getSummons() {
		return summons;
	}

	public int getTotalLuk() {
		return localLuk;
	}

	public int getTotalMagic() {
		return magic;
	}

	public int getTotalWatk() {
		return watk;
	}

	public Trade getTrade() {
		return trade;
	}

	public int getVanquisherKills() {
		return vanquisherKills;
	}

	public int getVanquisherStage() {
		return vanquisherStage;
	}

	public Collection<GameMapObject> getVisibleMapObjects() {
		return Collections.unmodifiableCollection(visibleMapObjects);
	}

	public byte getWorldId() {
		return worldId;
	}

	public void giveCoolDowns(final int skillid, long starttime, long length) {
		if (skillid == 5221999) {
			this.battleshipHp = (int) length;
			addCooldown(skillid, 0, length, null);
		} else {
			int time = (int) ((length + starttime) - System.currentTimeMillis());
			addCooldown(skillid, System.currentTimeMillis(), time, TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time));
		}
	}

	public int gmLevel() {
		return gmLevel;
	}

	private void guildUpdate() {
		if (this.guildId < 1) {
			return;
		}
		mgc.setLevel(level);
		mgc.setJobId(job.getId());
		try {
			Server.getInstance().memberLevelJobUpdate(this.mgc);
			int allianceId = getGuild().getAllianceId();
			if (allianceId > 0) {
				Server.getInstance().allianceMessage(allianceId, PacketCreator.updateAllianceJobLevel(this), getId(), -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleEnergyChargeGain() { // to get here energychargelevel has
											// to be > 0
		ISkill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
		StatEffect ceffect = null;
		ceffect = energycharge.getEffect(getSkillLevel(energycharge));
		TimerManager tMan = TimerManager.getInstance();
		if (energybar < 10000) {
			energybar += 102;
			if (energybar > 10000) {
				energybar = 10000;
			}
			List<BuffStatDelta> stat = Collections.singletonList(new BuffStatDelta(BuffStat.ENERGY_CHARGE, energybar));
			setBuffedValue(BuffStat.ENERGY_CHARGE, energybar);
			client.announce(PacketCreator.giveBuff(energybar, 0, stat));
			client.announce(PacketCreator.showOwnBuffEffect(energycharge.getId(), 2));
			getMap().broadcastMessage(this, PacketCreator.showBuffeffect(id, energycharge.getId(), 2));
			getMap().broadcastMessage(this, PacketCreator.giveForeignBuff(energybar, stat));
		}
		if (energybar >= 10000 && energybar < 11000) {
			energybar = 15000;
			final GameCharacter chr = this;
			tMan.schedule(new Runnable() {

				@Override
				public void run() {
					energybar = 0;
					List<BuffStatDelta> stat = Collections.singletonList(new BuffStatDelta(BuffStat.ENERGY_CHARGE, energybar));
					setBuffedValue(BuffStat.ENERGY_CHARGE, energybar);
					client.announce(PacketCreator.giveBuff(energybar, 0, stat));
					getMap().broadcastMessage(chr, PacketCreator.giveForeignBuff(energybar, stat));
				}
			}, ceffect.getDuration());
		}
	}

	public void handleOrbconsume() {
		int skillid = isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
		ISkill combo = SkillFactory.getSkill(skillid);
		List<BuffStatDelta> stat = Collections.singletonList(new BuffStatDelta(BuffStat.COMBO, 1));
		setBuffedValue(BuffStat.COMBO, 1);
		client.announce(PacketCreator.giveBuff(skillid, combo.getEffect(getSkillLevel(combo)).getDuration() + (int) ((getBuffedStarttime(BuffStat.COMBO) - System.currentTimeMillis())), stat));
		getMap().broadcastMessage(this, PacketCreator.giveForeignBuff(getId(), stat), false);
	}

	public boolean hasEntered(String script) {
		for (int mapId : entered.keySet()) {
			if (entered.get(mapId).equals(script)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasEntered(String script, int mapId) {
		if (entered.containsKey(mapId)) {
			if (entered.get(mapId).equals(script)) {
				return true;
			}
		}
		return false;
	}
	
	public FameStats getFameStats() {
		return this.fameStats;
	}

	public void addFameEntry(int targetId) {
		long timestamp = System.currentTimeMillis();
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `famelog` (`characterid`, `characterid_to`) VALUES (?, ?)");
			ps.setInt(1, getId());
			ps.setInt(2, targetId);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			return;
		}
		
		this.fameStats.addEntry(targetId, timestamp);
	}

	public boolean hasMerchant() {
		return hasMerchant;
	}

	public boolean haveItem(int itemid) {
		return getItemQuantity(itemid, false) > 0;
	}

	public void increaseGuildCapacity() { // hopefully nothing is null
		if (getMeso() < getGuild().getIncreaseGuildCost(getGuild().getCapacity())) {
			dropMessage(1, "You don't have enough mesos.");
			return;
		}
		Server.getInstance().increaseGuildCapacity(guildId);
		gainMeso(-getGuild().getIncreaseGuildCost(getGuild().getCapacity()), true, false, false);
	}

	public boolean isActiveBuffedValue(int skillid) {
		LinkedList<BuffStatValueHolder> allBuffs = new LinkedList<BuffStatValueHolder>(effects.values());
		for (BuffStatValueHolder mbsvh : allBuffs) {
			if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
				return true;
			}
		}
		return false;
	}

	public boolean isAlive() {
		return hp > 0;
	}
	
	public boolean isHardcoreDead() {
		return dead;
	}

	public boolean isBuffFrom(BuffStat stat, ISkill skill) {
		BuffStatValueHolder mbsvh = effects.get(stat);
		if (mbsvh == null) {
			return false;
		}
		return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
	}

	public boolean isCygnus() {
		return getJobType() == 1;
	}

	public boolean isAran() {
		return getJob().getId() >= 2000 && getJob().getId() <= 2112;
	}

	public boolean isBeginnerJob() {
		return (getJob().getId() == 0 || getJob().getId() == 1000 || getJob().getId() == 2000) && getLevel() < 11;
	}

	public boolean isGM() {
		return gmLevel > 1;
	}

	public boolean isHardcoreMode() {
		return hardcore && ServerConstants.ENABLE_HARDCORE_MODE;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public boolean isMapObjectVisible(GameMapObject mo) {
		return visibleMapObjects.contains(mo);
	}

	public boolean isPartyLeader() {
		return party.getLeader() == party.getMemberById(getId());
	}

	public void leaveMap() {
		controlled.clear();
		visibleMapObjects.clear();
		if (chair != 0) {
			chair = 0;
		}
		if (hpDecreaseTask != null) {
			hpDecreaseTask.cancel(false);
		}
	}

	public void levelUp(boolean takeexp) {
		ISkill improvingMaxHP = null;
		ISkill improvingMaxMP = null;
		int improvingMaxHPLevel = 0;
		int improvingMaxMPLevel = 0;

		if (isBeginnerJob() && getRebirths() == 0) {
			remainingAp = 0;
			if (getLevel() < 6) {
				str += 5;
			} else {
				str += 4;
				dex += 1;
			}
		} else {
			remainingAp += 5;
			if (isCygnus() && level < 70) {
				remainingAp++;
			}
		}
		if (job == Job.BEGINNER || job == Job.NOBLESSE || job == Job.LEGEND) {
			maxhp += Randomizer.rand(12, 16);
			maxmp += Randomizer.rand(10, 12);
		} else if (job.isA(Job.WARRIOR) || job.isA(Job.DAWNWARRIOR1)) {
			improvingMaxHP = isCygnus() ? SkillFactory.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory.getSkill(Swordsman.IMPROVED_MAX_HP_INCREASE);
			if (job.isA(Job.CRUSADER)) {
				improvingMaxMP = SkillFactory.getSkill(1210000);
			} else if (job.isA(Job.DAWNWARRIOR2)) {
				improvingMaxMP = SkillFactory.getSkill(11110000);
			}
			improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
			maxhp += Randomizer.rand(24, 28);
			maxmp += Randomizer.rand(4, 6);
		} else if (job.isA(Job.MAGICIAN) || job.isA(Job.BLAZEWIZARD1)) {
			improvingMaxMP = isCygnus() ? SkillFactory.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
			improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
			maxhp += Randomizer.rand(10, 14);
			maxmp += Randomizer.rand(22, 24);
		} else if (job.isA(Job.BOWMAN) || job.isA(Job.THIEF) || (job.getId() > 1299 && job.getId() < 1500)) {
			maxhp += Randomizer.rand(20, 24);
			maxmp += Randomizer.rand(14, 16);
		} else if (job.isA(Job.GM)) {
			maxhp = 30000;
			maxmp = 30000;
		} else if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
			improvingMaxHP = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory.getSkill(5100000);
			improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
			maxhp += Randomizer.rand(22, 28);
			maxmp += Randomizer.rand(18, 23);
		} else if (job.isA(Job.ARAN1)) {
			maxhp += Randomizer.rand(44, 48);
			int aids = Randomizer.rand(4, 8);
			maxmp += aids + Math.floor(aids * 0.1);
		}
		if (improvingMaxHPLevel > 0 && (job.isA(Job.WARRIOR) || job.isA(Job.PIRATE) || job.isA(Job.DAWNWARRIOR1))) {
			maxhp += improvingMaxHP.getEffect(improvingMaxHPLevel).getX();
		}
		if (improvingMaxMPLevel > 0 && (job.isA(Job.MAGICIAN) || job.isA(Job.CRUSADER) || job.isA(Job.BLAZEWIZARD1))) {
			maxmp += improvingMaxMP.getEffect(improvingMaxMPLevel).getX();
		}
		maxmp += localInt / 10;
		if (takeexp) {
			exp.addAndGet(-ExpTable.getExpNeededForLevel(level));
			if (exp.get() < 0) {
				exp.set(0);
			}
		}
		level++;
		if (level >= getMaxLevel()) {
			exp.set(0);
		}
		maxhp = Math.min(30000, maxhp);
		maxmp = Math.min(30000, maxmp);
		if (level == 200) {
			exp.set(0);
		}
		hp = maxhp;
		mp = maxmp;
		recalcLocalStats();
		List<StatDelta> statup = new ArrayList<StatDelta>(10);
		statup.add(new StatDelta(Stat.AVAILABLEAP, remainingAp));
		statup.add(new StatDelta(Stat.HP, localMaxHp));
		statup.add(new StatDelta(Stat.MP, localMaxMp));
		statup.add(new StatDelta(Stat.EXP, exp.get()));
		statup.add(new StatDelta(Stat.LEVEL, level));
		statup.add(new StatDelta(Stat.MAXHP, maxhp));
		statup.add(new StatDelta(Stat.MAXMP, maxmp));
		statup.add(new StatDelta(Stat.STR, str));
		statup.add(new StatDelta(Stat.DEX, dex));
		if (job.getId() % 1000 > 0) {
			remainingSp += 3;
			statup.add(new StatDelta(Stat.AVAILABLESP, remainingSp));
		}
		client.announce(PacketCreator.updatePlayerStats(statup));
		getMap().broadcastMessage(this, PacketCreator.showForeignEffect(getId(), 0), false);
		recalcLocalStats();
		setMPC(new PartyCharacter(this));
		silentPartyUpdate();
		if (this.guildId > 0) {
			getGuild().broadcast(PacketCreator.levelUpMessage(2, level, name), this.getId());
		}
		if (ServerConstants.PERFECT_PITCH) {
			// milestones?
			if (InventoryManipulator.checkSpace(client, 4310000, (short) 1, "")) {
				InventoryManipulator.addById(client, 4310000, (short) 1);
			}
		}
		guildUpdate();
		// saveToDB(true); NAH!
	}

	public static GameCharacter loadCharFromDB(int characterId, GameClient client, boolean forChannelServer) throws SQLException {
		Connection connection = DatabaseConnection.getConnection();
		try {
			GameCharacter character = new GameCharacter();
			character.client = client;
			character.id = characterId;
			PreparedStatement ps = connection.prepareStatement("SELECT * FROM `characters` WHERE `id` = ?");
			ps.setInt(1, characterId);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				throw new RuntimeException("Loading char failed (not found)");
			}
			character.name = rs.getString("name");
			character.level = rs.getInt("level");
			character.fame = rs.getInt("fame");
			character.str = rs.getInt("str");
			character.dex = rs.getInt("dex");
			character.int_ = rs.getInt("int");
			character.luk = rs.getInt("luk");
			character.exp.set(rs.getInt("exp"));
			character.gachaexp.set(rs.getInt("gachaexp"));
			character.hp = rs.getInt("hp");
			character.maxhp = rs.getInt("maxhp");
			character.mp = rs.getInt("mp");
			character.maxmp = rs.getInt("maxmp");
			character.hpMpApUsed = rs.getInt("hpMpUsed");
			character.hasMerchant = rs.getInt("HasMerchant") == 1;
			character.remainingSp = rs.getInt("sp");
			character.remainingAp = rs.getInt("ap");
			character.meso.set(rs.getInt("meso"));
			character.merchantmeso = rs.getInt("MerchantMesos");
			character.rebirths = rs.getInt("rebirths");
			character.gmLevel = rs.getInt("gm");
			character.skinColor = SkinColor.getById(rs.getInt("skincolor"));
			character.gender = rs.getInt("gender");
			character.job = Job.getById(rs.getInt("job"));
			character.vanquisherKills = rs.getInt("vanquisherKills");
			character.omokStats = new MinigameStats(rs.getInt("omokwins"), rs.getInt("omoklosses"), rs.getInt("omokties"), 2000);
			character.matchingCardStats = new MinigameStats(rs.getInt("matchcardwins"), rs.getInt("matchcardlosses"), rs.getInt("matchcardties"), 2000);
			character.hair = rs.getInt("hair");
			character.face = rs.getInt("face");
			character.accountId = rs.getInt("accountid");
			character.mapId = rs.getInt("map");
			character.initialSpawnPoint = rs.getInt("spawnpoint");
			character.worldId = rs.getByte("world");
			character.rank = rs.getInt("rank");
			character.rankMove = rs.getInt("rankMove");
			character.jobRank = rs.getInt("jobRank");
			character.jobRankMove = rs.getInt("jobRankMove");
			int mountexp = rs.getInt("mountexp");
			int mountlevel = rs.getInt("mountlevel");
			int mounttiredness = rs.getInt("mounttiredness");
			character.guildId = rs.getInt("guildid");
			character.guildRank = rs.getInt("guildrank");
			character.allianceRank = rs.getInt("allianceRank");
			character.familyId = rs.getInt("familyId");
			character.bookCover = rs.getInt("monsterbookcover");
			character.monsterbook = new MonsterBook();
			character.monsterbook.loadCards(characterId);
			character.vanquisherStage = rs.getInt("vanquisherStage");
			character.dojoState = new DojoState(rs);
			if (ServerConstants.USE_MAPLE_STOCKS) {
				character.stockPortfolio = MapleStockPortfolio.load(characterId);
			}
			if (ServerConstants.ENABLE_HARDCORE_MODE) {
				character.hardcore = rs.getInt("hardcore") == 1;
				character.dead = rs.getInt("dead") == 1;
			}
			if (character.guildId > 0) {
				character.mgc = new GuildCharacter(character);
			}
			int buddyCapacity = rs.getInt("buddyCapacity");
			character.buddylist = new BuddyList(buddyCapacity);
			character.getInventory(InventoryType.EQUIP).setSlotLimit(rs.getByte("equipslots"));
			character.getInventory(InventoryType.USE).setSlotLimit(rs.getByte("useslots"));
			character.getInventory(InventoryType.SETUP).setSlotLimit(rs.getByte("setupslots"));
			character.getInventory(InventoryType.ETC).setSlotLimit(rs.getByte("etcslots"));
			
			final boolean loadOnlyEquips = !forChannelServer;
			final List<ItemInventoryEntry> loadedItems = ItemFactory.INVENTORY.loadItems(character.id, loadOnlyEquips);
			
			for (ItemInventoryEntry entry : loadedItems) {
				character.getInventory(entry.type).addFromDB(entry.item);
				if (entry.type.equals(InventoryType.EQUIP) || entry.type.equals(InventoryType.EQUIPPED)) {
					IEquip equip = (IEquip) entry.item;
					if (equip.getRingId() > -1) {
						Ring ring = Ring.loadFromDb(equip.getRingId());
						if (entry.type.equals(InventoryType.EQUIPPED)) {
							ring.equip();
						}
						
						if (ring.getItemId() > 1112012) {
							character.addFriendshipRing(ring);
						} else {
							character.addCrushRing(ring);
						}
					}
				}
				
				IItem item = entry.item;
				if (item.getPetId() > -1) {
					Pet pet = Pet.loadFromDb(item);
					if (pet != null && pet.isSummoned()) {
						character.addPet(pet);
					}
					continue;
				}
				
				if (entry.type.equals(InventoryType.EQUIP) || entry.type.equals(InventoryType.EQUIPPED)) {
					IEquip equip = (IEquip) entry.item;
					if (equip.getRingId() > -1) {
						Ring ring = Ring.loadFromDb(equip.getRingId());
						if (entry.type.equals(InventoryType.EQUIPPED)) {
							ring.equip();
						}
						
						if (ring.getItemId() > 1112012) {
							character.addFriendshipRing(ring);
						} else {
							character.addCrushRing(ring);
						}
					}
				}
			}
			
			if (forChannelServer) {
				GameMapFactory mapFactory = client.getChannelServer().getMapFactory();
				character.map = mapFactory.getMap(character.mapId);
				if (character.map == null) {
					character.map = mapFactory.getMap(100000000);
				}
				
				Portal portal = character.map.getPortal(character.initialSpawnPoint);
				if (portal == null) {
					portal = character.map.getPortal(0);
					character.initialSpawnPoint = 0;
				}
				character.setPosition(portal.getPosition());
				int partyid = rs.getInt("party");
				Party party = Server.getInstance().getWorld(character.worldId).getParty(partyid);
				if (party != null) {
					character.mpc = party.getMemberById(character.id);
					if (character.mpc != null) {
						character.party = party;
					}
				}
				
				int messengerid = rs.getInt("messengerid");
				int position = rs.getInt("messengerposition");
				if (messengerid > 0 && position < 4 && position > -1) {
					Messenger messenger = Server.getInstance().getWorld(character.worldId).getMessenger(messengerid);
					if (messenger != null) {
						character.messenger = messenger;
						character.messengerposition = position;
					}
				}
			}
			rs.close();
			ps.close();
			ps = connection.prepareStatement("SELECT `mapid`,`vip` FROM `trocklocations` WHERE `characterid` = ? LIMIT 15");
			ps.setInt(1, characterId);
			rs = ps.executeQuery();
			character.teleportRocks = new TeleportRockInfo(rs);
			rs.close();
			ps.close();
			ps = connection.prepareStatement("SELECT `name` FROM `accounts` WHERE `id` = ?", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, character.accountId);
			rs = ps.executeQuery();
			if (rs.next()) {
				// TODO: External side-effect. If you do this here and the loading fails, you've got a problem.
				character.getClient().setAccountName(rs.getString("name"));
			}
			rs.close();
			ps.close();
			ps = connection.prepareStatement("SELECT `name`,`info` FROM `eventstats` WHERE `characterid` = ?");
			ps.setInt(1, character.id);
			rs = ps.executeQuery();
			while (rs.next()) {
				String name = rs.getString("name");
				if (rs.getString("name").equals("rescueGaga")) {
					character.events.put(name, new RescueGaga(rs.getInt("info")));
				}
				// ret.events = new MapleEvents(new
				// RescueGaga(rs.getInt("rescuegaga")), new
				// ArtifactHunt(rs.getInt("artifacthunt")));
			}
			rs.close();
			ps.close();
			character.cashshop = new CashShop(character.accountId, character.id, character.getJobType());
			character.autoban = new AutobanManager(character);
			character.marriageRing = null; // for now
			ps = connection.prepareStatement("SELECT `name`, `level` FROM `characters` WHERE `accountid` = ? AND `id` != ? ORDER BY `level` DESC LIMIT 1");
			ps.setInt(1, character.accountId);
			ps.setInt(2, characterId);
			rs = ps.executeQuery();
			if (rs.next()) {
				character.linkedName = rs.getString("name");
				character.linkedLevel = rs.getInt("level");
			}
			rs.close();
			ps.close();
			if (forChannelServer) {
				ps = connection.prepareStatement("SELECT * FROM `queststatus` WHERE `characterid` = ?");
				ps.setInt(1, characterId);
				rs = ps.executeQuery();
				PreparedStatement pse = connection.prepareStatement("SELECT * FROM `questprogress` WHERE `queststatusid` = ?");
				PreparedStatement psf = connection.prepareStatement("SELECT `mapid` FROM `medalmaps` WHERE `queststatusid` = ?");
				while (rs.next()) {
					Quest q = Quest.getInstance(rs.getShort("quest"));
					QuestStatus status = new QuestStatus(q, QuestStatus.Status.getById(rs.getInt("status")));
					long cTime = rs.getLong("time");
					if (cTime > -1) {
						status.setCompletionTime(cTime * 1000);
					}
					status.setForfeited(rs.getInt("forfeited"));
					character.quests.put(q, status);
					pse.setInt(1, rs.getInt("queststatusid"));
					ResultSet rsProgress = pse.executeQuery();
					while (rsProgress.next()) {
						status.setProgress(rsProgress.getInt("progressid"), rsProgress.getString("progress"));
					}
					rsProgress.close();
					psf.setInt(1, rs.getInt("queststatusid"));
					ResultSet medalmaps = psf.executeQuery();
					while (medalmaps.next()) {
						status.addMedalMap(medalmaps.getInt("mapid"));
					}
					medalmaps.close();
				}
				rs.close();
				ps.close();
				pse.close();
				psf.close();
				ps = connection.prepareStatement("SELECT `skillid`,`skilllevel`,`masterlevel`,`expiration` FROM `skills` WHERE `characterid` = ?");
				ps.setInt(1, characterId);
				rs = ps.executeQuery();
				while (rs.next()) {
					character.skills.put(SkillFactory.getSkill(rs.getInt("skillid")), new SkillEntry(rs.getByte("skilllevel"), rs.getInt("masterlevel"), rs.getLong("expiration")));
				}
				rs.close();
				ps.close();
				ps = connection.prepareStatement("SELECT `SkillID`,`StartTime`,`length` FROM `cooldowns` WHERE `charid` = ?");
				ps.setInt(1, character.getId());
				rs = ps.executeQuery();
				while (rs.next()) {
					final int skillid = rs.getInt("SkillID");
					final long length = rs.getLong("length"), startTime = rs.getLong("StartTime");
					if (skillid != 5221999 && (length + startTime < System.currentTimeMillis())) {
						continue;
					}
					character.giveCoolDowns(skillid, startTime, length);
				}
				rs.close();
				ps.close();
				ps = connection.prepareStatement("DELETE FROM `cooldowns` WHERE `charid` = ?");
				ps.setInt(1, character.getId());
				ps.executeUpdate();
				ps.close();
				ps = connection.prepareStatement("SELECT * FROM `skillmacros` WHERE `characterid` = ?");
				ps.setInt(1, characterId);
				rs = ps.executeQuery();
				while (rs.next()) {
					int position = rs.getInt("position");
					SkillMacro macro = new SkillMacro(position, rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"));
					character.skillMacros[position] = macro;
				}
				rs.close();
				ps.close();
				ps = connection.prepareStatement("SELECT `key`,`type`,`action` FROM `keymap` WHERE `characterid` = ?");
				ps.setInt(1, characterId);
				rs = ps.executeQuery();
				while (rs.next()) {
					int key = rs.getInt("key");
					int type = rs.getInt("type");
					int action = rs.getInt("action");
					character.keymap.put(Integer.valueOf(key), new KeyBinding(type, action));
				}
				rs.close();
				ps.close();
				ps = connection.prepareStatement("SELECT `locationtype`,`map`,`portal` FROM `savedlocations` WHERE `characterid` = ?");
				ps.setInt(1, characterId);
				rs = ps.executeQuery();
				while (rs.next()) {
					character.savedLocations[SavedLocationType.valueOf(rs.getString("locationtype")).ordinal()] = new SavedLocation(rs.getInt("map"), rs.getInt("portal"));
				}
				rs.close();
				ps.close();
				ps = connection.prepareStatement("SELECT `characterid_to`,`when` FROM `famelog` WHERE `characterid` = ? AND DATEDIFF(NOW(),`when`) < 30");
				ps.setInt(1, characterId);
				rs = ps.executeQuery();
				character.fameStats = new FameStats(rs);
				rs.close();
				ps.close();
				character.buddylist.loadFromDb(characterId);
				character.storage = Storage.loadOrCreateFromDB(character.accountId, character.worldId);
				character.recalcLocalStats();
				// ret.resetBattleshipHp();
				character.silentEnforceMaxHpMp();
			}
			int mountid = character.getJobType() * 10000000 + 1004;
			if (character.getInventory(InventoryType.EQUIPPED).getItem((byte) -18) != null) {
				character.mount = new Mount(character, character.getInventory(InventoryType.EQUIPPED).getItem((byte) -18).getItemId(), mountid);
			} else {
				character.mount = new Mount(character, 0, mountid);
			}
			character.mount.setExp(mountexp);
			character.mount.setLevel(mountlevel);
			character.mount.setTiredness(mounttiredness);
			character.mount.setActive(false);
			return character;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String makeMapleReadable(String in) {
		String i = in.replace('I', 'i');
		i = i.replace('l', 'L');
		i = i.replace("rn", "Rn");
		i = i.replace("vv", "Vv");
		i = i.replace("VV", "Vv");
		return i;

	}

	private static class BuffStatValueHolder {

		public StatEffect effect;
		public long startTime;
		public int value;
		public ScheduledFuture<?> schedule;

		public BuffStatValueHolder(StatEffect effect, long startTime, ScheduledFuture<?> schedule, int value) {
			super();
			this.effect = effect;
			this.startTime = startTime;
			this.schedule = schedule;
			this.value = value;
		}
	}

	public static class CooldownValueHolder {

		public int skillId;
		public long startTime, length;
		public ScheduledFuture<?> timer;

		public CooldownValueHolder(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
			super();
			this.skillId = skillId;
			this.startTime = startTime;
			this.length = length;
			this.timer = timer;
		}
	}

	public void message(String m) {
		dropMessage(5, m);
	}

	public void yellowMessage(String m) {
		announce(PacketCreator.sendYellowTip(m));
	}

	public void mobKilled(int id) {
		for (QuestStatus q : quests.values()) {
			if (q.getStatus() == QuestStatus.Status.COMPLETED || q.getQuest().canComplete(this, null)) {
				continue;
			}
			String progress = q.getProgress(id);
			if (!progress.isEmpty() && Integer.parseInt(progress) >= q.getQuest().getMobAmountNeeded(id)) {
				continue;
			}
			if (q.progress(id)) {
				client.announce(PacketCreator.updateQuest(q.getQuest().getId(), q.getQuestData()));
			}
		}
	}

	public void mount(int id, int skillid) {
		mount = new Mount(this, id, skillid);
	}

	public void playerNPC(GameCharacter v, int scriptId) {
		int npcId;
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT `id` FROM `playernpcs` WHERE `ScriptId` = ?");
			ps.setInt(1, scriptId);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps = con.prepareStatement("INSERT INTO `playernpcs` (`name`, `hair`, `face`, `skin`, `x`, `cy`, `map`, `ScriptId`, `Foothold`, `rx0`, `rx1`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, v.getName());
				ps.setInt(2, v.getHair());
				ps.setInt(3, v.getFace());
				ps.setInt(4, v.getSkinColor().getId());
				ps.setInt(5, getPosition().x);
				ps.setInt(6, getPosition().y);
				ps.setInt(7, getMapId());
				ps.setInt(8, scriptId);
				ps.setInt(9, getMap().getFootholds().findBelow(getPosition()).getId());
				ps.setInt(10, getPosition().x + 50);
				ps.setInt(11, getPosition().x - 50);
				ps.executeUpdate();
				rs = ps.getGeneratedKeys();
				rs.next();
				npcId = rs.getInt(1);
				ps.close();
				ps = con.prepareStatement("INSERT INTO `playernpcs_equip` (`NpcId`, `equipid`, `equippos`) VALUES (?, ?, ?)");
				ps.setInt(1, npcId);
				for (IItem equip : getInventory(InventoryType.EQUIPPED)) {
					int position = Math.abs(equip.getSlot());
					if ((position < 12 && position > 0) || (position > 100 && position < 112)) {
						ps.setInt(2, equip.getItemId());
						ps.setInt(3, equip.getSlot());
						ps.addBatch();
					}
				}
				ps.executeBatch();
				ps.close();
				rs.close();
				ps = con.prepareStatement("SELECT * FROM `playernpcs` WHERE `ScriptId` = ?");
				ps.setInt(1, scriptId);
				rs = ps.executeQuery();
				rs.next();
				PlayerNPCs pn = new PlayerNPCs(rs);
				for (Channel channel : Server.getInstance().getChannelsFromWorld(worldId)) {
					GameMap m = channel.getMapFactory().getMap(getMapId());
					m.broadcastMessage(PacketCreator.spawnPlayerNPC(pn));
					m.broadcastMessage(PacketCreator.getPlayerNPC(pn));
					m.addMapObject(pn);
				}
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void playerDead() {
		cancelAllBuffs(false);
		dispelDebuffs();
		if (getEventInstance() != null) {
			getEventInstance().playerKilled(this);
		}
		int[] charmID = {5130000, 4031283, 4140903};
		int possesed = 0;
		int i;
		for (i = 0; i < charmID.length; i++) {
			int quantity = getItemQuantity(charmID[i], false);
			if (possesed == 0 && quantity > 0) {
				possesed = quantity;
				break;
			}
		}
		if (possesed > 0) {
			message("You have used a safety charm, so your EXP points have not been decreased.");
			InventoryManipulator.removeById(client, ItemInfoProvider.getInstance().getInventoryType(charmID[i]), charmID[i], 1, true, false);
		} else if (mapId > 925020000 && mapId < 925030000) {
			this.dojoState.setStage(0);
		} else if (mapId > 980000100 && mapId < 980000700) {
			getMap().broadcastMessage(this, PacketCreator.CPQDied(this));
		} else if (getJob() != Job.BEGINNER) { // Hmm...
			int XPdummy = ExpTable.getExpNeededForLevel(getLevel());
			if (getMap().isTown()) {
				XPdummy /= 100;
			}
			if (XPdummy == ExpTable.getExpNeededForLevel(getLevel())) {
				if (getLuk() <= 100 && getLuk() > 8) {
					XPdummy *= (200 - getLuk()) / 2000;
				} else if (getLuk() < 8) {
					XPdummy /= 10;
				} else {
					XPdummy /= 20;
				}
			}
			if (getExp() > XPdummy) {
				gainExp(-XPdummy, false, false);
			} else {
				gainExp(-getExp(), false, false);
			}
		}
		if (getBuffedValue(BuffStat.MORPH) != null) {
			cancelEffectFromBuffStat(BuffStat.MORPH);
		}

		if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
			cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
		}

		if (getChair() == -1) {
			setChair(0);
			client.announce(PacketCreator.cancelChair(-1));
			getMap().broadcastMessage(this, PacketCreator.showChair(getId(), 0), false);
		}
		if (isHardcoreMode()) {
			dead = true;
			saveToDB(true);
			client.disconnect();
		}
		client.announce(PacketCreator.enableActions());
	}

	private void prepareDragonBlood(final StatEffect bloodEffect) {
		if (dragonBloodSchedule != null) {
			dragonBloodSchedule.cancel(false);
		}
		dragonBloodSchedule = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				addHP(-bloodEffect.getX());
				client.announce(PacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
				getMap().broadcastMessage(GameCharacter.this, PacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 5), false);
				checkBerserk();
			}
		}, 4000, 4000);
	}

	private void recalcLocalStats() {
		int oldmaxhp = localMaxHp;
		localMaxHp = getMaxHp();
		localMaxMp = getMaxMp();
		localDex = getDex();
		localInt = getInt();
		localStr = getStr();
		localLuk = getLuk();
		int speed = 100, jump = 100;
		magic = localInt;
		watk = 0;
		for (IItem item : getInventory(InventoryType.EQUIPPED)) {
			IEquip equip = (IEquip) item;
			localMaxHp += equip.getHp();
			localMaxMp += equip.getMp();
			localDex += equip.getDex();
			localInt += equip.getInt();
			localStr += equip.getStr();
			localLuk += equip.getLuk();
			magic += equip.getMatk() + equip.getInt();
			watk += equip.getWatk();
			speed += equip.getSpeed();
			jump += equip.getJump();
		}
		magic = Math.min(magic, 2000);
		Integer hbhp = getBuffedValue(BuffStat.HYPERBODYHP);
		if (hbhp != null) {
			localMaxHp += (hbhp.doubleValue() / 100) * localMaxHp;
		}
		Integer hbmp = getBuffedValue(BuffStat.HYPERBODYMP);
		if (hbmp != null) {
			localMaxMp += (hbmp.doubleValue() / 100) * localMaxMp;
		}
		localMaxHp = Math.min(30000, localMaxHp);
		localMaxMp = Math.min(30000, localMaxMp);
		Integer watkbuff = getBuffedValue(BuffStat.WATK);
		if (watkbuff != null) {
			watk += watkbuff.intValue();
		}
		if (job.isA(Job.BOWMAN)) {
			ISkill expert = null;
			if (job.isA(Job.MARKSMAN)) {
				expert = SkillFactory.getSkill(3220004);
			} else if (job.isA(Job.BOWMASTER)) {
				expert = SkillFactory.getSkill(3120005);
			}
			if (expert != null) {
				int boostLevel = getSkillLevel(expert);
				if (boostLevel > 0) {
					watk += expert.getEffect(boostLevel).getX();
				}
			}
		}
		Integer matkbuff = getBuffedValue(BuffStat.MATK);
		if (matkbuff != null) {
			magic += matkbuff.intValue();
		}
		Integer speedbuff = getBuffedValue(BuffStat.SPEED);
		if (speedbuff != null) {
			speed += speedbuff.intValue();
		}
		Integer jumpbuff = getBuffedValue(BuffStat.JUMP);
		if (jumpbuff != null) {
			jump += jumpbuff.intValue();
		}
		if (speed > 140) {
			speed = 140;
		}
		if (jump > 123) {
			jump = 123;
		}
		if (oldmaxhp != 0 && oldmaxhp != localMaxHp) {
			updatePartyMemberHP();
		}
	}

	public void receivePartyMemberHP() {
		if (party != null) {
			byte channel = client.getChannel();
			for (PartyCharacter partychar : party.getMembers()) {
				if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
					GameCharacter other = Server.getInstance().getWorld(worldId).getChannel(channel).getPlayerStorage().getCharacterByName(partychar.getName());
					if (other != null) {
						client.announce(PacketCreator.updatePartyMemberHP(other.getId(), other.getHp(), other.getCurrentMaxHp()));
					}
				}
			}
		}
	}

	public void registerEffect(StatEffect effect, long starttime, ScheduledFuture<?> schedule) {
		if (effect.isDragonBlood()) {
			prepareDragonBlood(effect);
		} else if (effect.isBerserk()) {
			checkBerserk();
		} else if (effect.isBeholder()) {
			final int beholder = DarkKnight.BEHOLDER;
			if (beholderHealingSchedule != null) {
				beholderHealingSchedule.cancel(false);
			}
			if (beholderBuffSchedule != null) {
				beholderBuffSchedule.cancel(false);
			}
			ISkill bHealing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER);
			int bHealingLvl = getSkillLevel(bHealing);
			if (bHealingLvl > 0) {
				final StatEffect healEffect = bHealing.getEffect(bHealingLvl);
				int healInterval = healEffect.getX() * 1000;
				beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {

					@Override
					public void run() {
						addHP(healEffect.getHp());
						client.announce(PacketCreator.showOwnBuffEffect(beholder, 2));
						getMap().broadcastMessage(GameCharacter.this, PacketCreator.summonSkill(getId(), beholder, 5), true);
						getMap().broadcastMessage(GameCharacter.this, PacketCreator.showOwnBuffEffect(beholder, 2), false);
					}
				}, healInterval, healInterval);
			}
			ISkill bBuff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER);
			if (getSkillLevel(bBuff) > 0) {
				final StatEffect buffEffect = bBuff.getEffect(getSkillLevel(bBuff));
				int buffInterval = buffEffect.getX() * 1000;
				beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {
					
					@Override
					public void run() {
						buffEffect.applyTo(GameCharacter.this);
						client.announce(PacketCreator.showOwnBuffEffect(beholder, 2));
						getMap().broadcastMessage(GameCharacter.this, PacketCreator.summonSkill(getId(), beholder, (int) (Math.random() * 3) + 6), true);
						getMap().broadcastMessage(GameCharacter.this, PacketCreator.showBuffeffect(getId(), beholder, 2), false);
					}
				}, buffInterval, buffInterval);
			}
		} else if (effect.isRecovery()) {
			final byte heal = (byte) effect.getX();
			recoverySchedule = TimerManager.getInstance().register(new Runnable() {

				@Override
				public void run() {
					addHP(heal);
					client.announce(PacketCreator.showOwnRecovery(heal));
					getMap().broadcastMessage(GameCharacter.this, PacketCreator.showRecovery(id, heal), false);
				}
			}, 5000, 5000);
		}
		for (BuffStatDelta statup : effect.getStatups()) {
			effects.put(statup.stat, new BuffStatValueHolder(effect, starttime, schedule, statup.delta));
		}
		recalcLocalStats();
	}

	public void removeAllCooldownsExcept(int id) {
		for (CooldownValueHolder mcvh : coolDowns.values()) {
			if (mcvh.skillId != id) {
				coolDowns.remove(mcvh.skillId);
			}
		}
	}

	public static void removeAriantRoom(int room) {
		ariantRoomLeader[room] = "";
		ariantRoomSlot[room] = 0;
	}

	public void removeCooldown(int skillId) {
		if (this.coolDowns.containsKey(skillId)) {
			this.coolDowns.remove(skillId);
		}
	}

	public void removePet(Pet pet, boolean shiftLeft) {
		int slot = -1;
		for (int i = 0; i < 3; i++) {
			if (pets[i] != null) {
				if (pets[i].getUniqueId() == pet.getUniqueId()) {
					pets[i] = null;
					slot = i;
					break;
				}
			}
		}
		if (shiftLeft) {
			if (slot > -1) {
				for (int i = slot; i < 3; i++) {
					if (i != 2) {
						pets[i] = pets[i + 1];
					} else {
						pets[i] = null;
					}
				}
			}
		}
	}

	public void removeVisibleMapObject(GameMapObject mo) {
		visibleMapObjects.remove(mo);
	}

	public void resetStats() {
		List<StatDelta> statup = new ArrayList<StatDelta>(5);
		int tap = 0, tsp = 1;
		int tstr = 4, tdex = 4, tint = 4, tluk = 4;
		int levelap = (isCygnus() ? 6 : 5);
		switch (job.getId()) {
			case 100:
			case 1100:
			case 2100:// ?
				tstr = 35;
				tap = ((getLevel() - 10) * levelap) + 14;
				tsp += ((getLevel() - 10) * 3);
				break;
			case 200:
			case 1200:
				tint = 20;
				tap = ((getLevel() - 8) * levelap) + 29;
				tsp += ((getLevel() - 8) * 3);
				break;
			case 300:
			case 1300:
			case 400:
			case 1400:
				tdex = 25;
				tap = ((getLevel() - 10) * levelap) + 24;
				tsp += ((getLevel() - 10) * 3);
				break;
			case 500:
			case 1500:
				tdex = 20;
				tap = ((getLevel() - 10) * levelap) + 29;
				tsp += ((getLevel() - 10) * 3);
				break;
		}
		this.remainingAp = tap;
		this.remainingSp = tsp;
		this.dex = tdex;
		this.int_ = tint;
		this.str = tstr;
		this.luk = tluk;
		statup.add(new StatDelta(Stat.AVAILABLEAP, tap));
		statup.add(new StatDelta(Stat.AVAILABLESP, tsp));
		statup.add(new StatDelta(Stat.STR, tstr));
		statup.add(new StatDelta(Stat.DEX, tdex));
		statup.add(new StatDelta(Stat.INT, tint));
		statup.add(new StatDelta(Stat.LUK, tluk));
		announce(PacketCreator.updatePlayerStats(statup));
	}

	public void resetBattleshipHp() {
		this.battleshipHp = 4000 * getSkillLevel(SkillFactory.getSkill(Corsair.BATTLE_SHIP)) + ((getLevel() - 120) * 2000);
	}

	public void resetEnteredScript() {
		if (entered.containsKey(map.getId())) {
			entered.remove(map.getId());
		}
	}

	public void resetEnteredScript(int mapId) {
		if (entered.containsKey(mapId)) {
			entered.remove(mapId);
		}
	}

	public void resetEnteredScript(String script) {
		for (int mapId : entered.keySet()) {
			if (entered.get(mapId).equals(script)) {
				entered.remove(mapId);
			}
		}
	}

	public void resetMGC() {
		this.mgc = null;
	}
	
	public void rebirthBeginner() {
		this.setLevel(1);
		this.setJob(Job.BEGINNER);
		this.setExp(0);
		this.updateSingleStat(Stat.LEVEL, 1);
		this.updateSingleStat(Stat.JOB, Job.BEGINNER.getId());
		this.updateSingleStat(Stat.EXP, 0);
		rebirths++;
	}
	
	public void rebirthNoblesse() {
		this.setLevel(1);
		this.setJob(Job.NOBLESSE);
		this.setExp(0);
		this.updateSingleStat(Stat.LEVEL, 1);
		this.updateSingleStat(Stat.JOB, Job.NOBLESSE.getId());
		this.updateSingleStat(Stat.EXP, 0);
		rebirths++;
	}
	
	public void rebirthAran() {
		this.setLevel(1);
		this.setJob(Job.ARAN1);
		this.setExp(0);
		this.updateSingleStat(Stat.LEVEL, 1);
		this.updateSingleStat(Stat.JOB, Job.ARAN1.getId());
		this.updateSingleStat(Stat.EXP, 0);
		rebirths++;
	}

	public void saveCooldowns() {
		if (getAllCooldowns().size() > 0) {
			try {
				Connection con = DatabaseConnection.getConnection();
				deleteWhereCharacterId(con, "DELETE FROM `cooldowns` WHERE `charid` = ?");
				PreparedStatement ps = con.prepareStatement("INSERT INTO `cooldowns` (`charid`, `SkillID`, `StartTime`, `length`) VALUES (?, ?, ?, ?)");
				ps.setInt(1, getId());
				for (PlayerCoolDownValueHolder cooling : getAllCooldowns()) {
					ps.setInt(2, cooling.skillId);
					ps.setLong(3, cooling.startTime);
					ps.setLong(4, cooling.length);
					ps.addBatch();
				}
				ps.executeBatch();
				ps.close();
			} catch (SQLException se) {
			}
		}
	}

	public void saveGuildStatus() {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `characters` SET `guildid` = ?, `guildrank` = ?, `allianceRank` = ? WHERE `id` = ?");
			ps.setInt(1, guildId);
			ps.setInt(2, guildRank);
			ps.setInt(3, allianceRank);
			ps.setInt(4, id);
			ps.execute();
			ps.close();
		} catch (SQLException se) {
		}
	}

	public void saveLocation(String type) {
		Portal closest = map.findClosestPortal(getPosition());
		savedLocations[SavedLocationType.fromString(type).ordinal()] = new SavedLocation(getMapId(), closest != null ? closest.getId() : 0);
	}

	public void saveToDB(boolean update) {
		Connection con = DatabaseConnection.getConnection();
		try {
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);
			PreparedStatement ps;
			if (ServerConstants.ENABLE_HARDCORE_MODE) {
				if (update) {
					ps = con.prepareStatement("UPDATE `characters` SET `level` = ?, `fame` = ?, `str` = ?, `dex` = ?, `luk` = ?, `int` = ?, `exp` = ?, `gachaexp` = ?, `hp` = ?, `mp` = ?, `maxhp` = ?, `maxmp` = ?, `sp` = ?, `ap` = ?, `rebirths` = ?, `gm` = ?, `skincolor` = ?, `gender` = ?, `job` = ?, `hair` = ?, `face` = ?, `map` = ?, `meso` = ?, `hpMpUsed` = ?, `spawnpoint` = ?, `party` = ?, `buddyCapacity` = ?, `messengerid` = ?, `messengerposition` = ?, `mountlevel` = ?, `mountexp` = ?, `mounttiredness` = ?, `equipslots` = ?, `useslots` = ?, `setupslots` = ?, `etcslots` = ?, `monsterbookcover` = ?, `vanquisherStage` = ?, `dojoPoints` = ?, `lastDojoStage` = ?, `finishedDojoTutorial` = ?, `vanquisherKills` = ?, `matchcardwins` = ?, `matchcardlosses` = ?, `matchcardties` = ?, `omokwins` = ?, `omoklosses` = ?, `omokties` = ?, `hardcore` = ?, `dead` = ? WHERE `id` = ?", Statement.RETURN_GENERATED_KEYS);
				} else {
					ps = con.prepareStatement("INSERT INTO `characters` (`level`, `fame`, `str`, `dex`, `luk`, `int`, `exp`, `gachaexp`, `hp`, `mp`, `maxhp`, `maxmp`, `sp`, `ap`, `rebirths`, `gm`, `skincolor`, `gender`, `job`, `hair`, `face`, `map`, `meso`, `hpMpUsed`, `spawnpoint`, `party`, `buddyCapacity`, `messengerid`, `messengerposition`, `mountlevel`, `mounttiredness`, `mountexp`, `equipslots`, `useslots`, `setupslots`, `etcslots`, `monsterbookcover`, `vanquisherStage`, `dojopoints`, `lastDojoStage`, `finishedDojoTutorial`, `vanquisherKills`, `matchcardwins`, `matchcardlosses`, `matchcardties`, `omokwins`, `omoklosses`, `omokties`, `hardcore`, `dead`, `accountid`, `name`, `world`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				}
			} else {
				if (update) {
					ps = con.prepareStatement("UPDATE `characters` SET `level` = ?, `fame` = ?, `str` = ?, `dex` = ?, `luk` = ?, `int` = ?, `exp` = ?, `gachaexp` = ?, `hp` = ?, `mp` = ?, `maxhp` = ?, `maxmp` = ?, `sp` = ?, `ap` = ?, `rebirths` = ?, `gm` = ?, `skincolor` = ?, `gender` = ?, `job` = ?, `hair` = ?, `face` = ?, `map` = ?, `meso` = ?, `hpMpUsed` = ?, `spawnpoint` = ?, `party` = ?, `buddyCapacity` = ?, `messengerid` = ?, `messengerposition` = ?, `mountlevel` = ?, `mountexp` = ?, `mounttiredness` = ?, `equipslots` = ?, `useslots` = ?, `setupslots` = ?, `etcslots` = ?, `monsterbookcover` = ?, `vanquisherStage` = ?, `dojoPoints` = ?, `lastDojoStage` = ?, `finishedDojoTutorial` = ?, `vanquisherKills` = ?, `matchcardwins` = ?, `matchcardlosses` = ?, `matchcardties` = ?, `omokwins` = ?, `omoklosses` = ?, `omokties` = ? WHERE `id` = ?", Statement.RETURN_GENERATED_KEYS);
				} else {
					ps = con.prepareStatement("INSERT INTO `characters` (`level`, `fame`, `str`, `dex`, `luk`, `int`, `exp`, `gachaexp`, `hp`, `mp`, `maxhp`, `maxmp`, `sp`, `ap`, `rebirths`, `gm`, `skincolor`, `gender`, `job`, `hair`, `face`, `map`, `meso`, `hpMpUsed`, `spawnpoint`, `party`, `buddyCapacity`, `messengerid`, `messengerposition`, `mountlevel`, `mounttiredness`, `mountexp`, `equipslots`, `useslots`, `setupslots`, `etcslots`, `monsterbookcover`, `vanquisherStage`, `dojopoints`, `lastDojoStage`, `finishedDojoTutorial`, `vanquisherKills`, `matchcardwins`, `matchcardlosses`, `matchcardties`, `omokwins`, `omoklosses`, `omokties`, `accountid`, `name`, `world`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				}
			}
			if (gmLevel < 1 && level > 199) {
				ps.setInt(1, isCygnus() ? 120 : 200);
			} else {
				ps.setInt(1, level);
			}
			ps.setInt(2, fame);
			ps.setInt(3, str);
			ps.setInt(4, dex);
			ps.setInt(5, luk);
			ps.setInt(6, int_);
			ps.setInt(7, Math.abs(exp.get()));
			ps.setInt(8, Math.abs(gachaexp.get()));
			ps.setInt(9, hp);
			ps.setInt(10, mp);
			ps.setInt(11, maxhp);
			ps.setInt(12, maxmp);
			ps.setInt(13, remainingSp);
			ps.setInt(14, remainingAp);
			ps.setInt(15, rebirths);
			ps.setInt(16, gmLevel);
			ps.setInt(17, skinColor.getId());
			ps.setInt(18, gender);
			ps.setInt(19, job.getId());
			ps.setInt(20, hair);
			ps.setInt(21, face);
			if (map == null) {
				if (getJob() == Job.BEGINNER) {
					ps.setInt(22, 0);
				} else if (getJob() == Job.NOBLESSE) {
					ps.setInt(22, 130030000);
				} else if (getJob() == Job.LEGEND) {
					ps.setInt(22, 914000000);
				} else if (getJob() == Job.GM || getJob() == Job.SUPERGM) {
					ps.setInt(22, 180000000);
				}
			} else {
				if (map.getForcedReturnId() != 999999999) {
					ps.setInt(22, map.getForcedReturnId());
				} else {
					ps.setInt(22, map.getId());
				}
			}
			ps.setInt(23, meso.get());
			ps.setInt(24, hpMpApUsed);
			if (map == null || map.getId() == 610020000 || map.getId() == 610020001) {
				ps.setInt(25, 0);
			} else {
				Portal closest = map.findClosestSpawnpoint(getPosition());
				if (closest != null) {
					ps.setInt(25, closest.getId());
				} else {
					ps.setInt(25, 0);
				}
			}
			if (party != null) {
				ps.setInt(26, party.getId());
			} else {
				ps.setInt(26, -1);
			}
			ps.setInt(27, buddylist.getCapacity());
			if (messenger != null) {
				ps.setInt(28, messenger.getId());
				ps.setInt(29, messengerposition);
			} else {
				ps.setInt(28, 0);
				ps.setInt(29, 4);
			}
			if (mount != null) {
				ps.setInt(30, mount.getLevel());
				ps.setInt(31, mount.getExp());
				ps.setInt(32, mount.getTiredness());
			} else {
				ps.setInt(30, 1);
				ps.setInt(31, 0);
				ps.setInt(32, 0);
			}
			for (int i = 1; i < 5; i++) {
				ps.setInt(i + 32, getSlots(i));
			}

			if (update) {
				monsterbook.saveCards(getId());
			}
			ps.setInt(37, bookCover);
			ps.setInt(38, vanquisherStage);
			ps.setInt(39, this.dojoState.getPoints());
			ps.setInt(40, this.dojoState.getStage());
			ps.setInt(41, this.dojoState.hasFinishedTutorial() ? 1 : 0);
			ps.setInt(42, vanquisherKills);
			ps.setInt(43, matchingCardStats.getWins());
			ps.setInt(44, matchingCardStats.getLosses());
			ps.setInt(45, matchingCardStats.getTies());
			ps.setInt(46, omokStats.getWins());
			ps.setInt(47, omokStats.getLosses());
			ps.setInt(48, omokStats.getTies());
			if (ServerConstants.ENABLE_HARDCORE_MODE) {
				ps.setInt(49, hardcore ? 1 : 0);
				ps.setInt(50, dead ? 1 : 0);
				if (update) {
					ps.setInt(51, id);
				} else {
					ps.setInt(51, accountId);
					ps.setString(52, name);
					ps.setInt(53, worldId);
				}
			} else {
				if (update) {
					ps.setInt(49, id);
				} else {
					ps.setInt(49, accountId);
					ps.setString(50, name);
					ps.setInt(51, worldId);
				}
			}
			if (ServerConstants.USE_MAPLE_STOCKS) {
				stockPortfolio.save();
			}
			int updateRows = ps.executeUpdate();
			if (!update) {
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					this.id = rs.getInt(1);
				} else {
					throw new RuntimeException("Inserting char failed.");
				}
			} else if (updateRows < 1) {
				throw new RuntimeException("Character not in database (" + id + ")");
			}
			for (int i = 0; i < 3; i++) {
				if (pets[i] != null) {
					pets[i].saveToDb();
				}
			}
			deleteWhereCharacterId(con, "DELETE FROM `keymap` WHERE `characterid` = ?");
			ps = con.prepareStatement("INSERT INTO `keymap` (`characterid,` `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
			ps.setInt(1, id);
			for (Entry<Integer, KeyBinding> keybinding : keymap.entrySet()) {
				ps.setInt(2, keybinding.getKey().intValue());
				ps.setInt(3, keybinding.getValue().getType());
				ps.setInt(4, keybinding.getValue().getAction());
				ps.addBatch();
			}
			ps.executeBatch();
			deleteWhereCharacterId(con, "DELETE FROM `skillmacros` WHERE `characterid` = ?");
			ps = con.prepareStatement("INSERT INTO `skillmacros` (`characterid`, `skill1`, `skill2`, `skill3`, `name`, `shout`, `position`) VALUES (?, ?, ?, ?, ?, ?, ?)");
			ps.setInt(1, getId());
			for (int i = 0; i < 5; i++) {
				SkillMacro macro = skillMacros[i];
				if (macro != null) {
					ps.setInt(2, macro.skill1);
					ps.setInt(3, macro.skill2);
					ps.setInt(4, macro.skill3);
					ps.setString(5, macro.name);
					ps.setInt(6, macro.shout);
					ps.setInt(7, i);
					ps.addBatch();
				}
			}
			ps.executeBatch();
			List<ItemInventoryEntry> itemsWithType = new ArrayList<ItemInventoryEntry>();

			for (Inventory iv : inventory) {
				for (IItem item : iv.list()) {
					itemsWithType.add(new ItemInventoryEntry(item, iv.getType()));
				}
			}

			ItemFactory.INVENTORY.saveItems(itemsWithType, id);
			deleteWhereCharacterId(con, "DELETE FROM `skills` WHERE `characterid` = ?");
			ps = con.prepareStatement("INSERT INTO `skills` (`characterid`, `skillid`, `skilllevel`, `masterlevel`, `expiration`) VALUES (?, ?, ?, ?, ?)");
			ps.setInt(1, id);
			for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
				ps.setInt(2, skill.getKey().getId());
				ps.setInt(3, skill.getValue().skillevel);
				ps.setInt(4, skill.getValue().masterlevel);
				ps.setLong(5, skill.getValue().expiration);
				ps.addBatch();
			}
			ps.executeBatch();
			deleteWhereCharacterId(con, "DELETE FROM `savedlocations` WHERE `characterid` = ?");
			ps = con.prepareStatement("INSERT INTO `savedlocations` (`characterid`, `locationtype`, `map`, `portal`) VALUES (?, ?, ?, ?)");
			ps.setInt(1, id);
			for (SavedLocationType savedLocationType : SavedLocationType.values()) {
				final SavedLocation location = savedLocations[savedLocationType.ordinal()];
				if (location != null) {
					ps.setString(2, savedLocationType.name());
					ps.setInt(3, location.mapId);
					ps.setInt(4, location.portal);
					ps.addBatch();
				}
			}
			ps.executeBatch();
			deleteWhereCharacterId(con, "DELETE FROM `trocklocations` WHERE `characterid` = ?");
			ps = con.prepareStatement("INSERT INTO `trocklocations` (`characterid`, `mapid`, `vip`) VALUES (?, ?, 0)");
			for (int mapId : this.teleportRocks.getRegularMaps()) {
				ps.setInt(1, this.id);
				ps.setInt(2, mapId);
				ps.addBatch();
			}
			ps.executeBatch();
			ps = con.prepareStatement("INSERT INTO `trocklocations` (`characterid`, `mapid`, `vip`) VALUES (?, ?, 1)");
			for (int mapId : this.teleportRocks.getVipMaps()) {
				ps.setInt(1, this.id);
				ps.setInt(2, mapId);
				ps.addBatch();
			}
			ps.executeBatch();
			deleteWhereCharacterId(con, "DELETE FROM `buddies` WHERE `characterid` = ? AND `pending` = 0");
			ps = con.prepareStatement("INSERT INTO `buddies` (`characterid`, `buddyid`, `pending`, `group`) VALUES (?, ?, 0, ?)");
			ps.setInt(1, id);
			for (BuddylistEntry entry : buddylist.getBuddies()) {
				if (entry.isVisible()) {
					ps.setInt(2, entry.getCharacterId());
					ps.setString(3, entry.getGroup());
					ps.addBatch();
				}
			}
			ps.executeBatch();
			deleteWhereCharacterId(con, "DELETE FROM `eventstats` WHERE `characterid` = ?");
			deleteWhereCharacterId(con, "DELETE FROM `queststatus` WHERE `characterid` = ?");
			ps = con.prepareStatement("INSERT INTO `queststatus` (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			PreparedStatement pse = con.prepareStatement("INSERT INTO `questprogress` VALUES (DEFAULT, ?, ?, ?)");
			PreparedStatement psf = con.prepareStatement("INSERT INTO `medalmaps` VALUES (DEFAULT, ?, ?)");
			ps.setInt(1, id);
			for (QuestStatus q : quests.values()) {
				ps.setInt(2, q.getQuest().getId());
				ps.setInt(3, q.getStatus().getId());
				ps.setInt(4, (int) (q.getCompletionTime() / 1000));
				ps.setInt(5, q.getForfeited());
				ps.executeUpdate();
				ResultSet rs = ps.getGeneratedKeys();
				rs.next();
				for (int mob : q.getProgress().keySet()) {
					pse.setInt(1, rs.getInt(1));
					pse.setInt(2, mob);
					pse.setString(3, q.getProgress(mob));
					pse.addBatch();
				}
				for (int i = 0; i < q.getMedalMaps().size(); i++) {
					psf.setInt(1, rs.getInt(1));
					psf.setInt(2, q.getMedalMaps().get(i));
					psf.addBatch();
				}
				pse.executeBatch();
				psf.executeBatch();
				rs.close();
			}
			pse.close();
			psf.close();
			ps = con.prepareStatement("UPDATE `accounts` SET `gm` = ? WHERE `id` = ?");
			ps.setInt(1, gmLevel);
			ps.setInt(2, client.getAccountId());
			ps.executeUpdate();
			if (cashshop != null) {
				cashshop.save();
			}
			if (storage != null) {
				storage.saveToDB();
			}
			ps.close();
			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				con.rollback();
			} catch (SQLException se) {
			}
		} finally {
			try {
				con.setAutoCommit(true);
				con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			} catch (Exception e) {
			}
		}
	}

	public void sendPolice(int greason, String reason, int duration) {
		announce(PacketCreator.sendPolice(greason, reason, duration));
		this.isbanned = true;
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				client.disconnect(); // NIBS
			}
		}, duration);
	}

	public void sendPolice(String text) {
		announce(PacketCreator.sendPolice(text));
		this.isbanned = true;
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				client.disconnect(); // NIBS
			}
		}, 6000);
	}
	
	public void sendPolice() {		
		announce(PacketCreator.sendPolice("You have been blocked by #bMooplePolice#k for the HACK reason."));
		this.isbanned = true;
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				client.disconnect(); // NIBS
			}
		}, 6000);
	}

	public void sendKeymap() {
		client.announce(PacketCreator.getKeymap(keymap));
	}

	public void sendMacros() {
		boolean macros = false;
		for (int i = 0; i < 5; i++) {
			if (skillMacros[i] != null) {
				macros = true;
			}
		}
		if (macros) {
			client.announce(PacketCreator.getMacros(skillMacros));
		}
	}

	public void sendNote(String to, String msg, byte fame) throws SQLException {
		PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `notes` (`to`, `from`, `message`, `timestamp`, `fame`) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, to);
		ps.setString(2, this.getName());
		ps.setString(3, msg);
		ps.setLong(4, System.currentTimeMillis());
		ps.setByte(5, fame);
		ps.executeUpdate();
		ps.close();
	}

	public void setAllianceRank(int rank) {
		allianceRank = rank;
		if (mgc != null) {
			mgc.setAllianceRank(rank);
		}
	}

	public void setAllowWarpToId(int id) {
		this.allowWarpToId = id;
	}

	public static void setAriantRoomLeader(int room, String charname) {
		ariantRoomLeader[room] = charname;
	}

	public static void setAriantSlotRoom(int room, int slot) {
		ariantRoomSlot[room] = slot;
	}

	public void setBattleshipHp(int battleshipHp) {
		this.battleshipHp = battleshipHp;
	}

	public void setBuddyCapacity(int capacity) {
		buddylist.setCapacity(capacity);
		client.announce(PacketCreator.updateBuddyCapacity(capacity));
	}

	public void setBuffedValue(BuffStat effect, int value) {
		BuffStatValueHolder mbsvh = effects.get(effect);
		if (mbsvh == null) {
			return;
		}
		mbsvh.value = value;
	}

	public void setChair(int chair) {
		this.chair = chair;
	}

	public void setChalkboard(String text) {
		this.chalktext = text;
	}
	
	public MtsState getMtsState() {
		return this.mtsState;
	}

	public void setDex(int dex) {
		this.dex = dex;
		recalcLocalStats();
	}



	public void setRates() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT-8"));
		World world = Server.getInstance().getWorld(worldId);
		int hr = cal.get(Calendar.HOUR_OF_DAY);
		if ((haveItem(5360001) && hr > 6 && hr < 12) || (haveItem(5360002) && hr > 9 && hr < 15) || (haveItem(536000) && hr > 12 && hr < 18) || (haveItem(5360004) && hr > 15 && hr < 21) || (haveItem(536000) && hr > 18) || (haveItem(5360006) && hr < 5) || (haveItem(5360007) && hr > 2 && hr < 6) || (haveItem(5360008) && hr >= 6 && hr < 11)) {
			this.dropRate = 2 * world.getDropRate();
			this.mesoRate = 2 * world.getMesoRate();
		} else {
			this.dropRate = world.getDropRate();
			this.mesoRate = world.getMesoRate();
		}
		if ((haveItem(5211000) && hr > 17 && hr < 21) || (haveItem(5211014) && hr > 6 && hr < 12) || (haveItem(5211015) && hr > 9 && hr < 15) || (haveItem(5211016) && hr > 12 && hr < 18) || (haveItem(5211017) && hr > 15 && hr < 21) || (haveItem(5211018) && hr > 14) || (haveItem(5211039) && hr < 5) || (haveItem(5211042) && hr > 2 && hr < 8) || (haveItem(5211045) && hr > 5 && hr < 11) || haveItem(5211048)) {
			if (isBeginnerJob() && ServerConstants.BEGINNERS_USE_GMS_RATES) {
				this.expRate = 2;
			} else {
				this.expRate = 2 * world.getExpRate();
			}
		} else {
			if (isBeginnerJob() && ServerConstants.BEGINNERS_USE_GMS_RATES) {
				this.expRate = 1;
			} else {
				this.expRate = world.getExpRate();
			}
		}
		if (isHardcoreMode()) {
			this.expRate *= 2;
			this.mesoRate *= 2;
		}
	}

	public void setEnergyBar(int set) {
		energybar = set;
	}

	public void setEventInstance(EventInstanceManager eventInstance) {
		this.eventInstance = eventInstance;
	}

	public void setExp(int amount) {
		this.exp.set(amount);
	}

	public void setGachaExp(int amount) {
		this.gachaexp.set(amount);
	}

	public void setFace(int face) {
		this.face = face;
	}

	public void setFame(int fame) {
		this.fame = fame;
	}
	
	public void setRebirths(int rebirths) {
		this.rebirths = rebirths;
	}

	public void setFamilyId(int familyId) {
		this.familyId = familyId;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public void setGM(int level) {
		this.gmLevel = level;
	}

	public void setGuildId(int _id) {
		guildId = _id;
		if (guildId > 0) {
			if (mgc == null) {
				mgc = new GuildCharacter(this);
			} else {
				mgc.setGuildId(guildId);
			}
		} else {
			mgc = null;
		}
	}

	public void setGuildRank(int _rank) {
		guildRank = _rank;
		if (mgc != null) {
			mgc.setGuildRank(_rank);
		}
	}

	public void setHair(int hair) {
		this.hair = hair;
	}

	public void setHasMerchant(boolean set) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `characters` SET `HasMerchant` = ? WHERE `id` = ?");
			ps.setInt(1, set ? 1 : 0);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		hasMerchant = set;
	}

	public void addMerchantMesos(int add) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `characters` SET `MerchantMesos` = ? WHERE `id` = ?", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, merchantmeso + add);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			return;
		}
		merchantmeso += add;
	}

	public void setMerchantMeso(int set) {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE `characters` SET `MerchantMesos` = ? WHERE `id` = ?", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, set);
			ps.setInt(2, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			return;
		}
		merchantmeso = set;
	}

	public void setHiredMerchant(HiredMerchant merchant) {
		this.hiredMerchant = merchant;
	}

	public void setHp(int newhp) {
		setHp(newhp, false);
	}

	public void setHp(int newhp, boolean silent) {
		int oldHp = hp;
		int thp = newhp;
		if (thp < 0) {
			thp = 0;
		}
		if (thp > localMaxHp) {
			thp = localMaxHp;
		}
		this.hp = thp;
		if (!silent) {
			updatePartyMemberHP();
		}
		if (oldHp > hp && !isAlive()) {
			playerDead();
		}
	}

	public void setHpMpApUsed(int mpApUsed) {
		this.hpMpApUsed = mpApUsed;
	}

	public void setHpMp(int x) {
		setHp(x);
		setMp(x);
		updateSingleStat(Stat.HP, hp);
		updateSingleStat(Stat.MP, mp);
	}

	public void setInt(int int_) {
		this.int_ = int_;
		recalcLocalStats();
	}

	public void setInventory(InventoryType type, Inventory inv) {
		inventory[type.ordinal()] = inv;
	}

	public void setItemEffect(int itemEffect) {
		this.itemEffect = itemEffect;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void setLastHealed(long time) {
		this.lastHealed = time;
	}

	public void setLastUsedCashItem(long time) {
		this.lastUsedCashItem = time;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setLuk(int luk) {
		this.luk = luk;
		recalcLocalStats();
	}

	public void setMarkedMonster(int markedMonster) {
		this.markedMonster = markedMonster;
	}

	public void setMaxHp(int hp) {
		this.maxhp = hp;
		recalcLocalStats();
	}

	public void setMaxHp(int hp, boolean ap) {
		hp = Math.min(30000, hp);
		if (ap) {
			setHpMpApUsed(getHpMpApUsed() + 1);
		}
		this.maxhp = hp;
		recalcLocalStats();
	}

	public void setMaxMp(int mp) {
		this.maxmp = mp;
		recalcLocalStats();
	}

	public void setMaxMp(int mp, boolean ap) {
		mp = Math.min(30000, mp);
		if (ap) {
			setHpMpApUsed(getHpMpApUsed() + 1);
		}
		this.maxmp = mp;
		recalcLocalStats();
	}

	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;
	}

	public void setMessengerPosition(int position) {
		this.messengerposition = position;
	}

	public void setActiveMinigame(Minigame miniGame) {
		this.activeMinigame = miniGame;
	}

	public void setMonsterBookCover(int bookCover) {
		this.bookCover = bookCover;
	}

	public void setMp(int newmp) {
		int tmp = newmp;
		if (tmp < 0) {
			tmp = 0;
		}
		if (tmp > localMaxMp) {
			tmp = localMaxMp;
		}
		this.mp = tmp;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setParty(Party party) {
		if (party == null) {
			this.mpc = null;
		}
		this.party = party;
	}

	public void setPlayerShop(PlayerShop playerShop) {
		this.playerShop = playerShop;
	}

	public void setRemainingAp(int remainingAp) {
		this.remainingAp = remainingAp;
	}

	public void setRemainingSp(int remainingSp) {
		this.remainingSp = remainingSp;
	}

	public void setSkinColor(SkinColor skinColor) {
		this.skinColor = skinColor;
	}

	public byte getSlots(int type) {
		return type == InventoryType.CASH.asByte() ? 96 : inventory[type].getSlotLimit();
	}

	public boolean gainSlots(int type, int slots) {
		return gainSlots(type, slots, true);
	}

	public boolean gainSlots(int type, int slots, boolean update) {
		slots += inventory[type].getSlotLimit();
		if (slots <= 96) {
			inventory[type].setSlotLimit(slots);

			saveToDB(true);
			if (update) {
				client.announce(PacketCreator.updateInventorySlotLimit(type, slots));
			}

			return true;
		}

		return false;
	}

	public void setShop(Shop shop) {
		this.shop = shop;
	}

	public void setSlot(int slotid) {
		slots = slotid;
	}
	
	public void setStat(Stat stat, int value) {
		this.updateSingleStat(stat, value);
		switch (stat) {
			case STR:
				setStr(value);
				break;
			case DEX:
				setDex(value);
				break;
			case INT:
				setInt(value);
				break;
			case LUK:
				setLuk(value);
				break;
			default:
				break;
		}
		this.updateSingleStat(stat, value);
	}

	public void setStr(int str) {
		this.str = str;
		recalcLocalStats();
	}

	public void setTrade(Trade trade) {
		this.trade = trade;
	}

	public void setVanquisherKills(int x) {
		this.vanquisherKills = x;
	}

	public void setVanquisherStage(int x) {
		this.vanquisherStage = x;
	}

	public void setWorldId(byte world) {
		this.worldId = world;
	}

	public void shiftPetsRight() {
		if (pets[2] == null) {
			pets[2] = pets[1];
			pets[1] = pets[0];
			pets[0] = null;
		}
	}

	public void showDojoClock(int mapId) {
		int stage = (mapId / 100) % 100;
		long seconds;
		if (stage % 6 == 1) {
			seconds = (stage > 36 ? 15 : stage / 6 + 5) * 60;
		} else {
			seconds = (this.dojoState.getFinishTimestamp() - System.currentTimeMillis()) / 1000;
		}
		if (stage % 6 > 0) {
			client.announce(PacketCreator.getClock((int) seconds));
		}
		int clockId = (mapId / 100) % 100;
		final boolean rightmap;
		if (mapId > clockId / 6 * 6 + 6 || mapId < clockId / 6 * 6) {
			rightmap = false;
		} else {
			rightmap = true;
		}

		// let the TIME'S UP display for 3 seconds, then warp
		final long delay = seconds * 1000 + 3000;
		
		TimerManager.getInstance().schedule(new DojoForceWarpAction(rightmap), delay); 
	}
	
	public boolean getGmText() {
		return whitetext;
	}
	
	public void toggleGmText() {
		this.whitetext = !this.whitetext;
	}

	public void showNote() {
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM notes WHERE `to`=? AND `deleted` = 0", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ps.setString(1, this.getName());
			ResultSet rs = ps.executeQuery();
			rs.last();
			int count = rs.getRow();
			rs.first();
			client.announce(PacketCreator.showNotes(rs, count));
			rs.close();
			ps.close();
		} catch (SQLException e) {
		}
	}

	private void silentEnforceMaxHpMp() {
		setMp(getMp());
		setHp(getHp(), true);
	}

	public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
		for (PlayerBuffValueHolder mbsvh : buffs) {
			mbsvh.effect.silentApplyBuff(this, mbsvh.startTime);
		}
	}

	public void silentPartyUpdate() {
		if (party != null) {
			Server.getInstance().getWorld(worldId).updateParty(party.getId(), PartyOperation.SILENT_UPDATE, getMPC());
		}
	}

	public static class SkillEntry {

		public final int masterlevel;
		public final byte skillevel;
		public final long expiration;

		public SkillEntry(byte skillevel, int masterlevel, long expiration) {
			this.skillevel = skillevel;
			this.masterlevel = masterlevel;
			this.expiration = expiration;
		}

		@Override
		public String toString() {
			return skillevel + ":" + masterlevel;
		}
	}

	public boolean skillisCooling(int skillId) {
		return coolDowns.containsKey(Integer.valueOf(skillId));
	}

	public void startFullnessSchedule(final int decrease, final Pet pet, int petSlot) {
		if (isGM() && ServerConstants.GM_PETS_NEVER_HUNGRY || ServerConstants.PETS_NEVER_HUNGRY) {
			// no fullness schedules :3
			return; 
		}
		ScheduledFuture<?> schedule = TimerManager.getInstance().register(
				new FullnessScheduleAction(pet, decrease), 
				ServerConstants.PET_FULLNESS_REPEAT_TIME, 
				ServerConstants.PET_FULLNESS_START_DELAY);
		
		fullnessSchedule[petSlot] = schedule;
	}

	public void startMapEffect(String msg, int itemId) {
		startMapEffect(msg, itemId, 30000);
	}

	public void startMapEffect(String msg, int itemId, int duration) {
		final GameMapEffect mapEffect = new GameMapEffect(msg, itemId);
		getClient().announce(mapEffect.makeStartData());
		TimerManager.getInstance().schedule(new MapEffectAction(mapEffect), duration);
	}

	public void stopControllingMonster(Monster monster) {
		controlled.remove(monster);
	}

	public void unequipAllPets() {
		for (int i = 0; i < 3; i++) {
			if (pets[i] != null) {
				unequipPet(pets[i], true);
			}
		}
	}

	public void unequipPet(Pet pet, boolean shiftLeft) {
		unequipPet(pet, shiftLeft, false);
	}

	public void unequipPet(Pet pet, boolean shiftLeft, boolean hunger) {
		final byte petIndex = this.getPetIndex(pet);
		if (this.getPet(petIndex) != null) {
			this.getPet(petIndex).setSummoned(false);
			this.getPet(petIndex).saveToDb();
		}
		cancelFullnessSchedule(petIndex);
		getMap().broadcastMessage(this, PacketCreator.showPet(this, pet, true, hunger), true);
		List<Pair<Stat, Integer>> stats = new ArrayList<Pair<Stat, Integer>>();
		stats.add(new Pair<Stat, Integer>(Stat.PET, Integer.valueOf(0)));
		client.getSession().write(PacketCreator.petStatUpdate(this));
		client.getSession().write(PacketCreator.enableActions());
		removePet(pet, shiftLeft);
	}

	public void updateMacro(int position, SkillMacro newMacro) {
		skillMacros[position] = newMacro;
	}

	public void updatePartyMemberHP() {
		if (party != null) {
			byte channel = client.getChannel();
			for (PartyCharacter partychar : party.getMembers()) {
				if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
					GameCharacter other = Server.getInstance().getWorld(worldId).getChannel(channel).getPlayerStorage().getCharacterByName(partychar.getName());
					if (other != null) {
						other.client.announce(PacketCreator.updatePartyMemberHP(getId(), this.hp, maxhp));
					}
				}
			}
		}
	}

	public void updateQuest(QuestStatus quest) {
		quests.put(quest.getQuest(), quest);
		if (quest.getStatus().equals(QuestStatus.Status.STARTED)) {
			announce(PacketCreator.questProgress((short) quest.getQuest().getId(), quest.getProgress(0)));
			if (quest.getQuest().getInfoNumber() > 0) {
				announce(PacketCreator.questProgress(quest.getQuest().getInfoNumber(), Integer.toString(quest.getMedalProgress())));
			}
			announce(PacketCreator.updateQuestInfo((short) quest.getQuest().getId(), quest.getNpc()));
		} else if (quest.getStatus().equals(QuestStatus.Status.COMPLETED)) {
			announce(PacketCreator.completeQuest((short) quest.getQuest().getId(), quest.getCompletionTime()));
		} else if (quest.getStatus().equals(QuestStatus.Status.NOT_STARTED)) {
			announce(PacketCreator.forfeitQuest((short) quest.getQuest().getId()));
		}
	}

	public void questTimeLimit(final Quest quest, int time) {
		ScheduledFuture<?> sf = TimerManager.getInstance().schedule(new QuestExpirationAction(quest), time);
		announce(PacketCreator.addQuestTimeLimit(quest.getId(), time));
		timers.add(sf);
	}

	public void updateSingleStat(Stat stat, int newval) {
		updateSingleStat(stat, newval, false);
	}

	private void updateSingleStat(Stat stat, int newval, boolean itemReaction) {
		announce(PacketCreator.updatePlayerStats(Collections.singletonList(new StatDelta(stat, Integer.valueOf(newval))), itemReaction));
	}

	public void announce(GamePacket packet) {
		client.announce(packet);
	}

	@Override
	public int getObjectId() {
		return getId();
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.PLAYER;
	}

	@Override
	public void sendDestroyData(GameClient client) {
		client.announce(PacketCreator.removePlayerFromMap(this.getObjectId()));
	}

	@Override
	public void sendSpawnData(GameClient client) {
		if (!this.isHidden() || client.getPlayer().gmLevel() > 0) {
			client.announce(PacketCreator.spawnPlayerMapObject(this));
		}
	}

	@Override
	public void setObjectId(int id) {
	}

	@Override
	public String toString() {
		return name;
	}

	private int givenRiceCakes;
	private boolean gottenRiceHat;

	public int getGivenRiceCakes() {
		return givenRiceCakes;
	}

	public void increaseGivenRiceCakes(int amount) {
		this.givenRiceCakes += amount;
	}

	public boolean getGottenRiceHat() {
		return gottenRiceHat;
	}

	public void setGottenRiceHat(boolean b) {
		this.gottenRiceHat = b;
	}

	public int getLinkedLevel() {
		return linkedLevel;
	}

	public String getLinkedName() {
		return linkedName;
	}

	public CashShop getCashShop() {
		return cashshop;
	}

	public void portalDelay(long delay) {
		this.portaldelay = System.currentTimeMillis() + delay;
	}

	public long portalDelay() {
		return portaldelay;
	}

	public void blockPortal(String scriptName) {
		if (!blockedPortals.contains(scriptName) && scriptName != null) {
			blockedPortals.add(scriptName);
			client.announce(PacketCreator.enableActions());
		}
	}

	public void unblockPortal(String scriptName) {
		if (blockedPortals.contains(scriptName) && scriptName != null) {
			blockedPortals.remove(scriptName);
		}
	}

	public List<String> getBlockedPortals() {
		return blockedPortals;
	}

	public boolean getAranIntroState(String mode) {
		if (area_data.contains(mode)) {
			return true;
		}
		return false;
	}

	public void addAreaData(int quest, String data) {
		if (!this.area_data.contains(data)) {
			this.area_data.add(data);
			
			Connection con = DatabaseConnection.getConnection();

			try (PreparedStatement ps = getInsertAreaData(con, data, quest)) {
				ps.executeUpdate();
			} catch (SQLException ex) {
				Output.print("An error has occurred with area data.");
				ex.printStackTrace();
			}
		}
	}

	private PreparedStatement getInsertAreaData(Connection connection,
			String data, int quest) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("INSERT INTO `char_area_info` VALUES (DEFAULT, ?, ?, ?)");
		ps.setInt(1, getId());
		ps.setInt(2, quest);
		ps.setString(3, data);
		return ps;
	}

	public void removeAreaData() {
		this.area_data.clear();
		
		Connection con = DatabaseConnection.getConnection();
		
		try (PreparedStatement ps = getDeleteAreaData(con)) {
			ps.executeUpdate();
		} catch (SQLException ex) {
			Output.print("An error has occurred with area data.");
			ex.printStackTrace();
		}
	}

	private PreparedStatement getDeleteAreaData(Connection connection)
			throws SQLException {
		PreparedStatement ps = connection.prepareStatement("DELETE FROM `char_area_info` WHERE `charid` = ?");
		ps.setInt(1, getId());
		return ps;
	}

	public void autoban(String reason, int greason) {
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
		Timestamp timestamp = new Timestamp(cal.getTimeInMillis());
		
		Connection con = DatabaseConnection.getConnection();
		try (PreparedStatement ps = getUpdateBanData(con, timestamp, reason, greason);) {
			ps.executeUpdate();
		} catch (SQLException ex) {
			Output.print("An error has occurred while writing autoban data.");
			ex.printStackTrace();
		}
	}

	private PreparedStatement getUpdateBanData(Connection connection,
			Timestamp timestamp, String reason, int reasonId)
			throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE `accounts` SET `banreason` = ?, `tempban` = ?, `greason` = ? WHERE `id` = ?");
		ps.setString(1, reason);
		ps.setTimestamp(2, timestamp);
		ps.setInt(3, reasonId);
		ps.setInt(4, accountId);
		return ps;
	}

	public boolean isBanned() {
		return isbanned;
	}

	public TeleportRockInfo getTeleportRockInfo() {
		return this.teleportRocks;
	}
	
	// EVENTS
	private byte team = 0;
	private MapleFitness fitness;
	private MapleOla ola;
	private long snowballattack;

	public byte getTeam() {
		return team;
	}

	public void setTeam(int team) {
		this.team = (byte) team;
	}

	public MapleOla getOla() {
		return ola;
	}

	public void setOla(MapleOla ola) {
		this.ola = ola;
	}

	public MapleFitness getFitness() {
		return fitness;
	}

	public void setFitness(MapleFitness fit) {
		this.fitness = fit;
	}

	public long getLastSnowballAttack() {
		return snowballattack;
	}

	public void setLastSnowballAttack(long time) {
		this.snowballattack = time;
	}

	// TODO: WHY HERE. WHY. WHY ADD THESE BYTES TO EVERY INSTANCE.
	// Monster Carnival
	private int cp = 0;
	private int obtainedcp = 0;
	private MonsterCarnivalParty carnivalparty;
	private MonsterCarnival carnival;

	public MonsterCarnivalParty getCarnivalParty() {
		return carnivalparty;
	}

	public void setCarnivalParty(MonsterCarnivalParty party) {
		this.carnivalparty = party;
	}

	public MonsterCarnival getCarnival() {
		return carnival;
	}

	public void setCarnival(MonsterCarnival car) {
		this.carnival = car;
	}

	public int getCP() {
		return cp;
	}

	public int getObtainedCP() {
		return obtainedcp;
	}

	public void addCP(int cp) {
		this.cp += cp;
		this.obtainedcp += cp;
	}

	public void useCP(int cp) {
		this.cp -= cp;
	}

	public void setObtainedCP(int cp) {
		this.obtainedcp = cp;
	}

	public int getAndRemoveCP() {
		int rCP = 10;
		if (cp < 9) {
			rCP = cp;
			cp = 0;
		} else {
			cp -= 10;
		}

		return rCP;
	}

	public AutobanManager getAutobanManager() {
		return autoban;
	}

	public void equipPendantOfSpirit() {
		if (pendantSchedule == null) {
			pendantSchedule = TimerManager.getInstance().register(new PendantHourlyAction(), 3600000); // 1 hour
		}
	}

	public void unequipPendantOfSpirit() {
		if (pendantSchedule != null) {
			pendantSchedule.cancel(false);
			pendantSchedule = null;
		}
		pendantExp = 0;
	}

	public void increaseEquipExp(int mobexp) {
		ItemInfoProvider mii = ItemInfoProvider.getInstance();
		for (IItem item : getInventory(InventoryType.EQUIPPED).list()) {
			Equip nEquip = (Equip) item;
			String itemName = mii.getName(nEquip.getItemId());
			if (itemName == null) {
				continue;
			}

			if ((itemName.contains("Reverse") && nEquip.getItemLevel() < 4) || itemName.contains("Timeless") && nEquip.getItemLevel() < 6) {
				nEquip.gainItemExp(client, mobexp, itemName.contains("Timeless"));
			}
		}
	}

	public Map<String, MapleEvents> getEvents() {
		return events;
	}

	public PartyQuest getPartyQuest() {
		return partyQuest;
	}

	public void setPartyQuest(PartyQuest pq) {
		this.partyQuest = pq;
	}

	public final void empty() {
		// lol serious shit here
		TimerManager.cancelSafely(this.dragonBloodSchedule, false);
		TimerManager.cancelSafely(this.hpDecreaseTask, false);
		TimerManager.cancelSafely(this.beholderHealingSchedule, false);
		TimerManager.cancelSafely(this.beholderBuffSchedule, false);
		TimerManager.cancelSafely(this.berserkSchedule, false);
		TimerManager.cancelSafely(this.recoverySchedule, false);

		cancelExpirationTask();
		for (ScheduledFuture<?> sf : timers) {
			sf.cancel(false);
		}
		timers.clear();
		timers = null;
		
		mount = null;
		diseases = null;
		partyQuest = null;
		events = null;
		skills = null;
		mpc = null;
		mgc = null;
		events = null;
		party = null;
		family = null;
		client = null;
	}

}
