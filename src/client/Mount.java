/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss

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
package client;

import java.util.concurrent.ScheduledFuture;
import server.TimerManager;
import tools.PacketCreator;

/**
 * @author PurpleMadness Patrick :O
 */
public class Mount {

	private int itemId;
	private int skillId;
	private int fatigue;
	private int exp;
	private int level;
	private ScheduledFuture<?> fatigueSchedule;
	private GameCharacter owner;
	private boolean active;

	public Mount(GameCharacter owner, int itemId, int skillId) {
		this.itemId = itemId;
		this.skillId = skillId;
		this.fatigue = 0;
		this.level = 1;
		this.exp = 0;
		this.owner = owner;
		active = true;
	}

	public int getItemId() {
		return itemId;
	}

	public int getSkillId() {
		return skillId;
	}

	/**
	 * 1902000 - Hog 1902001 - Silver Mane 1902002 - Red Draco 1902005 - Mimiana
	 * 1902006 - Mimio 1902007 - Shinjou 1902008 - Frog 1902009 - Ostrich
	 * 1902010 - Frog 1902011 - Turtle 1902012 - Yeti
	 * 
	 * @return the id
	 */
	public int getId() {
		if (this.itemId < 1903000) {
			return itemId - 1901999;
		}
		return 5;
	}

	public int getFatigue() {
		return fatigue;
	}

	public int getExp() {
		return exp;
	}

	public int getLevel() {
		return level;
	}

	public void setFatigue(int value) {
		this.fatigue = value;
		if (fatigue < 0) {
			fatigue = 0;
		}
	}

	public void increaseFatigue() {
		this.fatigue++;
		owner.getMap().broadcastMessage(PacketCreator.updateMount(owner.getId(), this, false));
		if (fatigue > 99) {
			this.fatigue = 95;
			owner.dispelSkill(owner.getJobType() * 10000000 + 1004);
		}
	}

	public void setExp(int value) {
		this.exp = value;
	}

	public void setLevel(int value) {
		this.level = value;
	}

	public void setItemId(int value) {
		this.itemId = value;
	}

	public void startSchedule() {
		this.fatigueSchedule = TimerManager.getInstance().register(new IncreaseFatigueAction(), 60000, 60000);
	}

	public void cancelSchedule() {
		TimerManager.cancelSafely(this.fatigueSchedule, false);
	}

	public void setActive(boolean set) {
		this.active = set;
	}

	public boolean isActive() {
		return active;
	}

	public void empty() {
		cancelSchedule();
		this.fatigueSchedule = null;
		this.owner = null;
	}

	private final class IncreaseFatigueAction implements Runnable {
		@Override
		public void run() {
			increaseFatigue();
		}
	}
}
