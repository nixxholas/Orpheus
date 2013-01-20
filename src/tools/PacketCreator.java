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
package tools;

import java.awt.Point;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import client.BuddylistEntry;
import client.DojoState;
import client.IEquip;
import client.IEquip.ScrollResult;
import client.IItem;
import client.ISkill;
import client.Item;
import client.ItemFactory;
import client.ItemInventoryEntry;
import client.BuffStat;
import client.GameCharacter;
import client.GameClient;
import client.DiseaseEntry;
import client.FamilyEntry;
import client.Inventory;
import client.InventoryType;
import client.KeyBinding;
import client.LinkedCharacterInfo;
import client.MinigameStats;
import client.MonsterBook;
import client.Mount;
import client.Pet;
import client.QuestStatus;
import client.Ring;
import client.Stat;
import client.StatDelta;
import client.SkillMacro;
import client.TeleportRockInfo;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ItemConstants;
import constants.ServerConstants;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import net.LongValueHolder;
import net.GamePacket;
import net.SendOpcode;
import net.server.handlers.channel.PlayerInteractionHandler;
import net.server.handlers.channel.SummonDamageHandler.SummonAttackEntry;
import net.server.Party;
import net.server.PartyCharacter;
import net.server.PartyOperation;
import net.server.PlayerCoolDownValueHolder;
import net.server.Server;
import net.server.guild.Alliance;
import net.server.guild.Guild;
import net.server.guild.GuildCharacter;
import net.server.guild.GuildSummary;
import server.CashShop.CashItem;
import server.CashShop.CashItemFactory;
import server.CashShop.SpecialCashItem;
import server.DueyPackages;
import server.GiftEntry;
import server.MTSItemInfo;
import server.BuffStatDelta;
import server.ItemInfoProvider;
import server.Minigame;
import server.PlayerShop;
import server.PlayerShopItem;
import server.ShopItem;
import server.Trade;
import server.WorldRecommendation;
import server.events.gm.MapleSnowball;
import server.partyquest.MonsterCarnivalParty;
import server.life.Monster;
import server.life.Npc;
import server.life.MobSkill;
import server.life.NpcDescriptionEntry;
import server.maps.HiredMerchant;
import server.maps.GameMap;
import server.maps.GameMapItem;
import server.maps.Mist;
import server.maps.Reactor;
import server.maps.Summon;
import server.maps.PlayerNPCs;
import server.movement.LifeMovementFragment;
import tools.data.output.PacketWriter;

/**
 * 
 * @author Frz
 */
public class PacketCreator {

