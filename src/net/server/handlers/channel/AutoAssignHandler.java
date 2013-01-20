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
import client.Stat;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Generic
 */
public class AutoAssignHandler extends AbstractPacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter chr = c.getPlayer();
		reader.skip(8);
		if (chr.getRemainingAp() < 1) {
			return;
		}
		int total = 0;
		int extras = 0;
		for (int i = 0; i < 2; i++) {
			int type = reader.readInt();
			int tempVal = reader.readInt();
			if (tempVal < 0 || tempVal > c.getPlayer().getRemainingAp()) {
				return;
			}
			total += tempVal;
			extras += gainStatByType(chr, Stat.getBy5ByteEncoding(type), tempVal);
		}
		int remainingAp = (chr.getRemainingAp() - total) + extras;
		chr.setRemainingAp(remainingAp);
		chr.updateSingleStat(Stat.AVAILABLEAP, remainingAp);
		c.announce(PacketCreator.enableActions());
	}

	private int gainStatByType(GameCharacter player, Stat type, int gain) {
		int newVal = 0;
		if (type.equals(Stat.STR)) {
			newVal = player.getStr() + gain;
			if (newVal > 999) {
				player.setStr(999);
			} else {
				player.setStr(newVal);
			}
		} else if (type.equals(Stat.INT)) {
			newVal = player.getInt() + gain;
			if (newVal > 999) {
				player.setInt(999);
			} else {
				player.setInt(newVal);
			}
		} else if (type.equals(Stat.LUK)) {
			newVal = player.getLuk() + gain;
			if (newVal > 999) {
				player.setLuk(999);
			} else {
				player.setLuk(newVal);
			}
		} else if (type.equals(Stat.DEX)) {
			newVal = player.getDex() + gain;
			if (newVal > 999) {
				player.setDex(999);
			} else {
				player.setDex(newVal);
			}
		}
		if (newVal > 999) {
			player.updateSingleStat(type, 999);
			return newVal - 999;
		}
		player.updateSingleStat(type, newVal);
		return 0;
	}
}
