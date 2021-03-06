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
package server.quest;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import client.GameCharacter;
import client.QuestCompletionState;
import client.QuestStatus;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.PacketCreator;

/**
 * 
 * @author Matze
 */
public class Quest {
	private static Map<Integer, Quest> quests = new HashMap<Integer, Quest>();
	protected short infoNumber, infoex, id;
	protected int timeLimit, timeLimit2;
	protected List<QuestRequirement> startReqs;
	protected List<QuestRequirement> completeReqs;
	protected List<QuestAction> startActs;
	protected List<QuestAction> completeActs;
	protected List<Integer> relevantMobs;
	private boolean autoStart;
	private boolean autoPreComplete;
	private boolean repeatable = false;
	private final static MapleDataProvider questData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Quest.wz"));
	private static MapleData actions = questData.getData("Act.img");
	private static MapleData requirements = questData.getData("Check.img");
	private static MapleData info = questData.getData("QuestInfo.img");

	private Quest(int id) {
		this.id = (short) id;
		relevantMobs = new LinkedList<Integer>();
		MapleData startReqData = requirements.getChildByPath(String.valueOf(id)).getChildByPath("0");
		startReqs = new LinkedList<QuestRequirement>();
		if (startReqData != null) {
			for (MapleData startReq : startReqData.getChildren()) {
				QuestRequirementType type = QuestRequirementType.getByWZName(startReq.getName());
				if (type.equals(QuestRequirementType.INTERVAL)) {
					repeatable = true;
				}
				QuestRequirement req = new QuestRequirement(this, type, startReq);
				if (req.getType().equals(QuestRequirementType.MOB)) {
					for (MapleData mob : startReq.getChildren()) {
						relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
					}
				}
				startReqs.add(req);
			}
		}
		MapleData completeReqData = requirements.getChildByPath(String.valueOf(id)).getChildByPath("1");
		completeReqs = new LinkedList<QuestRequirement>();
		if (completeReqData != null) {
			for (MapleData completeReq : completeReqData.getChildren()) {
				QuestRequirement req = new QuestRequirement(this, QuestRequirementType.getByWZName(completeReq.getName()), completeReq);
				if (req.getType().equals(QuestRequirementType.INFO_NUMBER))
					infoNumber = (short) MapleDataTool.getInt(completeReq, 0);
				if (req.getType().equals(QuestRequirementType.INFO_EX)) {
					MapleData zero = completeReq.getChildByPath("0");
					if (zero != null) {
						MapleData value = zero.getChildByPath("value");
						if (value != null)
							infoex = Short.parseShort(MapleDataTool.getString(value, "0"));
					}
				}
				if (req.getType().equals(QuestRequirementType.MOB)) {
					for (MapleData mob : completeReq.getChildren()) {
						relevantMobs.add(MapleDataTool.getInt(mob.getChildByPath("id")));
					}
					Collections.sort(relevantMobs);
				}
				completeReqs.add(req);
			}
		}
		MapleData startActData = actions.getChildByPath(String.valueOf(id)).getChildByPath("0");
		startActs = new LinkedList<QuestAction>();
		if (startActData != null) {
			for (MapleData startAct : startActData.getChildren()) {
				QuestActionType questActionType = QuestActionType.getByWZName(startAct.getName());
				startActs.add(new QuestAction(questActionType, startAct, this));
			}
		}
		MapleData completeActData = actions.getChildByPath(String.valueOf(id)).getChildByPath("1");
		completeActs = new LinkedList<QuestAction>();
		if (completeActData != null) {
			for (MapleData completeAct : completeActData.getChildren()) {
				completeActs.add(new QuestAction(QuestActionType.getByWZName(completeAct.getName()), completeAct, this));
			}
		}
		MapleData questInfo = info.getChildByPath(String.valueOf(id));

		timeLimit = MapleDataTool.getInt("timeLimit", questInfo, 0);
		timeLimit2 = MapleDataTool.getInt("timeLimit2", questInfo, 0);
		autoStart = MapleDataTool.getInt("autoStart", questInfo, 0) == 1;
		autoPreComplete = MapleDataTool.getInt("autoPreComplete", questInfo, 0) == 1;
	}

	public static Quest getInstance(int id) {
		Quest ret = quests.get(id);
		if (ret == null) {
			ret = new Quest(id);
			quests.put(id, ret);
		}
		return ret;
	}

	private boolean canStart(GameCharacter c, int npcid) {
		if (c.getQuest(this).getCompletionState() != QuestCompletionState.NOT_STARTED && !(c.getQuest(this).getCompletionState() == QuestCompletionState.COMPLETED && repeatable)) {
			return false;
		}
		for (QuestRequirement r : startReqs) {
			if (!r.check(c, npcid)) {
				return false;
			}
		}
		return true;
	}

