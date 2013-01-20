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

import client.GameCharacter;
import client.GameClient;
import client.autoban.AutobanType;
import client.autoban.AutobanManager;
import net.AbstractPacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public final class HealOvertimeHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter chr = c.getPlayer();
		AutobanManager abm = chr.getAutobanManager();
		abm.setTimestamp(0, reader.readInt());
		reader.skip(4);
		short healHP = reader.readShort();
		if (healHP != 0) {
			if ((abm.getLastSpam(0) + 1500) > System.currentTimeMillis()) {
				abm.addPoint(AutobanType.FAST_HP_HEALING, "Fast hp healing");
			}
			if (healHP > 140) {
				abm.autoban(AutobanType.HIGH_HP_HEALING, "Healing: " + healHP + "; Max is 140.");
				return;
			}
			chr.addHP(healHP);
			// chr.getMap().broadcastMessage(chr,
			// PacketCreator.showHpHealed(chr.getId(), healHP), false);
			chr.checkBerserk();
			abm.spam(0);
		}
		
		short healMP = reader.readShort();
		if (healMP != 0 && healMP < 1000) {
			if ((abm.getLastSpam(1) + 1500) > System.currentTimeMillis()) {
				abm.addPoint(AutobanType.FAST_MP_HEALING, "Fast mp healing");
			}
			chr.addMP(healMP);
			abm.spam(1);
		}
	}
}
