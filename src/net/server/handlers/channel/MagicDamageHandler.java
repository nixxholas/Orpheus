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

import client.ISkill;
import client.GameCharacter;
import client.GameCharacter.CancelCooldownAction;
import client.GameClient;
import client.SkillFactory;
import net.GamePacket;
import server.MapleStatEffect;
import server.TimerManager;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MagicDamageHandler extends AbstractDealDamageHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter player = c.getPlayer();
		AttackInfo attack = parseDamage(slea, player, false);
		GamePacket packet = PacketCreator.magicAttack(player, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, attack.allDamage, -1, attack.speed, attack.direction, attack.display);
		if (attack.skill == 2121001 || attack.skill == 2221001 || attack.skill == 2321001) {
			packet = PacketCreator.magicAttack(player, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.charge, attack.speed, attack.direction, attack.display);
		}
		player.getMap().broadcastMessage(player, packet, false, true);
		MapleStatEffect effect = attack.getAttackEffect(player, null);
		ISkill skill = SkillFactory.getSkill(attack.skill);
		MapleStatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
		if (effect_.getCooldown() > 0) {
			if (player.skillisCooling(attack.skill)) {
				return;
			} else {
				c.announce(PacketCreator.skillCooldown(attack.skill, effect_.getCooldown()));
				player.addCooldown(attack.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(player, attack.skill), effect_.getCooldown() * 1000));
			}
		}
		applyAttack(attack, player, effect.getAttackCount());

		// MP Eater, works with right job
		ISkill eaterSkill = SkillFactory.getSkill((player.getJob().getId() - (player.getJob().getId() % 10)) * 10000);
		int eaterLevel = player.getSkillLevel(eaterSkill);
		if (eaterLevel > 0) {
			for (Integer singleDamage : attack.allDamage.keySet()) {
				eaterSkill.getEffect(eaterLevel).applyPassive(player, player.getMap().getMapObject(singleDamage), 0);
			}
		}
	}
}
