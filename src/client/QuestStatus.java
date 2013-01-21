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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import server.quest.Quest;
import tools.StringUtil;

/**
 * 
 * @author Matze
 */
public class QuestStatus {
	
	private Quest quest;
	private QuestCompletionState completionState;
	private Map<Integer, String> progress = new LinkedHashMap<Integer, String>();
	private List<Integer> medalProgress = new LinkedList<Integer>();
	private int npc;
	private long completionTime;
	private int forfeited = 0;

	public QuestStatus(Quest quest, QuestCompletionState questCompletionState) {
		this.quest = quest;
		this.setCompletionState(questCompletionState);
		this.completionTime = System.currentTimeMillis();
		if (questCompletionState == QuestCompletionState.STARTED)
			registerMobs();
	}

	public QuestStatus(Quest quest, QuestCompletionState questCompletionState, int npc) {
		this.quest = quest;
		this.setCompletionState(questCompletionState);
		this.setNpc(npc);
		this.completionTime = System.currentTimeMillis();
		if (questCompletionState == QuestCompletionState.STARTED) {
			registerMobs();
		}
	}

	public Quest getQuest() {
		return quest;
	}

	public QuestCompletionState getCompletionState() {
		return completionState;
	}

	public final void setCompletionState(QuestCompletionState questCompletionState) {
		this.completionState = questCompletionState;
	}

	public int getNpc() {
		return npc;
	}

	public final void setNpc(int npc) {
		this.npc = npc;
	}

	private void registerMobs() {
		for (int i : quest.getRelevantMobs()) {
			progress.put(i, "000");
		}
	}

	public boolean addMedalMap(int mapId) {
		if (medalProgress.contains(mapId))
			return false;
		medalProgress.add(mapId);
		return true;
	}

	public int getMedalProgress() {
		return medalProgress.size();
	}

	public List<Integer> getMedalMaps() {
		return medalProgress;
	}

	public boolean progress(int id) {
		if (progress.get(id) != null) {
			int current = Integer.parseInt(progress.get(id));
			String str = StringUtil.getLeftPaddedStr(Integer.toString(current + 1), '0', 3);
			progress.put(id, str);
			return true;
		}
		return false;
	}

	public void setProgress(int id, String data) {
		progress.put(id, data);
	}

	public boolean madeProgress() {
		return progress.size() > 0;
	}

	public String getProgress(int id) {
		if (progress.get(id) == null)
			return "";
		return progress.get(id);
	}

	public Map<Integer, String> getProgress() {
		return Collections.unmodifiableMap(progress);
	}

	public long getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(long completionTime) {
		this.completionTime = completionTime;
	}

	public int getForfeited() {
		return forfeited;
	}

	public void setForfeited(int forfeited) {
		if (forfeited >= this.forfeited) {
			this.forfeited = forfeited;
		} else {
			throw new IllegalArgumentException("Can't set forfeits to something lower than before.");
		}
	}

	public String getQuestData() {
		StringBuilder str = new StringBuilder();
		for (String ps : progress.values()) {
			str.append(ps);
		}
		return str.toString();
	}
}