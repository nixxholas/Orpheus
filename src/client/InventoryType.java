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
public enum InventoryType {
	UNDEFINED(0), EQUIP(1), USE(2), SETUP(3), ETC(4), CASH(5), EQUIPPED(-1);
	private final byte type;

	private InventoryType(int type) {
		this.type = (byte) type;
	}

	public byte asByte() {
		return type;
	}
	
	public boolean is(InventoryType other) {
		return other != null && this.type == other.type;
	}

	public short getBitfieldEncoding() {
		return (short) (2 << type);
	}

	public static InventoryType fromByte(byte type) {
		for (InventoryType value : InventoryType.values()) {
			if (value.type == type) {
				return value;
			}
		}
		return null;
	}

	public static InventoryType getByWZName(String name) {
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