	private final static byte[] CHAR_INFO_MAGIC = new byte[] {(byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b};
	public static final List<StatDelta> EMPTY_STATUPDATE = Collections.emptyList();
	private final static byte[] ITEM_MAGIC = new byte[] {(byte) 0x80, 0x05};
	private final static int ITEM_YEAR2000 = -1085019342;
	private final static long REAL_YEAR2000 = 946681229830L;

	public static int getItemTimestamp(long realTimestamp) {
		// convert to minutes
		int time = (int) ((realTimestamp - REAL_YEAR2000) / 1000 / 60); 
		return (int) (time * 35.762787) + ITEM_YEAR2000;
	}

	private static int getQuestTimestamp(long realTimestamp) {
		return (int) (((int) (realTimestamp / 1000 / 60)) * 0.1396987) + 27111908;
	}

	private static long getKoreanTimestamp(long realTimestamp) {
		return realTimestamp * 10000 + 116444592000000000L;
	}

	private static long getTime(long realTimestamp) {
		return realTimestamp * 10000 + 116444592000000000L;
	}

	private static void addCharStats(PacketWriter w, GameCharacter chr) {
		w.writeInt(chr.getId()); // character id
		w.writePaddedString(chr.getName(), 13);
		w.writeAsByte(chr.getGender()); // gender (0 = male, 1 = female)
		w.writeAsByte(chr.getSkinColor().getId()); // skin color
		w.writeInt(chr.getFace()); // face
		w.writeInt(chr.getHair()); // hair

		for (int i = 0; i < 3; i++) {
			// Checked GMS.. and your pets stay when going into the cash shop.
			if (chr.getPet(i) != null) {
				w.writeLong(chr.getPet(i).getUniqueId());
			} else {
				w.writeLong(0);
			}
		}

		w.writeAsByte(chr.getLevel()); // level
		w.writeAsShort(chr.getJob().getId()); // job
		w.writeAsShort(chr.getStr()); // str
		w.writeAsShort(chr.getDex()); // dex
		w.writeAsShort(chr.getInt()); // int
		w.writeAsShort(chr.getLuk()); // luk
		w.writeAsShort(chr.getHp()); // hp (?)
		w.writeAsShort(chr.getMaxHp()); // maxhp
		w.writeAsShort(chr.getMp()); // mp (?)
		w.writeAsShort(chr.getMaxMp()); // maxmp
		w.writeAsShort(chr.getRemainingAp()); // remaining ap
		w.writeAsShort(chr.getRemainingSp()); // remaining sp
		w.writeInt(chr.getExp()); // current exp
		w.writeAsShort(chr.getFame()); // fame
		w.writeInt(chr.getGachaExp()); // Gacha Exp
		w.writeInt(chr.getMapId()); // current map id
		w.writeAsByte(chr.getInitialSpawnpoint()); // spawnpoint
		w.writeInt(0);
	}

	private static void addCharLook(PacketWriter w, GameCharacter chr, boolean mega) {
		w.writeAsByte(chr.getGender());
		w.writeAsByte(chr.getSkinColor().getId()); // skin color
		w.writeInt(chr.getFace()); // face
		w.writeAsByte(!mega);
		w.writeInt(chr.getHair()); // hair
		addCharEquips(w, chr);
	}

	private static void addCharacterInfo(PacketWriter w, GameCharacter chr) {
		w.writeLong(-1);
		w.writeAsByte(0);
		addCharStats(w, chr);
		w.writeAsByte(chr.getBuddylist().getCapacity());

		final LinkedCharacterInfo info = chr.getLinkedCharacter();
		if (info == null) {
			w.writeAsByte(0);
		} else {
			w.writeAsByte(1);
			w.writeLengthString(info.name);
		}

		w.writeInt(chr.getMeso());
		addInventoryInfo(w, chr);
		addSkillInfo(w, chr);
		addQuestInfo(w, chr);
		w.writeAsShort(0);
		addRingInfo(w, chr);
		addTeleportInfo(w, chr);
		addMonsterBookInfo(w, chr);
		w.writeAsShort(0);
		w.writeInt(0);
	}

	private static void addTeleportInfo(PacketWriter w, GameCharacter chr) {
		final TeleportRockInfo info = chr.getTeleportRockInfo();
		final List<Integer> regular = info.getRegularMaps();
		for (int i = 0; i < 5; i++) {
			if (i < regular.size()) {
				w.writeInt(regular.get(i));
			} else {
				w.writeInt(999999999);
			}
		}

		final List<Integer> vip = info.getVipMaps();
		for (int i = 0; i < 10; i++) {
			if (i < vip.size()) {
				w.writeInt(vip.get(i));
			} else {
				w.writeInt(999999999);
			}
		}
	}

	private static void addCharEquips(PacketWriter w, GameCharacter chr) {
		Inventory equip = chr.getInventory(InventoryType.EQUIPPED);
		Collection<IItem> ii = ItemInfoProvider.getInstance().canWearEquipment(chr, equip.list());
		Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
		Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
		for (IItem item : ii) {
			byte pos = (byte) (item.getSlot() * -1);
			if (pos < 100 && myEquip.get(pos) == null) {
				myEquip.put(pos, item.getItemId());
			} else if (pos > 100 && pos != 111) { 
				// don't ask. o.o
				pos -= 100;
				if (myEquip.get(pos) != null) {
					maskedEquip.put(pos, myEquip.get(pos));
				}
				myEquip.put(pos, item.getItemId());
			} else if (myEquip.get(pos) != null) {
				maskedEquip.put(pos, item.getItemId());
			}
		}
		for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
			w.writeAsByte(entry.getKey());
			w.writeInt(entry.getValue());
		}
		w.writeAsByte(0xFF);
		for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
			w.writeAsByte(entry.getKey());
			w.writeInt(entry.getValue());
		}
		w.writeAsByte(0xFF);
		IItem cWeapon = equip.getItem((byte) -111);
		w.writeInt(cWeapon != null ? cWeapon.getItemId() : 0);
		for (int i = 0; i < 3; i++) {
			if (chr.getPet(i) != null) {
				w.writeInt(chr.getPet(i).getItemId());
			} else {
				w.writeInt(0);
			}
		}
	}

	private static void addCharEntry(PacketWriter w, GameCharacter chr, boolean viewAll) {
		addCharStats(w, chr);
		addCharLook(w, chr, false);
		if (!viewAll) {
			w.writeAsByte(0);
		}
		if (chr.isGM()) {
			w.writeAsByte(0);
			return;
		}
		// world rank enabled (next 4 ints are not sent if disabled) Short??
		w.writeAsByte(1); 
		w.writeInt(chr.getRank()); // world rank
		w.writeInt(chr.getRankMove()); // move (negative is downwards)
		w.writeInt(chr.getJobRank()); // job rank
		w.writeInt(chr.getJobRankMove()); // move (negative is downwards)
	}

	private static void addQuestInfo(PacketWriter w, GameCharacter chr) {
		w.writeAsShort(chr.getStartedQuestsSize());
		for (QuestStatus q : chr.getStartedQuests()) {
			w.writeAsShort(q.getQuest().getId());
			w.writeLengthString(q.getQuestData());
			if (q.getQuest().getInfoNumber() > 0) {
				w.writeAsShort(q.getQuest().getInfoNumber());
				w.writeLengthString(Integer.toString(q.getMedalProgress()));
			}
		}
		List<QuestStatus> completed = chr.getCompletedQuests();
		w.writeAsShort(completed.size());
		for (QuestStatus q : completed) {
			w.writeAsShort(q.getQuest().getId());
			int time = getQuestTimestamp(q.getCompletionTime());
			w.writeInt(time);
			w.writeInt(time);
		}
	}

	private static void addItemInfo(PacketWriter w, IItem item) {
		addItemInfo(w, item, false);
	}

	private static void addExpirationTime(PacketWriter w, long time) {
		addExpirationTime(w, time, true);
	}

	private static void addExpirationTime(PacketWriter w, long time, boolean addzero) {
		if (addzero) {
			w.writeAsByte(0);
		}
		w.write(ITEM_MAGIC);
		if (time == -1) {
			w.writeInt(400967355);
			w.writeAsByte(2);
		} else {
			w.writeInt(getItemTimestamp(time));
			w.writeAsByte(1);
		}
	}

	private static void addItemInfo(PacketWriter w, IItem item, boolean zeroPosition) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		boolean isCash = ii.isCash(item.getItemId());
		boolean isPet = item.getPetId() > -1;
		boolean isRing = false;
		IEquip equip = null;
		byte pos = item.getSlot();
		if (item.getType() == IItem.EQUIP) {
			equip = (IEquip) item;
			isRing = equip.getRingId() > -1;
		}
		if (!zeroPosition) {
			if (equip != null) {
				if (pos < 0) {
					pos *= -1;
				}
				w.writeAsShort(pos > 100 ? pos - 100 : pos);
			} else {
				w.writeAsByte(pos);
			}
		}
		w.writeAsByte(item.getType());
		w.writeInt(item.getItemId());
		w.writeAsByte(isCash);
		if (isCash) {
			w.writeLong(isPet ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId());
		}
		addExpirationTime(w, item.getExpiration());
		if (isPet) {
			Pet pet = Pet.loadFromDb(item);
			w.writePaddedString(pet.getName(), 13);
			w.writeAsByte(pet.getLevel());
			w.writeAsShort(pet.getCloseness());
			w.writeAsByte(pet.getFullness());
			addExpirationTime(w, item.getExpiration());
			w.writeInt(0);
			// wonder what this is
			w.write(new byte[] {(byte) 0x50, (byte) 0x46}); 
			w.writeInt(0);
			return;
		}
		if (equip == null) {
			w.writeAsShort(item.getQuantity());
			w.writeLengthString(item.getOwner());
			w.writeAsShort(item.getFlag()); // flag

			if (ItemConstants.isRechargable(item.getItemId())) {
				w.writeInt(2);
				w.write(new byte[] {(byte) 0x54, 0, 0, (byte) 0x34});
			}
			return;
		}
		w.writeAsByte(equip.getUpgradeSlots()); // upgrade slots
		w.writeAsByte(equip.getLevel()); // level
		w.writeAsShort(equip.getStr()); // str
		w.writeAsShort(equip.getDex()); // dex
		w.writeAsShort(equip.getInt()); // int
		w.writeAsShort(equip.getLuk()); // luk
		w.writeAsShort(equip.getHp()); // hp
		w.writeAsShort(equip.getMp()); // mp
		w.writeAsShort(equip.getWatk()); // watk
		w.writeAsShort(equip.getMatk()); // matk
		w.writeAsShort(equip.getWdef()); // wdef
		w.writeAsShort(equip.getMdef()); // mdef
		w.writeAsShort(equip.getAcc()); // accuracy
		w.writeAsShort(equip.getAvoid()); // avoid
		w.writeAsShort(equip.getHands()); // hands
		w.writeAsShort(equip.getSpeed()); // speed
		w.writeAsShort(equip.getJump()); // jump
		w.writeLengthString(equip.getOwner()); // owner name
		w.writeAsShort(equip.getFlag()); // Item Flags

		if (isCash) {
			for (int i = 0; i < 10; i++) {
				w.writeAsByte(0x40);
			}
		} else {
			w.writeAsByte(0);
			w.writeAsByte(equip.getItemLevel()); // Item Level
			w.writeAsShort(0);
			w.writeAsShort(equip.getItemExp()); // Works pretty weird :s
			w.writeInt(equip.getVicious()); // WTF NEXON ARE YOU SERIOUS?
			w.writeLong(0);
		}
		w.write(new byte[] {0, (byte) 0x40, (byte) 0xE0, (byte) 0xFD, (byte) 0x3B, (byte) 0x37, (byte) 0x4F, 1});
		w.writeInt(-1);
	}

	private static void addInventoryInfo(PacketWriter w, GameCharacter chr) {
		for (byte i = 1; i <= 5; i++) {
			w.writeAsByte(chr.getInventory(InventoryType.fromByte(i)).getSlotLimit());
		}
		w.write(new byte[] {0, (byte) 0x40, (byte) 0xE0, (byte) 0xFD, (byte) 0x3B, (byte) 0x37, (byte) 0x4F, 1});
		Inventory iv = chr.getInventory(InventoryType.EQUIPPED);
		Collection<IItem> equippedC = iv.list();
		List<Item> equipped = new ArrayList<Item>(equippedC.size());
		List<Item> equippedCash = new ArrayList<Item>(equippedC.size());
		for (IItem item : equippedC) {
			if (item.getSlot() <= -100) {
				equippedCash.add((Item) item);
			} else {
				equipped.add((Item) item);
			}
		}
		Collections.sort(equipped);
		for (Item item : equipped) {
			addItemInfo(w, item);
		}
		w.writeAsShort(0); // start of equip cash
		for (Item item : equippedCash) {
			addItemInfo(w, item);
		}
		w.writeAsShort(0); // start of equip inventory
		for (IItem item : chr.getInventory(InventoryType.EQUIP).list()) {
			addItemInfo(w, item);
		}
		w.writeInt(0);
		for (IItem item : chr.getInventory(InventoryType.USE).list()) {
			addItemInfo(w, item);
		}
		w.writeAsByte(0);
		for (IItem item : chr.getInventory(InventoryType.SETUP).list()) {
			addItemInfo(w, item);
		}
		w.writeAsByte(0);
		for (IItem item : chr.getInventory(InventoryType.ETC).list()) {
			addItemInfo(w, item);
		}
		w.writeAsByte(0);
		for (IItem item : chr.getInventory(InventoryType.CASH).list()) {
			addItemInfo(w, item);
		}
	}

	private static void addSkillInfo(PacketWriter w, GameCharacter chr) {
		w.writeAsByte(0); // start of skills
		Map<ISkill, GameCharacter.SkillEntry> skills = chr.getSkills();
		w.writeAsShort(skills.size());
		for (Entry<ISkill, GameCharacter.SkillEntry> skill : skills.entrySet()) {
			w.writeInt(skill.getKey().getId());
			w.writeInt(skill.getValue().skillevel);
			addExpirationTime(w, skill.getValue().expiration);
			if (skill.getKey().isFourthJob()) {
				w.writeInt(skill.getValue().masterlevel);
			}
		}
		w.writeAsShort(chr.getAllCooldowns().size());
		for (PlayerCoolDownValueHolder cooling : chr.getAllCooldowns()) {
			w.writeInt(cooling.skillId);
			int timeLeft = (int) (cooling.length + cooling.startTime - System.currentTimeMillis());
			w.writeAsShort(timeLeft / 1000);
		}
	}

	private static void addMonsterBookInfo(PacketWriter w, GameCharacter chr) {
		w.writeInt(chr.getMonsterBookCover()); // cover
		w.writeAsByte(0);
		Map<Integer, Integer> cards = chr.getMonsterBook().getCards();
		w.writeAsShort(cards.size());
		for (Entry<Integer, Integer> all : cards.entrySet()) {
			w.writeAsShort(all.getKey() % 10000); // Id
			w.writeAsByte(all.getValue()); // Level
		}
	}

	public static GamePacket sendGuestTOS() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SEND_LINK.getValue());
		w.writeAsShort(0x100);
		w.writeInt(Randomizer.nextInt(999999));
		w.writeLong(0);
		w.write(new byte[] {(byte) 0x40, (byte) 0xE0, (byte) 0xFD, (byte) 0x3B, (byte) 0x37, (byte) 0x4F, 1});
		w.writeLong(getKoreanTimestamp(System.currentTimeMillis()));
		w.writeInt(0);
		w.writeLengthString("http://maplefags.com");
		return w.getPacket();
	}

	/**
	 * Sends a hello packet.
	 * 
	 * @param mapleVersion
	 *            The maple client version.
	 * @param sendIv
	 *            the IV used by the server for sending
	 * @param recvIv
	 *            the IV used by the server for receiving
	 * @return
	 */
	public static GamePacket getHello(short mapleVersion, byte[] sendIv, byte[] recvIv) {
		PacketWriter w = new PacketWriter(8);
		w.writeAsShort(0x0E);
		w.writeAsShort(mapleVersion);
		w.writeAsShort(1);
		w.writeAsByte(49);
		w.write(recvIv);
		w.write(sendIv);
		w.writeAsByte(8);
		return w.getPacket();
	}

	/**
	 * Sends a ping packet.
	 * 
	 * @return The packet.
	 */
	public static GamePacket getPing() {
		PacketWriter w = new PacketWriter(2);
		w.writeAsShort(SendOpcode.PING.getValue());
		return w.getPacket();
	}

	/**
	 * Gets a login failed packet.
	 * 
	 * Possible values for <code>reason</code>:<br>
	 * 3: ID deleted or blocked<br>
	 * 4: Incorrect password<br>
	 * 5: Not a registered id<br>
	 * 6: System error<br>
	 * 7: Already logged in<br>
	 * 8: System error<br>
	 * 9: System error<br>
	 * 10: Cannot process so many connections<br>
	 * 11: Only users older than 20 can use this channel<br>
	 * 13: Unable to log on as master at this ip<br>
	 * 14: Wrong gateway or personal info and weird korean button<br>
	 * 15: Processing request with that korean button!<br>
	 * 16: Please verify your account through email...<br>
	 * 17: Wrong gateway or personal info<br>
	 * 21: Please verify your account through email...<br>
	 * 23: License agreement<br>
	 * 25: Maple Europe notice =[ FUCK YOU NEXON<br>
	 * 27: Some weird full client notice, probably for trial versions<br>
	 * 
	 * @param reason
	 *            The reason logging in failed.
	 * @return The login failed packet.
	 */
	public static GamePacket getLoginFailed(int reason) {
		PacketWriter w = new PacketWriter(8);
		w.writeAsShort(SendOpcode.LOGIN_STATUS.getValue());
		w.writeInt(reason);
		w.writeAsShort(0);
		return w.getPacket();
	}

	/**
	 * Gets a login failed packet.
	 * 
	 * Possible values for <code>reason</code>:<br>
	 * 2: ID deleted or blocked<br>
	 * 3: ID deleted or blocked<br>
	 * 4: Incorrect password<br>
	 * 5: Not a registered id<br>
	 * 6: Trouble logging into the game?<br>
	 * 7: Already logged in<br>
	 * 8: Trouble logging into the game?<br>
	 * 9: Trouble logging into the game?<br>
	 * 10: Cannot process so many connections<br>
	 * 11: Only users older than 20 can use this channel<br>
	 * 12: Trouble logging into the game?<br>
	 * 13: Unable to log on as master at this ip<br>
	 * 14: Wrong gateway or personal info and weird korean button<br>
	 * 15: Processing request with that korean button!<br>
	 * 16: Please verify your account through email...<br>
	 * 17: Wrong gateway or personal info<br>
	 * 21: Please verify your account through email...<br>
	 * 23: Crashes<br>
	 * 25: Maple Europe notice =[ FUCK YOU NEXON<br>
	 * 27: Some weird full client notice, probably for trial versions<br>
	 * 
	 * @param reason
	 *            The reason logging in failed.
	 * @return The login failed packet.
	 */
	public static GamePacket getAfterLoginError(int reason) {// same as above
																// o.o
		PacketWriter w = new PacketWriter(8);
		w.writeAsShort(SendOpcode.AFTER_LOGIN_ERROR.getValue());
		w.writeAsShort(reason);// using other types then stated above = CRASH
		return w.getPacket();
	}

	public static GamePacket sendPolice(int reason, String reasoning, int duration) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GM_POLICE.getValue());
		w.writeInt(duration);
		w.writeAsByte(4); // Hmmm
		w.writeAsByte(reason);
		w.writeLengthString(reasoning);
		return w.getPacket();
	}

	public static GamePacket sendPolice(String text) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MAPLE_ADMIN.getValue());
		w.writeLengthString(text);
		return w.getPacket();
	}

	public static GamePacket getPermBan(byte reason) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.LOGIN_STATUS.getValue());
		w.writeAsShort(2); // Account is banned
		w.writeAsByte(0);
		w.writeAsByte(reason);
		w.write(new byte[] {1, 1, 1, 1, 0});

		return w.getPacket();
	}

	public static GamePacket getTempBan(long timestampTill, byte reason) {
		PacketWriter w = new PacketWriter(17);
		w.writeAsShort(SendOpcode.LOGIN_STATUS.getValue());
		w.writeAsByte(2);
		w.write0(5);
		w.writeAsByte(reason);
		w.writeLong(timestampTill); // Tempban date is handled as a 64-bit
										// long, number of 100NS intervals since
										// 1/1/1601. Lulz.

		return w.getPacket();
	}

	/**
	 * Gets a successful authentication and PIN Request packet.
	 * 
	 * @param c
	 * @param account
	 *            The account name.
	 * @return The PIN request packet.
	 */
	public static GamePacket getAuthSuccess(GameClient c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.LOGIN_STATUS.getValue());
		w.writeInt(0);
		w.writeAsShort(0);
		w.writeInt(c.getAccountId()); // user id
		w.writeAsByte(c.getGender());
		w.writeAsByte((c.gmLevel() > 0)); // admin byte
		w.writeAsByte(0);
		w.writeAsByte(0);
		w.writeLengthString(c.getAccountName());
		w.writeAsByte(0);
		w.writeAsByte(0); // isquietbanned
		w.writeLong(0);
		w.writeLong(0); // creation time
		w.writeInt(0);
		w.writeAsShort(2);// PIN

		return w.getPacket();
	}

	/**
	 * Gets a packet detailing a PIN operation.
	 * 
	 * Possible values for <code>mode</code>:<br>
	 * 0 - PIN was accepted<br>
	 * 1 - Register a new PIN<br>
	 * 2 - Invalid pin / Reenter<br>
	 * 3 - Connection failed due to system error<br>
	 * 4 - Enter the pin
	 * 
	 * @param mode
	 *            The mode.
	 * @return
	 */
	private static GamePacket pinOperation(byte mode) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PIN_OPERATION.getValue());
		w.writeAsByte(mode);
		return w.getPacket();
	}

	public static GamePacket pinRegistered() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PIN_ASSIGNED.getValue());
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket requestPin() {
		return pinOperation((byte) 4);
	}

	public static GamePacket requestPinAfterFailure() {
		return pinOperation((byte) 2);
	}

	public static GamePacket registerPin() {
		return pinOperation((byte) 1);
	}

	public static GamePacket pinAccepted() {
		return pinOperation((byte) 0);
	}

	public static GamePacket wrongPic() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.WRONG_PIC.getValue());
		w.writeAsByte(0);
		return w.getPacket();
	}

	/**
	 * Gets a packet detailing a server and its channels.
	 * 
	 * @param serverId
	 * @param serverName
	 *            The name of the server.
	 * @param channelLoad
	 *            Load of the channel - 1200 seems to be max.
	 * @return The server info packet.
	 */
	public static GamePacket getServerList(byte serverId, String serverName, byte flag, String eventmsg, Map<Byte, AtomicInteger> channelLoad) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SERVERLIST.getValue());
		w.writeAsByte(serverId);
		w.writeLengthString(serverName);
		w.writeAsByte(flag);
		w.writeLengthString(eventmsg);
		w.writeAsByte(0x64); // rate modifier, don't ask O.O!
		w.writeAsByte(0x0); // event xp * 2.6 O.O!
		w.writeAsByte(0x64); // rate modifier, don't ask O.O!
		w.writeAsByte(0x0); // drop rate * 2.6
		w.writeAsByte(0x0);
		int lastChannel = 1;
		Set<Byte> channels = channelLoad.keySet();
		for (byte i = 30; i > 0; i--) {
			if (channels.contains(i)) {
				lastChannel = i;
				break;
			}
		}
		w.writeAsByte(lastChannel);
		int load;
		for (byte i = 1; i <= lastChannel; i++) {
			if (channels.contains(i)) {
				load = (channelLoad.get(i).get() * 1200) / ServerConstants.CHANNEL_LOAD; // lolwut
			} else {
				load = ServerConstants.CHANNEL_LOAD;
			}
			w.writeLengthString(serverName + "-" + i);
			w.writeInt(load);
			w.writeAsByte(1);
			w.writeAsShort(i - 1);
		}
		w.writeAsShort(0);
		return w.getPacket();
	}

	/**
	 * Gets a packet saying that the server list is over.
	 * 
	 * @return The end of server list packet.
	 */
	public static GamePacket getEndOfServerList() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.SERVERLIST.getValue());
		w.writeAsByte(0xFF);
		return w.getPacket();
	}

	/**
	 * Gets a packet detailing a server status message.
	 * 
	 * Possible values for <code>status</code>:<br>
	 * 0 - Normal<br>
	 * 1 - Highly populated<br>
	 * 2 - Full
	 * 
	 * @param status
	 *            The server status.
	 * @return The server status packet.
	 */
	public static GamePacket getServerStatus(int status) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.SERVERSTATUS.getValue());
		w.writeAsShort(status);
		return w.getPacket();
	}

	/**
	 * Gets a packet telling the client the IP of the channel server.
	 * 
	 * @param inetAddr
	 *            The InetAddress of the requested channel server.
	 * @param port
	 *            The port the channel is on.
	 * @param clientId
	 *            The ID of the client.
	 * @return The server IP packet.
	 */
	public static GamePacket getServerIP(InetAddress inetAddr, int port, int clientId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SERVER_IP.getValue());
		w.writeAsShort(0);
		byte[] addr = inetAddr.getAddress();
		w.write(addr);
		w.writeAsShort(port);
		w.writeInt(clientId);
		w.write(new byte[] {0, 0, 0, 0, 0});
		return w.getPacket();
	}

	/**
	 * Gets a packet telling the client the IP of the new channel.
	 * 
	 * @param inetAddr
	 *            The InetAddress of the requested channel server.
	 * @param port
	 *            The port the channel is on.
	 * @return The server IP packet.
	 */
	public static GamePacket getChannelChange(InetAddress inetAddr, int port) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CHANGE_CHANNEL.getValue());
		w.writeAsByte(1);
		byte[] addr = inetAddr.getAddress();
		w.write(addr);
		w.writeAsShort(port);
		return w.getPacket();
	}

	/**
	 * Gets a packet with a list of characters.
	 * 
	 * @param c
	 *            The MapleClient to load characters of.
	 * @param serverId
	 *            The ID of the server requested.
	 * @return The character list packet.
	 */
	public static GamePacket getCharList(GameClient c, int serverId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CHARLIST.getValue());
		w.writeAsByte(0);
		List<GameCharacter> chars = c.loadCharacters(serverId);
		byte length = (byte) chars.size();
		for (GameCharacter chr : chars) {
			if (chr.isHardcoreDead()) {
				length--;
			}
		}
		w.writeAsByte(length);
		for (GameCharacter chr : chars) {
			addCharEntry(w, chr, false);
		}
		if (ServerConstants.ENABLE_PIC) {
			final boolean hasPic = c.getPic() != null && c.getPic().length() > 0;
			w.writeAsByte(hasPic);
		} else {
			w.writeAsByte(2);
		}

		w.writeInt(c.getCharacterSlots());
		return w.getPacket();
	}

	public static GamePacket enableTV() {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.ENABLE_TV.getValue());
		w.writeInt(0);
		w.writeAsByte(0);
		return w.getPacket();
	}

	/**
	 * Removes TV
	 * 
	 * @return The Remove TV Packet
	 */
	public static GamePacket removeTV() {
		PacketWriter w = new PacketWriter(2);
		w.writeAsShort(SendOpcode.REMOVE_TV.getValue());
		return w.getPacket();
	}

	/**
	 * Sends MapleTV
	 * 
	 * @param chr
	 *            The character shown in TV
	 * @param messages
	 *            The message sent with the TV
	 * @param type
	 *            The type of TV
	 * @param partner
	 *            The partner shown with chr
	 * @return the SEND_TV packet
	 */
	public static GamePacket sendTV(GameCharacter chr, List<String> messages, int type, GameCharacter partner) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SEND_TV.getValue());
		w.writeAsByte(partner != null ? 3 : 1);
		w.writeAsByte(type); // Heart = 2 Star = 1 Normal = 0
		addCharLook(w, chr, false);
		w.writeLengthString(chr.getName());
		if (partner != null) {
			w.writeLengthString(partner.getName());
		} else {
			w.writeAsShort(0);
		}
		for (int i = 0; i < messages.size(); i++) {
			if (i == 4 && messages.get(4).length() > 15) {
				w.writeLengthString(messages.get(4).substring(0, 15));
			} else {
				w.writeLengthString(messages.get(i));
			}
		}
		w.writeInt(1337); // time limit shit lol 'Your thing still start in
								// blah blah seconds'
		if (partner != null) {
			addCharLook(w, partner, false);
		}
		return w.getPacket();
	}

	/**
	 * Gets character info for a character.
	 * 
	 * @param chr
	 *            The character to get info about.
	 * @return The character info packet.
	 */
	public static GamePacket getCharInfo(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.WARP_TO_MAP.getValue());
		w.writeInt(chr.getClient().getChannel() - 1);
		w.writeAsByte(1);
		w.writeAsByte(1);
		w.writeAsShort(0);
		for (int i = 0; i < 3; i++) {
			w.writeInt(Randomizer.nextInt());
		}
		addCharacterInfo(w, chr);
		w.writeLong(getTime(System.currentTimeMillis()));
		return w.getPacket();
	}

	/**
	 * Gets an empty stat update.
	 * 
	 * @return The empy stat update packet.
	 */
	public static GamePacket enableActions() {
		return updatePlayerStats(EMPTY_STATUPDATE, true);
	}

	/**
	 * Gets an update for specified stats.
	 * 
	 * @param stats
	 *            The stats to update.
	 * @return The stat update packet.
	 */
	public static GamePacket updatePlayerStats(List<StatDelta> stats) {
		return updatePlayerStats(stats, false);
	}

	/**
	 * Gets an update for specified stats.
	 * 
	 * @param stats
	 *            The list of stats to update.
	 * @param isItemReaction
	 *            Result of an item reaction(?)
	 * @return The stat update packet.
	 */
	public static GamePacket updatePlayerStats(List<StatDelta> stats, boolean isItemReaction) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_STATS.getValue());
		w.writeAsByte(isItemReaction);
		int updateMask = 0;
		for (StatDelta statupdate : stats) {
			updateMask |= statupdate.stat.getValue();
		}
		List<StatDelta> mystats = stats;
		if (mystats.size() > 1) {
			Collections.sort(mystats, new Comparator<StatDelta>() {

				@Override
				public int compare(StatDelta o1, StatDelta o2) {
					int val1 = o1.stat.getValue();
					int val2 = o2.stat.getValue();
					return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
				}
			});
		}
		w.writeInt(updateMask);
		for (StatDelta statupdate : mystats) {
			if (statupdate.stat.getValue() >= 1) {
				if (statupdate.stat.getValue() == 0x1) {
					w.writeAsShort(statupdate.delta);
				} else if (statupdate.stat.getValue() <= 0x4) {
					w.writeInt(statupdate.delta);
				} else if (statupdate.stat.getValue() < 0x20) {
					w.writeAsByte(statupdate.delta);
				} else if (statupdate.stat.getValue() < 0xFFFF) {
					w.writeAsShort(statupdate.delta);
				} else {
					w.writeInt(statupdate.delta);
				}
			}
		}
		return w.getPacket();
	}

	/**
	 * Gets a packet telling the client to change maps.
	 * 
	 * @param to
	 *            The <code>GameMap</code> to warp to.
	 * @param spawnPoint
	 *            The spawn portal number to spawn at.
	 * @param chr
	 *            The character warping to <code>to</code>
	 * @return The map change packet.
	 */
	public static GamePacket getWarpToMap(GameMap to, int spawnPoint, GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.WARP_TO_MAP.getValue());
		w.writeInt(chr.getClient().getChannel() - 1);
		w.writeInt(0);// updated
		w.writeAsByte(0);// updated
		w.writeInt(to.getId());
		w.writeAsByte(spawnPoint);
		w.writeAsShort(chr.getHp());
		w.writeAsByte(0);
		w.writeLong(getTime(System.currentTimeMillis()));
		return w.getPacket();
	}

	/**
	 * Gets a packet to spawn a portal.
	 * 
	 * @param townId
	 *            The ID of the town the portal goes to.
	 * @param targetId
	 *            The ID of the target.
	 * @param pos
	 *            Where to put the portal.
	 * @return The portal spawn packet.
	 */
	public static GamePacket spawnPortal(int townId, int targetId, Point pos) {
		PacketWriter w = new PacketWriter(14);
		w.writeAsShort(SendOpcode.SPAWN_PORTAL.getValue());
		w.writeInt(townId);
		w.writeInt(targetId);
		if (pos != null) {
			w.writeVector(pos);
		}
		return w.getPacket();
	}

	/**
	 * Gets a packet to spawn a door.
	 * 
	 * @param oid
	 *            The door's object ID.
	 * @param pos
	 *            The position of the door.
	 * @param isTown
	 * @return The remove door packet.
	 */
	public static GamePacket spawnDoor(int oid, Point pos, boolean isTown) {
		PacketWriter w = new PacketWriter(11);
		w.writeAsShort(SendOpcode.SPAWN_DOOR.getValue());
		w.writeAsByte(isTown);
		w.writeInt(oid);
		w.writeVector(pos);
		return w.getPacket();
	}

	/**
	 * Gets a packet to remove a door.
	 * 
	 * @param oid
	 *            The door's ID.
	 * @param town
	 * @return The remove door packet.
	 */
	public static GamePacket removeDoor(int oid, boolean town) {
		PacketWriter w = new PacketWriter(10);
		if (town) {
			w.writeAsShort(SendOpcode.SPAWN_PORTAL.getValue());
			w.writeInt(999999999);
			w.writeInt(999999999);
		} else {
			w.writeAsShort(SendOpcode.REMOVE_DOOR.getValue());
			w.writeAsByte(0);
			w.writeInt(oid);
		}
		return w.getPacket();
	}

	/**
	 * Gets a packet to spawn a special map object.
	 * 
	 * @param summon
	 * @param skillLevel
	 *            The level of the skill used.
	 * @param animated
	 *            Animated spawn?
	 * @return The spawn packet for the map object.
	 */
	public static GamePacket spawnSummon(Summon summon, boolean animated) {
		PacketWriter w = new PacketWriter(25);
		w.writeAsShort(SendOpcode.SPAWN_SPECIAL_MAPOBJECT.getValue());
		w.writeInt(summon.getOwner().getId());
		w.writeInt(summon.getObjectId());
		w.writeInt(summon.getSkill());
		w.writeAsByte(0x0A); // v83
		w.writeAsByte(summon.getSkillLevel());
		w.writeVector(summon.getPosition());
		w.write0(3);
		w.writeAsByte(summon.getMovementType().getValue()); // 0 = don't move, 1 =
															// follow (4th mage
															// summons?), 2/4 =
															// only tele follow,
															// 3 = bird follow
		w.writeAsByte(!summon.isPuppet()); // 0 and the summon can't attack
												// - but puppets don't attack
												// with 1 either ^.-
		w.writeAsByte(!animated);
		return w.getPacket();
	}

	/**
	 * Gets a packet to remove a special map object.
	 * 
	 * @param summon
	 * @param animated
	 *            Animated removal?
	 * @return The packet removing the object.
	 */
	public static GamePacket removeSummon(Summon summon, boolean animated) {
		PacketWriter w = new PacketWriter(11);
		w.writeAsShort(SendOpcode.REMOVE_SPECIAL_MAPOBJECT.getValue());
		w.writeInt(summon.getOwner().getId());
		w.writeInt(summon.getObjectId());
		w.writeAsByte(animated ? 4 : 1); // ?
		return w.getPacket();
	}

	/**
	 * Gets the response to a relog request.
	 * 
	 * @return The relog response packet.
	 */
	public static GamePacket getRelogResponse() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.RELOG_RESPONSE.getValue());
		w.writeAsByte(1);// 1 O.O Must be more types ):
		return w.getPacket();
	}

	/**
	 * Gets a server message packet.
	 * 
	 * @param message
	 *            The message to convey.
	 * @return The server message packet.
	 */
	public static GamePacket serverMessage(String message) {
		return serverMessage(4, (byte) 0, message, true, false);
	}

	/**
	 * Gets a server notice packet.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: [Notice]<br>
	 * 1: Popup<br>
	 * 2: Megaphone<br>
	 * 3: Super Megaphone<br>
	 * 4: Scrolling message at top<br>
	 * 5: Pink Text<br>
	 * 6: Lightblue Text
	 * 
	 * @param type
	 *            The type of the notice.
	 * @param message
	 *            The message to convey.
	 * @return The server notice packet.
	 */
	public static GamePacket serverNotice(int type, String message) {
		return serverMessage(type, (byte) 0, message, false, false);
	}

	/**
	 * Gets a server notice packet.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: [Notice]<br>
	 * 1: Popup<br>
	 * 2: Megaphone<br>
	 * 3: Super Megaphone<br>
	 * 4: Scrolling message at top<br>
	 * 5: Pink Text<br>
	 * 6: Lightblue Text
	 * 
	 * @param type
	 *            The type of the notice.
	 * @param channel
	 *            The channel this notice was sent on.
	 * @param message
	 *            The message to convey.
	 * @return The server notice packet.
	 */
	public static GamePacket serverNotice(int type, byte channel, String message) {
		return serverMessage(type, channel, message, false, false);
	}

	public static GamePacket serverNotice(int type, byte channel, String message, boolean smegaEar) {
		return serverMessage(type, channel, message, false, smegaEar);
	}

	/**
	 * Gets a server message packet.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: [Notice]<br>
	 * 1: Popup<br>
	 * 2: Megaphone<br>
	 * 3: Super Megaphone<br>
	 * 4: Scrolling message at top<br>
	 * 5: Pink Text<br>
	 * 6: Lightblue Text
	 * 
	 * @param type
	 *            The type of the notice.
	 * @param channel
	 *            The channel this notice was sent on.
	 * @param message
	 *            The message to convey.
	 * @param servermessage
	 *            Is this a scrolling ticker?
	 * @return The server notice packet.
	 */
	private static GamePacket serverMessage(int type, byte channel, String message, boolean servermessage, boolean showMegaphoneEar) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SERVERMESSAGE.getValue());
		w.writeAsByte(type);
		if (servermessage) {
			w.writeAsByte(1);
		}
		w.writeLengthString(message);
		if (type == 3) {
			w.writeAsByte(channel - 1); // channel
			w.writeAsByte(showMegaphoneEar);
		} else if (type == 6) {
			w.writeInt(0);
		}
		return w.getPacket();
	}

	/**
	 * Sends a Avatar Super Megaphone packet.
	 * 
	 * @param chr
	 *            The character name.
	 * @param medal
	 *            The medal text.
	 * @param channel
	 *            Which channel.
	 * @param itemId
	 *            Which item used.
	 * @param message
	 *            The message sent.
	 * @param showMegaphoneEar
	 *            Whether or not the ear is shown for whisper.
	 * @return
	 */
	public static GamePacket getAvatarMega(GameCharacter chr, String medal, byte channel, int itemId, List<String> message, boolean showMegaphoneEar) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.AVATAR_MEGA.getValue());
		w.writeInt(itemId);
		w.writeLengthString(medal + chr.getName());
		for (String s : message) {
			w.writeLengthString(s);
		}
		w.writeInt(channel - 1); // channel
		w.writeAsByte(showMegaphoneEar);
		addCharLook(w, chr, true);
		return w.getPacket();
	}

	/**
	 * Sends the Gachapon green message when a user uses a gachapon ticket.
	 * 
	 * @param item
	 * @param town
	 * @param player
	 * @return
	 */
	public static GamePacket gachaponMessage(IItem item, String town, GameCharacter player) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SERVERMESSAGE.getValue());
		w.writeAsByte(0x0B);
		w.writeLengthString(player.getName() + " : got a(n)");
		w.writeInt(0); // random?
		w.writeLengthString(town);
		addItemInfo(w, item, true);
		return w.getPacket();
	}

	public static GamePacket spawnNPC(Npc life) {
		PacketWriter w = new PacketWriter(24);
		w.writeAsShort(SendOpcode.SPAWN_NPC.getValue());
		w.writeInt(life.getObjectId());
		w.writeInt(life.getId());
		w.writeAsShort(life.getPosition().x);
		w.writeAsShort(life.getCy());
		if (life.getF() == 1) {
			w.writeAsByte(0);
		} else {
			w.writeAsByte(1);
		}
		w.writeAsShort(life.getFh());
		w.writeAsShort(life.getRx0());
		w.writeAsShort(life.getRx1());
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket spawnNPCRequestController(Npc life, boolean onMiniMap) {
		PacketWriter w = new PacketWriter(23);
		w.writeAsShort(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
		w.writeAsByte(1);
		w.writeInt(life.getObjectId());
		w.writeInt(life.getId());
		w.writeAsShort(life.getPosition().x);
		w.writeAsShort(life.getCy());
		if (life.getF() == 1) {
			w.writeAsByte(0);
		} else {
			w.writeAsByte(1);
		}
		w.writeAsShort(life.getFh());
		w.writeAsShort(life.getRx0());
		w.writeAsShort(life.getRx1());
		w.writeAsByte(onMiniMap);
		return w.getPacket();
	}
	
	/**
	 * Makes any NPC in the game scriptable.
	 * @param npcId - The NPC's ID, found in WZ files/MCDB
	 * @param description - If the NPC has quests, this will be the text of the menu item
	 * @return 
	 */
    public static GamePacket setNPCScriptable(int npcId, String description) {
        PacketWriter w = new PacketWriter();
        w.writeAsShort(SendOpcode.SET_NPC_SCRIPTABLE.getValue());
        w.writeAsByte(1); // following structure is repeated n times
        w.writeInt(npcId);
        w.writeLengthString(description);
        w.writeInt(0); // start time
        w.writeInt(Integer.MAX_VALUE); // end time
        return w.getPacket();
    }
    
    /**
	 * Makes a list of any NPCs in the game scriptable.
	 * @param entries - a list of pairs of NPC IDs and descriptions.
	 * @return 
	 */
    public static GamePacket setNPCScriptable(List<NpcDescriptionEntry> entries) {
    	PacketWriter w = new PacketWriter();
    	w.writeAsShort(SendOpcode.SET_NPC_SCRIPTABLE.getValue());
    	w.writeAsByte(entries.size()); // following structure is repeated n times
    	for (NpcDescriptionEntry entry : entries) {
    		w.writeInt(entry.npcId);
    		w.writeLengthString(entry.description);
    		w.writeInt(0); // start time
    		w.writeInt(Integer.MAX_VALUE); // end time
    	}
    	return w.getPacket();
    }

	/**
	 * Gets a spawn monster packet.
	 * 
	 * @param life
	 *            The monster to spawn.
	 * @param newSpawn
	 *            Is it a new spawn?
	 * @return The spawn monster packet.
	 */
	public static GamePacket spawnMonster(Monster life, boolean newSpawn) {
		return spawnMonsterInternal(life, false, newSpawn, false, 0, false);
	}

	/**
	 * Gets a spawn monster packet.
	 * 
	 * @param life
	 *            The monster to spawn.
	 * @param newSpawn
	 *            Is it a new spawn?
	 * @param effect
	 *            The spawn effect.
	 * @return The spawn monster packet.
	 */
	public static GamePacket spawnMonster(Monster life, boolean newSpawn, int effect) {
		return spawnMonsterInternal(life, false, newSpawn, false, effect, false);
	}

	/**
	 * Gets a control monster packet.
	 * 
	 * @param life
	 *            The monster to give control to.
	 * @param newSpawn
	 *            Is it a new spawn?
	 * @param aggro
	 *            Aggressive monster?
	 * @return The monster control packet.
	 */
	public static GamePacket controlMonster(Monster life, boolean newSpawn, boolean aggro) {
		return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false);
	}

	/**
	 * Makes a monster invisible for Ariant PQ.
	 * 
	 * @param life
	 * @return
	 */
	public static GamePacket makeMonsterInvisible(Monster life) {
		return spawnMonsterInternal(life, true, false, false, 0, true);
	}

	/**
	 * Internal function to handler monster spawning and controlling.
	 * 
	 * @param life
	 *            The mob to perform operations with.
	 * @param requestController
	 *            Requesting control of mob?
	 * @param newSpawn
	 *            New spawn (fade in?)
	 * @param aggro
	 *            Aggressive mob?
	 * @param effect
	 *            The spawn effect to use.
	 * @return The spawn/control packet.
	 */
	private static GamePacket spawnMonsterInternal(Monster life, boolean requestController, boolean newSpawn, boolean aggro, int effect, boolean makeInvis) {
		PacketWriter w = new PacketWriter();
		if (makeInvis) {
			w.writeAsShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
			w.writeAsByte(0);
			w.writeInt(life.getObjectId());
			return w.getPacket();
		}
		if (requestController) {
			w.writeAsShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
			w.writeAsByte(aggro ? 2 : 1);
		} else {
			w.writeAsShort(SendOpcode.SPAWN_MONSTER.getValue());
		}
		w.writeInt(life.getObjectId());
		w.writeAsByte(life.getController() == null ? 5 : 1);
		w.writeInt(life.getId());
		w.write0(15);
		w.writeAsByte(0x88);
		w.write0(6);
		w.writeVector(life.getPosition());
		w.writeAsByte(life.getStance());
		w.writeAsShort(0); // Origin FH //life.getStartFh()
		w.writeAsShort(life.getFh());

		if (effect > 0) {
			w.writeAsByte(effect);
			w.writeAsByte(0);
			w.writeAsShort(0);
			if (effect == 15) {
				w.writeAsByte(0);
			}
		}
		w.writeAsByte(newSpawn ? -2 : -1);
		w.writeAsByte(life.getTeam());
		w.writeInt(0);
		return w.getPacket();
	}

	/**
	 * Handles monsters not being targettable, such as Zakum's first body.
	 * 
	 * @param life
	 *            The mob to spawn as non-targettable.
	 * @param effect
	 *            The effect to show when spawning.
	 * @return The packet to spawn the mob as non-targettable.
	 */
	public static GamePacket spawnFakeMonster(Monster life, int effect) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
		w.writeAsByte(1);
		w.writeInt(life.getObjectId());
		w.writeAsByte(5);
		w.writeInt(life.getId());
		w.write0(15);
		w.writeAsByte(0x88);
		w.write0(6);
		w.writeVector(life.getPosition());
		w.writeAsByte(life.getStance());
		w.writeAsShort(0);// life.getStartFh()
		w.writeAsShort(life.getFh());
		if (effect > 0) {
			w.writeAsByte(effect);
			w.writeAsByte(0);
			w.writeAsShort(0);
		}
		w.writeAsShort(-2);
		w.writeAsByte(life.getTeam());
		w.writeInt(0);
		return w.getPacket();
	}

	/**
	 * Makes a monster previously spawned as non-targettable, targettable.
	 * 
	 * @param life
	 *            The mob to make targettable.
	 * @return The packet to make the mob targettable.
	 */
	public static GamePacket makeMonsterReal(Monster life) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_MONSTER.getValue());
		w.writeInt(life.getObjectId());
		w.writeAsByte(5);
		w.writeInt(life.getId());
		w.write0(15);
		w.writeAsByte(0x88);
		w.write0(6);
		w.writeVector(life.getPosition());
		w.writeAsByte(life.getStance());
		w.writeAsShort(0);// life.getStartFh()
		w.writeAsShort(life.getFh());
		w.writeAsShort(-1);
		w.writeInt(0);
		return w.getPacket();
	}

	/**
	 * Gets a stop control monster packet.
	 * 
	 * @param oid
	 *            The ObjectID of the monster to stop controlling.
	 * @return The stop control monster packet.
	 */
	public static GamePacket stopControllingMonster(int oid) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.SPAWN_MONSTER_CONTROL.getValue());
		w.writeAsByte(0);
		w.writeInt(oid);
		return w.getPacket();
	}

	/**
	 * Gets a response to a move monster packet.
	 * 
	 * @param objectid
	 *            The ObjectID of the monster being moved.
	 * @param moveid
	 *            The movement ID.
	 * @param currentMp
	 *            The current MP of the monster.
	 * @param useSkills
	 *            Can the monster use skills?
	 * @return The move response packet.
	 */
	public static GamePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
		return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
	}

	/**
	 * Gets a response to a move monster packet.
	 * 
	 * @param objectid
	 *            The ObjectID of the monster being moved.
	 * @param moveid
	 *            The movement ID.
	 * @param currentMp
	 *            The current MP of the monster.
	 * @param useSkills
	 *            Can the monster use skills?
	 * @param skillId
	 *            The skill ID for the monster to use.
	 * @param skillLevel
	 *            The level of the skill to use.
	 * @return The move response packet.
	 */
	public static GamePacket moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
		PacketWriter w = new PacketWriter(13);
		w.writeAsShort(SendOpcode.MOVE_MONSTER_RESPONSE.getValue());
		w.writeInt(objectid);
		w.writeAsShort(moveid);
		w.writeAsByte(useSkills);
		w.writeAsShort(currentMp);
		w.writeAsByte(skillId);
		w.writeAsByte(skillLevel);
		return w.getPacket();
	}

	/**
	 * Gets a general chat packet.
	 * 
	 * @param cidfrom
	 *            The character ID who sent the chat.
	 * @param text
	 *            The text of the chat.
	 * @param whiteBG
	 * @param show
	 * @return The general chat packet.
	 */
	public static GamePacket getChatText(int cidfrom, String text, boolean isGm, int show) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CHATTEXT.getValue());
		w.writeInt(cidfrom);
		w.writeAsByte(isGm);
		w.writeLengthString(text);
		w.writeAsByte(show);
		return w.getPacket();
	}

	/**
	 * Gets a packet telling the client to show an EXP increase.
	 * 
	 * @param gain
	 *            The amount of EXP gained.
	 * @param isInChat
	 *            In the chat box?
	 * @param isWhite
	 *            White text or yellow?
	 * @return The exp gained packet.
	 */
	public static GamePacket getShowExpGain(int gain, int equip, boolean isInChat, boolean isWhite) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
		w.writeAsByte(isWhite);
		w.writeInt(gain);
		w.writeAsByte(isInChat);
		w.writeInt(0); // monster book bonus (Bonus Event Exp)
		w.writeAsShort(0); // Weird stuff
		w.writeInt(0); // wedding bonus
		w.writeAsByte(0); // 0 = party bonus, 1 = Bonus Event party Exp () x0
		w.writeInt(0); // party bonus
		w.writeInt(equip); // equip bonus
		w.writeInt(0); // Internet Cafe Bonus
		w.writeInt(0); // Rainbow Week Bonus
		if (isInChat) {
			w.writeAsByte(0);
		}
		return w.getPacket();
	}

	/**
	 * Gets a packet telling the client to show a fame gain.
	 * 
	 * @param gain
	 *            How many fame gained.
	 * @return The meso gain packet.
	 */
	public static GamePacket getShowFameGain(int gain) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(4);
		w.writeInt(gain);
		return w.getPacket();
	}

	/**
	 * Gets a packet telling the client to show a meso gain.
	 * 
	 * @param gain
	 *            How many mesos gained.
	 * @return The meso gain packet.
	 */
	public static GamePacket getShowMesoGain(int gain) {
		return getShowMesoGain(gain, false);
	}

	/**
	 * Gets a packet telling the client to show a meso gain.
	 * 
	 * @param gain
	 *            How many mesos gained.
	 * @param inChat
	 *            Show in the chat window?
	 * @return The meso gain packet.
	 */
	public static GamePacket getShowMesoGain(int gain, boolean inChat) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		if (!inChat) {
			w.writeAsByte(0);
			w.writeAsShort(1); // v83
		} else {
			w.writeAsByte(5);
		}
		w.writeInt(gain);
		w.writeAsShort(0);
		return w.getPacket();
	}

	/**
	 * Gets a packet telling the client to show a item gain.
	 * 
	 * @param itemId
	 *            The ID of the item gained.
	 * @param quantity
	 *            How many items gained.
	 * @return The item gain packet.
	 */
	public static GamePacket getShowItemGain(int itemId, short quantity) {
		return getShowItemGain(itemId, quantity, false);
	}

	/**
	 * Gets a packet telling the client to show an item gain.
	 * 
	 * @param itemId
	 *            The ID of the item gained.
	 * @param quantity
	 *            The number of items gained.
	 * @param inChat
	 *            Show in the chat window?
	 * @return The item gain packet.
	 */
	public static GamePacket getShowItemGain(int itemId, short quantity, boolean inChat) {
		PacketWriter w = new PacketWriter();
		if (inChat) {
			w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
			w.writeAsByte(3);
			w.writeAsByte(1);
			w.writeInt(itemId);
			w.writeInt(quantity);
		} else {
			w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
			w.writeAsShort(0);
			w.writeInt(itemId);
			w.writeInt(quantity);
			w.writeInt(0);
			w.writeInt(0);
		}
		return w.getPacket();
	}

	public static GamePacket killMonster(int oid, boolean animation) {
		return killMonster(oid, animation);
	}

	/**
	 * Gets a packet telling the client that a monster was killed.
	 * 
	 * @param oid
	 *            The objectID of the killed monster.
	 * @param animation
	 *            0 = dissapear, 1 = fade out, 2+ = special
	 * @return The kill monster packet.
	 */
	public static GamePacket killMonster(int oid, int animation) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.KILL_MONSTER.getValue());
		w.writeInt(oid);
		w.writeAsByte(animation);
		w.writeAsByte(animation);
		return w.getPacket();
	}

	public static GamePacket dropItemFromMapObject(GameMapItem drop, Point dropfrom, Point dropto, byte mod) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
		w.writeAsByte(mod);
		w.writeInt(drop.getObjectId());
		w.writeAsByte(drop.getMeso() > 0); // 1 mesos, 0 item, 2 and above
													// all item meso bag,
		w.writeInt(drop.getItemId()); // drop object ID
		w.writeInt(drop.getOwner()); // owner charid/paryid :)
		w.writeAsByte(drop.getDropType()); // 0 = timeout for non-owner, 1 =
											// timeout for non-owner's party, 2
											// = FFA, 3 = explosive/FFA
		w.writeVector(dropto);
		w.writeInt(drop.getDropType() == 0 ? drop.getOwner() : 0); // test

		if (mod != 2) {
			w.writeVector(dropfrom);
			w.writeAsShort(0);// Fh?
		}
		if (drop.getMeso() == 0) {
			addExpirationTime(w, drop.getItem().getExpiration(), true);
		}
		w.writeAsByte(!drop.isPlayerDrop()); // pet EQP pickup
		return w.getPacket();
	}

	/**
	 * Gets a packet spawning a player as a mapobject to other clients.
	 * 
	 * @param chr
	 *            The character to spawn to other clients.
	 * @return The spawn player packet.
	 */
	public static GamePacket spawnPlayerMapObject(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_PLAYER.getValue());
		w.writeInt(chr.getId());
		w.writeAsByte(chr.getLevel()); // v83
		w.writeLengthString(chr.getName());
		if (chr.getGuildId() < 1) {
			w.writeLengthString("");
			w.write(new byte[6]);
		} else {
			GuildSummary gs = chr.getClient().getWorldServer().getGuildSummary(chr.getGuildId());
			if (gs != null) {
				w.writeLengthString(gs.getName());
				w.writeAsShort(gs.getLogoBG());
				w.writeAsByte(gs.getLogoBGColor());
				w.writeAsShort(gs.getLogo());
				w.writeAsByte(gs.getLogoColor());
			} else {
				w.writeLengthString("");
				w.write(new byte[6]);
			}
		}
		w.writeInt(0);
		w.writeAsShort(0); // v83
		w.writeAsByte(0xFC);
		w.writeAsByte(1);
		if (chr.getBuffedValue(BuffStat.MORPH) != null) {
			w.writeInt(2);
		} else {
			w.writeInt(0);
		}
		long buffmask = 0;
		Integer buffvalue = null;
		if (chr.getBuffedValue(BuffStat.DARKSIGHT) != null && !chr.isHidden()) {
			buffmask |= BuffStat.DARKSIGHT.getValue();
		}
		if (chr.getBuffedValue(BuffStat.COMBO) != null) {
			buffmask |= BuffStat.COMBO.getValue();
			buffvalue = Integer.valueOf(chr.getBuffedValue(BuffStat.COMBO).intValue());
		}
		if (chr.getBuffedValue(BuffStat.SHADOWPARTNER) != null) {
			buffmask |= BuffStat.SHADOWPARTNER.getValue();
		}
		if (chr.getBuffedValue(BuffStat.SOULARROW) != null) {
			buffmask |= BuffStat.SOULARROW.getValue();
		}
		if (chr.getBuffedValue(BuffStat.MORPH) != null) {
			buffvalue = Integer.valueOf(chr.getBuffedValue(BuffStat.MORPH).intValue());
		}
		if (chr.getBuffedValue(BuffStat.ENERGY_CHARGE) != null) {
			buffmask |= BuffStat.ENERGY_CHARGE.getValue();
			buffvalue = Integer.valueOf(chr.getBuffedValue(BuffStat.ENERGY_CHARGE).intValue());
		}// AREN'T THESE
		w.writeInt((int) ((buffmask >> 32) & 0xffffffffL));
		if (buffvalue != null) {
			if (chr.getBuffedValue(BuffStat.MORPH) != null) { // TEST
				w.writeAsShort(buffvalue);
			} else {
				w.writeAsByte(buffvalue.byteValue());
			}
		}
		w.writeInt((int) (buffmask & 0xffffffffL));
		int CHAR_MAGIC_SPAWN = Randomizer.nextInt();
		w.write0(6);
		w.writeInt(CHAR_MAGIC_SPAWN);
		w.write0(11);
		w.writeInt(CHAR_MAGIC_SPAWN);// v74
		w.write0(11);
		w.writeInt(CHAR_MAGIC_SPAWN);
		w.writeAsShort(0);
		w.writeAsByte(0);
		IItem mount = chr.getInventory(InventoryType.EQUIPPED).getItem((byte) -18);
		if (chr.getBuffedValue(BuffStat.MONSTER_RIDING) != null && mount != null) {
			w.writeInt(mount.getItemId());
			w.writeInt(1004);
		} else {
			w.writeLong(0);
		}
		w.writeInt(CHAR_MAGIC_SPAWN);
		w.write0(9);
		w.writeInt(CHAR_MAGIC_SPAWN);
		w.writeAsShort(0);
		w.writeInt(0); // actually not 0, why is it 0 then?
		w.write0(10);
		w.writeInt(CHAR_MAGIC_SPAWN);
		w.write0(13);
		w.writeInt(CHAR_MAGIC_SPAWN);
		w.writeAsShort(0);
		w.writeAsByte(0);
		w.writeAsShort(chr.getJob().getId());
		addCharLook(w, chr, false);
		w.writeInt(chr.getInventory(InventoryType.CASH).countById(5110000));
		w.writeInt(chr.getItemEffect());
		w.writeInt(chr.getChair());
		w.writeVector(chr.getPosition());
		w.writeAsByte(chr.getStance());
		w.writeAsShort(0);// chr.getFh()
		w.writeAsByte(0);
		Pet[] pet = chr.getPets();
		for (int i = 0; i < 3; i++) {
			if (pet[i] != null) {
				addPetInfo(w, pet[i], false);
			}
		}
		w.writeAsByte(0); // end of pets
		if (chr.getMount() == null) {
			w.writeInt(1); // mob level
			w.writeLong(0); // mob exp + tiredness
		} else {
			w.writeInt(chr.getMount().getLevel());
			w.writeInt(chr.getMount().getExp());
			w.writeInt(chr.getMount().getTiredness());
		}
		if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr)) {
			if (chr.getPlayerShop().hasFreeSlot()) {
				addAnnounceBox(w, chr.getPlayerShop(), chr.getPlayerShop().getVisitors().length);
			} else {
				addAnnounceBox(w, chr.getPlayerShop(), 1);
			}
		} else if (chr.getActiveMinigame() != null && chr.getActiveMinigame().isOwner(chr)) {
			if (chr.getActiveMinigame().hasFreeSlot()) {
				addAnnounceBox(w, chr.getActiveMinigame(), 1, 0, 1, 0);
			} else {
				addAnnounceBox(w, chr.getActiveMinigame(), 1, 0, 2, 1);
			}
		} else {
			w.writeAsByte(0);
		}
		if (chr.getChalkboard() != null) {
			w.writeAsByte(1);
			w.writeLengthString(chr.getChalkboard());
		} else {
			w.writeAsByte(0);
		}
		addRingLook(w, chr, true);
		addRingLook(w, chr, false);
		addMarriageRingLook(w, chr);
		w.write0(3);
		w.writeAsByte(chr.getTeam());
		return w.getPacket();
	}

	private static void addRingLook(PacketWriter w, GameCharacter chr, boolean crush) {
		List<Ring> rings;
		if (crush) {
			rings = chr.getCrushRings();
		} else {
			rings = chr.getFriendshipRings();
		}
		boolean yes = false;
		for (Ring ring : rings) {
			if (ring.isEquipped()) {
				if (yes == false) {
					yes = true;
					w.writeAsByte(1);
				}
				w.writeInt(ring.getRingId());
				w.writeInt(0);
				w.writeInt(ring.getPartnerRingId());
				w.writeInt(0);
				w.writeInt(ring.getItemId());
			}
		}
		if (yes == false) {
			w.writeAsByte(0);
		}
	}

	private static void addMarriageRingLook(PacketWriter w, GameCharacter chr) {
		final boolean hasMarriageRing = chr.getMarriageRing() != null;
		if (hasMarriageRing && !chr.getMarriageRing().isEquipped()) {
			w.writeAsByte(0);
			return;
		}
		w.writeAsByte(hasMarriageRing);
		if (hasMarriageRing) {
			w.writeInt(chr.getId());
			w.writeInt(chr.getMarriageRing().getPartnerChrId());
			w.writeInt(chr.getMarriageRing().getRingId());
		}
	}

	/**
	 * Adds a announcement box to an existing PacketWriter.
	 * 
	 * @param w
	 *            The PacketWriter to add an announcement box
	 *            to.
	 * @param shop
	 *            The shop to announce.
	 */
	private static void addAnnounceBox(PacketWriter w, PlayerShop shop, int availability) {
		w.writeAsByte(4);
		w.writeInt(shop.getObjectId());
		w.writeLengthString(shop.getDescription());
		w.writeAsByte(0);
		w.writeAsByte(0);
		w.writeAsByte(1);
		w.writeAsByte(availability);
		w.writeAsByte(0);
	}

	private static void addAnnounceBox(PacketWriter w, Minigame game, int gametype, int type, int ammount, int joinable) {
		w.writeAsByte(gametype);
		w.writeInt(game.getObjectId()); // gameid/shopid
		w.writeLengthString(game.getDescription()); // desc
		w.writeAsByte(0);
		w.writeAsByte(type);
		w.writeAsByte(ammount);
		w.writeAsByte(2);
		w.writeAsByte(joinable);
	}

	public static GamePacket facialExpression(GameCharacter from, int expression) {
		PacketWriter w = new PacketWriter(10);
		w.writeAsShort(SendOpcode.FACIAL_EXPRESSION.getValue());
		w.writeInt(from.getId());
		w.writeInt(expression);
		return w.getPacket();
	}

	private static void serializeMovementList(PacketWriter w, List<LifeMovementFragment> moves) {
		w.writeAsByte(moves.size());
		for (LifeMovementFragment move : moves) {
			move.serialize(w);
		}
	}

	public static GamePacket movePlayer(int cid, List<LifeMovementFragment> moves) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MOVE_PLAYER.getValue());
		w.writeInt(cid);
		w.writeInt(0);
		serializeMovementList(w, moves);
		return w.getPacket();
	}

	public static GamePacket moveSummon(int cid, int oid, Point startPos, List<LifeMovementFragment> moves) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MOVE_SUMMON.getValue());
		w.writeInt(cid);
		w.writeInt(oid);
		w.writeVector(startPos);
		serializeMovementList(w, moves);
		return w.getPacket();
	}

	public static GamePacket moveMonster(int useskill, int skill, int skill_1, int skill_2, int skill_3, int skill_4, int oid, Point startPos, List<LifeMovementFragment> moves) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MOVE_MONSTER.getValue());
		w.writeInt(oid);
		w.writeAsByte(0);
		w.writeAsByte(useskill);
		w.writeAsByte(skill);
		w.writeAsByte(skill_1);
		w.writeAsByte(skill_2);
		w.writeAsByte(skill_3);
		w.writeAsByte(skill_4);
		w.writeVector(startPos);
		serializeMovementList(w, moves);
		return w.getPacket();
	}

	public static GamePacket summonAttack(int cid, int summonSkillId, byte direction, List<SummonAttackEntry> allDamage) {
		PacketWriter w = new PacketWriter();
		// b2 00 29 f7 00 00 9a a3 04 00 c8 04 01 94 a3 04 00 06 ff 2b 00
		w.writeAsShort(SendOpcode.SUMMON_ATTACK.getValue());
		w.writeInt(cid);
		w.writeInt(summonSkillId);
		w.writeAsByte(direction);
		w.writeAsByte(4);
		w.writeAsByte(allDamage.size());
		for (SummonAttackEntry attackEntry : allDamage) {
			w.writeInt(attackEntry.getMonsterOid()); // oid
			w.writeAsByte(6); // who knows
			w.writeInt(attackEntry.getDamage()); // damage
		}
		return w.getPacket();
	}

	public static GamePacket closeRangeAttack(GameCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CLOSE_RANGE_ATTACK.getValue());
		addAttackBody(w, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
		return w.getPacket();
	}

	public static GamePacket rangedAttack(GameCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.RANGED_ATTACK.getValue());
		addAttackBody(w, chr, skill, skilllevel, stance, numAttackedAndDamage, projectile, damage, speed, direction, display);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket magicAttack(GameCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, Map<Integer, List<Integer>> damage, int charge, int speed, int direction, int display) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MAGIC_ATTACK.getValue());
		addAttackBody(w, chr, skill, skilllevel, stance, numAttackedAndDamage, 0, damage, speed, direction, display);
		if (charge != -1) {
			w.writeInt(charge);
		}
		return w.getPacket();
	}

	private static void addAttackBody(PacketWriter w, GameCharacter chr, int skill, int skilllevel, int stance, int numAttackedAndDamage, int projectile, Map<Integer, List<Integer>> damage, int speed, int direction, int display) {
		w.writeInt(chr.getId());
		w.writeAsByte(numAttackedAndDamage);
		w.writeAsByte(0x5B);// ?
		w.writeAsByte(skilllevel);
		if (skilllevel > 0) {
			w.writeInt(skill);
		}
		w.writeAsByte(display);
		w.writeAsByte(direction);
		w.writeAsByte(stance);
		w.writeAsByte(speed);
		w.writeAsByte(0x0A);
		w.writeInt(projectile);
		for (Integer oned : damage.keySet()) {
			List<Integer> onedList = damage.get(oned);
			if (onedList != null) {
				w.writeInt(oned.intValue());
				w.writeAsByte(0xFF);
				if (skill == 4211006) {
					w.writeAsByte(onedList.size());
				}
				for (Integer eachd : onedList) {
					w.writeInt(eachd.intValue());
				}
			}
		}
	}

	private static int doubleToShortBits(double d) {
		return (int) (Double.doubleToLongBits(d) >> 48);
	}

	public static GamePacket getNPCShop(GameClient c, int sid, List<ShopItem> items) {
		ItemInfoProvider ii = ItemInfoProvider.getInstance();
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.OPEN_NPC_SHOP.getValue());
		w.writeInt(sid);
		w.writeAsShort(items.size()); // item count
		for (ShopItem item : items) {
			w.writeInt(item.getItemId());
			w.writeInt(item.getPrice());
			w.writeInt(item.getPrice() == 0 ? item.getPitch() : 0); // Perfect
																		// Pitch
			w.writeInt(0); // Can be used x minutes after purchase
			w.writeInt(0); // Hmm
			if (!ItemConstants.isRechargable(item.getItemId())) {
				w.writeAsShort(1); // stacksize o.o
				w.writeAsShort(item.getBuyable());
			} else {
				w.writeAsShort(0);
				w.writeInt(0);
				w.writeAsShort(doubleToShortBits(ii.getPrice(item.getItemId())));
				w.writeAsShort(ii.getSlotMax(c, item.getItemId()));
			}
		}
		return w.getPacket();
	}

	/*
	 * 00 = / 01 = You don't have enough in stock 02 = You do not have enough
	 * mesos 03 = Please check if your inventory is full or not 05 = You don't
	 * have enough in stock 06 = Due to an error, the trade did not happen 07 =
	 * Due to an error, the trade did not happen 08 = / 0D = You need more items
	 * 0E = CRASH; LENGTH NEEDS TO BE LONGER :O
	 */
	public static GamePacket shopTransaction(byte code) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
		w.writeAsByte(code);
		return w.getPacket();
	}

	public static GamePacket addInventorySlot(InventoryType type, IItem item) {
		return addInventorySlot(type, item, false);
	}

	public static GamePacket addInventorySlot(InventoryType type, IItem item, boolean isFromDrop) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.writeAsByte(isFromDrop);
		w.writeAsShort(1); // add mode
		w.writeAsByte(type.equals(InventoryType.EQUIPPED) ? 1 : type.asByte()); // iv
																					// type
		w.writeAsShort(item.getSlot()); // slot id
		addItemInfo(w, item, true);
		return w.getPacket();
	}

	public static GamePacket updateInventorySlot(InventoryType type, IItem item) {
		return updateInventorySlot(type, item, false);
	}

	public static GamePacket updateInventorySlot(InventoryType type, IItem item, boolean isFromDrop) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.writeAsByte(isFromDrop);
		w.writeAsShort(0x101); // update
		w.writeAsByte(type.equals(InventoryType.EQUIPPED) ? 1 : type.asByte()); // iv
																					// type
		w.writeAsShort(item.getSlot()); // slot id
		w.writeAsShort(item.getQuantity());
		return w.getPacket();
	}

	public static GamePacket updateInventorySlotLimit(int type, int newLimit) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_INVENTORY_SLOTS.getValue());
		w.writeAsByte(type);
		w.writeAsByte(newLimit);
		return w.getPacket();
	}

	public static GamePacket moveInventoryItem(InventoryType type, byte src, byte dst) {
		return moveInventoryItem(type, src, dst, (byte) -1);
	}

	public static GamePacket moveInventoryItem(InventoryType type, byte src, byte dst, byte equipIndicator) {
		// 1D 00 01 01 02 00 F5 FF 01 00 01
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.write(new byte[] {1, 1, 2});
		w.writeAsByte(type.asByte()); // iv type
		w.writeAsShort(src);
		w.writeAsShort(dst);
		if (equipIndicator != -1) {
			w.writeAsByte(equipIndicator);
		}
		return w.getPacket();
	}

	public static GamePacket moveAndMergeInventoryItem(InventoryType type, byte src, byte dst, short total) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.write(new byte[] {1, 2, 3});
		w.writeAsByte(type.asByte()); // iv type
		w.writeAsShort(src);
		w.writeAsByte(1); // merge mode?
		w.writeAsByte(type.asByte());
		w.writeAsShort(dst);
		w.writeAsShort(total);
		return w.getPacket();
	}

	public static GamePacket moveAndMergeWithRestInventoryItem(InventoryType type, byte src, byte dst, short srcQ, short dstQ) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.write(new byte[] {1, 2, 1});
		w.writeAsByte(type.asByte()); // iv type
		w.writeAsShort(src);
		w.writeAsShort(srcQ);
		w.writeAsByte(1);
		w.writeAsByte(type.asByte());
		w.writeAsShort(dst);
		w.writeAsShort(dstQ);
		return w.getPacket();
	}

	public static GamePacket clearInventoryItem(InventoryType type, byte slot, boolean isFromDrop) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.writeAsByte(isFromDrop);
		w.write(new byte[] {1, 3});
		// iv type
		w.writeAsByte(type.equals(InventoryType.EQUIPPED) ? 1 : type.asByte()); 
		w.writeAsShort(slot);
		if (!isFromDrop) {
			w.writeAsByte(2);
		}
		return w.getPacket();
	}

	public static GamePacket scrolledItem(IItem scroll, IItem item, boolean destroyed) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.writeAsByte(1); // fromdrop always true
		w.writeAsByte(destroyed ? 2 : 3);
		w.writeAsByte(scroll.getQuantity() > 0 ? 1 : 3);
		w.writeAsByte(InventoryType.USE.asByte());
		w.writeAsShort(scroll.getSlot());
		if (scroll.getQuantity() > 0) {
			w.writeAsShort(scroll.getQuantity());
		}
		w.writeAsByte(3);
		if (!destroyed) {
			w.writeAsByte(InventoryType.EQUIP.asByte());
			w.writeAsShort(item.getSlot());
			w.writeAsByte(0);
		}
		w.writeAsByte(InventoryType.EQUIP.asByte());
		w.writeAsShort(item.getSlot());
		if (!destroyed) {
			addItemInfo(w, item, true);
		}
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_SCROLL_EFFECT.getValue());
		w.writeInt(chr);
		switch (scrollSuccess) {
			case SUCCESS:
				w.writeAsShort(1);
				w.writeAsShort(legendarySpirit ? 1 : 0);
				break;
			case FAIL:
				w.writeAsShort(0);
				w.writeAsShort(legendarySpirit ? 1 : 0);
				break;
			case CURSE:
				w.writeAsByte(0);
				w.writeAsByte(1);
				w.writeAsShort(legendarySpirit ? 1 : 0);
				break;
		}
		return w.getPacket();
	}

	public static GamePacket removePlayerFromMap(int cid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
		w.writeInt(cid);
		return w.getPacket();
	}

	/**
	 * animation: 0 - expire<br/>
	 * 1 - without animation<br/>
	 * 2 - pickup<br/>
	 * 4 - explode<br/>
	 * cid is ignored for 0 and 1
	 * 
	 * @param oid
	 * @param animation
	 * @param cid
	 * @return
	 */
	public static GamePacket removeItemFromMap(int oid, int animation, int cid) {
		return removeItemFromMap(oid, animation, cid, false, 0);
	}

	/**
	 * animation: 0 - expire<br/>
	 * 1 - without animation<br/>
	 * 2 - pickup<br/>
	 * 4 - explode<br/>
	 * cid is ignored for 0 and 1.<br />
	 * <br />
	 * Flagging pet as true will make a pet pick up the item.
	 * 
	 * @param oid
	 * @param animation
	 * @param cid
	 * @param pet
	 * @param slot
	 * @return
	 */
	public static GamePacket removeItemFromMap(int oid, int animation, int cid, boolean pet, int slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.REMOVE_ITEM_FROM_MAP.getValue());
		w.writeAsByte(animation); // expire
		w.writeInt(oid);
		if (animation >= 2) {
			w.writeInt(cid);
			if (pet) {
				w.writeAsByte(slot);
			}
		}
		return w.getPacket();
	}

	public static GamePacket updateCharLook(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_CHAR_LOOK.getValue());
		w.writeInt(chr.getId());
		w.writeAsByte(1);
		addCharLook(w, chr, false);
		addRingLook(w, chr, true);
		addRingLook(w, chr, false);
		addMarriageRingLook(w, chr);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket dropInventoryItem(InventoryType type, short src) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.write(new byte[] {1, 1, 3});
		w.writeAsByte(type.asByte());
		w.writeAsShort(src);
		if (src < 0) {
			w.writeAsByte(1);
		}
		return w.getPacket();
	}

	public static GamePacket dropInventoryItemUpdate(InventoryType type, IItem item) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.write(new byte[] {1, 1, 1});
		w.writeAsByte(type.asByte());
		w.writeAsShort(item.getSlot());
		w.writeAsShort(item.getQuantity());
		return w.getPacket();
	}

	public static GamePacket damagePlayer(int skill, int monsteridfrom, int cid, int damage, int fake, int direction, boolean pgmr, int pgmr_1, boolean is_pg, int oid, int pos_x, int pos_y) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DAMAGE_PLAYER.getValue());
		w.writeInt(cid);
		w.writeAsByte(skill);
		w.writeInt(damage);
		w.writeInt(monsteridfrom);
		w.writeAsByte(direction);
		if (pgmr) {
			w.writeAsByte(pgmr_1);
			w.writeAsByte(is_pg);
			w.writeInt(oid);
			w.writeAsByte(6);
			w.writeAsShort(pos_x);
			w.writeAsShort(pos_y);
			w.writeAsByte(0);
		} else {
			w.writeAsShort(0);
		}
		w.writeInt(damage);
		if (fake > 0) {
			w.writeInt(fake);
		}
		return w.getPacket();
	}

	public static GamePacket charNameResponse(String charname, boolean isNameUsed) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CHAR_NAME_RESPONSE.getValue());
		w.writeLengthString(charname);
		w.writeAsByte(isNameUsed);
		return w.getPacket();
	}

	public static GamePacket addNewCharEntry(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ADD_NEW_CHAR_ENTRY.getValue());
		w.writeAsByte(0);
		addCharEntry(w, chr, false);
		return w.getPacket();
	}

	/**
	 * state 0 = del ok state 12 = invalid bday state 14 = incorrect pic
	 * 
	 * @param cid
	 * @param state
	 * @return
	 */
	public static GamePacket deleteCharResponse(int cid, int state) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DELETE_CHAR_RESPONSE.getValue());
		w.writeInt(cid);
		w.writeAsByte(state);
		return w.getPacket();
	}

	public static GamePacket selectWorld(int world) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SELECT_WORLD.getValue());
		w.writeInt(world);// According to GMS, it should be the world that
								// contains the most characters (most active)
		return w.getPacket();
	}

	public static GamePacket sendRecommended(List<WorldRecommendation> worlds) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SEND_RECOMMENDED.getValue());
		w.writeAsByte(worlds.size());// size
		for (WorldRecommendation world : worlds) {
			w.writeInt(world.worldId);
			w.writeLengthString(world.message);
		}
		return w.getPacket();
	}

	/**
	 * 
	 * @param chr
	 * @param isSelf
	 * @return
	 */
	public static GamePacket charInfo(GameCharacter chr) {
		// 3D 00 0A 43 01 00 02 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00
		// 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CHAR_INFO.getValue());
		w.writeInt(chr.getId());
		w.writeAsByte(chr.getLevel());
		w.writeAsShort(chr.getJob().getId());
		w.writeAsShort(chr.getFame());
		
		final boolean hasMarriageRing = chr.getMarriageRing() != null;
		w.writeAsByte(hasMarriageRing);
		
		String guildName = "";
		String allianceName = "";
		GuildSummary gs = chr.getClient().getWorldServer().getGuildSummary(chr.getGuildId());
		if (chr.getGuildId() > 0 && gs != null) {
			guildName = gs.getName();
			Alliance alliance = Server.getInstance().getAlliance(gs.getAllianceId());
			if (alliance != null) {
				allianceName = alliance.getName();
			}
		}
		w.writeLengthString(guildName);
		w.writeLengthString(allianceName);
		w.writeAsByte(0);
		
		Pet[] pets = chr.getPets();
		IItem inv = chr.getInventory(InventoryType.EQUIPPED).getItem((byte) -114);
		for (int i = 0; i < 3; i++) {
			final Pet pet = pets[i];
			if (pet != null) {
				w.writeAsByte(pets[i].getUniqueId());
				w.writeInt(pets[i].getItemId()); 
				w.writeLengthString(pets[i].getName());
				w.writeAsByte(pets[i].getLevel()); 
				w.writeAsShort(pets[i].getCloseness()); 
				w.writeAsByte(pets[i].getFullness());
				w.writeAsShort(0);
				w.writeInt(inv != null ? inv.getItemId() : 0);
			}
		}
		w.writeAsByte(0); // end of pets
		
		final Mount mount = chr.getMount();
		if (mount != null && chr.getInventory(InventoryType.EQUIPPED).getItem((byte) -18) != null) {
			w.writeAsByte(mount.getId());
			w.writeInt(mount.getLevel());
			w.writeInt(mount.getExp());
			w.writeInt(mount.getTiredness());
		} else {
			w.writeAsByte(0);
		}
		
		final List<Integer> wishList = chr.getCashShop().getWishList();
		w.writeAsByte(wishList.size());
		for (int sn : wishList) {
			w.writeInt(sn);
		}
		
		final MonsterBook monsterBook = chr.getMonsterBook();
		w.writeInt(monsterBook.getBookLevel());
		w.writeInt(monsterBook.getNormalCard());
		w.writeInt(monsterBook.getSpecialCard());
		w.writeInt(monsterBook.getTotalCards());
		w.writeInt(chr.getMonsterBookCover() > 0 ? ItemInfoProvider.getInstance().getCardMobId(chr.getMonsterBookCover()) : 0);
		
		IItem medal = chr.getInventory(InventoryType.EQUIPPED).getItem((byte) -49);
		if (medal != null) {
			w.writeInt(medal.getItemId());
		} else {
			w.writeInt(0);
		}
		ArrayList<Short> medalQuests = new ArrayList<Short>();
		List<QuestStatus> completed = chr.getCompletedQuests();
		for (QuestStatus q : completed) {
			if (q.getQuest().getId() >= 29000) { 
				// && q.getQuest().getId() <= 29923
				medalQuests.add(q.getQuest().getId());
			}
		}

		Collections.sort(medalQuests);
		w.writeAsShort(medalQuests.size());
		for (Short s : medalQuests) {
			w.writeAsShort(s);
		}
		return w.getPacket();
	}

	/**
	 * It is important that statups is in the correct order (see decleration
	 * order in BuffStat) since this method doesn't do automagical
	 * reordering.
	 * 
	 * @param buffid
	 * @param bufflength
	 * @param statups
	 * @return
	 */
	// 1F 00 00 00 00 00 03 00 00 40 00 00 00 E0 00 00 00 00 00 00 00 00 E0 01
	// 8E AA 4F 00 00 C2 EB 0B E0 01 8E AA 4F 00 00 C2 EB 0B 0C 00 8E AA 4F 00
	// 00 C2 EB 0B 44 02 8E AA 4F 00 00 C2 EB 0B 44 02 8E AA 4F 00 00 C2 EB 0B
	// 00 00 E0 7A 1D 00 8E AA 4F 00 00 00 00 00 00 00 00 03
	public static GamePacket giveBuff(int buffid, int bufflength, List<BuffStatDelta> statups) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_BUFF.getValue());
		boolean special = false;
		writeLongMask(w, statups);
		for (BuffStatDelta statup : statups) {
			if (statup.stat.equals(BuffStat.MONSTER_RIDING) || statup.stat.equals(BuffStat.HOMING_BEACON)) {
				special = true;
			}
			w.writeAsShort(statup.delta);
			w.writeInt(buffid);
			w.writeInt(bufflength);
		}
		w.writeInt(0);
		w.writeAsByte(0);
		
		// Homing beacon ...
		w.writeInt(statups.get(0).delta);

		if (special) {
			w.write0(3);
		}
		return w.getPacket();
	}

	/**
	 * 
	 * @param characterId
	 * @param statups
	 * @param mount
	 * @return
	 */
	public static GamePacket showMonsterRiding(int characterId, Mount mount) { 
		// Gtfo with this, this is just giveForeignBuff
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
		w.writeInt(characterId);
		
		// Thanks?
		w.writeLong(BuffStat.MONSTER_RIDING.getValue()); 
		
		w.writeLong(0);
		w.writeAsShort(0);
		w.writeInt(mount.getItemId());
		w.writeInt(mount.getSkillId());
		
		// Server Tick value.
		w.writeInt(0); 
		
		w.writeAsShort(0);
		
		// Times you have been buffed
		w.writeAsByte(0); 
		return w.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param quest
	 * @return
	 */
	public static GamePacket forfeitQuest(short quest) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(1);
		w.writeAsShort(quest);
		w.writeAsByte(0);
		return w.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param quest
	 * @return
	 */
	public static GamePacket completeQuest(short quest, long time) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(1);
		w.writeAsShort(quest);
		w.writeAsByte(2);
		w.writeLong(time);
		return w.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param quest
	 * @param npc
	 * @param progress
	 * @return
	 */
	public static GamePacket updateQuestInfo(short quest, int npc) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
		w.writeAsByte(8); // 0x0A in v95
		w.writeAsShort(quest);
		w.writeInt(npc);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket addQuestTimeLimit(final short quest, final int time) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
		w.writeAsByte(6);
		w.writeAsShort(1);// Size but meh, when will there be 2 at the same
							// time? And it won't even replace the old one :)
		w.writeAsShort(quest);
		w.writeInt(time);
		return w.getPacket();
	}

	public static GamePacket removeQuestTimeLimit(final short quest) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
		w.writeAsByte(7);
		w.writeAsShort(1);// Position
		w.writeAsShort(quest);
		return w.getPacket();
	}

	public static GamePacket updateQuest(final short quest, final String status) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(1);
		w.writeAsShort(quest);
		w.writeAsByte(1);
		w.writeLengthString(status);
		return w.getPacket();
	}

	private static <E extends LongValueHolder> long getLongMaskD(List<DiseaseEntry> entries) {
		long mask = 0;
		for (DiseaseEntry entry : entries) {
			mask |= entry.disease.getValue();
		}
		return mask;
	}

	public static GamePacket giveDebuff(List<DiseaseEntry> entries, MobSkill skill) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_BUFF.getValue());
		long mask = getLongMaskD(entries);
		w.writeLong(0);
		w.writeLong(mask);
		for (DiseaseEntry entry : entries) {
			w.writeAsShort(entry.level);
			w.writeAsShort(skill.getSkillId());
			w.writeAsShort(skill.getSkillLevel());
			w.writeInt((int) skill.getDuration());
		}
		w.writeAsShort(0); // ??? wk charges have 600 here o.o
		w.writeAsShort(900); // Delay
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket giveForeignDebuff(int cid, List<DiseaseEntry> entries, MobSkill skill) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
		w.writeInt(cid);
		long mask = getLongMaskD(entries);
		w.writeLong(0);
		w.writeLong(mask);
		for (int i = 0; i < entries.size(); i++) {
			w.writeAsShort(skill.getSkillId());
			w.writeAsShort(skill.getSkillLevel());
		}
		w.writeAsShort(0); // same as give_buff
		w.writeAsShort(900); // Delay
		return w.getPacket();
	}

	public static GamePacket cancelForeignDebuff(int cid, long mask) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
		w.writeInt(cid);
		w.writeLong(0);
		w.writeLong(mask);
		return w.getPacket();
	}

	public static GamePacket giveForeignBuff(int cid, List<BuffStatDelta> statups) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
		w.writeInt(cid);
		writeLongMask(w, statups);
		for (BuffStatDelta statup : statups) {
			w.writeAsShort(statup.delta);
		}
		w.writeInt(0);
		w.writeAsShort(0);
		return w.getPacket();
	}

	public static GamePacket cancelForeignBuff(int cid, List<BuffStat> statups) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CANCEL_FOREIGN_BUFF.getValue());
		w.writeInt(cid);
		writeLongMaskFromList(w, statups);
		return w.getPacket();
	}

	public static GamePacket cancelBuff(List<BuffStat> statups) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CANCEL_BUFF.getValue());
		writeLongMaskFromList(w, statups);
		w.writeAsByte(1);// ?
		return w.getPacket();
	}

	private static void writeLongMask(PacketWriter w, List<BuffStatDelta> statups) {
		long firstmask = 0;
		long secondmask = 0;
		for (BuffStatDelta statup : statups) {
			if (statup.stat.isFirst()) {
				firstmask |= statup.stat.getValue();
			} else {
				secondmask |= statup.stat.getValue();
			}
		}
		w.writeLong(firstmask);
		w.writeLong(secondmask);
	}

	private static void writeLongMaskFromList(PacketWriter w, List<BuffStat> statups) {
		long firstmask = 0;
		long secondmask = 0;
		for (BuffStat statup : statups) {
			if (statup.isFirst()) {
				firstmask |= statup.getValue();
			} else {
				secondmask |= statup.getValue();
			}
		}
		w.writeLong(firstmask);
		w.writeLong(secondmask);
	}

	public static GamePacket cancelDebuff(long mask) {
		PacketWriter w = new PacketWriter(19);
		w.writeAsShort(SendOpcode.CANCEL_BUFF.getValue());
		w.writeLong(0);
		w.writeLong(mask);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket getPlayerShopChat(GameCharacter c, String chat, boolean isOwner) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT.getCode());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
		w.writeAsByte(!isOwner);
		w.writeLengthString(c.getName() + " : " + chat);
		return w.getPacket();
	}

	public static GamePacket getPlayerShopNewVisitor(GameCharacter c, int slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.VISIT.getCode());
		w.writeAsByte(slot);
		addCharLook(w, c, false);
		w.writeLengthString(c.getName());
		return w.getPacket();
	}

	public static GamePacket getPlayerShopRemoveVisitor(int slot) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.EXIT.getCode());
		if (slot > 0) {
			w.writeAsByte(slot);
		}
		return w.getPacket();
	}

	public static GamePacket getTradePartnerAdd(GameCharacter c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.VISIT.getCode());
		w.writeAsByte(1);
		addCharLook(w, c, false);
		w.writeLengthString(c.getName());
		return w.getPacket();
	}

	public static GamePacket getTradeInvite(GameCharacter c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.INVITE.getCode());
		w.writeAsByte(3);
		w.writeLengthString(c.getName());
		w.write(new byte[] {(byte) 0xB7, (byte) 0x50, 0, 0});
		return w.getPacket();
	}

	public static GamePacket getTradeMesoSet(byte number, int meso) {
		PacketWriter w = new PacketWriter(8);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.SET_MESO.getCode());
		w.writeAsByte(number);
		w.writeInt(meso);
		return w.getPacket();
	}

	public static GamePacket getTradeItemAdd(byte number, IItem item) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.SET_ITEMS.getCode());
		w.writeAsByte(number);
		w.writeAsByte(item.getSlot());
		addItemInfo(w, item, true);
		return w.getPacket();
	}

	public static GamePacket getPlayerShopItemUpdate(PlayerShop shop) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.UPDATE_MERCHANT.getCode());
		w.writeAsByte(shop.getItems().size());
		for (PlayerShopItem item : shop.getItems()) {
			w.writeAsShort(item.getBundles());
			w.writeAsShort(item.getItem().getQuantity());
			w.writeInt(item.getPrice());
			addItemInfo(w, item.getItem(), true);
		}
		return w.getPacket();
	}

	/**
	 * 
	 * @param c
	 * @param shop
	 * @param isOwner
	 * @return
	 */
	public static GamePacket getPlayerShop(GameClient c, PlayerShop shop, boolean isOwner) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.ROOM.getCode());
		w.writeAsByte(4);
		w.writeAsByte(4);
		w.writeAsByte(!isOwner);
		w.writeAsByte(0);
		addCharLook(w, shop.getOwner(), false);
		w.writeLengthString(shop.getOwner().getName());
		w.writeAsByte(1);
		addCharLook(w, shop.getOwner(), false);
		w.writeLengthString(shop.getOwner().getName());
		w.writeAsByte(0xFF);
		w.writeLengthString(shop.getDescription());
		List<PlayerShopItem> items = shop.getItems();
		w.writeAsByte(0x10);
		w.writeAsByte(items.size());
		for (PlayerShopItem item : items) {
			w.writeAsShort(item.getBundles());
			w.writeAsShort(item.getItem().getQuantity());
			w.writeInt(item.getPrice());
			addItemInfo(w, item.getItem(), true);
		}
		return w.getPacket();
	}

	public static GamePacket getTradeStart(GameClient c, Trade trade, byte number) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.ROOM.getCode());
		w.writeAsByte(3);
		w.writeAsByte(2);
		w.writeAsByte(number);
		if (number == 1) {
			w.writeAsByte(0);
			addCharLook(w, trade.getPartner().getChr(), false);
			w.writeLengthString(trade.getPartner().getChr().getName());
		}
		w.writeAsByte(number);
		addCharLook(w, c.getPlayer(), false);
		w.writeLengthString(c.getPlayer().getName());
		w.writeAsByte(0xFF);
		return w.getPacket();
	}

	public static GamePacket getTradeConfirmation() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.CONFIRM.getCode());
		return w.getPacket();
	}

	public static GamePacket getTradeCompletion(byte number) {
		PacketWriter w = new PacketWriter(5);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.EXIT.getCode());
		w.writeAsByte(number);
		w.writeAsByte(6);
		return w.getPacket();
	}

	public static GamePacket getTradeCancel(byte number) {
		PacketWriter w = new PacketWriter(5);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.EXIT.getCode());
		w.writeAsByte(number);
		w.writeAsByte(2);
		return w.getPacket();
	}

	public static GamePacket addCharBox(GameCharacter c, int type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
		w.writeInt(c.getId());
		addAnnounceBox(w, c.getPlayerShop(), type);
		return w.getPacket();
	}

	public static GamePacket removeCharBox(GameCharacter c) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
		w.writeInt(c.getId());
		w.writeAsByte(0);
		return w.getPacket();
	}

	/**
	 * Possible values for <code>speaker</code>:<br>
	 * 0: Npc talking (left)<br>
	 * 1: Npc talking (right)<br>
	 * 2: Player talking (left)<br>
	 * 3: Player talking (left)<br>
	 * 
	 * @param npc
	 *            Npcid
	 * @param msgType
	 * @param talk
	 * @param endBytes
	 * @param speaker
	 * @return
	 */
	public static GamePacket getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte speaker) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.NPC_TALK.getValue());
		w.writeAsByte(4); // ?
		w.writeInt(npc);
		w.writeAsByte(msgType);
		w.writeAsByte(speaker);
		w.writeLengthString(talk);
		w.write(HexTool.getByteArrayFromHexString(endBytes));
		return w.getPacket();
	}

	public static GamePacket getDimensionalMirror(String talk) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.NPC_TALK.getValue());
		w.writeAsByte(4); // ?
		w.writeInt(9010022);
		w.writeAsByte(0x0E);
		w.writeAsByte(0);
		w.writeInt(0);
		w.writeLengthString(talk);
		return w.getPacket();
	}

	public static GamePacket getNPCTalkStyle(int npc, String talk, int styles[]) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.NPC_TALK.getValue());
		w.writeAsByte(4); // ?
		w.writeInt(npc);
		w.writeAsByte(7);
		w.writeAsByte(0); // speaker
		w.writeLengthString(talk);
		w.writeAsByte(styles.length);
		for (int i = 0; i < styles.length; i++) {
			w.writeInt(styles[i]);
		}
		return w.getPacket();
	}

	public static GamePacket getNPCTalkNum(int npc, String talk, int def, int min, int max) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.NPC_TALK.getValue());
		w.writeAsByte(4); // ?
		w.writeInt(npc);
		w.writeAsByte(3);
		w.writeAsByte(0); // speaker
		w.writeLengthString(talk);
		w.writeInt(def);
		w.writeInt(min);
		w.writeInt(max);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket getNPCTalkText(int npc, String talk, String def) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.NPC_TALK.getValue());
		w.writeAsByte(4); // Doesn't matter
		w.writeInt(npc);
		w.writeAsByte(2);
		w.writeAsByte(0); // speaker
		w.writeLengthString(talk);
		w.writeLengthString(def);// :D
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket showBuffeffect(int cid, int skillid, int effectid) {
		return showBuffeffect(cid, skillid, effectid, (byte) 3);
	}

	public static GamePacket showBuffeffect(int cid, int skillid, int effectid, byte direction) {
		PacketWriter w = new PacketWriter(12);
		w.writeAsShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
		w.writeInt(cid);
		w.writeAsByte(effectid); // buff level
		w.writeInt(skillid);
		w.writeAsByte(direction);
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket showOwnBuffEffect(int skillid, int effectid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(effectid);
		w.writeInt(skillid);
		w.writeAsByte(0xA9);
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket showOwnBerserk(int skilllevel, boolean Berserk) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(1);
		w.writeInt(1320006);
		w.writeAsByte(0xA9);
		w.writeAsByte(skilllevel);
		w.writeAsByte(Berserk);
		return w.getPacket();
	}

	public static GamePacket showBerserk(int cid, int skilllevel, boolean Berserk) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
		w.writeInt(cid);
		w.writeAsByte(1);
		w.writeInt(1320006);
		w.writeAsByte(0xA9);
		w.writeAsByte(skilllevel);
		w.writeAsByte(Berserk);
		return w.getPacket();
	}

	public static GamePacket updateSkill(int skillid, int level, int masterlevel, long expiration) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_SKILLS.getValue());
		w.writeAsByte(1);
		w.writeAsShort(1);
		w.writeInt(skillid);
		w.writeInt(level);
		w.writeInt(masterlevel);
		addExpirationTime(w, expiration);
		w.writeAsByte(4);
		return w.getPacket();
	}

	public static GamePacket getShowQuestCompletion(int id) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_QUEST_COMPLETION.getValue());
		w.writeAsShort(id);
		return w.getPacket();
	}

	public static GamePacket getKeymap(Map<Integer, KeyBinding> keybindings) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.KEYMAP.getValue());
		w.writeAsByte(0);
		for (int x = 0; x < 90; x++) {
			KeyBinding binding = keybindings.get(Integer.valueOf(x));
			if (binding != null) {
				w.writeAsByte(binding.getType());
				w.writeInt(binding.getAction());
			} else {
				w.writeAsByte(0);
				w.writeInt(0);
			}
		}
		return w.getPacket();
	}

	public static GamePacket getWhisper(String sender, byte channel, String text) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.WHISPER.getValue());
		w.writeAsByte(0x12);
		w.writeLengthString(sender);
		w.writeAsShort(channel - 1); // I guess this is the channel
		w.writeLengthString(text);
		return w.getPacket();
	}

	/**
	 * 
	 * @param target
	 *            name of the target character
	 * @param reply
	 *            error code: 0x0 = cannot find char, 0x1 = success
	 * @return the GamePacket
	 */
	public static GamePacket getWhisperReply(String target, byte reply) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.WHISPER.getValue());
		w.writeAsByte(0x0A); // whisper?
		w.writeLengthString(target);
		w.writeAsByte(reply);
		return w.getPacket();
	}

	public static GamePacket getInventoryFull() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.writeAsByte(1);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket getShowInventoryFull() {
		return getShowInventoryStatus(0xff);
	}

	public static GamePacket showItemUnavailable() {
		return getShowInventoryStatus(0xfe);
	}

	public static GamePacket getShowInventoryStatus(int mode) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(0);
		w.writeAsByte(mode);
		w.writeInt(0);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket getStorage(int npcId, byte slots, Collection<IItem> items, int meso) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.STORAGE.getValue());
		w.writeAsByte(0x16);
		w.writeInt(npcId);
		w.writeAsByte(slots);
		w.writeAsShort(0x7E);
		w.writeAsShort(0);
		w.writeInt(0);
		w.writeInt(meso);
		w.writeAsShort(0);
		w.writeAsByte((byte) items.size());
		for (IItem item : items) {
			addItemInfo(w, item, true);
		}
		w.writeAsShort(0);
		w.writeAsByte(0);
		return w.getPacket();
	}

	/*
	 * 0x0A = Inv full 0x0B = You do not have enough mesos 0x0C = One-Of-A-Kind
	 * error
	 */
	public static GamePacket getStorageError(byte i) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.STORAGE.getValue());
		w.writeAsByte(i);
		return w.getPacket();
	}

	public static GamePacket mesoStorage(byte slots, int meso) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.STORAGE.getValue());
		w.writeAsByte(0x13);
		w.writeAsByte(slots);
		w.writeAsShort(2);
		w.writeAsShort(0);
		w.writeInt(0);
		w.writeInt(meso);
		return w.getPacket();
	}

	public static GamePacket storeStorage(byte slots, InventoryType type, Collection<IItem> items) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.STORAGE.getValue());
		w.writeAsByte(0xD);
		w.writeAsByte(slots);
		w.writeAsShort(type.getBitfieldEncoding());
		w.writeAsShort(0);
		w.writeInt(0);
		w.writeAsByte(items.size());
		for (IItem item : items) {
			addItemInfo(w, item, true);
		}
		return w.getPacket();
	}

	public static GamePacket takeOutStorage(byte slots, InventoryType type, Collection<IItem> items) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.STORAGE.getValue());
		w.writeAsByte(0x9);
		w.writeAsByte(slots);
		w.writeAsShort(type.getBitfieldEncoding());
		w.writeAsShort(0);
		w.writeInt(0);
		w.writeAsByte(items.size());
		for (IItem item : items) {
			addItemInfo(w, item, true);
		}
		return w.getPacket();
	}

	/**
	 * 
	 * @param oid
	 * @param remhppercentage
	 * @return
	 */
	public static GamePacket showMonsterHP(int oid, int remhppercentage) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_MONSTER_HP.getValue());
		w.writeInt(oid);
		w.writeAsByte(remhppercentage);
		return w.getPacket();
	}

	public static GamePacket showBossHP(int oid, int currHP, int maxHP, byte tagColor, byte tagBgColor) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BOSS_ENV.getValue());
		w.writeAsByte(5);
		w.writeInt(oid);
		w.writeInt(currHP);
		w.writeInt(maxHP);
		w.writeAsByte(tagColor);
		w.writeAsByte(tagBgColor);
		return w.getPacket();
	}

	public static GamePacket giveFameResponse(int mode, String charname, int newfame) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FAME_RESPONSE.getValue());
		w.writeAsByte(0);
		w.writeLengthString(charname);
		w.writeAsByte(mode);
		w.writeAsShort(newfame);
		w.writeAsShort(0);
		return w.getPacket();
	}

	/**
	 * status can be: <br>
	 * 0: ok, use giveFameResponse<br>
	 * 1: the username is incorrectly entered<br>
	 * 2: users under level 15 are unable to toggle with fame.<br>
	 * 3: can't raise or drop fame anymore today.<br>
	 * 4: can't raise or drop fame for this character for this month anymore.<br>
	 * 5: received fame, use receiveFame()<br>
	 * 6: level of fame neither has been raised nor dropped due to an unexpected
	 * error
	 * 
	 * @param status
	 * @return
	 */
	public static GamePacket giveFameErrorResponse(int status) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FAME_RESPONSE.getValue());
		w.writeAsByte(status);
		return w.getPacket();
	}

	public static GamePacket receiveFame(int mode, String charnameFrom) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FAME_RESPONSE.getValue());
		w.writeAsByte(5);
		w.writeLengthString(charnameFrom);
		w.writeAsByte(mode);
		return w.getPacket();
	}

	public static GamePacket partyCreated() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PARTY_OPERATION.getValue());
		w.writeAsByte(8);
		w.writeAsShort(0x8b);
		w.writeAsShort(1);
		w.write(CHAR_INFO_MAGIC);
		w.write(CHAR_INFO_MAGIC);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket partyInvite(GameCharacter from) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PARTY_OPERATION.getValue());
		w.writeAsByte(4);
		w.writeInt(from.getParty().getId());
		w.writeLengthString(from.getName());
		w.writeAsByte(0);
		return w.getPacket();
	}

	/**
	 * 10: A beginner can't create a party. 1/11/14/19: Your request for a party
	 * didn't work due to an unexpected error. 13: You have yet to join a party.
	 * 16: Already have joined a party. 17: The party you're trying to join is
	 * already in full capacity. 19: Unable to find the requested character in
	 * this channel.
	 * 
	 * @param message
	 * @return
	 */
	public static GamePacket partyStatusMessage(int message) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PARTY_OPERATION.getValue());
		w.writeAsByte(message);
		return w.getPacket();
	}

	/**
	 * 23: 'Char' have denied request to the party.
	 * 
	 * @param message
	 * @param charname
	 * @return
	 */
	public static GamePacket partyStatusMessage(int message, String charname) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PARTY_OPERATION.getValue());
		w.writeAsByte(message);
		w.writeLengthString(charname);
		return w.getPacket();
	}

	private static void addPartyStatus(PacketWriter w, int forchannel, Party party, boolean leaving) {
		List<PartyCharacter> partymembers = new ArrayList<PartyCharacter>(party.getMembers());
		while (partymembers.size() < 6) {
			partymembers.add(new PartyCharacter());
		}
		for (PartyCharacter partychar : partymembers) {
			w.writeInt(partychar.getId());
		}
		for (PartyCharacter partychar : partymembers) {
			w.writePaddedString(partychar.getName(), 13);
		}
		for (PartyCharacter partychar : partymembers) {
			w.writeInt(partychar.getJobId());
		}
		for (PartyCharacter partychar : partymembers) {
			w.writeInt(partychar.getLevel());
		}
		for (PartyCharacter partychar : partymembers) {
			if (partychar.isOnline()) {
				w.writeInt(partychar.getChannel() - 1);
			} else {
				w.writeInt(-2);
			}
		}
		w.writeInt(party.getLeader().getId());
		for (PartyCharacter partychar : partymembers) {
			if (partychar.getChannel() == forchannel) {
				w.writeInt(partychar.getMapId());
			} else {
				w.writeInt(0);
			}
		}
		for (PartyCharacter partychar : partymembers) {
			if (partychar.getChannel() == forchannel && !leaving) {
				w.writeInt(partychar.getDoorTown());
				w.writeInt(partychar.getDoorTarget());
				w.writeInt(partychar.getDoorPosition().x);
				w.writeInt(partychar.getDoorPosition().y);
			} else {
				w.writeInt(999999999);
				w.writeInt(999999999);
				w.writeInt(0);
				w.writeInt(0);
			}
		}
	}

	public static GamePacket updateParty(int forChannel, Party party, PartyOperation op, PartyCharacter target) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PARTY_OPERATION.getValue());
		switch (op) {
			case DISBAND:
			case EXPEL:
			case LEAVE:
				w.writeAsByte(0x0C);
				w.writeInt(40546);
				w.writeInt(target.getId());
				if (op == PartyOperation.DISBAND) {
					w.writeAsByte(0);
					w.writeInt(party.getId());
				} else {
					w.writeAsByte(1);
					if (op == PartyOperation.EXPEL) {
						w.writeAsByte(1);
					} else {
						w.writeAsByte(0);
					}
					w.writeLengthString(target.getName());
					addPartyStatus(w, forChannel, party, false);
				}
				break;
			case JOIN:
				w.writeAsByte(0xF);
				w.writeInt(40546);
				w.writeLengthString(target.getName());
				addPartyStatus(w, forChannel, party, false);
				break;
			case SILENT_UPDATE:
			case LOG_ONOFF:
				w.writeAsByte(0x7);
				w.writeInt(party.getId());
				addPartyStatus(w, forChannel, party, false);
				break;
			case CHANGE_LEADER:
				w.writeAsByte(0x1B);
				w.writeInt(target.getId());
				w.writeAsByte(0);
				break;
		}
		return w.getPacket();
	}

	public static GamePacket partyPortal(int townId, int targetId, Point position) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PARTY_OPERATION.getValue());
		w.writeAsShort(0x23);
		w.writeInt(townId);
		w.writeInt(targetId);
		w.writeVector(position);
		return w.getPacket();
	}

	public static GamePacket updatePartyMemberHP(int cid, int curhp, int maxhp) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_PARTYMEMBER_HP.getValue());
		w.writeInt(cid);
		w.writeInt(curhp);
		w.writeInt(maxhp);
		return w.getPacket();
	}

	/**
	 * mode: 0 buddychat; 1 partychat; 2 guildchat
	 * 
	 * @param name
	 * @param chattext
	 * @param mode
	 * @return
	 */
	public static GamePacket multiChat(String name, String chattext, int mode) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MULTICHAT.getValue());
		w.writeAsByte(mode);
		w.writeLengthString(name);
		w.writeLengthString(chattext);
		return w.getPacket();
	}

	private static void writeIntMask(PacketWriter w, Map<MonsterStatus, Integer> stats) {
		int firstmask = 0;
		int secondmask = 0;
		for (MonsterStatus stat : stats.keySet()) {
			if (stat.isFirst()) {
				firstmask |= stat.getValue();
			} else {
				secondmask |= stat.getValue();
			}
		}
		w.writeInt(firstmask);
		w.writeInt(secondmask);
	}

	public static GamePacket applyMonsterStatus(final int oid, final MonsterStatusEffect mse) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.APPLY_MONSTER_STATUS.getValue());
		w.writeInt(oid);
		w.writeLong(0);
		writeIntMask(w, mse.getStati());
		for (Map.Entry<MonsterStatus, Integer> stat : mse.getStati().entrySet()) {
			w.writeAsShort(stat.getValue());
			if (mse.isMonsterSkill()) {
				w.writeAsShort(mse.getMobSkill().getSkillId());
				w.writeAsShort(mse.getMobSkill().getSkillLevel());
			} else {
				w.writeInt(mse.getSkill().getId());
			}
			w.writeAsShort(-1); // might actually be the buffTime but it's not
									// displayed anywhere
		}
		w.writeAsShort(0);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket cancelMonsterStatus(int oid, Map<MonsterStatus, Integer> stats) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CANCEL_MONSTER_STATUS.getValue());
		w.writeInt(oid);
		w.writeLong(0);
		w.writeInt(0);
		int mask = 0;
		for (MonsterStatus stat : stats.keySet()) {
			mask |= stat.getValue();
		}
		w.writeInt(mask);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket getClock(int time) { // time in seconds
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CLOCK.getValue());
		w.writeAsByte(2); // clock type. if you send 3 here you have to send
						// another byte (which does not matter at all) before
						// the timestamp
		w.writeInt(time);
		return w.getPacket();
	}

	public static GamePacket getClockTime(int hour, int min, int sec) { // Current
																			// Time
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CLOCK.getValue());
		w.writeAsByte(1); // Clock-Type
		w.writeAsByte(hour);
		w.writeAsByte(min);
		w.writeAsByte(sec);
		return w.getPacket();
	}

	public static GamePacket spawnMist(int oid, int ownerCid, int skill, int level, Mist mist) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_MIST.getValue());
		w.writeInt(oid);
		w.writeInt(mist.isMobMist() ? 0 : mist.isPoisonMist() ? 1 : 2);
		w.writeInt(ownerCid);
		w.writeInt(skill);
		w.writeAsByte(level);
		w.writeAsShort(mist.getSkillDelay()); // Skill delay
		w.writeInt(mist.getBox().x);
		w.writeInt(mist.getBox().y);
		w.writeInt(mist.getBox().x + mist.getBox().width);
		w.writeInt(mist.getBox().y + mist.getBox().height);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket removeMist(int oid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.REMOVE_MIST.getValue());
		w.writeInt(oid);
		return w.getPacket();
	}

	public static GamePacket damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DAMAGE_SUMMON.getValue());
		w.writeInt(cid);
		w.writeInt(summonSkillId);
		w.writeAsByte(unkByte);
		w.writeInt(damage);
		w.writeInt(monsterIdFrom);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket damageMonster(int oid, int damage) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DAMAGE_MONSTER.getValue());
		w.writeInt(oid);
		w.writeAsByte(0);
		w.writeInt(damage);
		w.writeAsByte(0);
		w.writeAsByte(0);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket healMonster(int oid, int heal) {
		return damageMonster(oid, -heal);
	}

	public static GamePacket updateBuddylist(Collection<BuddylistEntry> buddylist) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BUDDYLIST.getValue());
		w.writeAsByte(7);
		w.writeAsByte(buddylist.size());
		for (BuddylistEntry buddy : buddylist) {
			if (buddy.isVisible()) {
				w.writeInt(buddy.getCharacterId()); // cid
				w.writePaddedString(buddy.getName(), 13);
				w.writeAsByte(0); // opposite status
				w.writeInt(buddy.getChannel() - 1);
				w.writePaddedString(buddy.getGroup(), 13);
				w.writeInt(0);// mapid?
			}
		}
		for (int x = 0; x < buddylist.size(); x++) {
			w.writeInt(0);// mapid?
		}
		return w.getPacket();
	}

	public static GamePacket buddylistMessage(byte message) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BUDDYLIST.getValue());
		w.writeAsByte(message);
		return w.getPacket();
	}

	public static GamePacket requestBuddylistAdd(int cidFrom, int cid, String nameFrom) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BUDDYLIST.getValue());
		w.writeAsByte(9);
		w.writeInt(cidFrom);
		w.writeLengthString(nameFrom);
		w.writeInt(cidFrom);
		w.writePaddedString(nameFrom, 11);
		w.writeAsByte(0x09);
		w.writeAsByte(0xf0);
		w.writeAsByte(0x01);
		w.writeInt(0x0f);
		w.writeNullTerminatedString("Default Group");
		w.writeInt(cid);
		return w.getPacket();
	}

	public static GamePacket updateBuddyChannel(int characterid, byte channel) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BUDDYLIST.getValue());
		w.writeAsByte(0x14);
		w.writeInt(characterid);
		w.writeAsByte(0);
		w.writeInt(channel);
		return w.getPacket();
	}

	public static GamePacket itemEffect(int characterid, int itemid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_EFFECT.getValue());
		w.writeInt(characterid);
		w.writeInt(itemid);
		return w.getPacket();
	}

	public static GamePacket updateBuddyCapacity(int capacity) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BUDDYLIST.getValue());
		w.writeAsByte(0x15);
		w.writeAsByte(capacity);
		return w.getPacket();
	}

	public static GamePacket showChair(int characterid, int itemid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_CHAIR.getValue());
		w.writeInt(characterid);
		w.writeInt(itemid);
		return w.getPacket();
	}

	public static GamePacket cancelChair(int id) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CANCEL_CHAIR.getValue());
		if (id == -1) {
			w.writeAsByte(0);
		} else {
			w.writeAsByte(1);
			w.writeAsShort(id);
		}
		return w.getPacket();
	}

	// is there a way to spawn reactors non-animated?
	public static GamePacket spawnReactor(Reactor reactor) {
		PacketWriter w = new PacketWriter();
		Point pos = reactor.getPosition();
		w.writeAsShort(SendOpcode.REACTOR_SPAWN.getValue());
		w.writeInt(reactor.getObjectId());
		w.writeInt(reactor.getId());
		w.writeAsByte(reactor.getState());
		w.writeVector(pos);
		w.writeAsShort(0);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket triggerReactor(Reactor reactor, int stance) {
		PacketWriter w = new PacketWriter();
		Point pos = reactor.getPosition();
		w.writeAsShort(SendOpcode.REACTOR_HIT.getValue());
		w.writeInt(reactor.getObjectId());
		w.writeAsByte(reactor.getState());
		w.writeVector(pos);
		w.writeAsShort(stance);
		w.writeAsByte(0);
		w.writeAsByte(5); // frame delay, set to 5 since there doesn't appear to
						// be a fixed formula for it
		return w.getPacket();
	}

	public static GamePacket destroyReactor(Reactor reactor) {
		PacketWriter w = new PacketWriter();
		Point pos = reactor.getPosition();
		w.writeAsShort(SendOpcode.REACTOR_DESTROY.getValue());
		w.writeInt(reactor.getObjectId());
		w.writeAsByte(reactor.getState());
		w.writeVector(pos);
		return w.getPacket();
	}

	public static GamePacket musicChange(String song) {
		return environmentChange(song, 6);
	}

	public static GamePacket showEffect(String effect) {
		return environmentChange(effect, 3);
	}

	public static GamePacket playSound(String sound) {
		return environmentChange(sound, 4);
	}

	public static GamePacket environmentChange(String env, int mode) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BOSS_ENV.getValue());
		w.writeAsByte(mode);
		w.writeLengthString(env);
		return w.getPacket();
	}

	public static GamePacket startMapEffect(String msg, int itemid, boolean active) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MAP_EFFECT.getValue());
		w.writeAsByte(!active);
		w.writeInt(itemid);
		if (active) {
			w.writeLengthString(msg);
		}
		return w.getPacket();
	}

	public static GamePacket removeMapEffect() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MAP_EFFECT.getValue());
		w.writeAsByte(0);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket mapEffect(String path) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BOSS_ENV.getValue());
		w.writeAsByte(3);
		w.writeLengthString(path);
		return w.getPacket();
	}

	public static GamePacket mapSound(String path) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BOSS_ENV.getValue());
		w.writeAsByte(4);
		w.writeLengthString(path);
		return w.getPacket();
	}

	public static GamePacket showGuildInfo(GameCharacter c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x1A); // signature for showing guild info
		if (c == null) { // show empty guild (used for leaving, expelled)
			w.writeAsByte(0);
			return w.getPacket();
		}
		Guild g = c.getClient().getWorldServer().getGuild(c.getMGC());
		if (g == null) { // failed to read from DB - don't show a guild
			w.writeAsByte(0);
			return w.getPacket();
		} else {
			c.setGuildRank(c.getGuildRank());
		}
		w.writeAsByte(1); // bInGuild
		w.writeInt(g.getId());
		w.writeLengthString(g.getName());
		for (int i = 1; i <= 5; i++) {
			w.writeLengthString(g.getRankTitle(i));
		}
		Collection<GuildCharacter> members = g.getMembers();
		w.writeAsByte(members.size()); // then it is the size of all the members
		for (GuildCharacter mgc : members) {// and each of their character
													// ids o_O
			w.writeInt(mgc.getId());
		}
		for (GuildCharacter mgc : members) {
			w.writePaddedString(mgc.getName(), 13);
			w.writeInt(mgc.getJobId());
			w.writeInt(mgc.getLevel());
			w.writeInt(mgc.getGuildRank());
			w.writeInt(mgc.isOnline() ? 1 : 0);
			w.writeInt(g.getSignature());
			w.writeInt(mgc.getAllianceRank());
		}
		w.writeInt(g.getCapacity());
		w.writeAsShort(g.getLogoBG());
		w.writeAsByte(g.getLogoBGColor());
		w.writeAsShort(g.getLogo());
		w.writeAsByte(g.getLogoColor());
		w.writeLengthString(g.getNotice());
		w.writeInt(g.getGP());
		w.writeInt(g.getAllianceId());
		return w.getPacket();
	}

	public static GamePacket guildMemberOnline(int gid, int cid, boolean isOnline) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x3d);
		w.writeInt(gid);
		w.writeInt(cid);
		w.writeAsByte(isOnline);
		return w.getPacket();
	}

	public static GamePacket guildInvite(int gid, String charName) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x05);
		w.writeInt(gid);
		w.writeLengthString(charName);
		return w.getPacket();
	}

	/**
	 * 'Char' has denied your guild invitation.
	 * 
	 * @param charname
	 * @return
	 */
	public static GamePacket denyGuildInvitation(String charname) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x37);
		w.writeLengthString(charname);
		return w.getPacket();
	}

	public static GamePacket genericGuildMessage(byte code) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(code);
		return w.getPacket();
	}

	public static GamePacket newGuildMember(GuildCharacter mgc) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x27);
		w.writeInt(mgc.getGuildId());
		w.writeInt(mgc.getId());
		w.writePaddedString(mgc.getName(), 13);
		w.writeInt(mgc.getJobId());
		w.writeInt(mgc.getLevel());
		w.writeInt(mgc.getGuildRank()); // should be always 5 but whatevs
		w.writeInt(mgc.isOnline() ? 1 : 0); // should always be 1 too
		w.writeInt(1); // ? could be guild signature, but doesn't seem to
							// matter
		w.writeInt(3);
		return w.getPacket();
	}

	// someone leaving, mode == 0x2c for leaving, 0x2f for expelled
	public static GamePacket memberLeft(GuildCharacter mgc, boolean bExpelled) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(bExpelled ? 0x2f : 0x2c);
		w.writeInt(mgc.getGuildId());
		w.writeInt(mgc.getId());
		w.writeLengthString(mgc.getName());
		return w.getPacket();
	}

	// rank change
	public static GamePacket changeRank(GuildCharacter mgc) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x40);
		w.writeInt(mgc.getGuildId());
		w.writeInt(mgc.getId());
		w.writeAsByte(mgc.getGuildRank());
		return w.getPacket();
	}

	public static GamePacket guildNotice(int gid, String notice) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x44);
		w.writeInt(gid);
		w.writeLengthString(notice);
		return w.getPacket();
	}

	public static GamePacket guildMemberLevelJobUpdate(GuildCharacter mgc) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x3C);
		w.writeInt(mgc.getGuildId());
		w.writeInt(mgc.getId());
		w.writeInt(mgc.getLevel());
		w.writeInt(mgc.getJobId());
		return w.getPacket();
	}

	public static GamePacket rankTitleChange(int gid, String[] ranks) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x3E);
		w.writeInt(gid);
		for (int i = 0; i < 5; i++) {
			w.writeLengthString(ranks[i]);
		}
		return w.getPacket();
	}

	public static GamePacket guildDisband(int gid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x32);
		w.writeInt(gid);
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x42);
		w.writeInt(gid);
		w.writeAsShort(bg);
		w.writeAsByte(bgcolor);
		w.writeAsShort(logo);
		w.writeAsByte(logocolor);
		return w.getPacket();
	}

	public static GamePacket guildCapacityChange(int gid, int capacity) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x3A);
		w.writeInt(gid);
		w.writeAsByte(capacity);
		return w.getPacket();
	}

	public static void addThread(PacketWriter w, ResultSet rs) throws SQLException {
		w.writeInt(rs.getInt("localthreadid"));
		w.writeInt(rs.getInt("postercid"));
		w.writeLengthString(rs.getString("name"));
		w.writeLong(getKoreanTimestamp(rs.getLong("timestamp")));
		w.writeInt(rs.getInt("icon"));
		w.writeInt(rs.getInt("replycount"));
	}

	public static GamePacket BBSThreadList(ResultSet rs, int start) throws SQLException {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BBS_OPERATION.getValue());
		w.writeAsByte(0x06);
		if (!rs.last()) {
			w.writeAsByte(0);
			w.writeInt(0);
			w.writeInt(0);
			return w.getPacket();
		}
		int threadCount = rs.getRow();
		if (rs.getInt("localthreadid") == 0) { // has a notice
			w.writeAsByte(1);
			addThread(w, rs);
			threadCount--; // one thread didn't count (because it's a notice)
		} else {
			w.writeAsByte(0);
		}
		if (!rs.absolute(start + 1)) { // seek to the thread before where we
										// start
			rs.first(); // uh, we're trying to start at a place past possible
			start = 0;
		}
		w.writeInt(threadCount);
		w.writeInt(Math.min(10, threadCount - start));
		for (int i = 0; i < Math.min(10, threadCount - start); i++) {
			addThread(w, rs);
			rs.next();
		}
		return w.getPacket();
	}

	public static GamePacket showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS) throws SQLException, RuntimeException {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BBS_OPERATION.getValue());
		w.writeAsByte(0x07);
		w.writeInt(localthreadid);
		w.writeInt(threadRS.getInt("postercid"));
		w.writeLong(getKoreanTimestamp(threadRS.getLong("timestamp")));
		w.writeLengthString(threadRS.getString("name"));
		w.writeLengthString(threadRS.getString("startpost"));
		w.writeInt(threadRS.getInt("icon"));
		if (repliesRS != null) {
			int replyCount = threadRS.getInt("replycount");
			w.writeInt(replyCount);
			int i;
			for (i = 0; i < replyCount && repliesRS.next(); i++) {
				w.writeInt(repliesRS.getInt("replyid"));
				w.writeInt(repliesRS.getInt("postercid"));
				w.writeLong(getKoreanTimestamp(repliesRS.getLong("timestamp")));
				w.writeLengthString(repliesRS.getString("content"));
			}
			if (i != replyCount || repliesRS.next()) {
				throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
			}
		} else {
			w.writeInt(0);
		}
		return w.getPacket();
	}

	public static GamePacket showGuildRanks(int npcid, ResultSet rs) throws SQLException {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x49);
		w.writeInt(npcid);
		if (!rs.last()) { // no guilds o.o
			w.writeInt(0);
			return w.getPacket();
		}
		w.writeInt(rs.getRow()); // number of entries
		rs.beforeFirst();
		while (rs.next()) {
			w.writeLengthString(rs.getString("name"));
			w.writeInt(rs.getInt("GP"));
			w.writeInt(rs.getInt("logo"));
			w.writeInt(rs.getInt("logoColor"));
			w.writeInt(rs.getInt("logoBG"));
			w.writeInt(rs.getInt("logoBGColor"));
		}
		return w.getPacket();
	}

	public static GamePacket updateGP(int gid, int GP) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GUILD_OPERATION.getValue());
		w.writeAsByte(0x48);
		w.writeInt(gid);
		w.writeInt(GP);
		return w.getPacket();
	}

	public static GamePacket skillEffect(GameCharacter from, int skillId, int level, byte flags, int speed, byte direction) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SKILL_EFFECT.getValue());
		w.writeInt(from.getId());
		w.writeInt(skillId);
		w.writeAsByte(level);
		w.writeAsByte(flags);
		w.writeAsByte(speed);
		w.writeAsByte(direction); // Mmmk
		return w.getPacket();
	}

	public static GamePacket skillCancel(GameCharacter from, int skillId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CANCEL_SKILL_EFFECT.getValue());
		w.writeInt(from.getId());
		w.writeInt(skillId);
		return w.getPacket();
	}

	public static GamePacket showMagnet(int mobid, byte success) { 
		// Monster Magnet
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_MAGNET.getValue());
		w.writeInt(mobid);
		w.writeAsByte(success);
		w.write0(10); // Mmmk
		return w.getPacket();
	}

	/**
	 * Sends a player hint.
	 * 
	 * @param hint
	 *            The hint it's going to send.
	 * @param width
	 *            How tall the box is going to be.
	 * @param height
	 *            How long the box is going to be.
	 * @return The player hint packet.
	 */
	public static GamePacket sendHint(String hint, int width, int height) {
		if (width < 1) {
			width = hint.length() * 10;
			if (width < 40) {
				width = 40;
			}
		}
		if (height < 5) {
			height = 5;
		}
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_HINT.getValue());
		w.writeLengthString(hint);
		w.writeAsShort(width);
		w.writeAsShort(height);
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket messengerInvite(String from, int messengerid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESSENGER.getValue());
		w.writeAsByte(0x03);
		w.writeLengthString(from);
		w.writeAsByte(0);
		w.writeInt(messengerid);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket sendSpouseChat(GameCharacter wife, String msg) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPOUSE_CHAT.getValue());
		w.writeLengthString(wife.getName());
		w.writeLengthString(msg);
		return w.getPacket();
	}

	public static GamePacket addMessengerPlayer(String from, GameCharacter chr, int position, byte channel) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESSENGER.getValue());
		w.writeAsByte(0x00);
		w.writeAsByte(position);
		addCharLook(w, chr, true);
		w.writeLengthString(from);
		w.writeAsByte(channel);
		w.writeAsByte(0x00);
		return w.getPacket();
	}

	public static GamePacket removeMessengerPlayer(int position) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESSENGER.getValue());
		w.writeAsByte(0x02);
		w.writeAsByte(position);
		return w.getPacket();
	}

	public static GamePacket updateMessengerPlayer(String from, GameCharacter chr, int position, byte channel) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESSENGER.getValue());
		w.writeAsByte(0x07);
		w.writeAsByte(position);
		addCharLook(w, chr, true);
		w.writeLengthString(from);
		w.writeAsByte(channel);
		w.writeAsByte(0x00);
		return w.getPacket();
	}

	public static GamePacket joinMessenger(int position) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESSENGER.getValue());
		w.writeAsByte(0x01);
		w.writeAsByte(position);
		return w.getPacket();
	}

	public static GamePacket messengerChat(String text) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESSENGER.getValue());
		w.writeAsByte(0x06);
		w.writeLengthString(text);
		return w.getPacket();
	}

	public static GamePacket messengerNote(String text, int mode, int mode2) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESSENGER.getValue());
		w.writeAsByte(mode);
		w.writeLengthString(text);
		w.writeAsByte(mode2);
		return w.getPacket();
	}

	public static void addPetInfo(PacketWriter w, Pet pet, boolean showpet) {
		w.writeAsByte(1);
		if (showpet) {
			w.writeAsByte(0);
		}

		w.writeInt(pet.getItemId());
		w.writeLengthString(pet.getName());
		w.writeInt(pet.getUniqueId());
		w.writeInt(0);
		w.writeVector(pet.getPosition());
		w.writeAsByte(pet.getStance());
		w.writeInt(pet.getFoothold());
	}

	public static GamePacket showPet(GameCharacter chr, Pet pet, boolean remove, boolean hunger) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_PET.getValue());
		w.writeInt(chr.getId());
		w.writeAsByte(chr.getPetIndex(pet));
		if (remove) {
			w.writeAsByte(0);
			w.writeAsByte(hunger);
		} else {
			addPetInfo(w, pet, true);
		}
		return w.getPacket();
	}

	public static GamePacket movePet(int cid, int pid, byte slot, List<LifeMovementFragment> moves) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MOVE_PET.getValue());
		w.writeInt(cid);
		w.writeAsByte(slot);
		w.writeInt(pid);
		serializeMovementList(w, moves);
		return w.getPacket();
	}

	public static GamePacket petChat(int cid, byte index, int act, String text) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PET_CHAT.getValue());
		w.writeInt(cid);
		w.writeAsByte(index);
		w.writeAsByte(0);
		w.writeAsByte(act);
		w.writeLengthString(text);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket commandResponse(int cid, byte index, int animation, boolean success) {
		// AE 00 01 00 00 00 00 01 00 00
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PET_COMMAND.getValue());
		w.writeInt(cid);
		w.writeAsByte(index);
		w.writeAsByte((animation == 1 || !success));
		w.writeAsByte(animation);
		if (animation == 1) {
			w.writeAsByte(0);
		} else {
			w.writeAsShort(success ? 1 : 0);
		}
		return w.getPacket();
	}

	public static GamePacket showOwnPetLevelUp(byte index) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(4);
		w.writeAsByte(0);
		w.writeAsByte(index); // Pet Index
		return w.getPacket();
	}

	public static GamePacket showPetLevelUp(GameCharacter chr, byte index) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
		w.writeInt(chr.getId());
		w.writeAsByte(4);
		w.writeAsByte(0);
		w.writeAsByte(index);
		return w.getPacket();
	}

	public static GamePacket changePetName(GameCharacter chr, String newname, int slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PET_NAMECHANGE.getValue());
		w.writeInt(chr.getId());
		w.writeAsByte(0);
		w.writeLengthString(newname);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket petStatUpdate(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_STATS.getValue());
		int mask = 0;
		mask |= Stat.PET.getValue();
		w.writeAsByte(0);
		w.writeInt(mask);
		Pet[] pets = chr.getPets();
		for (int i = 0; i < 3; i++) {
			if (pets[i] != null) {
				w.writeInt(pets[i].getUniqueId());
				w.writeInt(0);
			} else {
				w.writeLong(0);
			}
		}
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket showForcedEquip(int team) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FORCED_MAP_EQUIP.getValue());
		if (team > -1) {
			w.writeAsByte(team); // 00 = red, 01 = blue
		}
		return w.getPacket();
	}

	public static GamePacket summonSkill(int cid, int summonSkillId, int newStance) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SUMMON_SKILL.getValue());
		w.writeInt(cid);
		w.writeInt(summonSkillId);
		w.writeAsByte(newStance);
		return w.getPacket();
	}

	public static GamePacket skillCooldown(int sid, int time) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.COOLDOWN.getValue());
		w.writeInt(sid);
		w.writeAsShort(time);// Int in v97
		return w.getPacket();
	}

	public static GamePacket skillBookSuccess(GameCharacter chr, int skillid, int maxlevel, boolean canUse, boolean success) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.USE_SKILL_BOOK.getValue());
		w.writeInt(chr.getId());
		w.writeAsByte(1);
		w.writeInt(skillid);
		w.writeInt(maxlevel);
		w.writeAsByte(canUse);
		w.writeAsByte(success);
		return w.getPacket();
	}

	public static GamePacket getMacros(SkillMacro[] macros) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SKILL_MACRO.getValue());
		int count = 0;
		for (int i = 0; i < 5; i++) {
			if (macros[i] != null) {
				count++;
			}
		}
		w.writeAsByte(count);
		for (int i = 0; i < 5; i++) {
			SkillMacro macro = macros[i];
			if (macro != null) {
				w.writeLengthString(macro.name);
				w.writeAsByte(macro.shout);
				w.writeInt(macro.skill1);
				w.writeInt(macro.skill2);
				w.writeInt(macro.skill3);
			}
		}
		return w.getPacket();
	}

	public static GamePacket getPlayerNPC(PlayerNPCs npc) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_NPC.getValue());
		w.writeAsByte(0x01);
		w.writeInt(npc.getId());
		w.writeLengthString(npc.getName());
		w.writeAsByte(0); // direction
		w.writeAsByte(npc.getSkin());
		w.writeInt(npc.getFace());
		w.writeAsByte(0);
		w.writeInt(npc.getHair());
		Map<Byte, Integer> equip = npc.getEquips();
		Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
		for (byte position : equip.keySet()) {
			byte pos = (byte) (position * -1);
			if (pos > 100) {
				pos -= 100;
				myEquip.put(pos, equip.get(position));
			} else {
				if (myEquip.get(pos) == null) {
					myEquip.put(pos, equip.get(position));
				}
			}
		}
		for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
			w.writeAsByte(entry.getKey());
			w.writeInt(entry.getValue());
		}
		w.writeAsShort(-1);
		Integer cWeapon = equip.get((byte) -111);
		if (cWeapon != null) {
			w.writeInt(cWeapon);
		} else {
			w.writeInt(0);
		}
		for (int i = 0; i < 12; i++) {
			w.writeAsByte(0);
		}
		return w.getPacket();
	}

	public static GamePacket updateAriantPQRanking(String name, int score, boolean isEmpty) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ARIANT_SCORE.getValue());
		w.writeAsByte(!isEmpty);
		if (!isEmpty) {
			w.writeLengthString(name);
			w.writeInt(score);
		}
		return w.getPacket();
	}

	public static GamePacket catchMonster(int monsobid, int itemid, byte success) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CATCH_MONSTER.getValue());
		w.writeInt(monsobid);
		w.writeInt(itemid);
		w.writeAsByte(success);
		return w.getPacket();
	}

	public static GamePacket catchMessage(int message) { // not done, I guess
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CATCH_MESSAGE.getValue());
		w.writeAsByte(message); // 1 = too strong, 2 = Elemental Rock
		w.writeInt(0);// Maybe itemid?
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket showAllCharacter(int chars, int unk) {
		PacketWriter w = new PacketWriter(11);
		w.writeAsShort(SendOpcode.ALL_CHARLIST.getValue());
		w.writeAsByte(1);
		w.writeInt(chars);
		w.writeInt(unk);
		return w.getPacket();
	}

	public static GamePacket showAllCharacterInfo(byte worldid, List<GameCharacter> chars) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALL_CHARLIST.getValue());
		w.writeAsByte(0);
		w.writeAsByte(worldid);
		w.writeAsByte(chars.size());
		for (GameCharacter chr : chars) {
			addCharEntry(w, chr, true);
		}
		return w.getPacket();
	}

	public static GamePacket updateMount(int charid, Mount mount, boolean levelup) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_MOUNT.getValue());
		w.writeInt(charid);
		w.writeInt(mount.getLevel());
		w.writeInt(mount.getExp());
		w.writeInt(mount.getTiredness());
		w.writeAsByte(levelup ? (byte) 1 : (byte) 0);
		return w.getPacket();
	}

	public static GamePacket boatPacket(boolean type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BOAT_EFFECT.getValue());
		w.writeAsShort(type ? 1 : 2);
		return w.getPacket();
	}

	public static GamePacket getOmokStats(GameClient c, Minigame minigame, boolean isOwner, int piece) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.ROOM.getCode());
		w.writeAsByte(1);
		w.writeAsByte(0);
		w.writeAsByte(!isOwner);
		w.writeAsByte(0);
		final GameCharacter owner = minigame.getOwner();
		addCharLook(w, owner, false);
		w.writeLengthString(owner.getName());
		
		GameCharacter visitor = minigame.getVisitor();
		if (visitor != null) {
			w.writeAsByte(1);
			addCharLook(w, visitor, false);
			w.writeLengthString(visitor.getName());
		}
		
		w.writeAsByte(0xFF);
		w.writeAsByte(0);
		w.writeInt(1);
		
		final MinigameStats ownerStats = owner.getOmokStats();
		ownerStats.serialize(w);
		
		if (visitor != null) {
			w.writeAsByte(1);
			w.writeInt(1);

			final MinigameStats visitorStats = visitor.getOmokStats();
			visitorStats.serialize(w);
		}
		
		w.writeAsByte(0xFF);
		w.writeLengthString(minigame.getDescription());
		w.writeAsByte(piece);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket getMiniGameReady(Minigame game) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.READY.getCode());
		return w.getPacket();
	}

	public static GamePacket getMiniGameUnReady(Minigame game) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.UN_READY.getCode());
		return w.getPacket();
	}

	public static GamePacket getMiniGameStart(Minigame game, int loser) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.START.getCode());
		w.writeAsByte(loser);
		return w.getPacket();
	}

	public static GamePacket getMiniGameSkipOwner(Minigame game) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.SKIP.getCode());
		w.writeAsByte(0x01);
		return w.getPacket();
	}

	public static GamePacket getMiniGameRequestTie(Minigame game) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.REQUEST_TIE.getCode());
		return w.getPacket();
	}

	public static GamePacket getMiniGameDenyTie(Minigame game) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.ANSWER_TIE.getCode());
		return w.getPacket();
	}

	public static GamePacket getMiniGameFull() {
		PacketWriter w = new PacketWriter(5);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.ROOM.getCode());
		w.writeAsByte(0);
		w.writeAsByte(2);
		return w.getPacket();
	}

	public static GamePacket getMiniGameSkipVisitor(Minigame game) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsShort(PlayerInteractionHandler.Action.SKIP.getCode());
		return w.getPacket();
	}

	public static GamePacket getMiniGameMoveOmok(Minigame game, int move1, int move2, int move3) {
		PacketWriter w = new PacketWriter(12);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.MOVE_OMOK.getCode());
		w.writeInt(move1);
		w.writeInt(move2);
		w.writeAsByte(move3);
		return w.getPacket();
	}

	public static GamePacket getOmokNewVisitor(GameCharacter c, int slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.VISIT.getCode());
		w.writeAsByte(slot);
		addCharLook(w, c, false);
		w.writeLengthString(c.getName());
		w.writeInt(1);
		final MinigameStats stats = c.getOmokStats();
		stats.serialize(w);
		return w.getPacket();
	}

	public static GamePacket getMiniGameRemoveVisitor() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.EXIT.getCode());
		w.writeAsByte(1);
		return w.getPacket();
	}
	
	private enum MinigameResult {
		OWNER_WIN,
		VISITOR_WIN,
		OWNER_FORFEIT,
		VISITOR_FORFEIT,
		TIE;
	}

	private static GamePacket getMiniGameResult(Minigame game, MinigameResult result, boolean omok) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.GET_RESULT.getCode());
		
		final GameCharacter owner = game.getOwner();
		final MinigameStats ownerStats = omok ? owner.getOmokStats() : owner.getMatchingCardStats();
		
		final GameCharacter visitor = game.getVisitor();
		final MinigameStats visitorStats = omok ? visitor.getOmokStats() : visitor.getMatchingCardStats();

		switch (result) {
		case OWNER_WIN:
		case VISITOR_FORFEIT:
			MinigameStats.processOwnerWin(ownerStats, visitorStats);
			w.writeAsByte(0);
			break;
		case TIE:
			MinigameStats.processTie(ownerStats, visitorStats);
			w.writeAsByte(1);
			break;
		case VISITOR_WIN:
		case OWNER_FORFEIT:
			MinigameStats.processOwnerLoss(ownerStats, visitorStats);
			w.writeAsByte(2);
			break;
		}

		w.writeAsByte(0); // owner
		w.writeInt(1); // unknown
		
		ownerStats.serialize(w);
		w.writeInt(1); // start of visitor; unknown
		visitorStats.serialize(w);
		return w.getPacket();
	}

	public static GamePacket getMiniGameOwnerWin(Minigame game) {
		return getMiniGameResult(game, MinigameResult.OWNER_WIN, true);
	}

	public static GamePacket getMiniGameVisitorWin(Minigame game) {
		return getMiniGameResult(game, MinigameResult.VISITOR_WIN, true);
	}

	public static GamePacket getMiniGameTie(Minigame game) {
		return getMiniGameResult(game, MinigameResult.TIE, true);
	}

	public static GamePacket getMiniGameOwnerForfeit(Minigame game) {
		return getMiniGameResult(game, MinigameResult.OWNER_FORFEIT, true);
	}

	public static GamePacket getMiniGameVisitorForfeit(Minigame game) {
		return getMiniGameResult(game, MinigameResult.VISITOR_FORFEIT, true);
	}

	public static GamePacket getMiniGameClose() {
		PacketWriter w = new PacketWriter(5);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.EXIT.getCode());
		w.writeAsByte(1);
		w.writeAsByte(3);
		return w.getPacket();
	}

	public static GamePacket getMatchCard(GameClient c, Minigame minigame, boolean isOwner, int piece) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.ROOM.getCode());
		w.writeAsByte(2);
		w.writeAsByte(2);
		w.writeAsByte(!isOwner);
		w.writeAsByte(0);
		final GameCharacter owner = minigame.getOwner();
		addCharLook(w, owner, false);
		w.writeLengthString(owner.getName());
		
		final GameCharacter visitor = minigame.getVisitor();
		if (visitor != null) {
			w.writeAsByte(1);
			addCharLook(w, visitor, false);
			w.writeLengthString(visitor.getName());
		}
		
		w.writeAsByte(0xFF);
		w.writeAsByte(0);
		w.writeInt(2);
		
		final MinigameStats ownerStats = owner.getMatchingCardStats();
		ownerStats.serialize(w);
		
		if (visitor != null) {
			final MinigameStats visitorStats = visitor.getMatchingCardStats();
			w.writeAsByte(1);
			w.writeInt(2);
			visitorStats.serialize(w);
		}
		
		w.writeAsByte(0xFF);
		w.writeLengthString(minigame.getDescription());
		w.writeAsByte(piece);
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket getMatchCardStart(Minigame game, int loser) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.START.getCode());
		w.writeAsByte(loser);
		w.writeAsByte(0x0C);
		int last = 13;
		if (game.getMatchesToWin() > 10) {
			last = 31;
		} else if (game.getMatchesToWin() > 6) {
			last = 21;
		}
		for (int i = 1; i < last; i++) {
			w.writeInt(game.getCardId(i));
		}
		return w.getPacket();
	}

	public static GamePacket getMatchCardNewVisitor(GameCharacter c, int slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.VISIT.getCode());
		w.writeAsByte(slot);
		addCharLook(w, c, false);
		w.writeLengthString(c.getName());
		w.writeInt(1);
		
		final MinigameStats ownerStats = c.getMatchingCardStats();
		ownerStats.serialize(w);
		return w.getPacket();
	}

	public static GamePacket getMatchCardSelect(Minigame game, int turn, int slot, int firstslot, int type) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.SELECT_CARD.getCode());
		w.writeAsByte(turn);
		if (turn == 1) {
			w.writeAsByte(slot);
		} else if (turn == 0) {
			w.writeAsByte(slot);
			w.writeAsByte(firstslot);
			w.writeAsByte(type);
		}
		return w.getPacket();
	}

	public static GamePacket getMatchCardOwnerWin(Minigame game) {
		return getMiniGameResult(game, MinigameResult.OWNER_WIN, false);
	}

	public static GamePacket getMatchCardVisitorWin(Minigame game) {
		return getMiniGameResult(game, MinigameResult.VISITOR_WIN, false);
	}

	public static GamePacket getMatchCardTie(Minigame game) {
		return getMiniGameResult(game, MinigameResult.TIE, false);
	}

	public static GamePacket fredrickMessage(byte operation) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FREDRICK_MESSAGE.getValue());
		w.writeAsByte(operation);
		return w.getPacket();
	}

	public static GamePacket getFredrick(byte op) {
		final PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FREDRICK.getValue());
		w.writeAsByte(op);

		switch (op) {
			case 0x24:
				w.write0(8);
				break;
			default:
				w.writeAsByte(0);
				break;
		}

		return w.getPacket();
	}

	public static GamePacket getFredrick(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FREDRICK.getValue());
		w.writeAsByte(0x23);
		w.writeInt(9030000); // Fredrick
		w.writeInt(32272); // id
		w.write0(5);
		w.writeInt(chr.getMerchantMeso());
		w.writeAsByte(0);
		try {
			List<ItemInventoryEntry> entries = ItemFactory.MERCHANT.loadItems(chr.getId(), false);
			w.writeAsByte(entries.size());

			for (int i = 0; i < entries.size(); i++) {
				addItemInfo(w, entries.get(i).item, true);
			}
		} catch (SQLException e) {
		}
		w.write0(3);
		return w.getPacket();
	}

	public static GamePacket addOmokBox(GameCharacter c, int amount, int type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
		w.writeInt(c.getId());
		addAnnounceBox(w, c.getActiveMinigame(), 1, 0, amount, type);
		return w.getPacket();
	}

	public static GamePacket removeOmokBox(GameCharacter c) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
		w.writeInt(c.getId());
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket addMatchCardBox(GameCharacter c, int amount, int type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
		w.writeInt(c.getId());
		addAnnounceBox(w, c.getActiveMinigame(), 2, 0, amount, type);
		return w.getPacket();
	}

	public static GamePacket removeMatchcardBox(GameCharacter c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_CHAR_BOX.getValue());
		w.writeInt(c.getId());
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket getPlayerShopChat(GameCharacter c, String chat, byte slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT.getCode());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
		w.writeAsByte(slot);
		w.writeLengthString(c.getName() + " : " + chat);
		return w.getPacket();
	}

	public static GamePacket getTradeChat(GameCharacter c, String chat, boolean isOwner) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT.getCode());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
		w.writeAsByte(!isOwner);
		w.writeLengthString(c.getName() + " : " + chat);
		return w.getPacket();
	}

	public static GamePacket hiredMerchantBox() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SEND_TITLE_BOX.getValue()); // header.
		w.writeAsByte(0x07);
		return w.getPacket();
	}

	public static GamePacket owlOfMinerva(GameClient c, int itemid, List<HiredMerchant> hms, List<PlayerShopItem> items) { 
		// Thanks moongra, you save me some time :)
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.OWL_OF_MINERVA.getValue());
		w.writeAsByte(6);
		w.writeInt(0);
		w.writeInt(itemid);
		w.writeInt(hms.size());
		for (HiredMerchant hm : hms) {
			for (PlayerShopItem item : items) {
				w.writeLengthString(hm.getOwner());
				w.writeInt(hm.getMapId());
				w.writeLengthString(hm.getDescription());
				w.writeInt(item.getItem().getQuantity());
				w.writeInt(item.getBundles());
				w.writeInt(item.getPrice());
				w.writeInt(hm.getOwnerId());
				final boolean isFull = hm.getFreeSlot() == -1;
				w.writeAsByte(isFull);
				GameCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterById(hm.getOwnerId());
				if ((chr != null) && (c.getChannel() == hm.getChannel())) {
					w.writeAsByte(1);
				} else {
					w.writeAsByte(2);
				}

				if (item.getItem().getItemId() / 1000000 == 1) {
					addItemInfo(w, item.getItem(), true);
				}
			}
		}
		return w.getPacket();
	}

	public static GamePacket retrieveFirstMessage() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SEND_TITLE_BOX.getValue()); // header.
		w.writeAsByte(0x09);
		return w.getPacket();
	}

	public static GamePacket remoteChannelChange(byte ch) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SEND_TITLE_BOX.getValue()); // header.
		w.writeAsByte(0x10);
		w.writeInt(0);// No idea yet
		w.writeAsByte(ch);
		return w.getPacket();
	}

	/*
	 * Possible things for SEND_TITLE_BOX 0x0E = 00 = Renaming Failed - Can't
	 * find the merchant, 01 = Renaming succesful 0x10 = Changes channel to the
	 * store (Store is open at Channel 1, do you want to change channels?) 0x11
	 * = You cannot sell any items when managing.. blabla 0x12 = FKING POPUP LOL
	 */

	public static GamePacket getHiredMerchant(GameCharacter chr, HiredMerchant hm, boolean isFirstTime) {
		// Thanks Dustin
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.ROOM.getCode());
		w.writeAsByte(0x05);
		w.writeAsByte(0x04);
		w.writeAsShort(hm.getVisitorSlot(chr) + 1);
		w.writeInt(hm.getItemId());
		w.writeLengthString("Hired Merchant");
		for (int i = 0; i < 3; i++) {
			if (hm.getVisitors()[i] != null) {
				w.writeAsByte(i + 1);
				addCharLook(w, hm.getVisitors()[i], false);
				w.writeLengthString(hm.getVisitors()[i].getName());
			}
		}
		w.writeAsByte(-1);
		if (hm.isOwner(chr)) {
			w.writeAsShort(hm.getMessages().size());
			for (int i = 0; i < hm.getMessages().size(); i++) {
				w.writeLengthString(hm.getMessages().get(i).message);
				w.writeAsByte(hm.getMessages().get(i).slot);
			}
		} else {
			w.writeAsShort(0);
		}
		w.writeLengthString(hm.getOwner());
		if (hm.isOwner(chr)) {
			w.writeInt(hm.getTimeLeft());
			w.writeAsByte(isFirstTime);
			// List<SoldItem> sold = hm.getSold();
			w.writeAsByte(0);// sold.size()
			/*
			 * for (SoldItem s : sold) { fix this w.writeInt(s.getItemId());
			 * w.writeShort(s.getQuantity()); w.writeInt(s.getMesos());
			 * w.writeMapleAsciiString(s.getBuyer()); }
			 */
			w.writeInt(chr.getMerchantMeso());// :D?
		}
		w.writeLengthString(hm.getDescription());
		w.writeAsByte(0x10); // SLOTS, which is 16 for most stores...slotMax
		w.writeInt(chr.getMeso());
		w.writeAsByte(hm.getItems().size());
		if (hm.getItems().isEmpty()) {
			w.writeAsByte(0);// Hmm??
		} else {
			for (PlayerShopItem item : hm.getItems()) {
				w.writeAsShort(item.getBundles());
				w.writeAsShort(item.getItem().getQuantity());
				w.writeInt(item.getPrice());
				addItemInfo(w, item.getItem(), true);
			}
		}
		return w.getPacket();
	}

	public static GamePacket updateHiredMerchant(HiredMerchant hm, GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.UPDATE_MERCHANT.getCode());
		w.writeInt(chr.getMeso());
		w.writeAsByte(hm.getItems().size());
		for (PlayerShopItem item : hm.getItems()) {
			w.writeAsShort(item.getBundles());
			w.writeAsShort(item.getItem().getQuantity());
			w.writeInt(item.getPrice());
			addItemInfo(w, item.getItem(), true);
		}
		return w.getPacket();
	}

	public static GamePacket hiredMerchantChat(String message, byte slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT.getCode());
		w.writeAsByte(PlayerInteractionHandler.Action.CHAT_THING.getCode());
		w.writeAsByte(slot);
		w.writeLengthString(message);
		return w.getPacket();
	}

	public static GamePacket hiredMerchantVisitorLeave(int slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.EXIT.getCode());
		if (slot != 0) {
			w.writeAsByte(slot);
		}
		return w.getPacket();
	}

	public static GamePacket hiredMerchantOwnerLeave() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.REAL_CLOSE_MERCHANT.getCode());
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket leaveHiredMerchant(int slot, int status2) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.EXIT.getCode());
		w.writeAsByte(slot);
		w.writeAsByte(status2);
		return w.getPacket();
	}

	public static GamePacket hiredMerchantVisitorAdd(GameCharacter chr, int slot) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(PlayerInteractionHandler.Action.VISIT.getCode());
		w.writeAsByte(slot);
		addCharLook(w, chr, false);
		w.writeLengthString(chr.getName());
		return w.getPacket();
	}

	public static GamePacket spawnHiredMerchant(HiredMerchant hm) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_HIRED_MERCHANT.getValue());
		w.writeInt(hm.getOwnerId());
		w.writeInt(hm.getItemId());
		w.writeAsShort((short) hm.getPosition().getX());
		w.writeAsShort((short) hm.getPosition().getY());
		w.writeAsShort(0);
		w.writeLengthString(hm.getOwner());
		w.writeAsByte(0x05);
		w.writeInt(hm.getObjectId());
		w.writeLengthString(hm.getDescription());
		w.writeAsByte(hm.getItemId() % 10);
		w.write(new byte[] {1, 4});
		return w.getPacket();
	}

	public static GamePacket destroyHiredMerchant(int id) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DESTROY_HIRED_MERCHANT.getValue());
		w.writeInt(id);
		return w.getPacket();
	}

	public static GamePacket spawnPlayerNPC(PlayerNPCs npc) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
		w.writeAsByte(1);
		w.writeInt(npc.getObjectId());
		w.writeInt(npc.getId());
		w.writeAsShort(npc.getPosition().x);
		w.writeAsShort(npc.getCY());
		w.writeAsByte(1);
		w.writeAsShort(npc.getFH());
		w.writeAsShort(npc.getRX0());
		w.writeAsShort(npc.getRX1());
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket sendYellowTip(String tip) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.YELLOW_TIP.getValue());
		w.writeAsByte(0xFF);
		w.writeLengthString(tip);
		w.writeAsShort(0);
		return w.getPacket();
	}

	public static GamePacket giveInfusion(int buffid, int bufflength, int speed) {
		// This ain't correct
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_BUFF.getValue());
		w.writeLong(BuffStat.SPEED_INFUSION.getValue());
		w.writeLong(0);
		w.writeAsShort(speed);
		w.writeInt(buffid);
		w.writeAsByte(0);
		w.writeAsShort(bufflength);
		w.writeAsShort(0);
		w.writeAsShort(0);
		w.writeAsShort(0);
		return w.getPacket();
	}

	public static GamePacket givePirateBuff(List<BuffStatDelta> statups, int buffid, int duration) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_BUFF.getValue());
		writeLongMask(w, statups);
		w.writeAsShort(0);
		for (BuffStatDelta stat : statups) {
			w.writeInt(stat.delta);
			w.writeInt(buffid);
			w.write0(5);
			w.writeAsShort(duration);
		}
		w.write0(3);
		return w.getPacket();
	}

	public static GamePacket giveForeignDash(int cid, int buffid, int time, List<BuffStatDelta> statups) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
		w.writeInt(cid);
		writeLongMask(w, statups);
		w.writeAsShort(0);
		for (BuffStatDelta statup : statups) {
			w.writeInt(statup.delta);
			w.writeInt(buffid);
			w.write0(5);
			w.writeAsShort(time);
		}
		w.writeAsShort(0);
		w.writeAsByte(2);
		return w.getPacket();
	}

	public static GamePacket giveForeignInfusion(int cid, int speed, int duration) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_FOREIGN_BUFF.getValue());
		w.writeInt(cid);
		w.writeLong(BuffStat.SPEED_INFUSION.getValue());
		w.writeLong(0);
		w.writeAsShort(0);
		w.writeInt(speed);
		w.writeInt(5121009);
		w.writeLong(0);
		w.writeInt(duration);
		w.writeAsShort(0);
		return w.getPacket();
	}

	public static GamePacket sendMts(List<MTSItemInfo> items, int tab, int type, int page, int pages) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x15); // operation
		w.writeInt(pages * 16); // testing, change to 10 if fails
		w.writeInt(items.size()); // number of items
		w.writeInt(tab);
		w.writeInt(type);
		w.writeInt(page);
		w.writeAsByte(1);
		w.writeAsByte(1);
		for (int i = 0; i < items.size(); i++) {
			MTSItemInfo item = items.get(i);
			addItemInfo(w, item.getItem(), true);
			w.writeInt(item.getID()); // id
			w.writeInt(item.getTaxes()); // this + below = price
			w.writeInt(item.getPrice()); // price
			w.writeLong(0);
			w.writeInt(getQuestTimestamp(item.getEndingDate()));
			w.writeLengthString(item.getSeller()); // account name (what
															// was nexon
															// thinking?)
			w.writeLengthString(item.getSeller()); // char name
			for (int j = 0; j < 28; j++) {
				w.writeAsByte(0);
			}
		}
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket noteSendMsg() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.NOTE_ACTION.getValue());
		w.writeAsByte(4);
		return w.getPacket();
	}

	/*
	 * 0 = Player online, use whisper 1 = Check player's name 2 = Receiver inbox
	 * full
	 */
	public static GamePacket noteError(byte error) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.NOTE_ACTION.getValue());
		w.writeAsByte(5);
		w.writeAsByte(error);
		return w.getPacket();
	}

	public static GamePacket showNotes(ResultSet notes, int count) throws SQLException {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.NOTE_ACTION.getValue());
		w.writeAsByte(3);
		w.writeAsByte(count);
		for (int i = 0; i < count; i++) {
			w.writeInt(notes.getInt("id"));
			// Stupid nexon forgot space lol
			w.writeLengthString(notes.getString("from") + " ");
			w.writeLengthString(notes.getString("message"));
			w.writeLong(getKoreanTimestamp(notes.getLong("timestamp")));
			w.writeAsByte(notes.getByte("fame"));// FAME :D
			notes.next();
		}
		return w.getPacket();
	}

	public static GamePacket useChalkboard(GameCharacter chr, boolean close) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CHALKBOARD.getValue());
		w.writeInt(chr.getId());
		if (close) {
			w.writeAsByte(0);
		} else {
			w.writeAsByte(1);
			w.writeLengthString(chr.getChalkboard());
		}
		return w.getPacket();
	}

	public static GamePacket refreshTeleportRockMaps(GameCharacter chr, boolean delete, boolean vip) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.TROCK_LOCATIONS.getValue());
		w.writeAsByte(delete ? 2 : 3);

		final TeleportRockInfo info = chr.getTeleportRockInfo();
		if (vip) {
			w.writeAsByte(1);
			final List<Integer> maps = info.getVipMaps();
			for (int i = 0; i < 10; i++) {
				if (i < maps.size()) {
					w.writeInt(maps.get(i));
				} else {
					w.writeInt(999999999);
				}
			}
		} else {
			w.writeAsByte(0);
			final List<Integer> maps = info.getRegularMaps();
			for (int i = 0; i < 10; i++) {
				if (i < maps.size()) {
					w.writeInt(maps.get(i));
				} else {
					w.writeInt(999999999);
				}
			}
		}
		return w.getPacket();
	}

	public static GamePacket showMTSCash(GameCharacter p) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION2.getValue());
		w.writeInt(p.getCashShop().getCash(4));
		w.writeInt(p.getCashShop().getCash(2));
		return w.getPacket();
	}

	public static GamePacket MTSWantedListingOver(int nx, int items) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x3D);
		w.writeInt(nx);
		w.writeInt(items);
		return w.getPacket();
	}

	public static GamePacket MTSConfirmSell() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x1D);
		return w.getPacket();
	}

	public static GamePacket MTSConfirmBuy() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x33);
		return w.getPacket();
	}

	public static GamePacket MTSFailBuy() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x34);
		w.writeAsByte(0x42);
		return w.getPacket();
	}

	public static GamePacket MTSConfirmTransfer(int quantity, int pos) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x27);
		w.writeInt(quantity);
		w.writeInt(pos);
		return w.getPacket();
	}

	public static GamePacket notYetSoldInv(List<MTSItemInfo> items) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x23);
		w.writeInt(items.size());
		if (!items.isEmpty()) {
			for (MTSItemInfo item : items) {
				addItemInfo(w, item.getItem(), true);
				w.writeInt(item.getID()); // id
				w.writeInt(item.getTaxes()); // this + below = price
				w.writeInt(item.getPrice()); // price
				w.writeLong(0);
				w.writeInt(getQuestTimestamp(item.getEndingDate()));
				
				// account name (what was nexon thinking?)
				w.writeLengthString(item.getSeller()); 
				// char name
				w.writeLengthString(item.getSeller()); 
				for (int i = 0; i < 28; i++) {
					w.writeAsByte(0);
				}
			}
		} else {
			w.writeInt(0);
		}
		return w.getPacket();
	}

	public static GamePacket transferInventory(List<MTSItemInfo> items) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MTS_OPERATION.getValue());
		w.writeAsByte(0x21);
		w.writeInt(items.size());
		if (!items.isEmpty()) {
			for (MTSItemInfo item : items) {
				addItemInfo(w, item.getItem(), true);
				w.writeInt(item.getID()); // id
				w.writeInt(item.getTaxes()); // taxes
				w.writeInt(item.getPrice()); // price
				w.writeLong(0);
				w.writeInt(getQuestTimestamp(item.getEndingDate()));

				// account name (what was nexon thinking?)
				w.writeLengthString(item.getSeller()); 
				// char name
				w.writeLengthString(item.getSeller());
				for (int i = 0; i < 28; i++) {
					w.writeAsByte(0);
				}
			}
		}
		w.writeAsByte(0xD0 + items.size());
		w.write(new byte[] {-1, -1, -1, 0});
		return w.getPacket();
	}

	public static GamePacket showCouponRedeemedItem(int itemid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());
		w.writeAsShort(0x49); // v72
		w.writeInt(0);
		w.writeInt(1);
		w.writeAsShort(1);
		w.writeAsShort(0x1A);
		w.writeInt(itemid);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket showCash(GameCharacter player) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_CASH.getValue());

		w.writeInt(player.getCashShop().getCash(1));
		w.writeInt(player.getCashShop().getCash(2));
		w.writeInt(player.getCashShop().getCash(4));

		return w.getPacket();
	}

	public static GamePacket enableCSUse() {
		PacketWriter w = new PacketWriter();
		w.writeAsByte(0x12);
		w.write0(6);
		return w.getPacket();
	}

	/**
	 * 
	 * @param target
	 * @param mapid
	 * @param MTSmapCSchannel
	 *            0: MTS 1: Map 2: CS 3: Different Channel
	 * @return
	 */
	public static GamePacket getFindReply(String target, int mapid, int MTSmapCSchannel) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.WHISPER.getValue());
		w.writeAsByte(9);
		w.writeLengthString(target);
		w.writeAsByte(MTSmapCSchannel); // 0: mts 1: map 2: cs
		w.writeInt(mapid); // -1 if mts, cs
		if (MTSmapCSchannel == 1) {
			w.write(new byte[8]);
		}
		return w.getPacket();
	}

	public static GamePacket sendAutoHpPot(int itemId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.AUTO_HP_POT.getValue());
		w.writeInt(itemId);
		return w.getPacket();
	}

	public static GamePacket sendAutoMpPot(int itemId) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.AUTO_MP_POT.getValue());
		w.writeInt(itemId);
		return w.getPacket();
	}

	public static GamePacket showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.OX_QUIZ.getValue());
		w.writeAsByte(askQuestion);
		w.writeAsByte(questionSet);
		w.writeAsShort(questionId);
		return w.getPacket();
	}

	public static GamePacket updateGender(GameCharacter chr) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.GENDER.getValue());
		w.writeAsByte(chr.getGender());
		return w.getPacket();
	}

	public static GamePacket enableReport() { // by snow
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.ENABLE_REPORT.getValue());
		w.writeAsByte(1);
		return w.getPacket();
	}

	public static GamePacket giveFinalAttack(int skillid, int time) {
		// packets found by lailainoob
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GIVE_BUFF.getValue());
		w.writeLong(0);
		w.writeAsShort(0);
		w.writeAsByte(0);// some 80 and 0 bs DIRECTION
		w.writeAsByte(0x80);// let's just do 80, then 0
		w.writeInt(0);
		w.writeAsShort(1);
		w.writeInt(skillid);
		w.writeInt(time);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket loadFamily(GameCharacter player) {
		String[] title = {"Family Reunion", "Summon Family", "My Drop Rate 1.5x (15 min)", "My EXP 1.5x (15 min)", "Family Bonding (30 min)", "My Drop Rate 2x (15 min)", "My EXP 2x (15 min)", "My Drop Rate 2x (30 min)", "My EXP 2x (30 min)", "My Party Drop Rate 2x (30 min)", "My Party EXP 2x (30 min)"};
		String[] description = {"[Target] Me\n[Effect] Teleport directly to the Family member of your choice.", "[Target] 1 Family member\n[Effect] Summon a Family member of choice to the map you're in.", "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the Drop Rate event is in progress, this will be nullified.", "[Target] Me\n[Time] 15 min.\n[Effect] EXP earned from hunting will be increased #c1.5x#.\n* If the EXP event is in progress, this will be nullified.", "[Target] At least 6 Family members online that are below me in the Pedigree\n[Time] 30 min.\n[Effect] Monster drop rate and EXP earned will be increased #c2x#. \n* If the EXP event is in progress, this will be nullified.", "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.", "[Target] Me\n[Time] 15 min.\n[Effect] EXP earned from hunting will be increased #c2x#.\n* If the EXP event is in progress, this will be nullified.", "[Target] Me\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.", "[Target] Me\n[Time] 30 min.\n[Effect] EXP earned from hunting will be increased #c2x#. \n* If the EXP event is in progress, this will be nullified.", "[Target] My party\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c2x#.\n* If the Drop Rate event is in progress, this will be nullified.", "[Target] My party\n[Time] 30 min.\n[Effect] EXP earned from hunting will be increased #c2x#.\n* If the EXP event is in progress, this will be nullified."};
		int[] repCost = {3, 5, 7, 8, 10, 12, 15, 20, 25, 40, 50};
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.LOAD_FAMILY.getValue());
		w.writeInt(11);
		for (int i = 0; i < 11; i++) {
			w.writeAsByte(i > 4 ? (i % 2) + 1 : i);
			w.writeInt(repCost[i] * 100);
			w.writeInt(1);
			w.writeLengthString(title[i]);
			w.writeLengthString(description[i]);
		}
		return w.getPacket();
	}

	public static GamePacket sendFamilyMessage() {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.FAMILY_MESSAGE.getValue());
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket getFamilyInfo(FamilyEntry f) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.OPEN_FAMILY.getValue());
		w.writeInt(f.getReputation()); // cur rep left
		w.writeInt(f.getTotalReputation()); // tot rep left
		w.writeInt(f.getTodaysRep()); // todays rep
		w.writeAsShort(f.getJuniors()); // juniors added
		w.writeAsShort(f.getTotalJuniors()); // juniors allowed
		w.writeAsShort(0); // Unknown
		w.writeInt(f.getId()); // id?
		w.writeLengthString(f.getFamilyName());
		w.writeInt(0);
		w.writeAsShort(0);
		return w.getPacket();
	}

	public static GamePacket showPedigree(int chrid, Map<Integer, FamilyEntry> members) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_PEDIGREE.getValue());
		// Hmmm xD
		return w.getPacket();
	}

	public static GamePacket updateAreaInfo(String mode, int quest) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(0x0A); // 0x0B in v95
		w.writeAsShort(quest);
		w.writeLengthString(mode);
		return w.getPacket();
	}

	public static GamePacket questProgress(short id, String process) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(1);
		w.writeAsShort(id);
		w.writeAsByte(1);
		w.writeLengthString(process);
		return w.getPacket();
	}

	public static GamePacket getItemMessage(int itemid) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(7);
		w.writeInt(itemid);
		return w.getPacket();
	}

	public static GamePacket addCard(boolean isFull, int cardid, int level) {
		PacketWriter w = new PacketWriter(11);
		w.writeAsShort(SendOpcode.MONSTERBOOK_ADD.getValue());
		w.writeAsByte(!isFull);
		w.writeInt(cardid);
		w.writeInt(level);
		return w.getPacket();
	}

	public static GamePacket showGainCard() {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(0x0D);
		return w.getPacket();
	}

	public static GamePacket showForeginCardEffect(int id) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
		w.writeInt(id);
		w.writeAsByte(0x0D);
		return w.getPacket();
	}

	public static GamePacket changeCover(int cardid) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.MONSTER_BOOK_CHANGE_COVER.getValue());
		w.writeInt(cardid);
		return w.getPacket();
	}

	public static GamePacket aranGodlyStats() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.TEMPORARY_STATS.getValue());
		w.write(new byte[] {(byte) 0x1F, (byte) 0x0F, 0, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0xFF, 0, (byte) 0xE7, 3, (byte) 0xE7, 3, (byte) 0x78, (byte) 0x8C});
		return w.getPacket();
	}

	public static GamePacket showIntro(String path) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(0x12);
		w.writeLengthString(path);
		return w.getPacket();
	}

	public static GamePacket showInfo(String path) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(0x17);
		w.writeLengthString(path);
		w.writeInt(1);
		return w.getPacket();
	}

	/**
	 * Sends a UI utility. 0x01 - Equipment Inventory. 0x02 - Stat Window. 0x03
	 * - Skill Window. 0x05 - Keyboard Settings. 0x06 - Quest window. 0x09 -
	 * Monsterbook Window. 0x0A - Char Info 0x0B - Guild BBS 0x12 - Monster
	 * Carnival Window 0x16 - Party Search. 0x17 - Item Creation Window. 0x1A -
	 * My Ranking O.O 0x1B - Family Window 0x1C - Family Pedigree 0x1D - GM
	 * Story Board /funny shet 0x1E - Envelop saying you got mail from an admin.
	 * lmfao 0x1F - Medal Window 0x20 - Maple Event (???) 0x21 - Invalid Pointer
	 * Crash
	 * 
	 * @param ui
	 * @return
	 */
	public static GamePacket openUI(byte ui) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.OPEN_UI.getValue());
		w.writeAsByte(ui);
		return w.getPacket();
	}

	public static GamePacket lockUI(boolean enable) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.LOCK_UI.getValue());
		w.writeAsByte(enable);
		return w.getPacket();
	}

	public static GamePacket disableUI(boolean enable) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DISABLE_UI.getValue());
		w.writeAsByte(enable);
		return w.getPacket();
	}

	public static GamePacket itemMegaphone(String msg, boolean showMegaphoneEar, byte channel, IItem item) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SERVERMESSAGE.getValue());
		w.writeAsByte(8);
		w.writeLengthString(msg);
		w.writeAsByte(channel - 1);
		w.writeAsByte(showMegaphoneEar);
		if (item == null) {
			w.writeAsByte(0);
		} else {
			w.writeAsByte(item.getSlot());
			addItemInfo(w, item, true);
		}
		return w.getPacket();
	}

	public static GamePacket removeNPC(int oid) { // Make npc's invisible
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
		w.writeAsByte(0);
		w.writeInt(oid);
		return w.getPacket();
	}

	/**
	 * Sends a report response
	 * 
	 * Possible values for <code>mode</code>:<br>
	 * 0: You have succesfully reported the user.<br>
	 * 1: Unable to locate the user.<br>
	 * 2: You may only report users 10 times a day.<br>
	 * 3: You have been reported to the GM's by a user.<br>
	 * 4: Your request did not go through for unknown reasons. Please try again
	 * later.<br>
	 * 
	 * @param mode
	 *            The mode
	 * @return Report Reponse packet
	 */
	public static GamePacket reportResponse(ReportResponseType type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.REPORT_RESPONSE.getValue());
		w.writeAsByte(type.asByte());
		return w.getPacket();
	}
	
	public static GamePacket sendHammerData(int hammerUsed) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.VICIOUS_HAMMER.getValue());
		w.writeAsByte(0x39);
		w.writeInt(0);
		w.writeInt(hammerUsed);
		return w.getPacket();
	}

	public static GamePacket sendHammerMessage() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.VICIOUS_HAMMER.getValue());
		w.writeAsByte(0x3D);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket hammerItem(IItem item) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		w.writeAsByte(0); // could be from drop
		w.writeAsByte(2); // always 2
		w.writeAsByte(3); // quantity > 0 (?)
		w.writeAsByte(1); // Inventory type
		w.writeAsShort(item.getSlot()); // item slot
		w.writeAsByte(0);
		w.writeAsByte(1);
		w.writeAsShort(item.getSlot()); // wtf repeat
		addItemInfo(w, item, true);
		return w.getPacket();
	}

	public static GamePacket playPortalSound() {
		return showSpecialEffect(7);
	}

	public static GamePacket showMonsterBookPickup() {
		return showSpecialEffect(14);
	}

	public static GamePacket showEquipmentLevelUp() {
		return showSpecialEffect(15);
	}

	public static GamePacket showItemLevelup() {
		return showSpecialEffect(15);
	}

	/**
	 * 6 = Exp did not drop (Safety Charms) 7 = Enter portal sound 8 = Job
	 * change 9 = Quest complete 10 = Recovery 14 = Monster book pickup 15 =
	 * Equipment levelup 16 = Maker Skill Success 19 = Exp card [500, 200, 50]
	 * 
	 * @param effect
	 * @return
	 */
	public static GamePacket showSpecialEffect(int effect) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(effect);
		return w.getPacket();
	}

	public static GamePacket showForeignEffect(int cid, int effect) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
		w.writeInt(cid);
		w.writeAsByte(effect);
		return w.getPacket();
	}

	public static GamePacket showOwnRecovery(byte heal) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(0x0A);
		w.writeAsByte(heal);
		return w.getPacket();
	}

	public static GamePacket showRecovery(int cid, byte amount) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_FOREIGN_EFFECT.getValue());
		w.writeInt(cid);
		w.writeAsByte(0x0A);
		w.writeAsByte(amount);
		return w.getPacket();
	}

	public static GamePacket showWheelsLeft(int left) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
		w.writeAsByte(0x15);
		w.writeAsByte(left);
		return w.getPacket();
	}

	public static GamePacket updateQuestFinish(short quest, int npc, short nextquest) { 
		// Check
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_QUEST_INFO.getValue()); // 0xF2 in v95
		w.writeAsByte(8);// 0x0A in v95
		w.writeAsShort(quest);
		w.writeInt(npc);
		w.writeAsShort(nextquest);
		return w.getPacket();
	}

	public static GamePacket showInfoText(String text) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(9);
		w.writeLengthString(text);
		return w.getPacket();
	}

	public static GamePacket questError(short quest) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
		w.writeAsByte(0x0A);
		w.writeAsShort(quest);
		return w.getPacket();
	}

	public static GamePacket questFailure(byte type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
		w.writeAsByte(type);// 0x0B = No meso, 0x0D = Worn by character, 0x0E =
							// Not having the item ?
		return w.getPacket();
	}

	public static GamePacket questExpire(short quest) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.UPDATE_QUEST_INFO.getValue());
		w.writeAsByte(0x0F);
		w.writeAsShort(quest);
		return w.getPacket();
	}

	public static GamePacket getMultiMegaphone(String[] messages, byte channel, boolean showMegaphoneEar) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SERVERMESSAGE.getValue());
		w.writeAsByte(0x0A);
		if (messages[0] != null) {
			w.writeLengthString(messages[0]);
		}
		w.writeAsByte(messages.length);
		for (int i = 1; i < messages.length; i++) {
			if (messages[i] != null) {
				w.writeLengthString(messages[i]);
			}
		}
		for (int i = 0; i < 10; i++) {
			w.writeAsByte(channel - 1);
		}
		w.writeAsByte(showMegaphoneEar);
		w.writeAsByte(1);
		return w.getPacket();
	}

	/**
	 * Gets a gm effect packet (ie. hide, banned, etc.)
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0x04: You have successfully blocked access.<br>
	 * 0x05: The unblocking has been successful.<br>
	 * 0x06 with Mode 0: You have successfully removed the name from the ranks.<br>
	 * 0x06 with Mode 1: You have entered an invalid character name.<br>
	 * 0x10: GM Hide, mode determines whether or not it is on.<br>
	 * 0x1E: Mode 0: Failed to send warning Mode 1: Sent warning<br>
	 * 0x13 with Mode 0: + mapid 0x13 with Mode 1: + ch (FF = Unable to find
	 * merchant)
	 * 
	 * @param type
	 *            The type
	 * @param mode
	 *            The mode
	 * @return The gm effect packet
	 */
	public static GamePacket getGMEffect(int type, byte mode) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GM_PACKET.getValue());
		w.writeAsByte(type);
		w.writeAsByte(mode);
		return w.getPacket();
	}

	public static GamePacket findMerchantResponse(boolean map, int extra) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GM_PACKET.getValue());
		w.writeAsByte(0x13);
		w.writeAsByte(!map); // 00 = mapid, 01 = ch
		if (map) {
			w.writeInt(extra);
		} else {
			w.writeAsByte(extra); // -1 = unable to find
		}
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket disableMinimap() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GM_PACKET.getValue());
		w.writeAsShort(0x1C);
		return w.getPacket();
	}

	public static GamePacket sendFamilyInvite(int playerId, String inviter) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FAMILY_INVITE.getValue());
		w.writeInt(playerId);
		w.writeLengthString(inviter);
		return w.getPacket();
	}

	public static GamePacket showBoughtCashPackage(List<IItem> cashPackage, int accountId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x89);
		w.writeAsByte(cashPackage.size());

		for (IItem item : cashPackage) {
			addCashItemInformation(w, item, accountId);
		}

		w.writeAsShort(0);

		return w.getPacket();
	}

	public static GamePacket showBoughtQuestItem(int itemId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x8D);
		w.writeInt(1);
		w.writeAsShort(1);
		w.writeAsByte(0x0B);
		w.writeAsByte(0);
		w.writeInt(itemId);

		return w.getPacket();
	}

	public static GamePacket updateSlot(IItem item) {
		// Just the same as merge... dst and src is the same...
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MODIFY_INVENTORY_ITEM.getValue());
		byte type = ItemConstants.getInventoryType(item.getItemId()).asByte();
		w.write(new byte[] {0, 2, 3});
		w.writeAsByte(type);
		w.writeAsShort(item.getSlot());
		w.writeAsByte(0);
		w.writeAsByte(type);
		w.writeAsShort(item.getSlot());
		addItemInfo(w, item, true);
		w.writeAsShort(0);
		return w.getPacket();
	}

	private static void getGuildInfo(PacketWriter w, Guild guild) {
		w.writeInt(guild.getId());
		w.writeLengthString(guild.getName());
		for (int i = 1; i <= 5; i++) {
			w.writeLengthString(guild.getRankTitle(i));
		}
		Collection<GuildCharacter> members = guild.getMembers();
		w.writeAsByte(members.size());
		for (GuildCharacter mgc : members) {
			w.writeInt(mgc.getId());
		}
		for (GuildCharacter mgc : members) {
			w.writePaddedString(mgc.getName(), 13);
			w.writeInt(mgc.getJobId());
			w.writeInt(mgc.getLevel());
			w.writeInt(mgc.getGuildRank());
			w.writeInt(mgc.isOnline() ? 1 : 0);
			w.writeInt(guild.getSignature());
			w.writeInt(mgc.getAllianceRank());
		}
		w.writeInt(guild.getCapacity());
		w.writeAsShort(guild.getLogoBG());
		w.writeAsByte(guild.getLogoBGColor());
		w.writeAsShort(guild.getLogo());
		w.writeAsByte(guild.getLogoColor());
		w.writeLengthString(guild.getNotice());
		w.writeInt(guild.getGP());
		w.writeInt(guild.getAllianceId());
	}

	public static GamePacket getAllianceInfo(Alliance alliance) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x0C);
		w.writeAsByte(1);
		w.writeInt(alliance.getId());
		w.writeLengthString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			w.writeLengthString(alliance.getRankTitle(i));
		}
		w.writeAsByte(alliance.getGuilds().size());
		w.writeInt(2); // probably capacity
		for (Integer guild : alliance.getGuilds()) {
			w.writeInt(guild);
		}
		w.writeLengthString(alliance.getNotice());
		return w.getPacket();
	}

	public static GamePacket makeNewAlliance(Alliance alliance, GameClient c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x0F);
		w.writeInt(alliance.getId());
		w.writeLengthString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			w.writeLengthString(alliance.getRankTitle(i));
		}
		w.writeAsByte(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			w.writeInt(guild);
		}
		w.writeInt(2); // probably capacity
		w.writeAsShort(0);
		for (Integer guildd : alliance.getGuilds()) {
			getGuildInfo(w, Server.getInstance().getGuild(guildd, c.getPlayer().getMGC()));
		}
		return w.getPacket();
	}

	public static GamePacket getGuildAlliances(Alliance alliance, GameClient c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x0D);
		w.writeInt(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			getGuildInfo(w, Server.getInstance().getGuild(guild, null));
		}
		return w.getPacket();
	}

	public static GamePacket addGuildToAlliance(Alliance alliance, int newGuild, GameClient c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x12);
		w.writeInt(alliance.getId());
		w.writeLengthString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			w.writeLengthString(alliance.getRankTitle(i));
		}
		w.writeAsByte(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			w.writeInt(guild);
		}
		w.writeInt(2);
		w.writeLengthString(alliance.getNotice());
		w.writeInt(newGuild);
		getGuildInfo(w, Server.getInstance().getGuild(newGuild, null));
		return w.getPacket();
	}

	public static GamePacket allianceMemberOnline(GameCharacter player, boolean isOnline) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x0E);
		w.writeInt(player.getGuild().getAllianceId());
		w.writeInt(player.getGuildId());
		w.writeInt(player.getId());
		w.writeAsByte(isOnline);
		return w.getPacket();
	}

	public static GamePacket allianceNotice(int id, String notice) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x1C);
		w.writeInt(id);
		w.writeLengthString(notice);
		return w.getPacket();
	}

	public static GamePacket changeAllianceRankTitle(int alliance, String[] ranks) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x1A);
		w.writeInt(alliance);
		for (int i = 0; i < 5; i++) {
			w.writeLengthString(ranks[i]);
		}
		return w.getPacket();
	}

	public static GamePacket updateAllianceJobLevel(GameCharacter player) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x18);
		w.writeInt(player.getGuild().getAllianceId());
		w.writeInt(player.getGuildId());
		w.writeInt(player.getId());
		w.writeInt(player.getLevel());
		w.writeInt(player.getJob().getId());
		return w.getPacket();
	}

	public static GamePacket removeGuildFromAlliance(Alliance alliance, int expelledGuild, GameClient c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x10);
		w.writeInt(alliance.getId());
		w.writeLengthString(alliance.getName());
		for (int i = 1; i <= 5; i++) {
			w.writeLengthString(alliance.getRankTitle(i));
		}
		w.writeAsByte(alliance.getGuilds().size());
		for (Integer guild : alliance.getGuilds()) {
			w.writeInt(guild);
		}
		w.writeInt(2);
		w.writeLengthString(alliance.getNotice());
		w.writeInt(expelledGuild);
		getGuildInfo(w, Server.getInstance().getGuild(expelledGuild, null));
		w.writeAsByte(0x01);
		return w.getPacket();
	}

	public static GamePacket disbandAlliance(int alliance) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ALLIANCE_OPERATION.getValue());
		w.writeAsByte(0x1D);
		w.writeInt(alliance);
		return w.getPacket();
	}

	public static GamePacket sendMesoLimit() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MESO_LIMIT.getValue()); 
		// Players under level 15 can only trade 1m per day 
		return w.getPacket();
	}

	public static GamePacket sendEngagementRequest(String name) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.RING_ACTION.getValue()); 
		// <name> has requested engagement. Will you accept this proposal?
		w.writeAsByte(0);
		w.writeLengthString(name); // name
		w.writeInt(10); // playerid
		return w.getPacket();
	}

	public static GamePacket sendGroomWishlist() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.RING_ACTION.getValue()); 
		// <name> has requested engagement. Will you accept this proposal?
		w.writeAsByte(9);
		return w.getPacket();
	}

	public static GamePacket sendBrideWishList(List<IItem> items) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.WEDDING_ACTION.getValue());
		w.writeAsByte(0x0A);
		w.writeLong(-1); // ?
		w.writeInt(0); // ?
		w.writeAsByte(items.size());
		for (IItem item : items) {
			addItemInfo(w, item, true);
		}
		return w.getPacket();
	}

	public static GamePacket addItemToWeddingRegistry(GameCharacter chr, IItem item) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.WEDDING_ACTION.getValue());
		w.writeAsByte(0x0B);
		w.writeInt(0);
		for (int i = 0; i < 0; i++) // f4
		{
			w.writeAsByte(0);
		}

		addItemInfo(w, item, true);
		return w.getPacket();
	}

	public static GamePacket sendFamilyJoinResponse(boolean accepted, String added) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FAMILY_MESSAGE2.getValue());
		w.writeAsByte(accepted);
		w.writeLengthString(added);
		return w.getPacket();
	}

	public static GamePacket getSeniorMessage(String name) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FAMILY_SENIOR_MESSAGE.getValue());
		w.writeLengthString(name);
		w.writeInt(0);
		return w.getPacket();
	}

	public static GamePacket sendGainRep(int gain, int mode) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.FAMILY_GAIN_REP.getValue());
		w.writeInt(gain);
		w.writeAsShort(0);
		return w.getPacket();
	}

	public static GamePacket removeItemFromDuey(boolean remove, int Package) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DUEY.getValue());
		w.writeAsByte(0x17);
		w.writeInt(Package);
		w.writeAsByte(remove ? 3 : 4);
		return w.getPacket();
	}

	public static GamePacket sendDueyMSG(byte operation) {
		return sendDuey(operation, null);
	}

	public static GamePacket sendDuey(byte operation, List<DueyPackages> packages) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DUEY.getValue());
		w.writeAsByte(operation);
		if (operation == 8) {
			w.writeAsByte(0);
			w.writeAsByte(packages.size());
			for (DueyPackages dp : packages) {
				w.writeInt(dp.getPackageId());
				w.writeString(dp.getSender());
				for (int i = dp.getSender().length(); i < 13; i++) {
					w.writeAsByte(0);
				}
				w.writeInt(dp.getMesos());
				w.writeLong(getQuestTimestamp(dp.sentTimeInMilliseconds()));
				w.writeLong(0); // Contains message o____o.
				for (int i = 0; i < 48; i++) {
					w.writeInt(Randomizer.nextInt(Integer.MAX_VALUE));
				}
				w.writeInt(0);
				w.writeAsByte(0);
				if (dp.getItem() != null) {
					w.writeAsByte(1);
					addItemInfo(w, dp.getItem(), true);
				} else {
					w.writeAsByte(0);
				}
			}
			w.writeAsByte(0);
		}
		return w.getPacket();
	}

	public static GamePacket sendDojoAnimation(byte firstByte, String animation) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BOSS_ENV.getValue());
		w.writeAsByte(firstByte);
		w.writeLengthString(animation);
		return w.getPacket();
	}

	public static GamePacket getDojoInfo(String info) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(10);
		w.write(new byte[] {(byte) 0xB7, 4});// QUEST ID f5
		w.writeLengthString(info);
		return w.getPacket();
	}

	public static GamePacket getDojoInfoMessage(String message) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(9);
		w.writeLengthString(message);
		return w.getPacket();
	}

	/**
	 * Gets a "block" packet (ie. the cash shop is unavailable, etc)
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 1: The portal is closed for now.<br>
	 * 2: You cannot go to that place.<br>
	 * 3: Unable to approach due to the force of the ground.<br>
	 * 4: You cannot teleport to or on this map.<br>
	 * 5: Unable to approach due to the force of the ground.<br>
	 * 6: This map can only be entered by party members.<br>
	 * 7: The Cash Shop is currently not available. Stay tuned...<br>
	 * 
	 * @param type
	 *            The type
	 * @return The "block" packet.
	 */
	public static GamePacket blockedMessage(int type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BLOCK_MESSAGE.getValue());
		w.writeAsByte(type);
		return w.getPacket();
	}

	/**
	 * Gets a "block" packet (ie. the cash shop is unavailable, etc)
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 1: You cannot move that channel. Please try again later.<br>
	 * 2: You cannot go into the cash shop. Please try again later.<br>
	 * 3: The Item-Trading Shop is currently unavailable. Please try again
	 * later.<br>
	 * 4: You cannot go into the trade shop, due to limitation of user count.<br>
	 * 5: You do not meet the minimum level requirement to access the Trade
	 * Shop.<br>
	 * 
	 * @param type
	 *            The type
	 * @return The "block" packet.
	 */
	public static GamePacket blockedMessage2(int type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BLOCK_MESSAGE2.getValue());
		w.writeAsByte(type);
		return w.getPacket();
	}

	public static GamePacket updateDojoStats(GameCharacter chr, int belt) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(10);
		w.write(new byte[] {(byte) 0xB7, 4}); // ?
		final DojoState state = chr.getDojoState();
		w.writeLengthString("pt=" + state.getPoints() + ";belt=" + belt + ";tuto=" + (state.hasFinishedTutorial() ? "1" : "0"));
		return w.getPacket();
	}

	/**
	 * Sends a "levelup" packet to the guild or family.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: <Family> ? has reached Lv. ?.<br>
	 * - The Reps you have received from ? will be reduced in half. 1: <Family>
	 * ? has reached Lv. ?.<br>
	 * 2: <Guild> ? has reached Lv. ?.<br>
	 * 
	 * @param type
	 *            The type
	 * @return The "levelup" packet.
	 */
	public static GamePacket levelUpMessage(int type, int level, String charname) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.LEVELUP_MSG.getValue());
		w.writeAsByte(type);
		w.writeInt(level);
		w.writeLengthString(charname);

		return w.getPacket();
	}

	/**
	 * Sends a "married" packet to the guild or family.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: <Guild ? is now married. Please congratulate them.<br>
	 * 1: <Family ? is now married. Please congratulate them.<br>
	 * 
	 * @param type
	 *            The type
	 * @return The "married" packet.
	 */
	public static GamePacket marriageMessage(int type, String charname) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MARRIAGE_MSG.getValue());
		w.writeAsByte(type);
		w.writeLengthString("> " + charname); 
		// To fix the stupid packet lol

		return w.getPacket();
	}

	/**
	 * Sends a "job advance" packet to the guild or family.
	 * 
	 * Possible values for <code>type</code>:<br>
	 * 0: <Guild ? has advanced to a(an) ?.<br>
	 * 1: <Family ? has advanced to a(an) ?.<br>
	 * 
	 * @param type
	 *            The type
	 * @return The "job advance" packet.
	 */
	public static GamePacket jobMessage(int type, int job, String charname) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.JOB_MSG.getValue());
		w.writeAsByte(type);
		w.writeInt(job); // Why fking int?
		w.writeLengthString("> " + charname); 
		// To fix the stupid packet lol

		return w.getPacket();
	}

	/**
	 * 
	 * @param type
	 *            - (0:Light&Long 1:Heavy&Short)
	 * @param delay
	 *            - seconds
	 * @return
	 */
	public static GamePacket trembleEffect(int type, int delay) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.BOSS_ENV.getValue());
		w.writeAsByte(1);
		w.writeAsByte(type);
		w.writeInt(delay);
		return w.getPacket();
	}

	public static GamePacket getEnergy(String info, int amount) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ENERGY.getValue());
		w.writeLengthString(info);
		w.writeLengthString(Integer.toString(amount));
		return w.getPacket();
	}

	public static GamePacket dojoWarpUp() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DOJO_WARP_UP.getValue());
		w.writeAsByte(0);
		w.writeAsByte(6);
		return w.getPacket();
	}

	public static GamePacket itemExpired(int itemid) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(2);
		w.writeInt(itemid);
		return w.getPacket();
	}

	public static GamePacket MobDamageMobFriendly(Monster mob, int damage) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.DAMAGE_MONSTER.getValue());
		w.writeInt(mob.getObjectId());
		w.writeAsByte(1); // direction ?
		w.writeInt(damage);
		int remainingHp = mob.getHp() - damage;
		if (remainingHp <= 0) {
			remainingHp = 0;
			mob.getMap().removeMapObject(mob);
		}
		mob.setHp(remainingHp);
		w.writeInt(remainingHp);
		w.writeInt(mob.getMaxHp());
		return w.getPacket();
	}

	public static GamePacket shopErrorMessage(int error, int type) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.PLAYER_INTERACTION.getValue());
		w.writeAsByte(0x0A);
		w.writeAsByte(type);
		w.writeAsByte(error);
		return w.getPacket();
	}

	private static void addRingInfo(PacketWriter w, GameCharacter chr) {
		w.writeAsShort(chr.getCrushRings().size());
		for (Ring ring : chr.getCrushRings()) {
			w.writeInt(ring.getPartnerChrId());
			w.writePaddedString(ring.getPartnerName(), 13);
			w.writeInt(ring.getRingId());
			w.writeInt(0);
			w.writeInt(ring.getPartnerRingId());
			w.writeInt(0);
		}
		w.writeAsShort(chr.getFriendshipRings().size());
		for (Ring ring : chr.getFriendshipRings()) {
			w.writeInt(ring.getPartnerChrId());
			w.writePaddedString(ring.getPartnerName(), 13);
			w.writeInt(ring.getRingId());
			w.writeInt(0);
			w.writeInt(ring.getPartnerRingId());
			w.writeInt(0);
			w.writeInt(ring.getItemId());
		}
		w.writeAsShort(chr.getMarriageRing() != null ? 1 : 0);
		int marriageId = 30000;
		if (chr.getMarriageRing() != null) {
			w.writeInt(marriageId);
			w.writeInt(chr.getId());
			w.writeInt(chr.getMarriageRing().getPartnerChrId());
			w.writeAsShort(3);
			w.writeInt(chr.getMarriageRing().getRingId());
			w.writeInt(chr.getMarriageRing().getPartnerRingId());
			w.writePaddedString(chr.getName(), 13);
			w.writePaddedString(chr.getMarriageRing().getPartnerName(), 13);
		}
	}

	public static GamePacket finishedSort(int inv) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.FINISH_SORT.getValue());
		w.writeAsByte(0);
		w.writeAsByte(inv);
		return w.getPacket();
	}

	public static GamePacket finishedSort2(int inv) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.FINISH_SORT2.getValue());
		w.writeAsByte(0);
		w.writeAsByte(inv);
		return w.getPacket();
	}

	public static GamePacket bunnyPacket() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHOW_STATUS_INFO.getValue());
		w.writeAsByte(9);
		w.writeString("Protect the Moon Bunny!!!");
		return w.getPacket();
	}

	public static GamePacket hpqMessage(String text) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MAP_EFFECT.getValue()); // not 100% sure
		w.writeAsByte(0);
		w.writeInt(5120016);
		w.writeString(text);
		return w.getPacket();
	}

	public static GamePacket showHPQMoon() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(0x83); // maybe?
		w.writeInt(-1);
		return w.getPacket();
	}

	public static GamePacket showEventInstructions() {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.GMEVENT_INSTRUCTIONS.getValue());
		w.writeAsByte(0);
		return w.getPacket();
	}

	public static GamePacket leftKnockBack() {
		PacketWriter w = new PacketWriter(2);
		w.writeAsShort(SendOpcode.LEFT_KNOCK_BACK.getValue());
		return w.getPacket();
	}

	public static GamePacket rollSnowBall(boolean entermap, int type, MapleSnowball ball0, MapleSnowball ball1) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.ROLL_SNOWBALL.getValue());
		if (entermap) {
			w.write0(21);
		} else {
			// 0 = move, 1 = roll, 2 is down disappear, 3 is up disappear
			w.writeAsByte(type);
			w.writeInt(ball0.getSnowmanHP() / 75);
			w.writeInt(ball1.getSnowmanHP() / 75);

			// distance snowball down, 84 03 = max
			w.writeAsShort(ball0.getPosition());
			w.writeAsByte(-1);
			
			// distance snowball up, 84 03 = max
			w.writeAsShort(ball1.getPosition());
			w.writeAsByte(-1);
		}
		return w.getPacket();
	}

	public static GamePacket hitSnowBall(int what, int damage) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.HIT_SNOWBALL.getValue());
		w.writeAsByte(what);
		w.writeInt(damage);
		return w.getPacket();
	}

	/**
	 * Sends a Snowball Message<br>
	 * 
	 * Possible values for <code>message</code>:<br>
	 * 1: ... Team's snowball has passed the stage 1.<br>
	 * 2: ... Team's snowball has passed the stage 2.<br>
	 * 3: ... Team's snowball has passed the stage 3.<br>
	 * 4: ... Team is attacking the snowman, stopping the progress<br>
	 * 5: ... Team is moving again<br>
	 * 
	 * @param message
	 **/
	public static GamePacket snowballMessage(int team, int message) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.SNOWBALL_MESSAGE.getValue());
		w.writeAsByte(team);// 0 is down, 1 is up
		w.writeInt(message);
		return w.getPacket();
	}

	public static GamePacket coconutScore(int team1, int team2) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.COCONUT_SCORE.getValue());
		w.writeAsShort(team1);
		w.writeAsShort(team2);
		return w.getPacket();
	}

	public static GamePacket hitCoconut(boolean spawn, int id, int type) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.HIT_COCONUT.getValue());
		if (spawn) {
			w.write(new byte[] {0, (byte) 0x80, 0, 0, 0}); // 00 80 00 00 00
		} else {
			w.writeInt(id);
			w.writeAsByte(type); // What action to do for the coconut.
		}
		return w.getPacket();
	}

	public static GamePacket customPacket(String packet) {
		PacketWriter w = new PacketWriter();
		w.write(HexTool.getByteArrayFromHexString(packet));
		return w.getPacket();
	}

	public static GamePacket customPacket(byte[] packet) {
		PacketWriter w = new PacketWriter(packet.length);
		w.write(packet);
		return w.getPacket();
	}

	public static GamePacket spawnGuide(boolean spawn) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.SPAWN_GUIDE.getValue());
		if (spawn) {
			w.writeAsByte(1);
		} else {
			w.writeAsByte(0);
		}
		return w.getPacket();
	}

	public static GamePacket talkGuide(String talk) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.TALK_GUIDE.getValue());
		w.writeAsByte(0);
		w.writeLengthString(talk);
		w.write(new byte[] {(byte) 0xC8, 0, 0, 0, (byte) 0xA0, (byte) 0x0F, 0, 0});
		return w.getPacket();
	}

	public static GamePacket guideHint(int hint) {
		PacketWriter w = new PacketWriter(11);
		w.writeAsShort(SendOpcode.TALK_GUIDE.getValue());
		w.writeAsByte(1);
		w.writeInt(hint);
		w.writeInt(7000);
		return w.getPacket();
	}

	public static void addCashItemInformation(PacketWriter w, IItem item, int accountId) {
		addCashItemInformation(w, item, accountId, null);
	}

	public static void addCashItemInformation(PacketWriter w, IItem item, int accountId, String giftMessage) {
		boolean isGift = giftMessage != null;
		boolean isRing = false;
		IEquip equip = null;
		if (item.getType() == IItem.EQUIP) {
			equip = (IEquip) item;
			isRing = equip.getRingId() > -1;
		}
		w.writeLong(item.getPetId() > -1 ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId());
		if (!isGift) {
			w.writeInt(accountId);
			w.writeInt(0);
		}
		w.writeInt(item.getItemId());
		if (!isGift) {
			w.writeInt(item.getSN());
			w.writeAsShort(item.getQuantity());
		}
		w.writePaddedString(item.getGiftFrom(), 13);
		if (isGift) {
			w.writePaddedString(giftMessage, 73);
			return;
		}
		addExpirationTime(w, item.getExpiration());
		w.writeLong(0);
	}

	public static GamePacket showWishList(GameCharacter player, boolean update) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		if (update) {
			w.writeAsByte(0x55);
		} else {
			w.writeAsByte(0x4F);
		}

		for (int sn : player.getCashShop().getWishList()) {
			w.writeInt(sn);
		}

		for (int i = player.getCashShop().getWishList().size(); i < 10; i++) {
			w.writeInt(0);
		}

		return w.getPacket();
	}

	public static GamePacket showBoughtCashItem(IItem item, int accountId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x57);
		addCashItemInformation(w, item, accountId);

		return w.getPacket();
	}

	/*
	 * 00 = Due to an unknown error, failed A4 = Due to an unknown error, failed
	 * + warpout A5 = You don't have enough cash. A6 = long as shet msg A7 = You
	 * have exceeded the allotted limit of price for gifts. A8 = You cannot send
	 * a gift to your own account. Log in on the char and purchase A9 = Please
	 * confirm whether the character's name is correct. AA = Gender restriction!
	 * //Skipped a few B0 = Wrong Coupon Code B1 = Disconnect from CS because of
	 * 3 wrong coupon codes < lol B2 = Expired Coupon B3 = Coupon has been used
	 * already B4 = Nexon internet cafes? lolfk
	 * 
	 * BB = inv full C2 = not enough mesos? Lol not even 1 mesos xD
	 */
	public static GamePacket showCashShopMessage(byte message) {
		PacketWriter w = new PacketWriter(4);
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x5C);
		w.writeAsByte(message);

		return w.getPacket();
	}

	public static GamePacket showCashInventory(GameClient c) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x4B);
		w.writeAsShort(c.getPlayer().getCashShop().getInventory().size());

		for (IItem item : c.getPlayer().getCashShop().getInventory()) {
			addCashItemInformation(w, item, c.getAccountId());
		}

		w.writeAsShort(c.getPlayer().getStorage().getSlots());
		w.writeAsShort(c.getCharacterSlots());

		return w.getPacket();
	}

	public static GamePacket showGifts(List<GiftEntry> gifts) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x4D);
		w.writeAsShort(gifts.size());

		for (GiftEntry gift : gifts) {
			addCashItemInformation(w, gift.item, 0, gift.message);
		}

		return w.getPacket();
	}

	public static GamePacket showGiftSucceed(String to, CashItem item) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x5E); // 0x5D, Couldn't be sent
		w.writeLengthString(to);
		w.writeInt(item.getItemId());
		w.writeAsShort(item.getCount());
		w.writeInt(item.getPrice());

		return w.getPacket();
	}

	public static GamePacket showBoughtInventorySlots(int type, short slots) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x60);
		w.writeAsByte(type);
		w.writeAsShort(slots);

		return w.getPacket();
	}

	public static GamePacket showBoughtStorageSlots(short slots) {
		PacketWriter w = new PacketWriter(5);
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x62);
		w.writeAsShort(slots);

		return w.getPacket();
	}

	public static GamePacket showBoughtCharacterSlot(short slots) {
		PacketWriter w = new PacketWriter(5);
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x64);
		w.writeAsShort(slots);

		return w.getPacket();
	}

	public static GamePacket takeFromCashInventory(IItem item) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x68);
		w.writeAsShort(item.getSlot());
		addItemInfo(w, item, true);

		return w.getPacket();
	}

	public static GamePacket putIntoCashInventory(IItem item, int accountId) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.CASHSHOP_OPERATION.getValue());

		w.writeAsByte(0x6A);
		addCashItemInformation(w, item, accountId);

		return w.getPacket();
	}

	public static GamePacket openCashShop(GameClient c, boolean mts) throws Exception {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(mts ? SendOpcode.OPEN_MTS.getValue() : SendOpcode.OPEN_CASHSHOP.getValue());

		addCharacterInfo(w, c.getPlayer());

		if (!mts) {
			w.writeAsByte(1);
		}

		w.writeLengthString(c.getAccountName());
		if (mts) {
			w.write(new byte[] {(byte) 0x88, 19, 0, 0, 7, 0, 0, 0, (byte) 0xF4, 1, 0, 0, (byte) 0x18, 0, 0, 0, (byte) 0xA8, 0, 0, 0, (byte) 0x70, (byte) 0xAA, (byte) 0xA7, (byte) 0xC5, (byte) 0x4E, (byte) 0xC1, (byte) 0xCA, 1});
		} else {
			w.writeInt(0);
			List<SpecialCashItem> lsci = CashItemFactory.getSpecialCashItems();
			w.writeAsShort(lsci.size());// Guess what
			for (SpecialCashItem sci : lsci) {
				w.writeInt(sci.getSN());
				w.writeInt(sci.getModifier());
				w.writeAsByte(sci.getInfo());
			}
			w.write0(121);

			for (int i = 1; i <= 8; i++) {
				for (int j = 0; j < 2; j++) {
					w.writeInt(i);
					w.writeInt(j);
					w.writeInt(50200004);

					w.writeInt(i);
					w.writeInt(j);
					w.writeInt(50200069);

					w.writeInt(i);
					w.writeInt(j);
					w.writeInt(50200117);

					w.writeInt(i);
					w.writeInt(j);
					w.writeInt(50100008);

					w.writeInt(i);
					w.writeInt(j);
					w.writeInt(50000047);
				}
			}

			w.writeInt(0);
			w.writeAsShort(0);
			w.writeAsByte(0);
			w.writeInt(75);
		}
		return w.getPacket();
	}

	public static GamePacket temporarySkills() {
		PacketWriter w = new PacketWriter(2);
		w.writeAsShort(SendOpcode.TEMPORARY_SKILLS.getValue());
		return w.getPacket();
	}

	public static GamePacket showCombo(int count) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.SHOW_COMBO.getValue());
		w.writeInt(count);
		return w.getPacket();
	}

	public static GamePacket earnTitleMessage(String msg) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.EARN_TITLE_MSG.getValue());
		w.writeLengthString(msg);
		return w.getPacket();
	}

	public static GamePacket startCPQ(GameCharacter chr, MonsterCarnivalParty enemy) {
		PacketWriter w = new PacketWriter(25);
		w.writeAsShort(SendOpcode.MONSTER_CARNIVAL_START.getValue());
		w.writeAsByte(chr.getTeam());
		w.writeAsShort(chr.getCP()); 
		w.writeAsShort(chr.getObtainedCP()); 
		w.writeAsShort(chr.getCarnivalParty().getAvailableCP()); 
		w.writeAsShort(chr.getCarnivalParty().getTotalCP()); 
		w.writeAsShort(enemy.getAvailableCP()); 
		w.writeAsShort(enemy.getTotalCP()); 
		w.writeAsShort(0); // Probably useless nexon shit
		w.writeLong(0); // Probably useless nexon shit
		return w.getPacket();
	}

	public static GamePacket updateCP(int cp, int tcp) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
		w.writeAsShort(cp); // Obtained CP - Used CP
		w.writeAsShort(tcp); // Total Obtained CP
		return w.getPacket();
	}

	public static GamePacket updatePartyCP(MonsterCarnivalParty party) {
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
		w.writeAsByte(party.getTeam()); // Team where the points are given to.
		w.writeAsShort(party.getAvailableCP()); 
		w.writeAsShort(party.getTotalCP());
		return w.getPacket();
	}

	public static GamePacket CPQSummon(int tab, int number, String name) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
		w.writeAsByte(tab); // Tab
		w.writeAsShort(number); // Number of summon inside the tab
		w.writeLengthString(name); // Name of the player that summons
		return w.getPacket();
	}

	public static GamePacket CPQDied(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
		w.writeAsByte(chr.getTeam()); // Team
		w.writeLengthString(chr.getName()); // Name of the player that
													// died
		w.writeAsByte(chr.getAndRemoveCP()); // Lost CP
		return w.getPacket();
	}

	/**
	 * Sends a CPQ Message<br>
	 * 
	 * Possible values for <code>message</code>:<br>
	 * 1: You don't have enough CP to continue.<br>
	 * 2: You can no longer summon the Monster.<br>
	 * 3: You can no longer summon the being.<br>
	 * 4: This being is already summoned.<br>
	 * 5: This request has failed due to an unknown error.<br>
	 * 
	 * @param message
	 *            Displays a message inside Carnival PQ
	 **/
	public static GamePacket CPQMessage(byte message) {
		PacketWriter w = new PacketWriter(3);
		w.writeAsShort(SendOpcode.MONSTER_CARNIVAL_MESSAGE.getValue());
		w.writeAsByte(message); // Message
		return w.getPacket();
	}

	public static GamePacket leaveCPQ(GameCharacter chr) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.MONSTER_CARNIVAL_LEAVE.getValue());
		w.writeAsByte(0); // Something
		w.writeAsByte(chr.getTeam()); // Team
		w.writeLengthString(chr.getName()); // Player name
		return w.getPacket();
	}

	public static GamePacket sheepRanchInfo(byte wolf, byte sheep) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHEEP_RANCH_INFO.getValue());
		w.writeAsByte(wolf);
		w.writeAsByte(sheep);
		return w.getPacket();
	}

	// Know what this is? ?? >=)

	public static GamePacket sheepRanchClothes(int id, byte clothes) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(SendOpcode.SHEEP_RANCH_CLOTHES.getValue());
		w.writeInt(id); // Character id
		w.writeAsByte(clothes); // 0 = sheep, 1 = wolf, 2 = Spectator (wolf
								// without wool)
		return w.getPacket();
	}

	public static GamePacket showInventoryFull() {
		PacketWriter w = new PacketWriter(8);
		w.writeAsShort(SendOpcode.SOMETHING_WITH_INVENTORY.getValue());
		w.write0(6);
		return w.getPacket();
	}

	public static GamePacket pyramidGauge(int gauge) {
		PacketWriter w = new PacketWriter(6);
		w.writeAsShort(SendOpcode.PYRAMID_GAUGE.getValue());
		w.writeInt(gauge);
		return w.getPacket();
	}

	// f2

	public static GamePacket pyramidScore(byte score, int exp) {
		// Type cannot be higher than 4 (Rank D), otherwise you'll crash
		PacketWriter w = new PacketWriter(7);
		w.writeAsShort(SendOpcode.PYRAMID_SCORE.getValue());
		w.writeAsByte(score);
		w.writeInt(exp);
		return w.getPacket();
	}
}
