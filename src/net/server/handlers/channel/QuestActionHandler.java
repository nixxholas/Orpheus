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
import net.AbstractPacketHandler;
import scripting.quest.QuestScriptManager;
import server.quest.Quest;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class QuestActionHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte action = reader.readByte();
		short questid = reader.readShort();
		GameCharacter player = c.getPlayer();
		Quest quest = Quest.getInstance(questid);
		if (action == 1) { // Start Quest
			int npc = reader.readInt();
			if (reader.available() >= 4) {
				reader.readInt();
			}
			quest.start(player, npc);
		} else if (action == 2) { // Complete Quest
			int npc = reader.readInt();
			reader.readInt();
			if (reader.available() >= 2) {
				int selection = reader.readShort();
				quest.complete(player, npc, selection);
			} else {
				quest.complete(player, npc);
			}
		} else if (action == 3) {// forfeit quest
			quest.forfeit(player);
		} else if (action == 4) { // scripted start quest
			// System.out.println(reader.toString());
			int npc = reader.readInt();
			reader.readInt();
			QuestScriptManager.getInstance().start(c, questid, npc);
		} else if (action == 5) { // scripted end quests
			// System.out.println(reader.toString());
			int npc = reader.readInt();
			reader.readInt();
			QuestScriptManager.getInstance().end(c, questid, npc);
		}
	}
}
