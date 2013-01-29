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

import client.Equip;
import client.IItem;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import client.Item;
import client.BuffStat;
import client.GameCharacter;
import client.InventoryType;
import client.Job;
import client.Stat;
import client.skills.ISkill;
import client.skills.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ItemConstants;
import constants.skills.Assassin;
import constants.skills.Bandit;
import constants.skills.Bishop;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.ChiefBandit;
import constants.skills.Cleric;
import constants.skills.Corsair;
import constants.skills.FPArchMage;
import constants.skills.Gunslinger;
import constants.skills.ILArchMage;
import constants.skills.Marauder;
import constants.skills.Marksman;
import constants.skills.NightWalker;
import constants.skills.Outlaw;
import constants.skills.Paladin;
import constants.skills.Rogue;
import constants.skills.Shadower;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import server.life.MonsterDropEntry;
import tools.Randomizer;
import net.AbstractPacketHandler;
import client.autoban.AutobanType;
import constants.skills.Aran;
import constants.skills.Crossbowman;
import constants.skills.DawnWarrior;
import constants.skills.Fighter;
import constants.skills.Hunter;
import constants.skills.Page;
import constants.skills.Spearman;
import java.util.HashMap;

import server.AttackInfo;
import server.BuffStatDelta;
import server.ItemInfoProvider;
import server.StatEffect;
import server.TimerManager;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.Monster;
import server.life.MonsterInfoProvider;
import server.maps.GameMap;
import server.maps.GameMapItem;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import tools.PacketCreator;
import tools.data.input.LittleEndianAccessor;

public abstract class AbstractDealDamageHandler extends AbstractPacketHandler {

