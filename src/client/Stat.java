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

import net.IntValueHolder;

public enum Stat implements IntValueHolder {

	SKIN(0x1), FACE(0x2), HAIR(0x4), LEVEL(0x10), JOB(0x20), STR(0x40), DEX(0x80), INT(0x100), LUK(0x200), HP(0x400), MAXHP(0x800), MP(0x1000), MAXMP(0x2000), AVAILABLEAP(0x4000), AVAILABLESP(0x8000), EXP(0x10000), FAME(0x20000), MESO(0x40000), PET(0x180008), GACHAEXP(0x200000);
	private final int i;

	private Stat(int i) {
		this.i = i;
	}

	@Override
	public int getValue() {
		return i;
	}
	
	public String toString() {
		switch (this) {
			default:
				return "Unknown";
			case SKIN:
				return "Skin";
			case FACE:
				return "Face";
			case HAIR:
				return "Hair";
			case LEVEL:
				return "Level";
			case JOB:
				return "Job";
			case STR:
				return "Strength";
			case DEX:
				return "Dexterity";
			case INT:
				return "Intellect";
			case LUK:
				return "Luck";
			case HP:
				return "HP";
			case MAXHP:
				return "Max HP";
			case MP:
				return "MP";
			case MAXMP:
				return "Max MP";
			case AVAILABLEAP:
				return "Available AP";
			case AVAILABLESP:
				return "Available SP";
			case EXP:
				return "Experience";
			case FAME:
				return "Fame";
			case MESO:
				return "Mesos";
		}
	}

	public static Stat getByValue(int value) {
		for (Stat stat : Stat.values()) {
			if (stat.getValue() == value) {
				return stat;
			}
		}
		return null;
	}

	public static Stat getBy5ByteEncoding(int encoded) {
		switch (encoded) {
			case 64:
				return STR;
			case 128:
				return DEX;
			case 256:
				return INT;
			case 512:
				return LUK;
		}
		return null;
	}

	public static Stat getByString(String type) {
		if (type.equals("SKIN")) {
			return SKIN;
		} else if (type.equals("FACE")) {
			return FACE;
		} else if (type.equals("HAIR")) {
			return HAIR;
		} else if (type.equals("LEVEL")) {
			return LEVEL;
		} else if (type.equals("JOB")) {
			return JOB;
		} else if (type.equals("STR")) {
			return STR;
		} else if (type.equals("DEX")) {
			return DEX;
		} else if (type.equals("INT")) {
			return INT;
		} else if (type.equals("LUK")) {
			return LUK;
		} else if (type.equals("HP")) {
			return HP;
		} else if (type.equals("MAXHP")) {
			return MAXHP;
		} else if (type.equals("MP")) {
			return MP;
		} else if (type.equals("MAXMP")) {
			return MAXMP;
		} else if (type.equals("AVAILABLEAP")) {
			return AVAILABLEAP;
		} else if (type.equals("AVAILABLESP")) {
			return AVAILABLESP;
		} else if (type.equals("EXP")) {
			return EXP;
		} else if (type.equals("FAME")) {
			return FAME;
		} else if (type.equals("MESO")) {
			return MESO;
		} else if (type.equals("PET")) {
			return PET;
		}
		return null;
	}
}
