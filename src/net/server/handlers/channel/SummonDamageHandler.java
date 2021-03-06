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

import java.util.ArrayList;
import java.util.List;
import client.GameCharacter;
import client.GameClient;
import client.skills.ISkill;
import client.skills.SkillFactory;
import client.status.MonsterStatusEffect;
import net.AbstractPacketHandler;
import server.StatEffect;
import server.life.Monster;
import server.maps.Summon;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class SummonDamageHandler extends AbstractPacketHandler {
	public final class SummonAttackEntry {

		private int monsterOid;
		private int damage;

		public SummonAttackEntry(int monsterOid, int damage) {
			this.monsterOid = monsterOid;
			this.damage = damage;
		}

		public int getMonsterOid() {
			return monsterOid;
		}

		public int getDamage() {
			return damage;
		}
	}

	public void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		int oid = reader.readInt();
		GameCharacter player = c.getPlayer();
		if (!player.isAlive()) {
			return;
		}
		Summon summon = null;
		for (Summon sum : player.getSummons().values()) {
			if (sum.getObjectId() == oid) {
				summon = sum;
			}
		}
		if (summon == null) {
			return;
		}
		ISkill summonSkill = SkillFactory.getSkill(summon.getSkill());
		StatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
		reader.skip(4);
		List<SummonAttackEntry> allDamage = new ArrayList<SummonAttackEntry>();
		byte direction = reader.readByte();
		int numAttacked = reader.readByte();
		reader.skip(8); // Thanks Gerald :D, I failed lol (mob x,y and summon x,y)
		for (int x = 0; x < numAttacked; x++) {
			int monsterOid = reader.readInt(); // attacked oid
			reader.skip(18);
			int damage = reader.readInt();
			allDamage.add(new SummonAttackEntry(monsterOid, damage));
		}
		player.getMap().broadcastMessage(player, PacketCreator.summonAttack(player.getId(), summon.getSkill(), direction, allDamage), summon.getPosition());
		for (SummonAttackEntry attackEntry : allDamage) {
			int damage = attackEntry.getDamage();
			Monster target = player.getMap().getMonsterByOid(attackEntry.getMonsterOid());
			if (target != null) {
				if (damage > 0 && summonEffect.getMonsterStati().size() > 0) {
					if (summonEffect.makeChanceResult()) {
						target.applyStatus(player, new MonsterStatusEffect(summonEffect.getMonsterStati(), summonSkill, null, false), summonEffect.isPoison(), 4000);
					}
				}
				player.getMap().damageMonster(player, target, damage);
			}
		}
	}
}