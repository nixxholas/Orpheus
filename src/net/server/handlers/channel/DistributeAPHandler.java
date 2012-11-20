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
import client.Job;
import client.Stat;
import net.AbstractPacketHandler;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DistributeAPHandler extends AbstractPacketHandler {
	private static final int max = Short.MAX_VALUE;

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		slea.readInt();
		int num = slea.readInt();
		if (c.getPlayer().getRemainingAp() > 0) {
			if (addStat(c, num)) {
				c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
				c.getPlayer().updateSingleStat(Stat.AVAILABLEAP, c.getPlayer().getRemainingAp());
			}
		}
		c.announce(PacketCreator.enableActions());
	}

	static boolean addStat(GameClient c, int id) {
		switch (id) {
			case 64: // Str
				if (c.getPlayer().getStr() >= max) {
					return false;
				}
				c.getPlayer().addStat(1, 1);
				break;
			case 128: // Dex
				if (c.getPlayer().getDex() >= max) {
					return false;
				}
				c.getPlayer().addStat(2, 1);
				break;
			case 256: // Int
				if (c.getPlayer().getInt() >= max) {
					return false;
				}
				c.getPlayer().addStat(3, 1);
				break;
			case 512: // Luk
				if (c.getPlayer().getLuk() >= max) {
					return false;
				}
				c.getPlayer().addStat(4, 1);
				break;
			case 2048: // HP
				addHP(c.getPlayer(), addHP(c));
				break;
			case 8192: // MP
				addMP(c.getPlayer(), addMP(c));
				break;
			default:
				c.announce(PacketCreator.updatePlayerStats(PacketCreator.EMPTY_STATUPDATE, true));
				return false;
		}
		return true;
	}

	static int addHP(GameClient c) {
		GameCharacter player = c.getPlayer();
		Job job = player.getJob();
		int MaxHP = player.getMaxHp();
		if (player.getHpMpApUsed() > 9999 || MaxHP >= 30000) {
			return MaxHP;
		}
		if (job.isA(Job.WARRIOR) || job.isA(Job.DAWNWARRIOR1) || job.isA(Job.ARAN1)) {
			MaxHP += 20;
		} else if (job.isA(Job.MAGICIAN) || job.isA(Job.BLAZEWIZARD1)) {
			MaxHP += 6;
		} else if (job.isA(Job.BOWMAN) || job.isA(Job.WINDARCHER1) || job.isA(Job.THIEF) || job.isA(Job.NIGHTWALKER1)) {
			MaxHP += 16;
		} else if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
			MaxHP += 18;
		} else {
			MaxHP += 8;
		}
		return MaxHP;
	}

	static int addMP(GameClient c) {
		GameCharacter player = c.getPlayer();
		int MaxMP = player.getMaxMp();
		Job job = player.getJob();
		if (player.getHpMpApUsed() > 9999 || player.getMaxMp() >= 30000) {
			return MaxMP;
		}
		if (job.isA(Job.WARRIOR) || job.isA(Job.DAWNWARRIOR1) || job.isA(Job.ARAN1)) {
			MaxMP += 2;
		} else if (job.isA(Job.MAGICIAN) || job.isA(Job.BLAZEWIZARD1)) {
			MaxMP += 18;
		} else if (job.isA(Job.BOWMAN) || job.isA(Job.WINDARCHER1) || job.isA(Job.THIEF) || job.isA(Job.NIGHTWALKER1)) {
			MaxMP += 10;
		} else if (job.isA(Job.PIRATE) || job.isA(Job.THUNDERBREAKER1)) {
			MaxMP += 14;
		} else {
			MaxMP += 6;
		}
		return MaxMP;
	}

	static void addHP(GameCharacter player, int MaxHP) {
		MaxHP = Math.min(30000, MaxHP);
		player.setHpMpApUsed(player.getHpMpApUsed() + 1);
		player.setMaxHp(MaxHP);
		player.updateSingleStat(Stat.MAXHP, MaxHP);
	}

	static void addMP(GameCharacter player, int MaxMP) {
		MaxMP = Math.min(30000, MaxMP);
		player.setHpMpApUsed(player.getHpMpApUsed() + 1);
		player.setMaxMp(MaxMP);
		player.updateSingleStat(Stat.MAXMP, MaxMP);
	}
}
