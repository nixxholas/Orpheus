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

import client.GameClient;
import net.AbstractPacketHandler;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * 
 * @author Matze
 */
public final class NPCMoreTalkHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		byte lastMsg = reader.readByte(); // 00 (last msg type I think)
		byte action = reader.readByte(); // 00 = end chat, 01 == follow
		if (lastMsg == 2) {
			if (action != 0) {
				String returnText = reader.readMapleAsciiString();
				if (c.getQM() != null) {
					c.getQM().setGetText(returnText);
					if (c.getQM().isStart()) {
						QuestScriptManager.getInstance().start(c, action, lastMsg, -1);
					} else {
						QuestScriptManager.getInstance().end(c, action, lastMsg, -1);
					}
				} else {
					c.getCM().setGetText(returnText);
					NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
				}
			} else if (c.getQM() != null) {
				c.getQM().dispose();
			} else {
				c.getCM().dispose();
			}
		} else {
			int selection = -1;
			if (reader.available() >= 4) {
				selection = reader.readInt();
			} else if (reader.available() > 0) {
				selection = reader.readByte();
			}
			if (c.getQM() != null) {
				if (c.getQM().isStart()) {
					QuestScriptManager.getInstance().start(c, action, lastMsg, selection);
				} else {
					QuestScriptManager.getInstance().end(c, action, lastMsg, selection);
				}
			} else if (c.getCM() != null) {
				NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
			}
		}
	}
}