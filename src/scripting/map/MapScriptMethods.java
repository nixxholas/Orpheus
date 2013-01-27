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
package scripting.map;

import scripting.AbstractPlayerInteraction;
import server.quest.Quest;
import tools.PacketCreator;
import client.GameClient;
import client.QuestStatus;
import client.SkillFactory;

public class MapScriptMethods extends AbstractPlayerInteraction {

	public MapScriptMethods(GameClient c) {
		super(c);
	}

	String rewardstring = " title has been rewarded. Please see NPC Dalair to receive your Medal.";

	public void displayAranIntro() {
		final int gender = c.getPlayer().getGender();
		String path;
		boolean lockUi = false;
		switch (c.getPlayer().getMapId()) {
			case 914090010:
				lockUi = true;
				path = "Effect/Direction1.img/aranTutorial/Scene0";
				break;
				
			case 914090011:
				path = "Effect/Direction1.img/aranTutorial/Scene1" + gender;
				break;
				
			case 914090012:
				path = "Effect/Direction1.img/aranTutorial/Scene2" + gender;
				break;
				
			case 914090013:
				path = "Effect/Direction1.img/aranTutorial/Scene3";
				break;
				
			case 914090100:
				lockUi = true;
				path = "Effect/Direction1.img/aranTutorial/HandedPoleArm" + gender;
				break;
				
			default:
				throw new RuntimeException("Unknown Aran intro map.");
		}

		if (lockUi) {
			lockUI();
		}
		
		c.announce(PacketCreator.showIntro(path));
	}

	public void arriveIceCave() {
		unlockUI();
		c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000014), (byte) -1, 0, -1);
		c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000015), (byte) -1, 0, -1);
		c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000016), (byte) -1, 0, -1);
		c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000017), (byte) -1, 0, -1);
		c.getPlayer().changeSkillLevel(SkillFactory.getSkill(20000018), (byte) -1, 0, -1);
		c.getPlayer().setRemainingSp(0);
		c.announce(PacketCreator.showIntro("Effect/Direction1.img/aranTutorial/ClickLilin"));
	}

	public void startExplorerExperience() {
		final int gender = c.getPlayer().getGender();
		String path;
		switch (c.getPlayer().getMapId()) {
		case 1020100:
			// Swordman
			path = "Effect/Direction3.img/swordman/Scene" + gender;
			break;

		case 1020200:
			// Magician
			path = "Effect/Direction3.img/magician/Scene" + gender;
			break;

		case 1020300:
			// Archer
			path = "Effect/Direction3.img/archer/Scene" + gender;
			break;

		case 1020400:
			// Rogue
			path = "Effect/Direction3.img/rogue/Scene" + gender;
			break;

		case 1020500:
			// Pirate
			path = "Effect/Direction3.img/pirate/Scene" + gender;
			break;

		default:
			throw new RuntimeException("Unknown adventurer intro map.");
		}
		
		c.announce(PacketCreator.showIntro(path));
	}

	public void enterRien() {
		if (c.getPlayer().getJob().getId() == 2100 && !c.getPlayer().getAranIntroState("ck=1")) {
			c.getPlayer().addAreaData(21019, "miss=o;arr=o;ck=1;helper=clear");
			c.announce(PacketCreator.updateAreaInfo("miss=o;arr=o;ck=1;helper=clear", 21019));
			unlockUI();
		}
	}

	public void goAdventure() {
		lockUI();
		c.announce(PacketCreator.showIntro("Effect/Direction3.img/goAdventure/Scene" + c.getPlayer().getGender()));
	}

	public void goLith() {
		lockUI();
		c.announce(PacketCreator.showIntro("Effect/Direction3.img/goLith/Scene" + c.getPlayer().getGender()));
	}

	public void explorerQuest(short questid, String questName) {
		Quest quest = Quest.getInstance(questid);
		if (!isQuestStarted(questid)) {
			if (!quest.forceStart(getPlayer(), 9000066)) {
				return;
			}
		}
		QuestStatus q = getPlayer().getQuest(quest);
		if (!q.addMedalMap(getPlayer().getMapId())) {
			return;
		}
		String status = Integer.toString(q.getMedalProgress());
		int infoex = quest.getInfoEx();
		getPlayer().announce(PacketCreator.questProgress(quest.getInfoNumber(), status));
		StringBuilder smp = new StringBuilder();
		StringBuilder etm = new StringBuilder();
		if (q.getMedalProgress() == infoex) {
			etm.append("Earned the ").append(questName).append(" title!");
			smp.append("You have earned the <").append(questName).append(">").append(rewardstring);
			getPlayer().announce(PacketCreator.getShowQuestCompletion(quest.getId()));
		} else {
			getPlayer().announce(PacketCreator.earnTitleMessage(status + "/" + infoex + " regions explored."));
			etm.append("Trying for the ").append(questName).append(" title.");
			smp.append("You made progress on the ").append(questName).append(" title. ").append(status).append("/").append(infoex);
		}
		getPlayer().announce(PacketCreator.earnTitleMessage(etm.toString()));
		showInfoText(smp.toString());
	}

	public void touchTheSky() { // 29004
		Quest quest = Quest.getInstance(29004);
		if (!isQuestStarted(29004)) {
			if (!quest.forceStart(getPlayer(), 9000066)) {
				return;
			}
		}
		QuestStatus q = getPlayer().getQuest(quest);
		if (!q.addMedalMap(getPlayer().getMapId())) {
			return;
		}
		String status = Integer.toString(q.getMedalProgress());
		getPlayer().announce(PacketCreator.questProgress(quest.getInfoNumber(), status));
		getPlayer().announce(PacketCreator.earnTitleMessage(status + "/5 Completed"));
		getPlayer().announce(PacketCreator.earnTitleMessage("The One Who's Touched the Sky title in progress."));
		if (q.getMedalProgress() == quest.getInfoEx()) {
			showInfoText("The One Who's Touched the Sky" + rewardstring);
			getPlayer().announce(PacketCreator.getShowQuestCompletion(quest.getId()));
		} else {
			showInfoText("The One Who's Touched the Sky title in progress. " + status + "/5 Completed");
		}
	}
}
