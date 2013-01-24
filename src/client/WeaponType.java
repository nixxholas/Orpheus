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

public enum WeaponType {
	
	SWORD1H(30, 4.0), 
	AXE1H(31, 4.4), 
	BLUNT1H(32, 4.4), 
	DAGGER(33, 4),
	// 34 - not a weapon
	// 35 - not a weapon
	// 36 - not a weapon
	WAND(37, 3.6),
	STAFF(38, 3.6),
	// 39 - not a weapon
	SWORD2H(40, 4.6), 
	AXE2H(41, 4.8), 
	BLUNT2H(42, 4.8), 
	SPEAR(43, 5.0), 
	POLE_ARM(44, 5.0), 
	BOW(45, 3.4), 
	CROSSBOW(46, 3.6), 
	CLAW(47, 3.6), 
	KNUCKLE(48, 4.8), 
	GUN(49, 3.6), 
	;
	
	private final int partialId;
	private final double damageMultiplier;

	private WeaponType(int code, double maxDamageMultiplier) {
		this.partialId = code;
		this.damageMultiplier = maxDamageMultiplier;
	}

	public double getMaxDamageMultiplier() {
		return damageMultiplier;
	}
	
	public static WeaponType fromPartialId(int partialId) {
		for (WeaponType type : WeaponType.values()) {
			if (type.partialId == partialId) {
				return type;
			}
		}
		
		return null;
	}
}
