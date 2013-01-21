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
	
	NOT_A_WEAPON(0), 
	AXE1H(4.4), 
	AXE2H(4.8), 
	BLUNT1H(4.4), 
	BLUNT2H(4.8), 
	BOW(3.4), 
	CLAW(3.6), 
	CROSSBOW(3.6), 
	DAGGER(4), 
	GUN(3.6), 
	KNUCKLE(4.8), 
	POLE_ARM(5.0), 
	SPEAR(5.0), 
	STAFF(3.6), 
	SWORD1H(4.0), 
	SWORD2H(4.6), 
	WAND(3.6);
	
	private final double damageMultiplier;

	private WeaponType(double maxDamageMultiplier) {
		this.damageMultiplier = maxDamageMultiplier;
	}

	public double getMaxDamageMultiplier() {
		return damageMultiplier;
	}
}
