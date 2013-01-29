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

import java.util.ArrayList;
import java.util.List;
import server.StatEffect;
import server.life.Element;

public class Skill implements ISkill {
	private final int skillId;
	private final Element element;
	private final int animationTime;
	private final boolean isAction;

	private final List<StatEffect> effects;

	public Skill(int id, Element element, int animationTime, boolean action, List<StatEffect> effects) {
		this.skillId = id;
		this.element = element;
		this.animationTime = animationTime;
		this.isAction = action;
		
		this.effects = new ArrayList<StatEffect>(effects);
	}

	@Override
	public int getId() {
		return skillId;
	}

	@Override
	public StatEffect getEffect(int level) {
		return effects.get(level - 1);
	}

	@Override
	public int getMaxLevel() {
		return effects.size();
	}

	@Override
	public boolean isFourthJob() {
		return (skillId / 10000) % 10 == 2;
	}

	@Override
	public Element getElement() {
		return element;
	}

	@Override
	public int getAnimationTime() {
		return animationTime;
	}

	@Override
	public boolean isBeginnerSkill() {
		return skillId % 10000000 < 10000;
	}

	@Override
	public boolean getAction() {
		return isAction;
	}
}