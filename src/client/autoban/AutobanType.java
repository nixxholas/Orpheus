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

/**
 * 
 * @author kevintjuh93
 */
public enum AutobanType {
	MOB_COUNT, FIX_DAMAGE, HIGH_HP_HEALING, FAST_HP_HEALING(15), FAST_MP_HEALING(15), GACHA_EXP, TUBI(20, 15000), SHORT_ITEM_VAC, ITEM_VAC, FAST_ATTACK(10, 30000), MPCON(25, 30000);

	private int points;
	private long expiration;

	private AutobanType() {
		this(1, -1);
	}

	private AutobanType(int points) {
		this.points = points;
		this.expiration = -1;
	}

	private AutobanType(int points, long expire) {
		this.points = points;
		this.expiration = expire;
	}

	public int getMaximum() {
		return points;
	}

	public long getExpiration() {
		return expiration;
	}
}
