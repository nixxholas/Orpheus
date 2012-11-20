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

import client.IItem;
import client.ISkill;
import client.BuffStat;
import client.GameCharacter;
import client.GameCharacter.CancelCooldownAction;
import client.GameClient;
import client.Inventory;
import client.InventoryType;
import client.WeaponType;
import client.SkillFactory;
import constants.ItemConstants;
import constants.ServerConstants;
import constants.skills.Aran;
import constants.skills.Buccaneer;
import constants.skills.NightLord;
import constants.skills.NightWalker;
import constants.skills.Shadower;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import tools.Randomizer;
import net.GamePacket;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.MapleStatEffect;
import server.TimerManager;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class RangedAttackHandler extends AbstractDealDamageHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter player = c.getPlayer();
		AttackInfo attack = parseDamage(slea, player, true);
		if (attack.skill == Buccaneer.ENERGY_ORB || attack.skill == ThunderBreaker.SPARK || attack.skill == Shadower.TAUNT || attack.skill == NightLord.TAUNT) {
			player.getMap().broadcastMessage(player, PacketCreator.rangedAttack(player, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, 0, attack.allDamage, attack.speed, attack.direction, attack.display), false);
			applyAttack(attack, player, 1);
		} else if (attack.skill == Aran.COMBO_SMASH || attack.skill == Aran.COMBO_PENRIL || attack.skill == Aran.COMBO_TEMPEST) {
			player.getMap().broadcastMessage(player, PacketCreator.rangedAttack(player, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, 0, attack.allDamage, attack.speed, attack.direction, attack.display), false);
			if (attack.skill == Aran.COMBO_SMASH && player.getCombo() >= 30) {
				applyAttack(attack, player, 1);
			} else if (attack.skill == Aran.COMBO_PENRIL && player.getCombo() >= 100) {
				applyAttack(attack, player, 2);
			} else if (attack.skill == Aran.COMBO_TEMPEST && player.getCombo() >= 200) {
				applyAttack(attack, player, 4);
			}
		} else {
			IItem weapon = player.getInventory(InventoryType.EQUIPPED).getItem((byte) -11);
			WeaponType type = ItemInfoProvider.getInstance().getWeaponType(weapon.getItemId());
			if (type == WeaponType.NOT_A_WEAPON) {
				return;
			}
			int projectile = 0;
			byte bulletCount = 1;
			MapleStatEffect effect = null;
			if (attack.skill != 0) {
				effect = attack.getAttackEffect(player, null);
				bulletCount = effect.getBulletCount();
				if (effect.getCooldown() > 0) {
					c.announce(PacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
				}
			}
			boolean hasShadowPartner = player.getBuffedValue(BuffStat.SHADOWPARTNER) != null;
			if (hasShadowPartner) {
				bulletCount *= 2;
			}
			Inventory inv = player.getInventory(InventoryType.USE);
			for (byte i = 0; i < inv.getSlotLimit(); i++) {
				IItem item = inv.getItem(i);
				if (item != null) {
					int id = item.getItemId();
					boolean bow = ItemConstants.isArrowForBow(id);
					boolean cbow = ItemConstants.isArrowForCrossBow(id);
					if (item.getQuantity() > (bulletCount == 1 ? 0 : bulletCount)) {
						if (type == WeaponType.CLAW && ItemConstants.isThrowingStar(id) && weapon.getItemId() != 1472063) {
							if (((id == 2070007 || id == 2070018) && player.getLevel() < 70) || (id == 2070016 && player.getLevel() < 50)) {
							} else {
								projectile = id;
								break;
							}
						} else if ((type == WeaponType.GUN && ItemConstants.isBullet(id))) {
							if (id == 2331000 && id == 2332000) {
								if (player.getLevel() > 69) {
									projectile = id;
									break;
								}
							} else if (player.getLevel() > (id % 10) * 20 + 9) {
								projectile = id;
								break;
							}
						} else if ((type == WeaponType.BOW && bow) || (type == WeaponType.CROSSBOW && cbow) || (weapon.getItemId() == 1472063 && (bow || cbow))) {
							projectile = id;
							break;
						}
					}
				}
			}
			boolean soulArrow = player.getBuffedValue(BuffStat.SOULARROW) != null;
			boolean shadowClaw = player.getBuffedValue(BuffStat.SHADOW_CLAW) != null;
			if (!soulArrow && !shadowClaw && attack.skill != 11101004 && attack.skill != 15111007 && attack.skill != 14101006) {
				byte bulletConsume = bulletCount;
				if (effect != null && effect.getBulletConsume() != 0) {
					bulletConsume = (byte) (effect.getBulletConsume() * (hasShadowPartner ? 2 : 1));
				}
				if (!ServerConstants.UNLIMITED_PROJECTILES) {
					InventoryManipulator.removeById(c, InventoryType.USE, projectile, bulletConsume, false, true);
				}
			}

			if (projectile != 0 || soulArrow || attack.skill == 11101004 || attack.skill == 15111007 || attack.skill == 14101006) {
				int visProjectile = projectile; // visible projectile sent to
												// players
				if (ItemConstants.isThrowingStar(projectile)) {
					Inventory cash = player.getInventory(InventoryType.CASH);
					for (int i = 0; i < 96; i++) { // impose order...
						IItem item = cash.getItem((byte) i);
						if (item != null) {
							if (item.getItemId() / 1000 == 5021) {
								visProjectile = item.getItemId();
								break;
							}
						}
					}
				} else // bow, crossbow
				if (soulArrow || attack.skill == 3111004 || attack.skill == 3211004 || attack.skill == 11101004 || attack.skill == 15111007 || attack.skill == 14101006) {
					visProjectile = 0;
				}
				GamePacket packet;
				switch (attack.skill) {
					case 3121004: // Hurricane
					case 3221001: // Pierce
					case 5221004: // Rapid Fire
					case 13111002: // KoC Hurricane
						packet = PacketCreator.rangedAttack(player, attack.skill, attack.skilllevel, attack.rangedirection, attack.numAttackedAndDamage, visProjectile, attack.allDamage, attack.speed, attack.direction, attack.display);
						break;
					default:
						packet = PacketCreator.rangedAttack(player, attack.skill, attack.skilllevel, attack.stance, attack.numAttackedAndDamage, visProjectile, attack.allDamage, attack.speed, attack.direction, attack.display);
						break;
				}
				player.getMap().broadcastMessage(player, packet, false, true);
				if (effect != null) {
					int money = effect.getMoneyCon();
					if (money != 0) {
						int moneyMod = money / 2;
						money += Randomizer.nextInt(moneyMod);
						if (money > player.getMeso()) {
							money = player.getMeso();
						}
						player.gainMeso(-money, false);
					}
				}
				if (attack.skill != 0) {
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
				}
				if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 || player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0) && player.getBuffedValue(BuffStat.DARKSIGHT) != null && attack.numAttacked > 0 && player.getBuffSource(BuffStat.DARKSIGHT) != 9101004) {
					player.cancelEffectFromBuffStat(BuffStat.DARKSIGHT);
					player.cancelBuffStats(BuffStat.DARKSIGHT);
				}
				applyAttack(attack, player, bulletCount);
			}
		}
	}
}