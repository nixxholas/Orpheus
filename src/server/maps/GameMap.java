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
package server.maps;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Calendar;
import client.Equip;
import client.IItem;
import client.Item;
import client.BuffStat;
import client.GameCharacter;
import client.GameClient;
import client.InventoryType;
import client.Pet;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ItemConstants;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import tools.Output;
import tools.Randomizer;
import net.GamePacket;
import net.server.Channel;
import net.server.Server;
import scripting.map.MapScriptManager;
import server.ItemInfoProvider;
import server.Portal;
import server.StatEffect;
import server.TimerManager;
import server.life.Monster;
import server.life.Npc;
import server.life.SpawnPoint;
import tools.PacketCreator;
import server.events.gm.MapleCoconut;
import server.events.gm.MapleFitness;
import server.events.gm.MapleOla;
import server.events.gm.MapleOxQuiz;
import server.events.gm.MapleSnowball;
import server.partyquest.MonsterCarnival;
import server.partyquest.MonsterCarnivalParty;
import server.life.CoolDamageEntry;
import server.life.LifeFactory;
import server.life.LifeFactory.selfDestruction;
import server.life.MonsterInfoProvider;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.partyquest.Pyramid;

public class GameMap {

	private static final List<GameMapObjectType> rangedMapobjectTypes = Arrays.asList(GameMapObjectType.SHOP, GameMapObjectType.ITEM, GameMapObjectType.NPC, GameMapObjectType.MONSTER, GameMapObjectType.DOOR, GameMapObjectType.SUMMON, GameMapObjectType.REACTOR);
	private Map<Integer, GameMapObject> mapobjects = new LinkedHashMap<Integer, GameMapObject>();
	private Collection<SpawnPoint> monsterSpawn = Collections.synchronizedList(new LinkedList<SpawnPoint>());
	private AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
	private Collection<GameCharacter> characters = new LinkedHashSet<GameCharacter>();
	private Map<Integer, Portal> portals = new HashMap<Integer, Portal>();
	private List<Rectangle> areas = new ArrayList<Rectangle>();
	private FootholdTree footholds = null;
	private int mapid;
	private int runningOid = 100;
	private int returnMapId;
	private byte channel, world;
	private byte monsterRate;
	private boolean clock;
	private boolean boat;
	private boolean docked;
	private String mapName;
	private String streetName;
	private GameMapEffect mapEffect = null;
	private boolean everlast = false;
	private int forcedReturnMap = 999999999;
	private long timeLimit;
	private int hpDecrease = 0;
	private int protectItem = 0;
	private boolean town;
	private MapleOxQuiz ox;
	private boolean isOxQuiz = false;
	private boolean dropsOn = true;
	private String onFirstUserEnter;
	private String onUserEnter;
	private int fieldType;
	private int fieldLimit = 0;
	private int mobCapacity = -1;
	private ScheduledFuture<?> mapMonitor = null;
	private TimeMobEntry timeMob = null;
	private short mobInterval = 5000;
	// HPQ
	private int riceCakeNum = 0; // bad place to put this (why is it in here
									// then)
	private boolean allowHPQSummon = false; // bad place to put this
	// events
	private boolean eventstarted = false;
	private MapleSnowball snowball0 = null;
	private MapleSnowball snowball1 = null;
	private MapleCoconut coconut;
	// locks
	private final ReadLock chrRLock;
	private final WriteLock chrWLock;
	private final ReadLock objectRLock;
	private final WriteLock objectWLock;

	public GameMap(int mapid, byte world, byte channel, int returnMapId, float monsterRate) {
		this.mapid = mapid;
		this.channel = channel;
		this.world = world;
		this.returnMapId = returnMapId;
		this.monsterRate = (byte) Math.round(monsterRate);
		if (this.monsterRate == 0) {
			this.monsterRate = 1;
		}
		final ReentrantReadWriteLock chrLock = new ReentrantReadWriteLock(true);
		chrRLock = chrLock.readLock();
		chrWLock = chrLock.writeLock();

		final ReentrantReadWriteLock objectLock = new ReentrantReadWriteLock(true);
		objectRLock = objectLock.readLock();
		objectWLock = objectLock.writeLock();
	}

	public void broadcastMessage(GameCharacter source, GamePacket packet) {
		chrRLock.lock();
		try {
			for (GameCharacter chr : characters) {
				if (chr != source) {
					chr.getClient().announce(packet);
				}
			}
		} finally {
			chrRLock.unlock();
		}
	}

	public void broadcastGMMessage(GameCharacter source, GamePacket packet) {
		chrRLock.lock();
		try {
			for (GameCharacter chr : characters) {
				if (chr != source && (chr.getGmLevel() > source.getGmLevel())) {
					chr.getClient().announce(packet);
				}
			}
		} finally {
			chrRLock.unlock();
		}
	}

	public void toggleDrops() {
		this.dropsOn = !dropsOn;
	}

	public List<GameMapObject> getMapObjectsInRect(Rectangle box, List<GameMapObjectType> types) {
		objectRLock.lock();
		final List<GameMapObject> ret = new LinkedList<GameMapObject>();
		try {
			for (GameMapObject l : mapobjects.values()) {
				if (types.contains(l.getType())) {
					if (box.contains(l.getPosition())) {
						ret.add(l);
					}
				}
			}
		} finally {
			objectRLock.unlock();
		}
		return ret;
	}

	public int getId() {
		return mapid;
	}

	public GameMap getReturnMap() {
		return Server.getInstance().getWorld(world).getChannel(channel).getMapFactory().getMap(returnMapId);
	}

	public int getReturnMapId() {
		return returnMapId;
	}

	public void setReactorState() {
		objectRLock.lock();
		try {
			for (GameMapObject o : mapobjects.values()) {
				if (o.getType() == GameMapObjectType.REACTOR) {
					if (((Reactor) o).getState() < 1) {
						((Reactor) o).setState((byte) 1);
						broadcastMessage(PacketCreator.triggerReactor((Reactor) o, 1));
					}
				}
			}
		} finally {
			objectRLock.unlock();
		}
	}

	public int getForcedReturnId() {
		return forcedReturnMap;
	}

	public GameMap getForcedReturnMap() {
		return Server.getInstance().getWorld(world).getChannel(channel).getMapFactory().getMap(forcedReturnMap);
	}

	public void setForcedReturnMap(int map) {
		this.forcedReturnMap = map;
	}

	public long getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(int timeLimit) {
		this.timeLimit = timeLimit;
	}

	public int getTimeLeft() {
		return (int) ((timeLimit - System.currentTimeMillis()) / 1000);
	}

	public int getCurrentPartyId() {
		for (GameCharacter chr : this.getCharacters()) {
			if (chr.getPartyId() != -1) {
				return chr.getPartyId();
			}
		}
		return -1;
	}

	public void addMapObject(GameMapObject mapobject) {
		objectWLock.lock();
		try {
			mapobject.setObjectId(runningOid);
			this.mapobjects.put(Integer.valueOf(runningOid), mapobject);
			incrementRunningOid();
		} finally {
			objectWLock.unlock();
		}
	}

	private void spawnAndAddRangedMapObject(GameMapObject mapobject, DelayedPacketCreation packetbakery) {
		spawnAndAddRangedMapObject(mapobject, packetbakery, null);
	}

	private void spawnAndAddRangedMapObject(GameMapObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
		chrRLock.lock();
		try {
			mapobject.setObjectId(runningOid);
			for (GameCharacter chr : characters) {
				if (condition == null || condition.canSpawn(chr)) {
					if (chr.getPosition().distanceSq(mapobject.getPosition()) <= 722500) {
						packetbakery.sendPackets(chr.getClient());
						chr.addVisibleMapObject(mapobject);
					}
				}
			}
		} finally {
			chrRLock.unlock();
		}
		objectWLock.lock();
		try {
			this.mapobjects.put(Integer.valueOf(runningOid), mapobject);
		} finally {
			objectWLock.unlock();
		}
		incrementRunningOid();
	}

