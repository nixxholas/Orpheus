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
package client.autoban;

import client.GameCharacter;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author kevintjuh93
 */
public class AutobanManager {
	private GameCharacter chr;
	private Map<AutobanType, Integer> points = new HashMap<AutobanType, Integer>();
	private Map<AutobanType, Long> lastTime = new HashMap<AutobanType, Long>();
	private int misses = 0;
	private int lastMisses = 0;
	private int sameMissCount = 0;
	private long spam[] = new long[20];
	private int timestamp[] = new int[20];
	private byte timestampCounter[] = new byte[20];

	public AutobanManager(GameCharacter chr) {
		this.chr = chr;
	}

	public void addPoint(AutobanType fac, String reason) {
		if (lastTime.containsKey(fac)) {
			if (lastTime.get(fac) < (System.currentTimeMillis() - fac.getExpiration())) {
				// Divide by 2 so the points are not completely gone.
				points.put(fac, points.get(fac) / 2); 
			}
		}
		if (fac.getExpiration() != -1){
			lastTime.put(fac, System.currentTimeMillis());
		}

		if (points.containsKey(fac)) {
			points.put(fac, points.get(fac) + 1);
		} else {
			points.put(fac, 1);
		}

		if (points.get(fac) >= fac.getMaximum()) {
			chr.autoban("Autobanned for " + fac.name() + " ;" + reason, 1);
			chr.sendPolice("You have been blocked by #bMooplePolice for the HACK reason#k.");
		}
	}

	public void addMiss() {
		this.misses++;
	}

	public void resetMisses() {
		if (lastMisses == misses && misses > 6) {
			sameMissCount++;
		}
		
		if (sameMissCount > 4) {
			chr.autoban("Autobanned for : " + misses + " Miss godmode", 1);
			chr.sendPolice("You have been blocked by #bMooplePolice for the HACK reason#k.");
		} else if (sameMissCount > 0) {
			this.lastMisses = misses;
		}
		
		this.misses = 0;
	}

	// Don't use the same type for more than 1 thing
	public void spam(int type) {
		this.spam[type] = System.currentTimeMillis();
	}

	public long getLastSpam(int type) {
		return spam[type];
	}

	/**
	 * Timestamp checker
	 * 
	 * <code>type</code>:<br>
	 * 0: HealOverTime<br>
	 * 1: Pet Food<br>
	 * 2: ItemSort<br>
	 * 3: ItemIdSort<br>
	 * 4: SpecialMove<br>
	 * 5: UseCatchItem<br>
	 * 
	 * @param type
	 *            type
	 * @return Timestamp checker
	 */
	public void setTimestamp(int type, int time) {
		if (this.timestamp[type] == time) {
			this.timestampCounter[type]++;
			if (this.timestampCounter[type] > 3) {
				chr.getClient().disconnect();
				// System.out.println("Same timestamp for type: " + type +
				// "; Character: " + chr);
			}
			return;
		}
		this.timestamp[type] = time;
	}
	
	public void autoban(AutobanType type, String message) {
		final String autobanMessage = 
				String.format("Autobanned for (%s: %s)", type.name(), message);
		chr.autoban(autobanMessage, 1);
		chr.sendPolice("You have been blocked by #bMooplePolice#k for the HACK reason.");
	}
}
