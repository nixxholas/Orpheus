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
package net.server.handlers.channel;

import client.GameCharacter;
import client.GameClient;
import net.AbstractMaplePacketHandler;
import scripting.quest.QuestScriptManager;
import server.quest.MapleQuest;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class QuestActionHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		byte action = slea.readByte();
		short questid = slea.readShort();
		GameCharacter player = c.getPlayer();
		MapleQuest quest = MapleQuest.getInstance(questid);
		if (action == 1) { // Start Quest
			int npc = slea.readInt();
			if (slea.available() >= 4) {
				slea.readInt();
			}
			quest.start(player, npc);
		} else if (action == 2) { // Complete Quest
			int npc = slea.readInt();
			slea.readInt();
			if (slea.available() >= 2) {
				int selection = slea.readShort();
				quest.complete(player, npc, selection);
			} else {
				quest.complete(player, npc);
			}
		} else if (action == 3) {// forfeit quest
			quest.forfeit(player);
		} else if (action == 4) { // scripted start quest
			// System.out.println(slea.toString());
			int npc = slea.readInt();
			slea.readInt();
			QuestScriptManager.getInstance().start(c, questid, npc);
		} else if (action == 5) { // scripted end quests
			// System.out.println(slea.toString());
			int npc = slea.readInt();
			slea.readInt();
			QuestScriptManager.getInstance().end(c, questid, npc);
		}
	}
}
