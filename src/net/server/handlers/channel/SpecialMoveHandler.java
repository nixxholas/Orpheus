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
import java.util.concurrent.ScheduledFuture;
import client.ISkill;
import client.GameCharacter;
import client.GameCharacter.CancelCooldownAction;
import client.GameClient;
import client.Stat;
import client.SkillFactory;
import constants.skills.Brawler;
import constants.skills.Buccaneer;
import constants.skills.Corsair;
import constants.skills.DarkKnight;
import constants.skills.Hero;
import constants.skills.Paladin;
import constants.skills.Priest;
import net.AbstractPacketHandler;
import net.server.Channel;
import net.server.MapleParty;
import net.server.MaplePartyCharacter;
import net.server.Server;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.MapleMonster;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SpecialMoveHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		chr.getAutobanManager().setTimestamp(4, slea.readInt());
		int skillid = slea.readInt();
		Point pos = null;
		int __skillLevel = slea.readByte();
		ISkill skill = SkillFactory.getSkill(skillid);
		int skillLevel = chr.getSkillLevel(skill);
		if (skillid % 10000000 == 1010 || skillid % 10000000 == 1011) {
			skillLevel = 1;
			chr.setDojoEnergy(0);
			c.announce(PacketCreator.getEnergy("energy", 0));
		}
		if (skillLevel == 0 || skillLevel != __skillLevel)
			return;

		MapleStatEffect effect = skill.getEffect(skillLevel);
		if (effect.getCooldown() > 0) {
			if (chr.skillisCooling(skillid)) {
				return;
			} else if (skillid != Corsair.BATTLE_SHIP) {
				c.announce(PacketCreator.skillCooldown(skillid, effect.getCooldown()));
				ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), skillid), effect.getCooldown() * 1000);
				chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000, timer);
			}
		}
		if (skillid == Hero.MONSTER_MAGNET || skillid == Paladin.MONSTER_MAGNET || skillid == DarkKnight.MONSTER_MAGNET) { // Monster
																															// Magnet
			int num = slea.readInt();
			int mobId;
			byte success;
			for (int i = 0; i < num; i++) {
				mobId = slea.readInt();
				success = slea.readByte();
				chr.getMap().broadcastMessage(c.getPlayer(), PacketCreator.showMagnet(mobId, success), false);
				MapleMonster monster = chr.getMap().getMonsterByOid(mobId);
				if (monster != null) {
					monster.switchController(c.getPlayer(), monster.isControllerHasAggro());
				}
			}
			byte direction = slea.readByte();
			chr.getMap().broadcastMessage(c.getPlayer(), PacketCreator.showBuffeffect(chr.getId(), skillid, chr.getSkillLevel(skillid), direction), false);
			c.announce(PacketCreator.enableActions());
			return;
		} else if (skillid == Buccaneer.TIME_LEAP) { // Timeleap
			MapleParty p = chr.getParty();
			if (p != null) {
				for (MaplePartyCharacter mpc : p.getMembers()) {
					for (Channel cserv : Server.getInstance().getChannelsFromWorld(c.getWorld())) {
						if (cserv.getPlayerStorage().getCharacterById(mpc.getId()) != null) {
							cserv.getPlayerStorage().getCharacterById(mpc.getId()).removeAllCooldownsExcept(5121010);
						}
					}
				}
			}
			chr.removeAllCooldownsExcept(Buccaneer.TIME_LEAP);
		} else if (skillid == Brawler.MP_RECOVERY) {// MP Recovery
			ISkill s = SkillFactory.getSkill(skillid);
			MapleStatEffect ef = s.getEffect(chr.getSkillLevel(s));
			int lose = chr.getMaxHp() / ef.getX();
			chr.setHp(chr.getHp() - lose);
			chr.updateSingleStat(Stat.HP, chr.getHp());
			int gain = lose * (ef.getY() / 100);
			chr.setMp(chr.getMp() + gain);
			chr.updateSingleStat(Stat.MP, chr.getMp());
		} else if (skillid % 10000000 == 1004) {
			slea.readShort();
		}
		if (slea.available() == 5) {
			pos = new Point(slea.readShort(), slea.readShort());
		}
		if (chr.isAlive()) {
			if (skill.getId() != Priest.MYSTIC_DOOR || chr.canDoor()) {
				skill.getEffect(skillLevel).applyTo(c.getPlayer(), pos);
			} else {
				chr.message("Please wait 5 seconds before casting Mystic Door again");
				c.announce(PacketCreator.enableActions());
			}
		} else {
			c.announce(PacketCreator.enableActions());
		}
	}
}