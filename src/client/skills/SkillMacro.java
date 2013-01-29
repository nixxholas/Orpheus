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
package client.skills;

public class SkillMacro {
	
	public final int position;

	public final int skill1;
	public final int skill2;
	public final int skill3;

	public final String name;
	public final int shout;

	public SkillMacro(int position, int skill1, int skill2, int skill3, String name, int shout) {
		this.skill1 = skill1;
		this.skill2 = skill2;
		this.skill3 = skill3;
		this.name = name;
		this.shout = shout;
		this.position = position;
	}
}
