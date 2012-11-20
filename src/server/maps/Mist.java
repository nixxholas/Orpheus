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

import client.ISkill;
import client.GameCharacter;
import client.GameClient;
import client.SkillFactory;
import java.awt.Point;
import java.awt.Rectangle;
import net.GamePacket;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.PacketCreator;

/**
 * 
 * @author LaiLaiNoob
 */
public class Mist extends AbstractGameMapObject {
	private Rectangle mistPosition;
	private GameCharacter owner = null;
	private MapleMonster mob = null;
	private MapleStatEffect source;
	private MobSkill skill;
	private boolean isMobMist, isPoisonMist;
	private int skillDelay;

	public Mist(Rectangle mistPosition, MapleMonster mob, MobSkill skill) {
		this.mistPosition = mistPosition;
		this.mob = mob;
		this.skill = skill;
		isMobMist = true;
		isPoisonMist = true;
		skillDelay = 0;
	}

	public Mist(Rectangle mistPosition, GameCharacter owner, MapleStatEffect source) {
		this.mistPosition = mistPosition;
		this.owner = owner;
		this.source = source;
		this.skillDelay = 8;
		this.isMobMist = false;
		switch (source.getSourceId()) {
			case 4221006: // Smoke Screen
				isPoisonMist = false;
				break;
			case 2111003: // FP mist
			case 12111005: // Flame Gear
			case 14111006: // Poison Bomb
				isPoisonMist = true;
				break;
		}
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.MIST;
	}

	@Override
	public Point getPosition() {
		return mistPosition.getLocation();
	}

	public ISkill getSourceSkill() {
		return SkillFactory.getSkill(source.getSourceId());
	}

	public boolean isMobMist() {
		return isMobMist;
	}

	public boolean isPoisonMist() {
		return isPoisonMist;
	}

	public int getSkillDelay() {
		return skillDelay;
	}

	public MapleMonster getMobOwner() {
		return mob;
	}

	public GameCharacter getOwner() {
		return owner;
	}

	public Rectangle getBox() {
		return mistPosition;
	}

	@Override
	public void setPosition(Point position) {
		throw new UnsupportedOperationException();
	}

	public GamePacket makeDestroyData() {
		return PacketCreator.removeMist(getObjectId());
	}

	public GamePacket makeSpawnData() {
		if (owner != null) {
			return PacketCreator.spawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId())), this);
		}
		return PacketCreator.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
	}

	public GamePacket makeFakeSpawnData(int level) {
		if (owner != null) {
			return PacketCreator.spawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), level, this);
		}
		return PacketCreator.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
	}

	@Override
	public void sendSpawnData(GameClient client) {
		client.getSession().write(makeSpawnData());
	}

	@Override
	public void sendDestroyData(GameClient client) {
		client.getSession().write(makeDestroyData());
	}

	public boolean makeChanceResult() {
		return source.makeChanceResult();
	}
}
