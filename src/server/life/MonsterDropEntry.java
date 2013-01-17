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
package server.life;

import tools.Randomizer;

/**
 * 
 * @author LightPepsi
 */

public final class MonsterDropEntry {
	
	public final short QuestId;
	public final int ItemId; 
	public final int Chance;

	private final int Minimum;
	private final int Maximum;

	public MonsterDropEntry(int itemId, int chance, int minimum, int maximum, short questId) {
		this.ItemId = itemId;
		this.Chance = chance;
		this.QuestId = questId;
		this.Minimum = minimum;
		this.Maximum = maximum;
	}
	
	public int getRandomQuantity() {
		if (this.Maximum == 1) {
			return 1;
		} else {
			return Randomizer.nextInt(this.Maximum - this.Minimum) + this.Minimum;
		}
	}
}