	public boolean canComplete(GameCharacter c, Integer npcid) {
		if (!c.getQuest(this).getCompletionState().equals(QuestCompletionState.STARTED)) {
			return false;
		}
		for (QuestRequirement r : completeReqs) {
			if (!r.check(c, npcid)) {
				return false;
			}
		}
		return true;
	}

	public void start(GameCharacter c, int npc) {
		if ((autoStart || checkNpcOnMap(c, npc)) && canStart(c, npc)) {
			for (QuestAction a : startActs) {
				a.run(c, null);
			}
			forceStart(c, npc);
		}
	}

	public void complete(GameCharacter c, int npc) {
		complete(c, npc, null);
	}

	public void complete(GameCharacter c, int npc, Integer selection) {
		if ((autoPreComplete || checkNpcOnMap(c, npc)) && canComplete(c, npc)) {
			/*
			 * for (QuestAction a : completeActs) { if (!a.check(c)) {
			 * return; } }
			 */
			forceComplete(c, npc);
			for (QuestAction a : completeActs) {
				a.run(c, selection);
			}
		}
	}

	public void reset(GameCharacter c) {
		c.updateQuest(new QuestStatus(this, QuestCompletionState.NOT_STARTED));
	}

	public void forfeit(GameCharacter c) {
		if (!c.getQuest(this).getCompletionState().equals(QuestCompletionState.STARTED)) {
			return;
		}
		if (timeLimit > 0) {
			c.announce(PacketCreator.removeQuestTimeLimit(id));
		}
		QuestStatus newStatus = new QuestStatus(this, QuestCompletionState.NOT_STARTED);
		newStatus.setForfeited(c.getQuest(this).getForfeited() + 1);
		c.updateQuest(newStatus);
	}

	public boolean forceStart(GameCharacter c, int npc) {
		if (!canStart(c, npc))
			return false;

		QuestStatus newStatus = new QuestStatus(this, QuestCompletionState.STARTED, npc);
		newStatus.setForfeited(c.getQuest(this).getForfeited());

		if (timeLimit > 0)
			c.questTimeLimit(this, 30000);// timeLimit * 1000
		if (timeLimit2 > 0) {// =\

		}
		c.updateQuest(newStatus);
		return true;
	}

	public boolean forceComplete(GameCharacter c, int npc) {
		if (!canComplete(c, npc))
			return false;

		QuestStatus newStatus = new QuestStatus(this, QuestCompletionState.COMPLETED, npc);
		newStatus.setForfeited(c.getQuest(this).getForfeited());
		newStatus.setCompletionTime(System.currentTimeMillis());
		c.announce(PacketCreator.showSpecialEffect(9));
		c.getMap().broadcastMessage(c, PacketCreator.showForeignEffect(c.getId(), 9), false);
		c.updateQuest(newStatus);

		return true;
	}

	public short getId() {
		return id;
	}

	public List<Integer> getRelevantMobs() {
		return relevantMobs;
	}

	private boolean checkNpcOnMap(GameCharacter player, int npcid) {
		return player.getMap().containsNpc(npcid);
	}

	public int getItemAmountNeeded(int itemid) {
		MapleData data = requirements.getChildByPath(String.valueOf(id)).getChildByPath("1");
		if (data != null) {
			for (MapleData req : data.getChildren()) {
				QuestRequirementType type = QuestRequirementType.getByWZName(req.getName());
				if (!type.equals(QuestRequirementType.ITEM))
					continue;

				for (MapleData d : req.getChildren()) {
					if (MapleDataTool.getInt(d.getChildByPath("id"), 0) == itemid)
						return MapleDataTool.getInt(d.getChildByPath("count"), 0);
				}
			}
		}
		return 0;
	}

	public int getMobAmountNeeded(int mid) {
		MapleData data = requirements.getChildByPath(String.valueOf(id)).getChildByPath("1");
		if (data != null) {
			for (MapleData req : data.getChildren()) {
				QuestRequirementType type = QuestRequirementType.getByWZName(req.getName());
				if (!type.equals(QuestRequirementType.MOB))
					continue;

				for (MapleData d : req.getChildren()) {
					if (MapleDataTool.getInt(d.getChildByPath("id"), 0) == mid)
						return MapleDataTool.getInt(d.getChildByPath("count"), 0);
				}
			}
		}
		return 0;
	}

	public short getInfoNumber() {
		return infoNumber;
	}

	public short getInfoEx() {
		return infoex;
	}

	public int getTimeLimit() {
		return timeLimit;
	}
}
