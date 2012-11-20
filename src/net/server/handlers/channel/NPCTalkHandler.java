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
import server.life.Npc;
import server.maps.GameMapObject;
import server.maps.PlayerNPCs;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class NPCTalkHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		if (!c.getPlayer().isAlive()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		int oid = slea.readInt();
		GameMapObject obj = c.getPlayer().getMap().getMapObject(oid);
		if (obj instanceof Npc) {
			Npc npc = (Npc) obj;
			if (npc.getId() == 9010009) {
				c.announce(PacketCreator.sendDuey((byte) 8, DueyHandler.loadItems(c.getPlayer())));
			} else if (npc.hasShop()) {
				if (c.getPlayer().getShop() != null) {
					return;
				}
				npc.sendShop(c);
			} else {
				if (c.getCM() != null || c.getQM() != null) {
					c.announce(PacketCreator.enableActions());
					return;
				}
				NPCScriptManager.getInstance().start(c, npc.getId(), null, null);
			}
		} else if (obj instanceof PlayerNPCs) {
			NPCScriptManager.getInstance().start(c, ((PlayerNPCs) obj).getId(), null, null);
		}
	}
}