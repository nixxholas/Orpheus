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

import client.MapleBuffStat;
import client.GameCharacter;
import client.GameClient;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import server.maps.MapleSummon;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DamageSummonHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int skillid = slea.readInt(); // Bugged? might not be skillid.
		int unkByte = slea.readByte();
		int damage = slea.readInt();
		int monsterIdFrom = slea.readInt();
		if (SkillFactory.getSkill(skillid) != null) {
			GameCharacter player = c.getPlayer();
			MapleSummon summon = player.getSummons().get(skillid);
			if (summon != null) {
				summon.addHP(-damage);
				if (summon.getHP() <= 0) {
					player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
				}
			}
			player.getMap().broadcastMessage(player, MaplePacketCreator.damageSummon(player.getId(), skillid, damage, unkByte, monsterIdFrom), summon.getPosition());
		}
	}
}
