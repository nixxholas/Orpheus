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
package client.status;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import client.skills.ISkill;
import server.life.MobSkill;
import tools.ArrayMap;

public class MonsterStatusEffect {

	private final Map<MonsterStatus, Integer> statuses;
	private final ISkill skill;
	private final MobSkill mobskill;
	private final boolean monsterSkill;
	
	private ScheduledFuture<?> cancelTask;
	private ScheduledFuture<?> damageSchedule;

	public MonsterStatusEffect(Map<MonsterStatus, Integer> stati, ISkill skillId, MobSkill mobskill, boolean monsterSkill) {
		this.statuses = new ArrayMap<MonsterStatus, Integer>(stati);
		this.skill = skillId;
		this.monsterSkill = monsterSkill;
		this.mobskill = mobskill;
	}

	public Map<MonsterStatus, Integer> getStatuses() {
		return statuses;
	}

	public Integer setValue(MonsterStatus status, Integer newVal) {
		return statuses.put(status, newVal);
	}

	public ISkill getSkill() {
		return skill;
	}

	public boolean isMonsterSkill() {
		return monsterSkill;
	}

	public final void cancelTask() {
		if (cancelTask != null) {
			cancelTask.cancel(false);
		}
		cancelTask = null;
	}

	public ScheduledFuture<?> getCancelTask() {
		return cancelTask;
	}

	public void setCancelTask(ScheduledFuture<?> cancelTask) {
		this.cancelTask = cancelTask;
	}

	public void removeActiveStatus(MonsterStatus stat) {
		statuses.remove(stat);
	}

	public void setDamageSchedule(ScheduledFuture<?> damageSchedule) {
		this.damageSchedule = damageSchedule;
	}

	public void cancelDamageSchedule() {
		if (damageSchedule != null) {
			damageSchedule.cancel(false);
		}
	}

	public MobSkill getMobSkill() {
		return mobskill;
	}
}
