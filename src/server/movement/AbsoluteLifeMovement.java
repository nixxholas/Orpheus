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
package server.movement;

import java.awt.Point;
import tools.data.output.LittleEndianWriter;

public class AbsoluteLifeMovement extends AbstractLifeMovement {
	private Point pixelsPerSecond;
	private int unk;

	public AbsoluteLifeMovement(byte type, Point position, int duration, byte newstate) {
		super(type, position, duration, newstate);
	}

	public Point getPixelsPerSecond() {
		return pixelsPerSecond;
	}

	public void setPixelsPerSecond(Point wobble) {
		this.pixelsPerSecond = wobble;
	}

	public int getUnk() {
		return unk;
	}

	public void setUnk(int unk) {
		this.unk = unk;
	}

	@Override
	public void serialize(LittleEndianWriter w) {
		w.write(getType());
		w.writeShort(getPosition().x);
		w.writeShort(getPosition().y);
		w.writeShort(pixelsPerSecond.x);
		w.writeShort(pixelsPerSecond.y);
		w.writeShort(unk);
		w.write(getNewstate());
		w.writeShort(getDuration());
	}
}
