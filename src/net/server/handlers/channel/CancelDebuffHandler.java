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
import tools.data.input.SeekableLittleEndianAccessor;

public final class CancelDebuffHandler extends AbstractPacketHandler {// TIP: BAD STUFF LOL

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		/*
		 * List<Disease> diseases = c.getPlayer().getDiseases();
		 * List<Disease> diseases_ = new ArrayList<Disease>(); for
		 * (Disease disease : diseases) { List<Disease> disease_ = new
		 * ArrayList<Disease>(); disease_.add(disease);
		 * diseases_.add(disease);
		 * c.announce(PacketCreator.cancelDebuff(disease_));
		 * c.getPlayer().getMap().broadcastMessage(c.getPlayer(),
		 * PacketCreator.cancelForeignDebuff(c.getPlayer().getId(),
		 * disease_), false); } for (Disease disease : diseases_) {
		 * c.getPlayer().removeDisease(disease); }
		 */
	}
}