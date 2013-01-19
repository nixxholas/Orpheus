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
package server.quest;

/**
 * 
 * @author Matze
 */
public enum QuestActionType {
	UNDEFINED(-1), EXP(0), ITEM(1), NEXTQUEST(2), MESO(3), QUEST(4), SKILL(5), FAME(6), BUFF(7), PETSKILL(8), YES(9), NO(10), NPC(11), MIN_LEVEL(12), NORMAL_AUTO_START(13), ZERO(14);
	final byte type;

	private QuestActionType(int type) {
		this.type = (byte) type;
	}

	public static QuestActionType getByWZName(String name) {
		if (name.equals("exp")) {
			return EXP;
		} else if (name.equals("money")) {
			return MESO;
		} else if (name.equals("item")) {
			return ITEM;
		} else if (name.equals("skill")) {
			return SKILL;
		} else if (name.equals("nextQuest")) {
			return NEXTQUEST;
		} else if (name.equals("pop")) {
			return FAME;
		} else if (name.equals("buffItemID")) {
			return BUFF;
		} else if (name.equals("petskill")) {
			return PETSKILL;
		} else if (name.equals("no")) {
			return NO;
		} else if (name.equals("yes")) {
			return YES;
		} else if (name.equals("npc")) {
			return NPC;
		} else if (name.equals("lvmin")) {
			return MIN_LEVEL;
		} else if (name.equals("normalAutoStart")) {
			return NORMAL_AUTO_START;
		} else if (name.equals("0")) {
			return ZERO;
		} else {
			return UNDEFINED;
		}
	}
}