	private void incrementRunningOid() {
		runningOid++;
		if (runningOid >= 30000) {
			runningOid = 1000;// Lol, like there are monsters with the same oid
								// NO
		}
		objectRLock.lock();
		try {
			if (!this.mapobjects.containsKey(Integer.valueOf(runningOid))) {
				return;
			}
		} finally {
			objectRLock.unlock();
		}
		throw new RuntimeException("Out of OIDs on map " + mapid + " (channel: " + channel + ")");
	}

	public void removeMapObject(int num) {
		objectWLock.lock();
		try {
			this.mapobjects.remove(Integer.valueOf(num));
		} finally {
			objectWLock.unlock();
		}
	}

	public void removeMapObject(final GameMapObject obj) {
		removeMapObject(obj.getObjectId());
	}

	private Point calcPointBelow(Point initial) {
		Foothold fh = footholds.findBelow(initial);
		if (fh == null) {
			return null;
		}
		int dropY = fh.getY1();
		if (!fh.isWall() && fh.getY1() != fh.getY2()) {
			double s1 = Math.abs(fh.getY2() - fh.getY1());
			double s2 = Math.abs(fh.getX2() - fh.getX1());
			double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
			if (fh.getY2() < fh.getY1()) {
				dropY = fh.getY1() - (int) s5;
			} else {
				dropY = fh.getY1() + (int) s5;
			}
		}
		return new Point(initial.x, dropY);
	}

	public Point calcDropPos(Point initial, Point fallback) {
		Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
		if (ret == null) {
			return fallback;
		}
		return ret;
	}

	private void dropFromMonster(final GameCharacter chr, final Monster mob) {
		if (mob.dropsDisabled() || !dropsOn) {
			return;
		}
		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);
		final int mobpos = mob.getPosition().x;
		IItem idrop;
		double rate = chr.rates().drop();
		byte d = 1;
		Point pos = new Point(0, mob.getPosition().y);

		Map<MonsterStatus, MonsterStatusEffect> stati = mob.getStatuses();
		if (stati.containsKey(MonsterStatus.SHOWDOWN)) {
			rate *= (stati.get(MonsterStatus.SHOWDOWN).getStatuses().get(MonsterStatus.SHOWDOWN).doubleValue() / 100.0 + 1.0);
		}

		final MonsterInfoProvider mi = MonsterInfoProvider.getInstance();
		final List<MonsterDropEntry> dropEntry = new ArrayList<MonsterDropEntry>(mi.retrieveDrop(mob.getId()));

		Collections.shuffle(dropEntry);
		for (final MonsterDropEntry de : dropEntry) {
			if (Randomizer.nextInt(999999) < de.Chance * rate) {
				if (droptype == 3) {
					pos.x = (int) (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
				} else {
					pos.x = (int) (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
				}
				if (de.ItemId == 0) { // meso
					int mesos = de.getRandomQuantity();

					if (mesos > 0) {
						if (chr.getBuffedValue(BuffStat.MESOUP) != null) {
							mesos = (int) (mesos * chr.getBuffedValue(BuffStat.MESOUP).doubleValue() / 100.0);
						}
						spawnMesoDrop((int) (mesos * chr.rates().meso()), calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
					}
				} else {
					if (ItemConstants.getInventoryType(de.ItemId) == InventoryType.EQUIP) {
						idrop = ii.randomizeStats((Equip) ii.getEquipById(de.ItemId));
					} else {
						idrop = new Item(de.ItemId, (byte) 0, (short) de.getRandomQuantity());
					}
					spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.QuestId);
				}
				d++;
			}
		}
		final List<MonsterGlobalDropEntry> globalEntry = mi.getGlobalDrop();
		// Global Drops
		for (final MonsterGlobalDropEntry de : globalEntry) {
			if (Randomizer.nextInt(999999) < de.Chance) {
				if (droptype == 3) {
					pos.x = (int) (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
				} else {
					pos.x = (int) (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
				}
				if (de.ItemId == 0) {
					// chr.getCashShop().gainCash(1, 80);
				} else {
					if (ItemConstants.getInventoryType(de.ItemId) == InventoryType.EQUIP) {
						idrop = ii.randomizeStats((Equip) ii.getEquipById(de.ItemId));
					} else {
						idrop = new Item(de.ItemId, (byte) 0, (short) de.getRandomQuantity());
					}
					spawnDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.QuestId);
					d++;
				}
			}
		}
	}

	private void spawnDrop(final IItem idrop, final Point dropPos, final Monster mob, final GameCharacter chr, final byte droptype, final short questid) {
		final GameMapItem mdrop = new GameMapItem(idrop, dropPos, mob, chr, droptype, false, questid);
		spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				if (questid <= 0 || (c.getPlayer().getQuestStatus(questid) == 1 && c.getPlayer().needQuestItem(questid, idrop.getItemId()))) {
					c.getSession().write(PacketCreator.dropItemFromMapObject(mdrop, mob.getPosition(), dropPos, (byte) 1));
				}
			}
		}, null);

