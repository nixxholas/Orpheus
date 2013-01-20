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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import net.AbstractPacketHandler;
import server.maps.AnimatedGameMapObject;
import server.movement.AbsoluteLifeMovement;
import server.movement.ChairMovement;
import server.movement.ChangeEquip;
import server.movement.JumpDownMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.RelativeLifeMovement;
import tools.data.input.LittleEndianAccessor;

public abstract class AbstractMovementPacketHandler extends AbstractPacketHandler {
	protected List<LifeMovementFragment> parseMovement(LittleEndianAccessor reader) {
		List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
		byte numCommands = reader.readByte();
		for (byte i = 0; i < numCommands; i++) {
			byte command = reader.readByte();
			switch (command) {
				case 0: // normal move
				case 5:
				case 17: { // Float
					short xpos = reader.readShort();
					short ypos = reader.readShort();
					short xwobble = reader.readShort();
					short ywobble = reader.readShort();
					short unk = reader.readShort();
					byte newstate = reader.readByte();
					short duration = reader.readShort();
					AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
					alm.setUnk(unk);
					alm.setPixelsPerSecond(new Point(xwobble, ywobble));
					res.add(alm);
					break;
				}
				case 1:
				case 2:
				case 6: // fj
				case 12:
				case 13: // Shot-jump-back thing
				case 16: { // Float
					short xpos = reader.readShort();
					short ypos = reader.readShort();
					byte newstate = reader.readByte();
					short duration = reader.readShort();
					RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xpos, ypos), duration, newstate);
					res.add(rlm);
					break;
				}
				case 3:
				case 4: // tele... -.-
				case 7: // assaulter
				case 8: // assassinate
				case 9: // rush
				case 14: { // Before Jump Down - fixes item/mobs dissappears
					reader.skip(9);
					break;
					/*
					 * case 14: { short xpos = reader.readShort(); short ypos =
					 * reader.readShort(); short xwobble = reader.readShort(); short
					 * ywobble = reader.readShort(); byte newstate =
					 * reader.readByte(); TeleportMovement tm = new
					 * TeleportMovement(command, new Point(xpos, ypos),
					 * newstate); tm.setPixelsPerSecond(new Point(xwobble,
					 * ywobble)); res.add(tm); break; }
					 */
				}
				case 10: // Change Equip
					res.add(new ChangeEquip(reader.readByte()));
					break;
				case 11: { // Chair
					short xpos = reader.readShort();
					short ypos = reader.readShort();
					short unk = reader.readShort();
					byte newstate = reader.readByte();
					short duration = reader.readShort();
					ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
					cm.setUnk(unk);
					res.add(cm);
					break;
				}
				case 15: {
					short xpos = reader.readShort();
					short ypos = reader.readShort();
					short xwobble = reader.readShort();
					short ywobble = reader.readShort();
					short unk = reader.readShort();
					short fh = reader.readShort();
					byte newstate = reader.readByte();
					short duration = reader.readShort();
					JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
					jdm.setUnk(unk);
					jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
					jdm.setFH(fh);
					res.add(jdm);
					break;
				}
				case 21: {// Causes aran to do weird stuff when attacking o.o
					/*
					 * byte newstate = reader.readByte(); short unk =
					 * reader.readShort(); AranMovement am = new
					 * AranMovement(command, null, unk, newstate); res.add(am);
					 */
					reader.skip(3);
					break;
				}
				default:
					return null;
			}
		}
		return res;
	}

	protected void updatePosition(List<LifeMovementFragment> movement, AnimatedGameMapObject target, int yoffset) {
		for (LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					Point position = ((LifeMovement) move).getPosition();
					position.y += yoffset;
					target.setPosition(position);
				}
				target.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}