    protected synchronized void applyAttack(AttackInfo attack, final GameCharacter player, int attackCount) {
        ISkill theSkill = null;
        StatEffect attackEffect = null;
        try {
            if (player.isBanned()) {
                return;
            }

            if (attack.skill.id != 0) {
                theSkill = SkillFactory.getSkill(attack.skill.id);
                attackEffect = attack.getAttackEffect(player, theSkill);
                if (attackEffect == null) {
                    player.getClient().announce(PacketCreator.enableActions());
                    return;
                }

                if (player.getMp() < attackEffect.getMpCon()) {
                	player.getAutobanManager().addPoint(AutobanType.MPCON, "Skill: " + attack.skill.id + "; Player MP: " + player.getMp() + "; MP Needed: " + attackEffect.getMpCon());
                }

                if (attack.skill.id != Cleric.HEAL) {
                    if (player.isAlive()) {
                        attackEffect.applyTo(player);
                    } else {
                        player.getClient().announce(PacketCreator.enableActions());
                    }
                }
                int mobCount = attackEffect.getMobCount();
                if (attack.skill.id == DawnWarrior.FINAL_ATTACK || attack.skill.id == Page.FINAL_ATTACK_BW || attack.skill.id == Page.FINAL_ATTACK_SWORD || attack.skill.id == Fighter.FINAL_ATTACK_SWORD
                        || attack.skill.id == Fighter.FINAL_ATTACK_AXE || attack.skill.id == Spearman.FINAL_ATTACK_SPEAR || attack.skill.id == Spearman.FINAL_ATTACK_POLEARM || attack.skill.id == WindArcher.FINAL_ATTACK
                        || attack.skill.id == DawnWarrior.FINAL_ATTACK || attack.skill.id == Hunter.FINAL_ATTACK || attack.skill.id == Crossbowman.FINAL_ATTACK) {
                    mobCount = 15;//:(
                }
                if (attack.numAttacked > mobCount) {
                	String message = "Skill: " + attack.skill.id + "; Count: " + attack.numAttacked + " Max: " + attackEffect.getMobCount();
                	player.getAutobanManager().autoban(AutobanType.MOB_COUNT, message);
                    return;
                }
            }
            if (!player.isAlive()) {
                return;
            }

            // WTF IS THIS F3,1
//			if (attackCount != attack.numDamage && attack.skill != ChiefBandit.MESO_EXPLOSION && attack.skill != NightWalker.VAMPIRE && attack.skill != WindArcher.WIND_SHOT && attack.skill != Aran.COMBO_SMASH && attack.skill != Aran.COMBO_PENRIL && attack.skill != Aran.COMBO_TEMPEST && attack.skill != NightLord.NINJA_AMBUSH && attack.skill != Shadower.NINJA_AMBUSH) {
//			    return;
//			}
            
            int totDamage = 0;
            final GameMap map = player.getMap();

            if (attack.skill.id == ChiefBandit.MESO_EXPLOSION) {
                int delay = 0;
                for (Integer oned : attack.allDamage.keySet()) {
                    GameMapObject mapobject = map.getMapObject(oned.intValue());
                    if (mapobject != null && mapobject.getType() == GameMapObjectType.ITEM) {
                        final GameMapItem mapitem = (GameMapItem) mapobject;
                        if (mapitem.getMeso() > 9) {
                            synchronized (mapitem) {
                                if (mapitem.isPickedUp()) {
                                    return;
                                }
                                TimerManager.getInstance().schedule(new Runnable() {

                                    @Override
                                    public void run() {
                                        map.removeMapObject(mapitem);
                                        map.broadcastMessage(PacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
                                        mapitem.setPickedUp(true);
                                    }
                                }, delay);
                                delay += 100;
                            }
                        } else if (mapitem.getMeso() == 0) {
                            return;
                        }
                    } else if (mapobject != null && mapobject.getType() != GameMapObjectType.MONSTER) {
                        return;
                    }
                }
            }
            for (Integer oned : attack.allDamage.keySet()) {
                final Monster monster = map.getMonsterByOid(oned.intValue());
                if (monster != null) {
                    int totDamageToOneMonster = 0;
                    List<Integer> onedList = attack.allDamage.get(oned);
                    for (Integer eachd : onedList) {
                        totDamageToOneMonster += eachd.intValue();
                    }
                    totDamage += totDamageToOneMonster;
                    player.checkMonsterAggro(monster);
                    if (player.getBuffedValue(BuffStat.PICKPOCKET) != null && (attack.skill.id == 0 || attack.skill.id == Rogue.DOUBLE_STAB || attack.skill.id == Bandit.SAVAGE_BLOW || attack.skill.id == ChiefBandit.ASSAULTER || attack.skill.id == ChiefBandit.BAND_OF_THIEVES || attack.skill.id == Shadower.ASSASSINATE || attack.skill.id == Shadower.TAUNT || attack.skill.id == Shadower.BOOMERANG_STEP)) {
                        ISkill pickpocket = SkillFactory.getSkill(ChiefBandit.PICKPOCKET);
                        int delay = 0;
                        final int maxmeso = player.getBuffedValue(BuffStat.PICKPOCKET).intValue();
                        for (final Integer eachd : onedList) {
                            if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                                TimerManager.getInstance().schedule(new Runnable() {

                                    @Override
                                    public void run() {
                                        player.getMap().spawnMesoDrop(Math.min((int) Math.max(((double) eachd / (double) 20000) * (double) maxmeso, (double) 1), maxmeso), new Point((int) (monster.getPosition().getX() + Randomizer.nextInt(100) - 50), (int) (monster.getPosition().getY())), monster, player, true, (byte) 0);
                                    }
                                }, delay);
                                delay += 100;
                            }
                        }
                    } else if (attack.skill.id == Marksman.SNIPE) {
                        totDamageToOneMonster = 195000 + Randomizer.nextInt(5000);
                    } else if (attack.skill.id == Marauder.ENERGY_DRAIN || attack.skill.id == ThunderBreaker.ENERGY_DRAIN || attack.skill.id == NightWalker.VAMPIRE || attack.skill.id == Assassin.DRAIN) {
                        player.addHP(Math.min(monster.getMaxHp(), Math.min((int) ((double) totDamage * (double) SkillFactory.getSkill(attack.skill.id).getEffect(player.getSkillLevel(SkillFactory.getSkill(attack.skill.id))).getX() / 100.0), player.getMaxHp() / 2)));
                    } else if (attack.skill.id == Bandit.STEAL) {
                        ISkill steal = SkillFactory.getSkill(Bandit.STEAL);
                        if (Math.random() < 0.3 && steal.getEffect(player.getSkillLevel(steal)).makeChanceResult()) { //Else it drops too many cool stuff :(
                            List<MonsterDropEntry> toSteals = MonsterInfoProvider.getInstance().retrieveDrop(monster.getId());
                            Collections.shuffle(toSteals);
                            int toSteal = toSteals.get(rand(0, (toSteals.size() - 1))).itemId;
                            ItemInfoProvider ii = ItemInfoProvider.getInstance();
                            IItem item = null;
                            if (ItemConstants.getInventoryType(toSteal).equals(InventoryType.EQUIP)) {
                                item = ii.randomizeStats((Equip) ii.getEquipById(toSteal));
                            } else {
                                item = new Item(toSteal, (byte) 0, (short) 1);
                            }
                            player.getMap().spawnItemDrop(monster, player, item, monster.getPosition(), false, false);
                            monster.addStolen(toSteal);
                        }
                    } else if (attack.skill.id == FPArchMage.FIRE_DEMON) {
                        monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(FPArchMage.FIRE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(FPArchMage.FIRE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill.id == ILArchMage.ICE_DEMON) {
                        monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(ILArchMage.ICE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(ILArchMage.ICE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill.id == Outlaw.HOMING_BEACON || attack.skill.id == Corsair.BULLSEYE) {
                        player.getMarkedMonsterState().setMonster(monster.getObjectId());
                        player.announce(PacketCreator.giveBuff(1, attack.skill.id, Collections.singletonList(new BuffStatDelta(BuffStat.HOMING_BEACON, monster.getObjectId()))));
                    }
                    if (player.getBuffedValue(BuffStat.HAMSTRING) != null) {
                        ISkill hamstring = SkillFactory.getSkill(Bowmaster.HAMSTRING);
                        if (hamstring.getEffect(player.getSkillLevel(hamstring)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, hamstring.getEffect(player.getSkillLevel(hamstring)).getX()), hamstring, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, hamstring.getEffect(player.getSkillLevel(hamstring)).getY() * 1000);
                        }
                    }
                    if (player.getBuffedValue(BuffStat.BLIND) != null) {
                        ISkill blind = SkillFactory.getSkill(Marksman.BLIND);
                        if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, blind.getEffect(player.getSkillLevel(blind)).getX()), blind, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, blind.getEffect(player.getSkillLevel(blind)).getY() * 1000);
                        }
                    }
                    final int id = player.getJob().getId();
                    if (id == 121 || id == 122) {
                        for (int charge = 1211005; charge < 1211007; charge++) {
                            ISkill chargeSkill = SkillFactory.getSkill(charge);
                            if (player.isBuffFrom(BuffStat.WK_CHARGE, chargeSkill)) {
                                final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                                if (totDamageToOneMonster > 0 && iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
                                    monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), chargeSkill, null, false), false, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 2000);
                                }
                                break;
                            }
                        }
                    } else if (player.getBuffedValue(BuffStat.BODY_PRESSURE) != null || player.getBuffedValue(BuffStat.COMBO_DRAIN) != null) {
                        ISkill skill;
                        if (player.getBuffedValue(BuffStat.BODY_PRESSURE) != null) {
                            skill = SkillFactory.getSkill(21101003);
                            final StatEffect eff = skill.getEffect(player.getSkillLevel(skill));

                            if (eff.makeChanceResult()) {
                                monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.NEUTRALISE, 1), skill, null, false), false, eff.getX() * 1000, false);
                            }
                        }
                        if (player.getBuffedValue(BuffStat.COMBO_DRAIN) != null) {
                            skill = SkillFactory.getSkill(21100005);
                            player.setHp(player.getHp() + ((totDamage * skill.getEffect(player.getSkillLevel(skill)).getX()) / 100), true);
                            player.updateSingleStat(Stat.HP, player.getHp());
                        }
                    } else if (id == 412 || id == 422 || id == 1411) {
                        ISkill type = SkillFactory.getSkill(player.getJob().getId() == 412 ? 4120005 : (player.getJob().getId() == 1411 ? 14110004 : 4220005));
                        if (player.getSkillLevel(type) > 0) {
                            StatEffect venomEffect = type.getEffect(player.getSkillLevel(type));
                            for (int i = 0; i < attackCount; i++) {
                                if (venomEffect.makeChanceResult()) {
                                    if (monster.getVenomMulti() < 3) {
                                        monster.setVenomMulti((monster.getVenomMulti() + 1));
                                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                        monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                                    }
                                }
                            }
                        }
                    }
                    if (attack.skill.id != 0) {
                        if (attackEffect.getFixDamage() != -1) {
                            if (totDamageToOneMonster != attackEffect.getFixDamage() && totDamageToOneMonster != 0) {
                            	player.getAutobanManager().autoban(AutobanType.FIX_DAMAGE, String.valueOf(totDamageToOneMonster) + " damage");                                
                            }
                        }
                    }
                    if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                        if (attackEffect.makeChanceResult()) {
                            monster.applyStatus(player, new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, null, false), attackEffect.isPoison(), attackEffect.getDuration());
                        }
                    }
                    if (player.getJob().equals(Job.LEGEND) || player.getJob().isA(Job.ARAN4)) {
                        byte comboLevel = (byte) (player.getJob().equals(Job.LEGEND) ? 10 : player.getSkillLevel(Aran.COMBO_ABILITY));
                        if (comboLevel > 0) {
                            final long currentTime = System.currentTimeMillis();
                            short combo = 0;
                            if (attack.skill.id == Aran.COMBO_SMASH || attack.skill.id == Aran.COMBO_PENRIL || attack.skill.id == Aran.COMBO_TEMPEST) {
                            	 // WHY NOT USE COMBO LOL
                            	player.setCombo(combo);
                            }
                            
                            // TODO: See this amount thing? Yeah? It's not used.
                            for (Integer amount : onedList) {
                                combo = player.getCombo();
                                if ((currentTime - player.getLastCombo()) > 3000 && combo > 0) {
                                    combo = 0;
                                    player.cancelEffectFromBuffStat(BuffStat.ARAN_COMBO);
                                }
                                combo++;
                                switch (combo) {
                                    case 10:
                                    case 20:
                                    case 30:
                                    case 40:
                                    case 50:
                                    case 60:
                                    case 70:
                                    case 80:
                                    case 90:
                                    case 100:
                                        if ((combo / 10) <= comboLevel) {
                                            SkillFactory.getSkill(21000000).getEffect(combo / 10).applyComboBuff(player, combo);
                                        }
                                        break;
                                }
                                player.setCombo(combo);
                            }
                            player.setLastCombo(currentTime);
                        }
                    }
                    if (attack.isHH && !monster.isBoss()) {
                        map.damageMonster(player, monster, monster.getHp() - 1);
                    } else if (attack.isHH) {
                        int HHDmg = (player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillFactory.getSkill(Paladin.HEAVENS_HAMMER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Paladin.HEAVENS_HAMMER))).getDamage() / 100));
                        map.damageMonster(player, monster, (int) (Math.floor(Math.random() * (HHDmg / 5) + HHDmg * .8)));
                    } else {
                        map.damageMonster(player, monster, totDamageToOneMonster);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected AttackInfo parseDamage(LittleEndianAccessor reader, GameCharacter player, boolean isRanged) {
        AttackInfo info = new AttackInfo();
        reader.readByte();
        info.numAttackedAndDamage = reader.readByte();
        info.numAttacked = (info.numAttackedAndDamage >>> 4) & 0xF;
        info.numDamage = info.numAttackedAndDamage & 0xF;
        info.allDamage = new HashMap<Integer, List<Integer>>();
        info.skill.id = reader.readInt();
        if (info.skill.id > 0) {
            info.skill.level = player.getSkillLevel(info.skill.id);
        }
        if (info.skill.id == FPArchMage.BIG_BANG || info.skill.id == ILArchMage.BIG_BANG || info.skill.id == Bishop.BIG_BANG || info.skill.id == Gunslinger.GRENADE || info.skill.id == Brawler.CORKSCREW_BLOW || info.skill.id == ThunderBreaker.CORKSCREW_BLOW || info.skill.id == NightWalker.POISON_BOMB) {
            info.charge = reader.readInt();
        } else {
            info.charge = 0;
        }
        if (info.skill.id == Paladin.HEAVENS_HAMMER) {
            info.isHH = true;
        }
        reader.skip(8);
        info.display = reader.readByte();
        info.direction = reader.readByte();
        info.stance = reader.readByte();
        if (info.skill.id == ChiefBandit.MESO_EXPLOSION) {
            if (info.numAttackedAndDamage == 0) {
                reader.skip(10);
                int bullets = reader.readByte();
                for (int j = 0; j < bullets; j++) {
                    int mesoid = reader.readInt();
                    reader.skip(1);
                    info.allDamage.put(Integer.valueOf(mesoid), null);
                }
                return info;
            } else {
                reader.skip(6);
            }
            for (int i = 0; i < info.numAttacked + 1; i++) {
                int oid = reader.readInt();
                if (i < info.numAttacked) {
                    reader.skip(12);
                    int bullets = reader.readByte();
                    List<Integer> allDamageNumbers = new ArrayList<Integer>();
                    for (int j = 0; j < bullets; j++) {
                        int damage = reader.readInt();
                        allDamageNumbers.add(Integer.valueOf(damage));
                    }
                    info.allDamage.put(Integer.valueOf(oid), allDamageNumbers);
                    reader.skip(4);
                } else {
                    int bullets = reader.readByte();
                    for (int j = 0; j < bullets; j++) {
                        int mesoid = reader.readInt();
                        reader.skip(1);
                        info.allDamage.put(Integer.valueOf(mesoid), null);
                    }
                }
            }
            return info;
        }
        if (isRanged) {
            reader.readByte();
            info.speed = reader.readByte();
            reader.readByte();
            info.rangedirection = reader.readByte();
            reader.skip(7);
            if (info.skill.id == Bowmaster.HURRICANE || info.skill.id == Marksman.PIERCING_ARROW || info.skill.id == Corsair.RAPID_FIRE || info.skill.id == WindArcher.HURRICANE) {
                reader.skip(4);
            }
        } else {
            reader.readByte();
            info.speed = reader.readByte();
            reader.skip(4);
        }
        for (int i = 0; i < info.numAttacked; i++) {
            int oid = reader.readInt();
            reader.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<Integer>();
            for (int j = 0; j < info.numDamage; j++) {
                int damage = reader.readInt();
                if (info.skill.id == Marksman.SNIPE) {
                    damage += 0x80000000; //Critical
                }
                allDamageNumbers.add(Integer.valueOf(damage));
            }
            if (info.skill.id != 5221004) {
                reader.skip(4);
            }
            info.allDamage.put(Integer.valueOf(oid), allDamageNumbers);
        }
        return info;
    }

    private static int rand(int l, int u) {
        return (int) ((Math.random() * (u - l + 1)) + l);
    }
}