		TimerManager.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
		activateItemReactors(mdrop, chr.getClient());
	}

	public final void spawnMesoDrop(final int meso, final Point position, final GameMapObject dropper, final GameCharacter owner, final boolean playerDrop, final byte droptype) {
		final Point droppos = calcDropPos(position, position);
		final GameMapItem mdrop = new GameMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

		spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.getSession().write(PacketCreator.dropItemFromMapObject(mdrop, dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);

		TimerManager.getInstance().schedule(new ExpireMapItemJob(mdrop), 180000);
	}

	public final void disappearingItemDrop(final GameMapObject dropper, final GameCharacter owner, final IItem item, final Point pos) {
		final Point droppos = calcDropPos(pos, pos);
		final GameMapItem drop = new GameMapItem(item, droppos, dropper, owner, (byte) 1, false);
		broadcastMessage(PacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
	}

	public Monster getMonsterById(int id) {
		objectRLock.lock();
		try {
			for (GameMapObject obj : mapobjects.values()) {
				if (obj.getType() == GameMapObjectType.MONSTER) {
					if (((Monster) obj).getId() == id) {
						return (Monster) obj;
					}
				}
			}
		} finally {
			objectRLock.unlock();
		}
		return null;
	}

	public int countMonster(int id) {
		int count = 0;
		for (GameMapObject m : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.MONSTER))) {
			Monster mob = (Monster) m;
			if (mob.getId() == id) {
				count++;
			}
		}
		return count;
	}

	public boolean damageMonster(final GameCharacter chr, final Monster monster, final int damage) {
		if (monster.getId() == 8800000) {
			for (GameMapObject object : chr.getMap().getMapObjects()) {
				Monster mons = chr.getMap().getMonsterByOid(object.getObjectId());
				if (mons != null) {
					if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
						return true;
					}
				}
			}
		}
		if (monster.isAlive()) {
			boolean killed = false;
			monster.monsterLock.lock();
			try {
				if (!monster.isAlive()) {
					return false;
				}
				CoolDamageEntry cool = monster.getStats().getCool();
				if (cool != null) {
					Pyramid pq = (Pyramid) chr.getPartyQuest();
					if (pq != null) {
						if (damage > 0) {
							if (damage >= cool.damage) {
								if ((Math.random() * 100) < cool.probability) {
									pq.cool();
								} else {
									pq.kill();
								}
							} else {
								pq.kill();
							}
						} else {
							pq.miss();
						}
						killed = true;
					}
				}
				if (damage > 0) {
					monster.damage(chr, damage, true);
					if (!monster.isAlive()) { // monster just died
						// killMonster(monster, chr, true);
						killed = true;
					}
				} else if (monster.getId() >= 8810002 && monster.getId() <= 8810009) {
					for (GameMapObject object : chr.getMap().getMapObjects()) {
						Monster mons = chr.getMap().getMonsterByOid(object.getObjectId());
						if (mons != null) {
							if (mons.getId() == 8810018) {
								damageMonster(chr, mons, damage);
							}
						}
					}
				}
			} finally {
				monster.monsterLock.unlock();
			}
			if (killed && monster != null) {
				killMonster(monster, chr, true);
				monster.empty();
				nullifyObject(monster);
			}
			return true;
		}
		return false;
	}

	public void killMonster(final Monster monster, final GameCharacter chr, final boolean withDrops) {
		killMonster(monster, chr, withDrops, false, 1);
	}

	public void killMonster(final Monster monster, final GameCharacter chr, final boolean withDrops, final boolean secondTime, int animation) {
		if (monster.getId() == 8810018 && !secondTime) {
			TimerManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					killMonster(monster, chr, withDrops, true, 1);
					killAllMonsters();
				}
			}, 3000);
			return;
		}
		if (chr == null) {
			spawnedMonstersOnMap.decrementAndGet();
			monster.setHp(0);
			broadcastMessage(PacketCreator.killMonster(monster.getObjectId(), animation), monster.getPosition());
			removeMapObject(monster);
			monster.empty();
			nullifyObject(monster);
			return;
		}
		/*
		 * if (chr.getQuest(Quest.getInstance(29400)).getStatus().equals(
		 * QuestStatus.Status.STARTED)) { if (chr.getLevel() >= 120 &&
		 * monster.getStats().getLevel() >= 120) { //FIX MEDAL SHET } else if
		 * (monster.getStats().getLevel() >= chr.getLevel()) { } }
		 */
		int buff = monster.getBuffToGive();
		if (buff > -1) {
			ItemInfoProvider mii = ItemInfoProvider.getInstance();
			for (GameMapObject mmo : this.getAllPlayer()) {
				GameCharacter character = (GameCharacter) mmo;
				if (character.isAlive()) {
					StatEffect statEffect = mii.getItemEffect(buff);
					character.getClient().announce(PacketCreator.showOwnBuffEffect(buff, 1));
					broadcastMessage(character, PacketCreator.showBuffeffect(character.getId(), buff, 1), false);
					statEffect.applyTo(character);
				}
			}
		}
		if (monster.getId() == 8810018) {
			for (Channel cserv : Server.getInstance().getWorld(world).getChannels()) {
				for (GameCharacter player : cserv.getPlayerStorage().getAllCharacters()) {
					if (player.getMapId() == 240000000) {
						player.message("Mysterious power arose as I heard the powerful cry of the Nine Spirit Baby Dragon.");
					} else {
						player.dropMessage("To the crew that have finally conquered Horned Tail after numerous attempts, I salute thee! You are the true heroes of Leafre!!");
						if (player.isGM()) {
							player.message("[GM-Message] Horntail was killed by : " + chr.getName());
						}
					}
				}
			}
		}
		spawnedMonstersOnMap.decrementAndGet();
		monster.setHp(0);
		broadcastMessage(PacketCreator.killMonster(monster.getObjectId(), animation), monster.getPosition());
		if (monster.getStats().selfDestruction() == null) {// FUU BOMBS D:
			removeMapObject(monster);
		}
		if (monster.getCP() > 0 && chr.getCarnival() != null) {
			chr.getCarnivalParty().addCP(chr, monster.getCP());
			chr.announce(PacketCreator.updateCP(chr.getCP(), chr.getObtainedCP()));
			broadcastMessage(PacketCreator.updatePartyCP(chr.getCarnivalParty()));
			// they drop items too ):
		}
		if (monster.getId() >= 8800003 && monster.getId() <= 8800010) {
			boolean makeZakReal = true;
			Collection<GameMapObject> objects = getMapObjects();
			for (GameMapObject object : objects) {
				Monster mons = getMonsterByOid(object.getObjectId());
				if (mons != null) {
					if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
						makeZakReal = false;
						break;
					}
				}
			}
			if (makeZakReal) {
				for (GameMapObject object : objects) {
					Monster mons = chr.getMap().getMonsterByOid(object.getObjectId());
					if (mons != null) {
						if (mons.getId() == 8800000) {
							makeMonsterReal(mons);
							updateMonsterController(mons);
							break;
						}
					}
				}
			}
		}
		GameCharacter dropOwner = monster.killBy(chr);
		if (withDrops && !monster.dropsDisabled()) {
			if (dropOwner == null) {
				dropOwner = chr;
			}
			dropFromMonster(dropOwner, monster);
		}
	}

	public void killMonster(int monsId) {
		for (GameMapObject mmo : getMapObjects()) {
			if (mmo instanceof Monster) {
				if (((Monster) mmo).getId() == monsId) {
					this.killMonster((Monster) mmo, (GameCharacter) getAllPlayer().get(0), false);
				}
			}
		}
	}

	public void killAllMonsters() {
		for (GameMapObject monstermo : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.MONSTER))) {
			Monster monster = (Monster) monstermo;
			spawnedMonstersOnMap.decrementAndGet();
			monster.setHp(0);
			broadcastMessage(PacketCreator.killMonster(monster.getObjectId(), true), monster.getPosition());
			removeMapObject(monster);
			monster.empty();
			nullifyObject(monster);
		}
	}

	public List<GameMapObject> getAllPlayer() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.PLAYER));
	}

	public void destroyReactor(int oid) {
		final Reactor reactor = getReactorByOid(oid);
		TimerManager tMan = TimerManager.getInstance();
		broadcastMessage(PacketCreator.destroyReactor(reactor));
		reactor.setAlive(false);
		removeMapObject(reactor);
		reactor.setTimerActive(false);
		if (reactor.getDelay() > 0) {
			tMan.schedule(new Runnable() {

				@Override
				public void run() {
					respawnReactor(reactor);
				}
			}, reactor.getDelay());
		}
	}

	public void resetReactors() {
		objectRLock.lock();
		try {
			for (GameMapObject o : mapobjects.values()) {
				if (o.getType() == GameMapObjectType.REACTOR) {
					final Reactor r = ((Reactor) o);
					r.setState((byte) 0);
					r.setTimerActive(false);
					broadcastMessage(PacketCreator.triggerReactor(r, 0));
				}
			}
		} finally {
			objectRLock.unlock();
		}
	}

	public void shuffleReactors() {
		List<Point> points = new ArrayList<Point>();
		objectRLock.lock();
		try {
			for (GameMapObject o : mapobjects.values()) {
				if (o.getType() == GameMapObjectType.REACTOR) {
					points.add(((Reactor) o).getPosition());
				}
			}
			Collections.shuffle(points);
			for (GameMapObject o : mapobjects.values()) {
				if (o.getType() == GameMapObjectType.REACTOR) {
					((Reactor) o).setPosition(points.remove(points.size() - 1));
				}
			}
		} finally {
			objectRLock.unlock();
		}
	}

	public Reactor getReactorById(int Id) {
		objectRLock.lock();
		try {
			for (GameMapObject obj : mapobjects.values()) {
				if (obj.getType() == GameMapObjectType.REACTOR) {
					if (((Reactor) obj).getId() == Id) {
						return (Reactor) obj;
					}
				}
			}
			return null;
		} finally {
			objectRLock.unlock();
		}
	}

	/**
	 * Automagically finds a new controller for the given monster from the chars
	 * on the map...
	 * 
	 * @param monster
	 */
	public void updateMonsterController(Monster monster) {
		monster.monsterLock.lock();
		try {
			if (!monster.isAlive()) {
				return;
			}
			
			// FIXME: This is just asking for a race condition.
			if (monster.getController() != null) {
				if (monster.getController().getMap() != this) {
					monster.getController().stopControllingMonster(monster);
				} else {
					return;
				}
			}
			int mincontrolled = -1;
			GameCharacter newController = null;
			chrRLock.lock();
			try {
				for (GameCharacter chr : characters) {
					if (!chr.isHidden() && (chr.getControlledMonsters().size() < mincontrolled || mincontrolled == -1)) {
						mincontrolled = chr.getControlledMonsters().size();
						newController = chr;
					}
				}
			} finally {
				chrRLock.unlock();
			}
			
			if (newController != null) {
				// was a new controller found? (if not no one is on the map)
				if (monster.isFirstAttack()) {
					// FIXME: what if the newController left the map by now?
					// FIXME: race condition...
					newController.controlMonster(monster, true);
					monster.setControllerHasAggro(true);
					monster.setControllerKnowsAboutAggro(true);
				} else {
					newController.controlMonster(monster, false);
				}
			}
		} finally {
			monster.monsterLock.unlock();
		}
	}

	public Collection<GameMapObject> getMapObjects() {
		return Collections.unmodifiableCollection(mapobjects.values());
	}

	public boolean containsNPC(int npcid) {
		if (npcid == 9000066) {
			return true;
		}
		objectRLock.lock();
		try {
			for (GameMapObject obj : mapobjects.values()) {
				if (obj.getType() == GameMapObjectType.NPC) {
					if (((Npc) obj).getId() == npcid) {
						return true;
					}
				}
			}
		} finally {
			objectRLock.unlock();
		}
		return false;
	}

	public GameMapObject getMapObject(int oid) {
		return mapobjects.get(oid);
	}

	/**
	 * returns a monster with the given oid, if no such monster exists returns
	 * null
	 * 
	 * @param oid
	 * @return
	 */
	public Monster getMonsterByOid(int oid) {
		GameMapObject mapObject = getMapObject(oid);
		if (mapObject == null) {
			return null;
		}
		if (mapObject.getType() == GameMapObjectType.MONSTER) {
			return (Monster) mapObject;
		}
		return null;
	}

	public Reactor getReactorByOid(int oid) {
		GameMapObject mapObject = getMapObject(oid);
		if (mapObject == null) {
			return null;
		}
		return mapObject.getType() == GameMapObjectType.REACTOR ? (Reactor) mapObject : null;
	}

	public Reactor getReactorByName(String name) {
		objectRLock.lock();
		try {
			for (GameMapObject obj : mapobjects.values()) {
				if (obj.getType() == GameMapObjectType.REACTOR) {
					if (((Reactor) obj).getName().equals(name)) {
						return (Reactor) obj;
					}
				}
			}
		} finally {
			objectRLock.unlock();
		}
		return null;
	}

	public void spawnMonsterOnGroudBelow(Monster mob, Point pos) {
		spawnMonsterOnGroundBelow(mob, pos);
	}

	public void spawnMonsterOnGroundBelow(Monster mob, Point pos) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = calcPointBelow(spos);
		spos.y--;
		mob.setPosition(spos);
		spawnMonster(mob);
	}

	public void spawnCPQMonster(Monster mob, Point pos, int team) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = calcPointBelow(spos);
		spos.y--;
		mob.setPosition(spos);
		mob.setTeam(team);
		spawnMonster(mob);
	}

	private void monsterItemDrop(final Monster m, final IItem item, long delay) {
		final ScheduledFuture<?> monsterItemDrop = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				if (GameMap.this.getMonsterById(m.getId()) != null) {
					if (item.getItemId() == 4001101) {
						GameMap.this.broadcastMessage(PacketCreator.serverNotice(6, "The Moon Bunny made rice cake number " + (GameMap.this.riceCakeNum + 1)));
					}
					spawnItemDrop(m, null, item, m.getPosition(), true, true);
				}
			}
		}, delay, delay);
		if (getMonsterById(m.getId()) == null) {
			monsterItemDrop.cancel(true);
		}
	}

	public void spawnFakeMonsterOnGroundBelow(Monster mob, Point pos) {
		Point spos = getGroundBelow(pos);
		mob.setPosition(spos);
		spawnFakeMonster(mob);
	}

	public Point getGroundBelow(Point pos) {
		Point spos = new Point(pos.x, pos.y - 1);
		spos = calcPointBelow(spos);
		spos.y--;
		return spos;
	}

	public void spawnRevives(final Monster monster) {
		monster.setMap(this);

		spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.announce(PacketCreator.spawnMonster(monster, false));
			}
		});
		updateMonsterController(monster);
		spawnedMonstersOnMap.incrementAndGet();
	}

	public void spawnMonster(final Monster monster) {
		if (mobCapacity != -1 && mobCapacity == spawnedMonstersOnMap.get()) {
			return;// PyPQ
		}
		monster.setMap(this);
		spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.announce(PacketCreator.spawnMonster(monster, true));
			}
		}, null);
		updateMonsterController(monster);

		if (monster.getDropPeriodTime() > 0) { // 9300102 - Watchhog, 9300061 -
												// Moon Bunny (HPQ)
			if (monster.getId() == 9300102) {
				monsterItemDrop(monster, new Item(4031507, (byte) 0, (short) 1), monster.getDropPeriodTime());
			} else if (monster.getId() == 9300061) {
				monsterItemDrop(monster, new Item(4001101, (byte) 0, (short) 1), monster.getDropPeriodTime() / 3);
			} else {
				Output.print("UNCODED TIMED MOB DETECTED: " + monster.getId());
			}
		}
		spawnedMonstersOnMap.incrementAndGet();
		final selfDestruction selfDestruction = monster.getStats().selfDestruction();
		if (monster.getStats().removeAfter() > 0 || selfDestruction != null) {
			if (selfDestruction == null) {
				TimerManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						killMonster(monster, (GameCharacter) getAllPlayer().get(0), false);
					}
				}, monster.getStats().removeAfter() * 1000);
			} else {
				TimerManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						killMonster(monster, (GameCharacter) getAllPlayer().get(0), false, false, selfDestruction.getAction());
					}
				}, selfDestruction.removeAfter() * 1000);
			}
		}
		if (mapid == 910110000 && !this.allowHPQSummon) { 
			// HPQ make monsters invisible
			this.broadcastMessage(PacketCreator.makeMonsterInvisible(monster));
		}
	}

	public void spawnDojoMonster(final Monster monster) {
		Point[] pts = {new Point(140, 0), new Point(190, 7), new Point(187, 7)};
		spawnMonsterWithEffect(monster, 15, pts[Randomizer.nextInt(3)]);
	}

	public void spawnMonsterWithEffect(final Monster monster, final int effect, Point pos) {
		monster.setMap(this);
		Point spos = new Point(pos.x, pos.y - 1);
		spos = calcPointBelow(spos);
		spos.y--;
		monster.setPosition(spos);
		if (mapid < 925020000 || mapid > 925030000) {
			monster.disableDrops();
		}
		spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.announce(PacketCreator.spawnMonster(monster, true, effect));
			}
		});
		if (monster.hasBossHPBar()) {
			broadcastMessage(monster.makeBossHPBarPacket(), monster.getPosition());
		}
		updateMonsterController(monster);

		spawnedMonstersOnMap.incrementAndGet();
	}

	public void spawnFakeMonster(final Monster monster) {
		monster.setMap(this);
		monster.setFake(true);
		spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.announce(PacketCreator.spawnFakeMonster(monster, 0));
			}
		});

		spawnedMonstersOnMap.incrementAndGet();
	}

	public void makeMonsterReal(final Monster monster) {
		monster.setFake(false);
		broadcastMessage(PacketCreator.makeMonsterReal(monster));
		updateMonsterController(monster);
	}

	public void spawnReactor(final Reactor reactor) {
		reactor.setMap(this);
		spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.announce(reactor.makeSpawnData());
			}
		});

	}

	private void respawnReactor(final Reactor reactor) {
		reactor.setState((byte) 0);
		reactor.setAlive(true);
		spawnReactor(reactor);
	}

	public void spawnDoor(final Door door) {
		spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.announce(PacketCreator.spawnDoor(door.getOwner().getId(), door.getTargetPosition(), false));
				if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsMembers(c.getPlayer().getPartyCharacter()))) {
					c.announce(PacketCreator.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
				}
				c.announce(PacketCreator.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
				c.announce(PacketCreator.enableActions());
			}
		}, new SpawnCondition() {

			@Override
			public boolean canSpawn(GameCharacter chr) {
				return chr.getMapId() == door.getTarget().getId() || chr == door.getOwner() && chr.getParty() == null;
			}
		});

	}

	public List<GameCharacter> getPlayersInRange(Rectangle box, List<GameCharacter> chr) {
		List<GameCharacter> character = new LinkedList<GameCharacter>();
		chrRLock.lock();
		try {
			for (GameCharacter a : characters) {
				if (chr.contains(a.getClient().getPlayer())) {
					if (box.contains(a.getPosition())) {
						character.add(a);
					}
				}
			}
			return character;
		} finally {
			chrRLock.unlock();
		}
	}

	public void spawnSummon(final Summon summon) {
		spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				if (summon != null) {
					c.announce(PacketCreator.spawnSummon(summon, true));
				}
			}
		}, null);
	}

	public void spawnMist(final Mist mist, final int duration, boolean poison, boolean fake) {
		addMapObject(mist);
		broadcastMessage(fake ? mist.makeFakeSpawnData(30) : mist.makeSpawnData());
		TimerManager tMan = TimerManager.getInstance();
		final ScheduledFuture<?> poisonSchedule;
		if (poison) {
			Runnable poisonTask = new Runnable() {

				@Override
				public void run() {
					List<GameMapObject> affectedMonsters = getMapObjectsInBox(mist.getBox(), Collections.singletonList(GameMapObjectType.MONSTER));
					for (GameMapObject mo : affectedMonsters) {
						if (mist.makeChanceResult()) {
							MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), null, false);
							((Monster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration);
						}
					}
				}
			};
			poisonSchedule = tMan.register(poisonTask, 2000, 2500);
		} else {
			poisonSchedule = null;
		}
		tMan.schedule(new Runnable() {

			@Override
			public void run() {
				removeMapObject(mist);
				if (poisonSchedule != null) {
					poisonSchedule.cancel(false);
				}
				broadcastMessage(mist.makeDestroyData());
			}
		}, duration);
	}

	public final void spawnItemDrop(final GameMapObject dropper, final GameCharacter owner, final IItem item, Point pos, final boolean ffaDrop, final boolean playerDrop) {
		final Point droppos = calcDropPos(pos, pos);
		final GameMapItem drop = new GameMapItem(item, droppos, dropper, owner, (byte) (ffaDrop ? 2 : 0), playerDrop);

		spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {

			@Override
			public void sendPackets(GameClient c) {
				c.getSession().write(PacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 1));
			}
		}, null);
		broadcastMessage(PacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 0));

		if (!everlast) {
			TimerManager.getInstance().schedule(new ExpireMapItemJob(drop), 180000);
			activateItemReactors(drop, owner.getClient());
		}
	}

	private void activateItemReactors(final GameMapItem drop, final GameClient c) {
		final IItem item = drop.getItem();

		for (final GameMapObject o : getAllReactor()) {
			final Reactor react = (Reactor) o;

			if (react.getReactorType() == 100) {
				if (react.getReactItem((byte) 0).itemId == item.getItemId() && react.getReactItem((byte) 0).quantity == item.getQuantity()) {

					if (react.getArea().contains(drop.getPosition())) {
						if (!react.isTimerActive()) {
							TimerManager.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
							react.setTimerActive(true);
							break;
						}
					}
				}
			}
		}
	}

	public final List<GameMapObject> getAllReactor() {
		return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.REACTOR));
	}

	public void startMapEffect(String msg, int itemId) {
		startMapEffect(msg, itemId, 30000);
	}

	public void startMapEffect(String msg, int itemId, long time) {
		if (mapEffect != null) {
			return;
		}
		mapEffect = new GameMapEffect(msg, itemId);
		broadcastMessage(mapEffect.makeStartData());
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				broadcastMessage(mapEffect.makeDestroyData());
				mapEffect = null;
			}
		}, time);
	}

	public void addPlayer(final GameCharacter chr) {
		chrWLock.lock();
		try {
			this.characters.add(chr);
		} finally {
			chrWLock.unlock();
		}
		if (onFirstUserEnter.length() != 0 && !chr.hasEntered(onFirstUserEnter, mapid) && MapScriptManager.getInstance().scriptExists(onFirstUserEnter, true)) {
			if (getAllPlayer().size() <= 1) {
				chr.enteredScript(onFirstUserEnter, mapid);
				MapScriptManager.getInstance().getMapScript(chr.getClient(), onFirstUserEnter, true);
			}
		}
		if (onUserEnter.length() != 0) {
			if (onUserEnter.equals("cygnusTest") && (mapid < 913040000 || mapid > 913040006)) {
				chr.saveLocation("INTRO");
			}
			MapScriptManager.getInstance().getMapScript(chr.getClient(), onUserEnter, false);
		}
		if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && chr.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
			chr.cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
			chr.cancelBuffStats(BuffStat.MONSTER_RIDING);
		}
		if (mapid == 923010000 && getMonsterById(9300102) == null) { 
			// Kenta's Mount Quest
			spawnMonsterOnGroundBelow(LifeFactory.getMonster(9300102), new Point(77, 426));
		} else if (mapid == 910110000) { 
			// Henesys Party Quest
			chr.getClient().announce(PacketCreator.getClock(15 * 60));
			TimerManager.getInstance().register(new Runnable() {

				@Override
				public void run() {
					if (mapid == 910110000) {
						chr.getClient().getPlayer().changeMap(chr.getClient().getChannelServer().getMapFactory().getMap(925020000));
					}
				}
			}, 15 * 60 * 1000 + 3000);
		}
		
		Pet[] pets = chr.getPets();
		for (int i = 0; i < chr.getPets().length; i++) {
			if (pets[i] != null) {
				pets[i].setPosition(getGroundBelow(chr.getPosition()));
				chr.announce(PacketCreator.showPet(chr, pets[i], false, false));
			} else {
				break;
			}
		}

		if (chr.isHidden()) {
			broadcastGMMessage(chr, PacketCreator.spawnPlayerMapObject(chr), false);
			chr.announce(PacketCreator.getGMEffect(0x10, (byte) 1));
		} else {
			broadcastMessage(chr, PacketCreator.spawnPlayerMapObject(chr), false);
		}

		sendObjectPlacement(chr.getClient());
		if (isStartingEventMap() && !eventStarted()) {
			chr.getMap().getPortal("join00").setPortalStatus(false);
		}
		if (hasForcedEquip()) {
			chr.getClient().announce(PacketCreator.showForcedEquip(-1));
		}
		if (specialEquip()) {
			chr.getClient().announce(PacketCreator.coconutScore(0, 0));
			chr.getClient().announce(PacketCreator.showForcedEquip(chr.getTeam()));
		}
		objectWLock.lock();
		try {
			this.mapobjects.put(Integer.valueOf(chr.getObjectId()), chr);
		} finally {
			objectWLock.unlock();
		}
		if (chr.getPlayerShop() != null) {
			addMapObject(chr.getPlayerShop());
		}
		StatEffect summonStat = chr.getStatForBuff(BuffStat.SUMMON);
		if (summonStat != null) {
			Summon summon = chr.getSummons().get(summonStat.getSourceId());
			summon.setPosition(chr.getPosition());
			chr.getMap().spawnSummon(summon);
			updateMapObjectVisibility(chr, summon);
		}
		if (mapEffect != null) {
			mapEffect.sendStartData(chr.getClient());
		}
		if (mapid == 914000200 || mapid == 914000210 || mapid == 914000220) {
			TimerManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					chr.getClient().announce(PacketCreator.aranGodlyStats());
				}
			}, 1500);
		}
		if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
			chr.getClient().announce(PacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
		}
		if (chr.getFitness() != null && chr.getFitness().isTimerStarted()) {
			chr.getClient().announce(PacketCreator.getClock((int) (chr.getFitness().getTimeLeft() / 1000)));
		}

		if (chr.getOla() != null && chr.getOla().isTimerStarted()) {
			chr.getClient().announce(PacketCreator.getClock((int) (chr.getOla().getTimeLeft() / 1000)));
		}

		if (mapid == 109060000) {
			chr.announce(PacketCreator.rollSnowBall(true, 0, null, null));
		}

		MonsterCarnival carnival = chr.getCarnival();
		MonsterCarnivalParty cparty = chr.getCarnivalParty();
		if (carnival != null && cparty != null && (mapid == 980000101 || mapid == 980000201 || mapid == 980000301 || mapid == 980000401 || mapid == 980000501 || mapid == 980000601)) {
			chr.getClient().announce(PacketCreator.getClock((int) (carnival.getTimeLeft() / 1000)));
			chr.getClient().announce(PacketCreator.startCPQ(chr, carnival.oppositeTeam(cparty)));
		}
		if (hasClock()) {
			Calendar cal = Calendar.getInstance();
			chr.getClient().announce((PacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
		}
		if (hasBoat() == 2) {
			chr.getClient().announce((PacketCreator.boatPacket(true)));
		} else if (hasBoat() == 1 && (chr.getMapId() != 200090000 || chr.getMapId() != 200090010)) {
			chr.getClient().announce(PacketCreator.boatPacket(false));
		}
		chr.receivePartyMemberHP();
	}

	public Portal findClosestPortal(Point from) {
		Portal closest = null;
		double shortestDistance = Double.POSITIVE_INFINITY;
		for (Portal portal : portals.values()) {
			double distance = portal.getPosition().distanceSq(from);
			if (distance < shortestDistance) {
				closest = portal;
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public Portal getRandomSpawnpoint() {
		List<Portal> spawnPoints = new ArrayList<Portal>();
		for (Portal portal : portals.values()) {
			if (portal.getType() >= 0 && portal.getType() <= 2) {
				spawnPoints.add(portal);
			}
		}
		Portal portal = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
		return portal != null ? portal : getPortal(0);
	}

	public void removePlayer(GameCharacter chr) {
		chrWLock.lock();
		try {
			characters.remove(chr);
		} finally {
			chrWLock.unlock();
		}
		removeMapObject(Integer.valueOf(chr.getObjectId()));
		if (!chr.isHidden()) {
			broadcastMessage(PacketCreator.removePlayerFromMap(chr.getId()));
		} else {
			broadcastGMMessage(PacketCreator.removePlayerFromMap(chr.getId()));
		}

		for (Monster monster : chr.getControlledMonsters()) {
			monster.setController(null);
			monster.setControllerHasAggro(false);
			monster.setControllerKnowsAboutAggro(false);
			updateMonsterController(monster);
		}
		chr.leaveMap();
		chr.cancelMapTimeLimitTask();
		for (Summon summon : chr.getSummons().values()) {
			if (summon.isStationary()) {
				chr.cancelBuffStats(BuffStat.PUPPET);
			} else {
				removeMapObject(summon);
			}
		}
	}

	public void broadcastMessage(GamePacket packet) {
		broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
	}

	public void broadcastGMMessage(GamePacket packet) {
		broadcastGMMessage(null, packet, Double.POSITIVE_INFINITY, null);
	}

	/**
	 * Nonranged. Repeat to source according to parameter.
	 * 
	 * @param source
	 * @param packet
	 * @param repeatToSource
	 */
	public void broadcastMessage(GameCharacter source, GamePacket packet, boolean repeatToSource) {
		broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
	}

	/**
	 * Ranged and repeat according to parameters.
	 * 
	 * @param source
	 * @param packet
	 * @param repeatToSource
	 * @param ranged
	 */
	public void broadcastMessage(GameCharacter source, GamePacket packet, boolean repeatToSource, boolean ranged) {
		broadcastMessage(repeatToSource ? null : source, packet, ranged ? 722500 : Double.POSITIVE_INFINITY, source.getPosition());
	}

	/**
	 * Always ranged from Point.
	 * 
	 * @param packet
	 * @param rangedFrom
	 */
	public void broadcastMessage(GamePacket packet, Point rangedFrom) {
		broadcastMessage(null, packet, 722500, rangedFrom);
	}

	/**
	 * Always ranged from point. Does not repeat to source.
	 * 
	 * @param source
	 * @param packet
	 * @param rangedFrom
	 */
	public void broadcastMessage(GameCharacter source, GamePacket packet, Point rangedFrom) {
		broadcastMessage(source, packet, 722500, rangedFrom);
	}

	private void broadcastMessage(GameCharacter source, GamePacket packet, double rangeSq, Point rangedFrom) {
		chrRLock.lock();
		try {
			for (GameCharacter chr : characters) {
				if (chr != source) {
					if (rangeSq < Double.POSITIVE_INFINITY) {
						if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
							chr.getClient().announce(packet);
						}
					} else {
						chr.getClient().announce(packet);
					}
				}
			}
		} finally {
			chrRLock.unlock();
		}
	}

	private boolean isNonRangedType(GameMapObjectType type) {
		switch (type) {
			case NPC:
			case PLAYER:
			case HIRED_MERCHANT:
			case PLAYER_NPC:
			case MIST:
				return true;
			default:
				return false;
		}
	}

	private void sendObjectPlacement(GameClient mapleClient) {
		GameCharacter chr = mapleClient.getPlayer();
		objectRLock.lock();
		try {
			for (GameMapObject o : mapobjects.values()) {
				if (o.getType() == GameMapObjectType.SUMMON) {
					Summon summon = (Summon) o;
					if (summon.getOwner() == chr) {
						if (chr.getSummons().isEmpty() || !chr.getSummons().containsValue(summon)) {
							objectWLock.lock();
							try {
								mapobjects.remove(o);
							} finally {
								objectWLock.unlock();
							}
							continue;
						}
					}
				}
				if (isNonRangedType(o.getType())) {
					o.sendSpawnData(mapleClient);
				} else if (o.getType() == GameMapObjectType.MONSTER) {
					updateMonsterController((Monster) o);
				}
			}
		} finally {
			objectRLock.unlock();
		}
		if (chr != null) {
			for (GameMapObject o : getMapObjectsInRange(chr.getPosition(), 722500, rangedMapobjectTypes)) {
				if (o.getType() == GameMapObjectType.REACTOR) {
					if (((Reactor) o).isAlive()) {
						o.sendSpawnData(chr.getClient());
						chr.addVisibleMapObject(o);
					}
				} else {
					o.sendSpawnData(chr.getClient());
					chr.addVisibleMapObject(o);
				}
			}
		}
	}

	public List<GameMapObject> getMapObjectsInRange(Point from, double rangeSq, List<GameMapObjectType> types) {
		List<GameMapObject> ret = new LinkedList<GameMapObject>();
		objectRLock.lock();
		try {
			for (GameMapObject l : mapobjects.values()) {
				if (types.contains(l.getType())) {
					if (from.distanceSq(l.getPosition()) <= rangeSq) {
						ret.add(l);
					}
				}
			}
			return ret;
		} finally {
			objectRLock.unlock();
		}
	}

	public List<GameMapObject> getMapObjectsInBox(Rectangle box, List<GameMapObjectType> types) {
		List<GameMapObject> ret = new LinkedList<GameMapObject>();
		objectRLock.lock();
		try {
			for (GameMapObject l : mapobjects.values()) {
				if (types.contains(l.getType())) {
					if (box.contains(l.getPosition())) {
						ret.add(l);
					}
				}
			}
			return ret;
		} finally {
			objectRLock.unlock();
		}
	}

	public void addPortal(Portal myPortal) {
		portals.put(myPortal.getId(), myPortal);
	}

	public Portal getPortal(String portalname) {
		for (Portal port : portals.values()) {
			if (port.getName().equals(portalname)) {
				return port;
			}
		}
		return null;
	}

	public Portal getPortal(int portalid) {
		return portals.get(portalid);
	}

	public void addMapleArea(Rectangle rec) {
		areas.add(rec);
	}

	public List<Rectangle> getAreas() {
		return new ArrayList<Rectangle>(areas);
	}

	public Rectangle getArea(int index) {
		return areas.get(index);
	}

	public void setFootholds(FootholdTree footholds) {
		this.footholds = footholds;
	}

	public FootholdTree getFootholds() {
		return footholds;
	}

	/**
	 * it's threadsafe, gtfo :D
	 * 
	 * @param monster
	 * @param mobTime
	 */
	public void addMonsterSpawn(Monster monster, int mobTime, int team) {
		Point newpos = calcPointBelow(monster.getPosition());
		newpos.y -= 1;
		SpawnPoint sp = new SpawnPoint(monster.getId(), newpos, !monster.isMobile(), mobTime, mobInterval, team);
		monsterSpawn.add(sp);
		if (sp.shouldSpawn() || mobTime == -1) {// -1 does not respawn and
												// should not either but force
												// ONE spawn
			spawnMonster(sp.getMonster());
		}

	}

	public float getMonsterRate() {
		return monsterRate;
	}

	public Collection<GameCharacter> getCharacters() {
		return Collections.unmodifiableCollection(this.characters);
	}

	public GameCharacter getCharacterById(int id) {
		chrRLock.lock();
		try {
			for (GameCharacter c : this.characters) {
				if (c.getId() == id) {
					return c;
				}
			}
		} finally {
			chrRLock.unlock();
		}
		return null;
	}

	private void updateMapObjectVisibility(GameCharacter chr, GameMapObject mo) {
		if (!chr.isMapObjectVisible(mo)) { // monster entered view range
			if (mo.getType() == GameMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= 722500) {
				chr.addVisibleMapObject(mo);
				mo.sendSpawnData(chr.getClient());
			}
		} else if (mo.getType() != GameMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > 722500) {
			chr.removeVisibleMapObject(mo);
			mo.sendDestroyData(chr.getClient());
		}
	}

	public void moveMonster(Monster monster, Point reportedPos) {
		monster.setPosition(reportedPos);
		chrRLock.lock();
		try {
			for (GameCharacter chr : characters) {
				updateMapObjectVisibility(chr, monster);
			}
		} finally {
			chrRLock.unlock();
		}
	}

	public void movePlayer(GameCharacter player, Point newPosition) {
		player.setPosition(newPosition);
		Collection<GameMapObject> visibleObjects = player.getVisibleMapObjects();
		GameMapObject[] visibleObjectsNow = visibleObjects.toArray(new GameMapObject[visibleObjects.size()]);
		try {
			for (GameMapObject mo : visibleObjectsNow) {
				if (mo != null) {
					if (mapobjects.get(mo.getObjectId()) == mo) {
						updateMapObjectVisibility(player, mo);
					} else {
						player.removeVisibleMapObject(mo);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (GameMapObject mo : getMapObjectsInRange(player.getPosition(), 722500, rangedMapobjectTypes)) {
			if (!player.isMapObjectVisible(mo)) {
				mo.sendSpawnData(player.getClient());
				player.addVisibleMapObject(mo);
			}
		}
	}

	public Portal findClosestSpawnpoint(Point from) {
		Portal closest = null;
		double shortestDistance = Double.POSITIVE_INFINITY;
		for (Portal portal : portals.values()) {
			double distance = portal.getPosition().distanceSq(from);
			if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
				closest = portal;
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public Collection<Portal> getPortals() {
		return Collections.unmodifiableCollection(portals.values());
	}

	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public String getStreetName() {
		return streetName;
	}

	public void setClock(boolean hasClock) {
		this.clock = hasClock;
	}

	public boolean hasClock() {
		return clock;
	}

	public void setTown(boolean isTown) {
		this.town = isTown;
	}

	public boolean isTown() {
		return town;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	public void setEverlast(boolean everlast) {
		this.everlast = everlast;
	}

	public boolean getEverlast() {
		return everlast;
	}

	public int getSpawnedMonstersOnMap() {
		return spawnedMonstersOnMap.get();
	}

	public void setMobCapacity(int capacity) {
		this.mobCapacity = capacity;

	}

	public void nullifyObject(GameMapObject mmobj) {// nice one Simon (: thanks
														// <3
		mmobj.nullifyPosition();
		mmobj = null;
	}

	private class ExpireMapItemJob implements Runnable {

		private GameMapItem mapitem;

		public ExpireMapItemJob(GameMapItem mapitem) {
			this.mapitem = mapitem;
		}

		@Override
		public void run() {
			if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
				mapitem.itemLock.lock();
				try {
					if (mapitem.isPickedUp()) {
						return;
					}
					GameMap.this.broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
					mapitem.setPickedUp(true);
				} finally {
					mapitem.itemLock.unlock();
					GameMap.this.removeMapObject(mapitem);
				}
			}
		}
	}

	private class ActivateItemReactor implements Runnable {

		private GameMapItem mapitem;
		private Reactor reactor;
		private GameClient c;

		public ActivateItemReactor(GameMapItem mapitem, Reactor reactor, GameClient c) {
			this.mapitem = mapitem;
			this.reactor = reactor;
			this.c = c;
		}

		@Override
		public void run() {
			if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId())) {
				mapitem.itemLock.lock();
				try {
					TimerManager tMan = TimerManager.getInstance();
					if (mapitem.isPickedUp()) {
						return;
					}
					GameMap.this.broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
					GameMap.this.removeMapObject(mapitem);
					reactor.hitReactor(c);
					reactor.setTimerActive(false);
					if (reactor.getDelay() > 0) {
						tMan.schedule(new Runnable() {

							@Override
							public void run() {
								reactor.setState((byte) 0);
								broadcastMessage(PacketCreator.triggerReactor(reactor, 0));
							}
						}, reactor.getDelay());
					}
				} finally {
					mapitem.itemLock.unlock();
				}
			}
		}
	}

	public void respawn() {
		if (characters.isEmpty()) {
			return;
		}
		short numShouldSpawn = (short) ((monsterSpawn.size() - spawnedMonstersOnMap.get()) * monsterRate);// Fking
																											// lol'd
		if (numShouldSpawn > 0) {
			List<SpawnPoint> randomSpawn = new ArrayList<SpawnPoint>(monsterSpawn);
			Collections.shuffle(randomSpawn);
			short spawned = 0;
			for (SpawnPoint spawnPoint : randomSpawn) {
				if (spawnPoint.shouldSpawn()) {
					spawnMonster(spawnPoint.getMonster());
					spawned++;
				}
				if (spawned >= numShouldSpawn) {
					break;

				}
			}
		}
	}

	private static interface DelayedPacketCreation {

		void sendPackets(GameClient c);
	}

	private static interface SpawnCondition {

		boolean canSpawn(GameCharacter chr);
	}

	public int getHpDecrease() {
		return hpDecrease;
	}

	public void setHpDecrease(int delta) {
		hpDecrease = delta;
	}

	public int getProtectionItemId() {
		return protectItem;
	}

	public void setProtectionItemId(int itemId) {
		this.protectItem = itemId;
	}

	private int hasBoat() {
		return docked ? 2 : (boat ? 1 : 0);
	}

	public void setBoat(boolean hasBoat) {
		this.boat = hasBoat;
	}

	public void setDocked(boolean isDocked) {
		this.docked = isDocked;
	}

	public void broadcastGMMessage(GameCharacter source, GamePacket packet, boolean repeatToSource) {
		broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
	}

	private void broadcastGMMessage(GameCharacter source, GamePacket packet, double rangeSq, Point rangedFrom) {
		chrRLock.lock();
		try {
			for (GameCharacter chr : characters) {
				if (chr != source && chr.isGM()) {
					if (rangeSq < Double.POSITIVE_INFINITY) {
						if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
							chr.getClient().announce(packet);
						}
					} else {
						chr.getClient().announce(packet);
					}
				}
			}
		} finally {
			chrRLock.unlock();
		}
	}

	public void broadcastNONGMMessage(GameCharacter source, GamePacket packet, boolean repeatToSource) {
		chrRLock.lock();
		try {
			for (GameCharacter chr : characters) {
				if (chr != source && !chr.isGM()) {
					chr.getClient().announce(packet);
				}
			}
		} finally {
			chrRLock.unlock();
		}
	}

	public MapleOxQuiz getOx() {
		return ox;
	}

	public void setOx(MapleOxQuiz set) {
		this.ox = set;
	}

	public void setOxQuiz(boolean b) {
		this.isOxQuiz = b;
	}

	public boolean isOxQuiz() {
		return isOxQuiz;
	}

	public void setOnUserEnter(String onUserEnter) {
		this.onUserEnter = onUserEnter;
	}

	public String getOnUserEnter() {
		return onUserEnter;
	}

	public void setOnFirstUserEnter(String onFirstUserEnter) {
		this.onFirstUserEnter = onFirstUserEnter;
	}

	public String getOnFirstUserEnter() {
		return onFirstUserEnter;
	}

	private boolean hasForcedEquip() {
		return fieldType == 81 || fieldType == 82;
	}

	public void setFieldType(int fieldType) {
		this.fieldType = fieldType;
	}

	public void clearDrops(GameCharacter player) {
		List<GameMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM));
		for (GameMapObject i : items) {
			player.getMap().removeMapObject(i);
			player.getMap().broadcastMessage(PacketCreator.removeItemFromMap(i.getObjectId(), 0, player.getId()));
		}
	}

	public void clearDrops() {
		for (GameMapObject i : getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM))) {
			removeMapObject(i);
		}
	}

	public void addMapTimer(int time) {
		timeLimit = System.currentTimeMillis() + (time * 1000);
		broadcastMessage(PacketCreator.getClock(time));
		mapMonitor = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				if (timeLimit != 0 && timeLimit < System.currentTimeMillis()) {
					warpEveryone(getForcedReturnId());
				}
				if (getCharacters().isEmpty()) {
					resetReactors();
					killAllMonsters();
					clearDrops();
					timeLimit = 0;
					if (mapid >= 922240100 && mapid <= 922240119) {
						toggleHiddenNPC(9001108);
					}
					mapMonitor.cancel(true);
					mapMonitor = null;
				}
			}
		}, 1000);
	}

	public void setFieldLimit(int fieldLimit) {
		this.fieldLimit = fieldLimit;
	}

	public int getFieldLimit() {
		return fieldLimit;
	}

	public void resetRiceCakes() {
		this.riceCakeNum = 0;
	}

	public void setAllowHPQSummon(boolean b) {
		this.allowHPQSummon = b;
	}

	public void warpEveryone(int to) {
		for (GameCharacter chr : getCharacters()) {
			chr.changeMap(to);
		}
	}

	// BEGIN EVENTS
	public void setSnowball(int team, MapleSnowball ball) {
		switch (team) {
			case 0:
				this.snowball0 = ball;
				break;
			case 1:
				this.snowball1 = ball;
				break;
			default:
				break;
		}
	}

	public MapleSnowball getSnowball(int team) {
		switch (team) {
			case 0:
				return snowball0;
			case 1:
				return snowball1;
			default:
				return null;
		}
	}

	private boolean specialEquip() {// Maybe I shouldn't use fieldType :\
		return fieldType == 4 || fieldType == 19;
	}

	public void setCoconut(MapleCoconut nut) {
		this.coconut = nut;
	}

	public MapleCoconut getCoconut() {
		return coconut;
	}

	public void warpOutByTeam(int team, int mapid) {
		for (GameCharacter chr : getCharacters()) {
			if (chr != null) {
				if (chr.getTeam() == team) {
					chr.changeMap(mapid);
				}
			}
		}
	}

	public void startEvent(final GameCharacter chr) {
		if (this.mapid == 109080000) {
			setCoconut(new MapleCoconut(this));
			coconut.startEvent();

		} else if (this.mapid == 109040000) {
			chr.setFitness(new MapleFitness(chr));
			chr.getFitness().startFitness();

		} else if (this.mapid == 109030001 || this.mapid == 109030101) {
			chr.setOla(new MapleOla(chr));
			chr.getOla().startOla();

		} else if (this.mapid == 109020001 && getOx() == null) {
			setOx(new MapleOxQuiz(this));
			getOx().sendQuestion();
			setOxQuiz(true);

		} else if (this.mapid == 109060000 && getSnowball(chr.getTeam()) == null) {
			setSnowball(0, new MapleSnowball(0, this));
			setSnowball(1, new MapleSnowball(1, this));
			getSnowball(chr.getTeam()).startEvent();
		}
	}

	public boolean eventStarted() {
		return eventstarted;
	}

	public void startEvent() {
		this.eventstarted = true;
	}

	public void setEventStarted(boolean event) {
		this.eventstarted = event;
	}

	public String getEventNPC() {
		StringBuilder sb = new StringBuilder();
		sb.append("Talk to ");
		if (mapid == 60000) {
			sb.append("Paul!");
		} else if (mapid == 104000000) {
			sb.append("Jean!");
		} else if (mapid == 200000000) {
			sb.append("Martin!");
		} else if (mapid == 220000000) {
			sb.append("Tony!");
		} else {
			return null;
		}
		return sb.toString();
	}

	public boolean hasEventNPC() {
		return this.mapid == 60000 || this.mapid == 104000000 || this.mapid == 200000000 || this.mapid == 220000000;
	}

	public boolean isStartingEventMap() {
		return this.mapid == 109040000 || this.mapid == 109020001 || this.mapid == 109010000 || this.mapid == 109030001 || this.mapid == 109030101;
	}

	public boolean isEventMap() {
		return this.mapid >= 109010000 && this.mapid < 109050000 || this.mapid > 109050001 && this.mapid <= 109090000;
	}

	public void timeMob(int id, String msg) {
		timeMob = new TimeMobEntry(id, msg);
	}

	public TimeMobEntry getTimeMob() {
		return timeMob;
	}

	public void toggleHiddenNPC(int id) {
		for (GameMapObject obj : mapobjects.values()) {
			if (obj.getType() == GameMapObjectType.NPC) {
				Npc npc = (Npc) obj;
				if (npc.getId() == id) {
					npc.setHide(!npc.isHidden());
					if (!npc.isHidden()) // Should only be hidden upon changing
											// maps
					{
						broadcastMessage(PacketCreator.spawnNPC(npc));
					}
				}
			}
		}
	}

	public void setMobInterval(short interval) {
		this.mobInterval = interval;
	}

	public short getMobInterval() {
		return mobInterval;
	}
}
