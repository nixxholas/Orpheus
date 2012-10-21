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

import client.MapleClient;
import constants.skills.Bishop;
import constants.skills.Bowmaster;
import constants.skills.Brawler;
import constants.skills.ChiefBandit;
import constants.skills.Corsair;
import constants.skills.DarkKnight;
import constants.skills.FPArchMage;
import constants.skills.FPMage;
import constants.skills.Gunslinger;
import constants.skills.Hero;
import constants.skills.ILArchMage;
import constants.skills.Marksman;
import constants.skills.NightWalker;
import constants.skills.Paladin;
import constants.skills.ThunderBreaker;
import constants.skills.WindArcher;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SkillEffectHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		int skillId = slea.readInt();
		int level = slea.readByte();
		byte flags = slea.readByte();
		int speed = slea.readByte();
		byte aids = slea.readByte();// Mmmk
		switch (skillId) {
			case FPMage.EXPLOSION:
			case FPArchMage.BIG_BANG:
			case ILArchMage.BIG_BANG:
			case Bishop.BIG_BANG:
			case Bowmaster.HURRICANE:
			case Marksman.PIERCING_ARROW:
			case ChiefBandit.CHAKRA:
			case Brawler.CORKSCREW_BLOW:
			case Gunslinger.GRENADE:
			case Corsair.RAPID_FIRE:
			case WindArcher.HURRICANE:
			case NightWalker.POISON_BOMB:
			case ThunderBreaker.CORKSCREW_BLOW:
			case Paladin.MONSTER_MAGNET:
			case DarkKnight.MONSTER_MAGNET:
			case Hero.MONSTER_MAGNET:
				c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.skillEffect(c.getPlayer(), skillId, level, flags, speed, aids), false);
				return;
			default:
				Output.print(c.getPlayer() + " entered SkillEffectHandler without being handled using " + skillId + ".");
				return;
		}
	}
}