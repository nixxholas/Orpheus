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
package server.events.gm;

import client.GameCharacter;
import java.util.LinkedList;
import java.util.List;
import server.TimerManager;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

/**
 * 
 * @author kevintjuh93
 */
public class MapleSnowball {
	private MapleMap map;
	private int position = 0;
	private int hits = 25;
	private int snowmanhp = 7500;
	private boolean hittable = false;
	private int team;
	private boolean winner = false;
	List<GameCharacter> characters = new LinkedList<GameCharacter>();

	public MapleSnowball(int team, MapleMap map) {
		this.map = map;
		this.team = team;

		for (GameCharacter chr : map.getCharacters()) {
			if (chr.getTeam() == team)
				characters.add(chr);
		}
	}

	public void startEvent() {
		if (hittable == true)
			return;

		for (GameCharacter chr : characters) {
			if (chr != null) {
				chr.announce(MaplePacketCreator.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)));
				chr.announce(MaplePacketCreator.getClock(600));
			}
		}
		hittable = true;
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (map.getSnowball(team).getPosition() > map.getSnowball(team == 0 ? 1 : 0).getPosition()) {
					for (GameCharacter chr : characters) {
						if (chr != null)
							chr.announce(MaplePacketCreator.rollSnowBall(false, 3, map.getSnowball(0), map.getSnowball(0)));
					}
					winner = true;
				} else if (map.getSnowball(team == 0 ? 1 : 0).getPosition() > map.getSnowball(team).getPosition()) {
					for (GameCharacter chr : characters) {
						if (chr != null)
							chr.announce(MaplePacketCreator.rollSnowBall(false, 4, map.getSnowball(0), map.getSnowball(0)));
					}
					winner = true;
				} // Else
				warpOut();
			}
		}, 600000);

	}

	public boolean isHittable() {
		return hittable;
	}

	public void setHittable(boolean hit) {
		this.hittable = hit;
	}

	public int getPosition() {
		return position;
	}

	public int getSnowmanHP() {
		return snowmanhp;
	}

	public void setSnowmanHP(int hp) {
		this.snowmanhp = hp;
	}

	public void hit(int what, int damage) {
		if (what < 2)
			if (damage > 0)
				this.hits--;
			else {
				if (this.snowmanhp - damage < 0) {
					this.snowmanhp = 0;

					TimerManager.getInstance().schedule(new Runnable() {

						@Override
						public void run() {
							setSnowmanHP(7500);
							message(5);
						}
					}, 10000);
				} else
					this.snowmanhp -= damage;
				map.broadcastMessage(MaplePacketCreator.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)));
			}

		if (this.hits == 0) {
			this.position += 1;
			if (this.position == 45)
				map.getSnowball(team == 0 ? 1 : 0).message(1);
			else if (this.position == 290)
				map.getSnowball(team == 0 ? 1 : 0).message(2);
			else if (this.position == 560)
				map.getSnowball(team == 0 ? 1 : 0).message(3);

			this.hits = 25;
			map.broadcastMessage(MaplePacketCreator.rollSnowBall(false, 0, map.getSnowball(0), map.getSnowball(1)));
			map.broadcastMessage(MaplePacketCreator.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)));
		}
		map.broadcastMessage(MaplePacketCreator.hitSnowBall(what, damage));
	}

	public void message(int message) {
		for (GameCharacter chr : characters) {
			if (chr != null)
				chr.announce(MaplePacketCreator.snowballMessage(team, message));
		}
	}

	public void warpOut() {
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (winner == true)
					map.warpOutByTeam(team, 109050000);
				else
					map.warpOutByTeam(team, 109050001);

				map.setSnowball(team, null);
			}
		}, 10000);
	}
}