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
package server.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import client.GameCharacter;
import client.Disease;
import client.status.MonsterStatus;
import java.util.LinkedList;
import java.util.Map;
import tools.Randomizer;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.maps.Mist;
import tools.ArrayMap;

/**
 * 
 * @author Danny (Leifde)
 */
public class MobSkill {
	private int skillId, skillLevel, mpCon;
	private List<Integer> toSummon = new ArrayList<Integer>();
	private int spawnEffect, hp, x, y;
	private long duration, cooltime;
	private float prop;
	private Point lt, rb;
	private int limit;

	public MobSkill(int skillId, int level) {
		this.skillId = skillId;
		this.skillLevel = level;
	}

	public void setMpCon(int mpCon) {
		this.mpCon = mpCon;
	}

	public void addSummons(List<Integer> toSummon) {
		for (Integer summon : toSummon) {
			this.toSummon.add(summon);
		}
	}

	public void setSpawnEffect(int spawnEffect) {
		this.spawnEffect = spawnEffect;
	}

	public void setHp(int hp) {
		this.hp = hp;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void setCoolTime(long cooltime) {
		this.cooltime = cooltime;
	}

	public void setProp(float prop) {
		this.prop = prop;
	}

	public void setLtRb(Point lt, Point rb) {
		this.lt = lt;
		this.rb = rb;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void applyEffect(GameCharacter player, Monster monster, boolean skill) {
		Disease disease = null;
		Map<MonsterStatus, Integer> stats = new ArrayMap<MonsterStatus, Integer>();
		List<Integer> reflection = new LinkedList<Integer>();
		switch (skillId) {
			case 100:
			case 110:
			case 150:
				stats.put(MonsterStatus.WEAPON_ATTACK_UP, Integer.valueOf(x));
				break;
			case 101:
			case 111:
			case 151:
				stats.put(MonsterStatus.MAGIC_ATTACK_UP, Integer.valueOf(x));
				break;
			case 102:
			case 112:
			case 152:
				stats.put(MonsterStatus.WEAPON_DEFENSE_UP, Integer.valueOf(x));
				break;
			case 103:
			case 113:
			case 153:
				stats.put(MonsterStatus.MAGIC_DEFENSE_UP, Integer.valueOf(x));
				break;
			case 114:
				if (lt != null && rb != null && skill) {
					List<GameMapObject> objects = getObjectsInRange(monster, GameMapObjectType.MONSTER);
					final int hps = (getX() / 1000) * (int) (950 + 1050 * Math.random());
					for (GameMapObject mons : objects) {
						((Monster) mons).heal(hps, getY());
					}
				} else {
					monster.heal(getX(), getY());
				}
				break;
			case 120:
				disease = Disease.SEAL;
				break;
			case 121:
				disease = Disease.DARKNESS;
				break;
			case 122:
				disease = Disease.WEAKEN;
				break;
			case 123:
				disease = Disease.STUN;
				break;
			case 124:
				disease = Disease.CURSE;
				break;
			case 125:
				disease = Disease.POISON;
				break;
			case 126: // Slow
				disease = Disease.SLOW;
				break;
			case 127:
				if (lt != null && rb != null && skill) {
					for (GameCharacter character : getPlayersInRange(monster, player)) {
						character.dispel();
					}
				} else {
					player.dispel();
				}
				break;
			case 128: // Seduce
				disease = Disease.SEDUCE;
				break;
			case 129: // Banish
				if (lt != null && rb != null && skill) {
					for (GameCharacter chr : getPlayersInRange(monster, player)) {
						chr.changeMapBanish(monster.getBanish().getMap(), monster.getBanish().getPortal(), monster.getBanish().getMsg());
					}
				} else {
					player.changeMapBanish(monster.getBanish().getMap(), monster.getBanish().getPortal(), monster.getBanish().getMsg());
				}
				break;
			case 131: // Mist
				monster.getMap().spawnMist(new Mist(calculateBoundingBox(monster.getPosition(), true), monster, this), x * 10, false, false);
				break;
			case 132:
				disease = Disease.CONFUSE;
				break;
			case 133: // zombify
				break;
			case 140:
				if (makeChanceResult() && !monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
					stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(x));
				}
				break;
			case 141:
				if (makeChanceResult() && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
					stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(x));
				}
				break;
			case 143: // Weapon Reflect
				stats.put(MonsterStatus.WEAPON_REFLECT, Integer.valueOf(x));
				stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(x));
				reflection.add(x);
				break;
			case 144: // Magic Reflect
				stats.put(MonsterStatus.MAGIC_REFLECT, Integer.valueOf(x));
				stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(x));
				reflection.add(x);
				break;
			case 145: // Weapon / Magic reflect
				stats.put(MonsterStatus.WEAPON_REFLECT, Integer.valueOf(x));
				stats.put(MonsterStatus.WEAPON_IMMUNITY, Integer.valueOf(x));
				stats.put(MonsterStatus.MAGIC_REFLECT, Integer.valueOf(x));
				stats.put(MonsterStatus.MAGIC_IMMUNITY, Integer.valueOf(x));
				reflection.add(x);
				break;
			case 154: // accuracy up
			case 155: // avoid up
			case 156: // speed up
				break;
			case 200:
				if (monster.getMap().getSpawnedMonstersOnMap() < 80) {
					for (Integer mobId : getSummons()) {
						Monster toSpawn = LifeFactory.getMonster(mobId);
						toSpawn.setPosition(monster.getPosition());
						int ypos, xpos;
						xpos = (int) monster.getPosition().getX();
						ypos = (int) monster.getPosition().getY();
						switch (mobId) {
							case 8500003: // Pap bomb high
								toSpawn.setFh((int) Math.ceil(Math.random() * 19.0));
								ypos = -590;
								break;
							case 8500004: // Pap bomb
								xpos = (int) (monster.getPosition().getX() + Randomizer.nextInt(1000) - 500);
								if (ypos != -590) {
									ypos = (int) monster.getPosition().getY();
								}
								break;
							case 8510100: // Pianus bomb
								if (Math.ceil(Math.random() * 5) == 1) {
									ypos = 78;
									xpos = (int) Randomizer.nextInt(5) + (Randomizer.nextInt(2) == 1 ? 180 : 0);
								} else {
									xpos = (int) (monster.getPosition().getX() + Randomizer.nextInt(1000) - 500);
								}
								break;
						}
						switch (monster.getMap().getId()) {
							case 220080001: // Pap map
								if (xpos < -890) {
									xpos = (int) (Math.ceil(Math.random() * 150) - 890);
								} else if (xpos > 230) {
									xpos = (int) (230 - Math.ceil(Math.random() * 150));
								}
								break;
							case 230040420: // Pianus map
								if (xpos < -239) {
									xpos = (int) (Math.ceil(Math.random() * 150) - 239);
								} else if (xpos > 371) {
									xpos = (int) (371 - Math.ceil(Math.random() * 150));
								}
								break;
						}
						toSpawn.setPosition(new Point(xpos, ypos));
						monster.getMap().spawnMonsterWithEffect(toSpawn, getSpawnEffect(), toSpawn.getPosition());
					}
				}
				break;
		}
		if (stats.size() > 0) {
			if (lt != null && rb != null && skill) {
				for (GameMapObject mons : getObjectsInRange(monster, GameMapObjectType.MONSTER)) {
					((Monster) mons).applyMonsterBuff(stats, getX(), getSkillId(), getDuration(), this, reflection);
				}
			} else {
				monster.applyMonsterBuff(stats, getX(), getSkillId(), getDuration(), this, reflection);
			}
		}
		if (disease != null) {
			if (lt != null && rb != null && skill) {
				int i = 0;
				for (GameCharacter character : getPlayersInRange(monster, player)) {
					if (!character.isActiveBuffedValue(2321005)) {
						if (disease.equals(Disease.SEDUCE)) {
							if (i < 10) {
								character.giveDebuff(Disease.SEDUCE, this);
								i++;
							}
						} else {
							character.giveDebuff(disease, this);
						}
					}
				}
			} else {
				player.giveDebuff(disease, this);
			}
		}
		monster.usedSkill(skillId, skillLevel, cooltime);
		monster.setMp(monster.getMp() - getMpCon());
	}

	private List<GameCharacter> getPlayersInRange(Monster monster, GameCharacter player) {
		List<GameCharacter> players = new ArrayList<GameCharacter>();
		players.add(player);
		return monster.getMap().getPlayersInRange(calculateBoundingBox(monster.getPosition(), monster.isFacingLeft()), players);
	}

	public int getSkillId() {
		return skillId;
	}

	public int getSkillLevel() {
		return skillLevel;
	}

	public int getMpCon() {
		return mpCon;
	}

	public List<Integer> getSummons() {
		return Collections.unmodifiableList(toSummon);
	}

	public int getSpawnEffect() {
		return spawnEffect;
	}

	public int getHP() {
		return hp;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public long getDuration() {
		return duration;
	}

	public long getCoolTime() {
		return cooltime;
	}

	public Point getLt() {
		return lt;
	}

	public Point getRb() {
		return rb;
	}

	public int getLimit() {
		return limit;
	}

	public boolean makeChanceResult() {
		return prop == 1.0 || Math.random() < prop;
	}

	private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
		int multiplier = facingLeft ? 1 : -1;
		Point mylt = new Point(lt.x * multiplier + posFrom.x, lt.y + posFrom.y);
		Point myrb = new Point(rb.x * multiplier + posFrom.x, rb.y + posFrom.y);
		return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
	}

	private List<GameMapObject> getObjectsInRange(Monster monster, GameMapObjectType objectType) {
		List<GameMapObjectType> objectTypes = new ArrayList<GameMapObjectType>();
		objectTypes.add(objectType);
		return monster.getMap().getMapObjectsInBox(calculateBoundingBox(monster.getPosition(), monster.isFacingLeft()), objectTypes);
	}
}
