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

/**
 * @author Matze
 */
public enum MapleInventoryType {
	UNDEFINED(0), EQUIP(1), USE(2), SETUP(3), ETC(4), CASH(5), EQUIPPED(-1);
	private final byte type;

	private MapleInventoryType(int type) {
		this.type = (byte) type;
	}

	public byte asByte() {
		return type;
	}

	public short getBitfieldEncoding() {
		return (short) (2 << type);
	}

	public static MapleInventoryType fromByte(byte type) {
		for (MapleInventoryType l : MapleInventoryType.values()) {
			if (l.asByte() == type) {
				return l;
			}
		}
		return null;
	}

	public static MapleInventoryType getByWZName(String name) {
		switch(name) {
		case "Install":
			return SETUP;
		case "Consume":
			return USE;
		case "Etc":
			return ETC;
		case "Cash":
			return CASH;
		case "Pet":
			return CASH;
		default:
			return UNDEFINED;
		}
	}
}
