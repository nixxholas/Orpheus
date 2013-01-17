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
package tools;

import java.util.Random;

public class Randomizer {

	private final static Random rng = new Random();

	public static int nextInt() {
		return rng.nextInt();
	}

	public static int nextInt(final int arg0) {
		return rng.nextInt(arg0);
	}

	public static void nextBytes(final byte[] bytes) {
		rng.nextBytes(bytes);
	}

	public static boolean nextBoolean() {
		return rng.nextBoolean();
	}

	public static double nextDouble() {
		return rng.nextDouble();
	}

	public static float nextFloat() {
		return rng.nextFloat();
	}

	public static long nextLong() {
		return rng.nextLong();
	}

	public static int rand(final int lowerBound, final int upperBound) {
		return (int) ((rng.nextDouble() * (upperBound - lowerBound + 1)) + lowerBound);
	}
}