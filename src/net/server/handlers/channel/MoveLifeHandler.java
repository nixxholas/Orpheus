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
package net.server.handlers.channel;

import java.awt.Point;
import java.util.List;
import client.GameClient;
import tools.Randomizer;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillEntry;
import server.life.MobSkillFactory;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.movement.LifeMovementFragment;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MoveLifeHandler extends AbstractMovementPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int objectid = slea.readInt();
		short moveid = slea.readShort();
		GameMapObject mmo = c.getPlayer().getMap().getMapObject(objectid);
		if (mmo == null || mmo.getType() != GameMapObjectType.MONSTER) {
			return;
		}
		MapleMonster monster = (MapleMonster) mmo;
		List<LifeMovementFragment> res = null;
		byte skillByte = slea.readByte();
		byte skill = slea.readByte();
		int skill_1 = slea.readByte() & 0xFF;
		byte skill_2 = slea.readByte();
		byte skill_3 = slea.readByte();
		byte skill_4 = slea.readByte();
		slea.read(8);
		MobSkill toUse = null;
		if (skillByte == 1 && monster.getNoSkills() > 0) {
			int random = Randomizer.nextInt(monster.getNoSkills());
			MobSkillEntry skillToUse = monster.getSkills().get(random);
			toUse = MobSkillFactory.getMobSkill(skillToUse.skillId, skillToUse.level);
			int percHpLeft = (monster.getHp() / monster.getMaxHp()) * 100;
			if (toUse.getHP() < percHpLeft || !monster.canUseSkill(toUse)) {
				toUse = null;
			}
		}
		if ((skill_1 >= 100 && skill_1 <= 200) && monster.hasSkill(skill_1, skill_2)) {
			MobSkill skillData = MobSkillFactory.getMobSkill(skill_1, skill_2);
			if (skillData != null && monster.canUseSkill(skillData)) {
				skillData.applyEffect(c.getPlayer(), monster, true);
			}
		}
		slea.readByte();
		slea.readInt(); // whatever
		short start_x = slea.readShort(); // hmm.. startpos?
		short start_y = slea.readShort(); // hmm...
		Point startPos = new Point(start_x, start_y);
		res = parseMovement(slea);
		if (monster.getController() != c.getPlayer()) {
			if (monster.isAttackedBy(c.getPlayer())) {// aggro and controller
														// change
				monster.switchController(c.getPlayer(), true);
			} else {
				return;
			}
		} else if (skill == -1 && monster.isControllerKnowsAboutAggro() && !monster.isMobile() && !monster.isFirstAttack()) {
			monster.setControllerHasAggro(false);
			monster.setControllerKnowsAboutAggro(false);
		}
		boolean aggro = monster.isControllerHasAggro();
		if (toUse != null) {
			c.announce(PacketCreator.moveMonsterResponse(objectid, moveid, monster.getMp(), aggro, toUse.getSkillId(), toUse.getSkillLevel()));
		} else {
			c.announce(PacketCreator.moveMonsterResponse(objectid, moveid, monster.getMp(), aggro));
		}
		if (aggro) {
			monster.setControllerKnowsAboutAggro(true);
		}
		if (res != null) {
			c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.moveMonster(skillByte, skill, skill_1, skill_2, skill_3, skill_4, objectid, startPos, res), monster.getPosition());
			updatePosition(res, monster, -1);
			c.getPlayer().getMap().moveMonster(monster, monster.getPosition());
		}
	}
}
