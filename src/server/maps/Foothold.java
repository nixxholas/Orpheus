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
package server.maps;

import java.awt.Point;

/**
 * 
 * @author Matze
 */
public class Foothold implements Comparable<Foothold> {
	private Point p1;
	private Point p2;
	private int id;
	private int next, prev;

	public Foothold(Point p1, Point p2, int id) {
		this.p1 = p1;
		this.p2 = p2;
		this.id = id;
	}

	public boolean isWall() {
		return p1.x == p2.x;
	}

	public int getX1() {
		return p1.x;
	}

	public int getX2() {
		return p2.x;
	}

	public int getY1() {
		return p1.y;
	}

	public int getY2() {
		return p2.y;
	}

	public int compareTo(Foothold o) {
		Foothold other = o;
		if (p2.y < other.getY1()) {
			return -1;
		} else if (p1.y > other.getY2()) {
			return 1;
		} else {
			return 0;
		}
	}

	public int getId() {
		return id;
	}

	public int getNext() {
		return next;
	}

	public void setNext(int next) {
		this.next = next;
	}

	public int getPrev() {
		return prev;
	}

	public void setPrev(int prev) {
		this.prev = prev;
	}
}
