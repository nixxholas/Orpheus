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
import tools.Randomizer;
import java.io.File;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.TimerManager;
import server.maps.GameMap;
import tools.PacketCreator;

/**
 * 
 * @author FloppyDisk
 */
public final class MapleOxQuiz {
	private int round = 1;
	private int question = 1;
	private GameMap map = null;
	private int expGain = 200;
	private static MapleDataProvider stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));

	public MapleOxQuiz(GameMap map) {
		this.map = map;
		this.round = Randomizer.nextInt(9);
		this.question = 1;
	}

	private boolean isCorrectAnswer(GameCharacter chr, int answer) {
		double x = chr.getPosition().getX();
		double y = chr.getPosition().getY();
		if ((x > -234 && y > -26 && answer == 0) || (x < -234 && y > -26 && answer == 1)) {
			chr.dropMessage("Correct!");
			return true;
		}
		return false;
	}

	public void sendQuestion() {
		int gm = 0;
		for (GameCharacter player : map.getCharacters()) {
			if (player.gmLevel() > 0) {
				gm++;
			}
		}
		final int number = gm;
		map.broadcastMessage(PacketCreator.showOXQuiz(round, question, true));
		TimerManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				map.broadcastMessage(PacketCreator.showOXQuiz(round, question, true));
				for (GameCharacter chr : map.getCharacters()) {
					// make sure they aren't null... maybe something can happen in 12 seconds.
					if (chr != null) 
					{
						if (!isCorrectAnswer(chr, getOXAnswer(round, question)) && !chr.isGM()) {
							chr.changeMap(chr.getMap().getReturnMap());
						} else {
							chr.gainExp(expGain, true, true);
						}
					}
				}
				// do question
				if ((round == 1 && question == 29) || ((round == 2 || round == 3) && question == 17) || ((round == 4 || round == 8) && question == 12) || (round == 5 && question == 26) || (round == 9 && question == 44) || ((round == 6 || round == 7) && question == 16)) {
					question = 100;
				} else {
					question++;
				}
				// send question
				if (map.getCharacters().size() - number <= 1) {
					map.broadcastMessage(PacketCreator.serverNotice(6, "The event has ended"));
					map.getPortal("join00").setPortalStatus(true);
					map.setOx(null);
					map.setOxQuiz(false);
					// prizes here
					return;
				}
				sendQuestion();
			}
		}, 30000); // Time to answer = 30 seconds ( Ox Quiz packet shows a 30
					// second timer.
	}

	private static int getOXAnswer(int imgdir, int id) {
		return MapleDataTool.getInt(stringData.getData("OXQuiz.img").getChildByPath("" + imgdir + "").getChildByPath("" + id + "").getChildByPath("a"));
	}
}
