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
import client.BuffStat;
import client.GameCharacter;
import client.GameCharacter.CancelCooldownAction;
import client.GameClient;
import client.Job;
import client.Stat;
import client.SkillFactory;
import constants.skills.Crusader;
import constants.skills.DawnWarrior;
import constants.skills.DragonKnight;
import constants.skills.Hero;
import constants.skills.NightWalker;
import constants.skills.Rogue;
import constants.skills.WindArcher;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import server.AttackInfo;
import server.BuffStatDelta;
import server.StatEffect;
import server.TimerManager;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CloseRangeDamageHandler extends AbstractDealDamageHandler {

	private boolean isFinisher(int skillId) {
		return skillId > 1111002 && skillId < 1111007 || skillId == 11111002 || skillId == 11111003;
	}

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter player = c.getPlayer();
		AttackInfo attack = parseDamage(reader, player, false);
		player.getMap().broadcastMessage(player, PacketCreator.closeRangeAttack(player, attack.skill.id, attack.skill.level, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed, attack.direction, attack.display), false, true);
		int numFinisherOrbs = 0;
		Integer comboBuff = player.getBuffedValue(BuffStat.COMBO);
		if (isFinisher(attack.skill.id)) {
			if (comboBuff != null) {
				numFinisherOrbs = comboBuff.intValue() - 1;
			}
			player.handleOrbconsume();
		} else if (attack.numAttacked > 0) {
			if (attack.skill.id != 1111008 && comboBuff != null) {
				int orbcount = player.getBuffedValue(BuffStat.COMBO);
				int oid = player.isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
				int advcomboid = player.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO;
				ISkill combo = SkillFactory.getSkill(oid);
				ISkill advcombo = SkillFactory.getSkill(advcomboid);
				StatEffect ceffect = null;
				int advComboSkillLevel = player.getSkillLevel(advcombo);
				if (advComboSkillLevel > 0) {
					ceffect = advcombo.getEffect(advComboSkillLevel);
				} else {
					ceffect = combo.getEffect(player.getSkillLevel(combo));
				}
				
				if (orbcount < ceffect.getX() + 1) {
					int neworbcount = orbcount + 1;
					if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
						if (neworbcount <= ceffect.getX()) {
							neworbcount++;
						}
					}
					int duration = combo.getEffect(player.getSkillLevel(oid)).getDuration();
					List<BuffStatDelta> stat = Collections.singletonList(new BuffStatDelta(BuffStat.COMBO, neworbcount));
					player.setBuffedValue(BuffStat.COMBO, neworbcount);
					duration -= (int) (System.currentTimeMillis() - player.getBuffedStarttime(BuffStat.COMBO));
					c.announce(PacketCreator.giveBuff(oid, duration, stat));
					player.getMap().broadcastMessage(player, PacketCreator.giveForeignBuff(player.getId(), stat), false);
				}
			} else if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(15100004) : SkillFactory.getSkill(5110001)) > 0 && (player.getJob().isA(Job.MARAUDER) || player.getJob().isA(Job.THUNDERBREAKER2))) {
				for (int i = 0; i < attack.numAttacked; i++) {
					player.handleEnergyChargeGain();
				}
			}
		}
		if (attack.numAttacked > 0 && attack.skill.id == DragonKnight.SACRIFICE) {
			// sacrifice attacks only 1 mob with 1 attack
			int totDamageToOneMonster = 0; 
			final Iterator<List<Integer>> dmgIt = attack.allDamage.values().iterator();
			if (dmgIt.hasNext()) {
				totDamageToOneMonster = dmgIt.next().get(0).intValue();
			}
			
			int remainingHP = player.getHp() - totDamageToOneMonster * attack.getAttackEffect(player, null).getX() / 100;
			if (remainingHP > 1) {
				player.setHp(remainingHP);
			} else {
				player.setHp(1);
			}
			
			player.updateSingleStat(Stat.HP, player.getHp());
			player.checkBerserk();
		}
		
		if (attack.numAttacked > 0 && attack.skill.id == 1211002) {
			boolean advcharge_prob = false;
			int advcharge_level = player.getSkillLevel(SkillFactory.getSkill(1220010));
			if (advcharge_level > 0) {
				advcharge_prob = SkillFactory.getSkill(1220010).getEffect(advcharge_level).makeChanceResult();
			}
			
			if (!advcharge_prob) {
				player.cancelEffectFromBuffStat(BuffStat.WK_CHARGE);
			}
		}
		
		int attackCount = 1;
		if (attack.skill.id != 0) {
			attackCount = attack.getAttackEffect(player, null).getAttackCount();
		}
		
		if (numFinisherOrbs == 0 && isFinisher(attack.skill.id)) {
			return;
		}
		
		if (attack.skill.id > 0) {
			ISkill skill = SkillFactory.getSkill(attack.skill.id);
			StatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
			if (effect_.getCooldown() > 0) {
				if (player.skillisCooling(attack.skill.id)) {
					return;
				} else {
					c.announce(PacketCreator.skillCooldown(attack.skill.id, effect_.getCooldown()));
					player.addCooldown(attack.skill.id, System.currentTimeMillis(), effect_.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(player, attack.skill.id), effect_.getCooldown() * 1000));
				}
			}
		}
		if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 
				|| player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0 
				|| player.getSkillLevel(SkillFactory.getSkill(Rogue.DARK_SIGHT)) > 0)
				
				&& player.getBuffedValue(BuffStat.DARKSIGHT) != null) {
			// && player.getBuffSource(BuffStat.DARKSIGHT) != 9101004
			player.cancelEffectFromBuffStat(BuffStat.DARKSIGHT);
			player.cancelBuffStats(BuffStat.DARKSIGHT);
		}
		applyAttack(attack, player, attackCount);
	}
}