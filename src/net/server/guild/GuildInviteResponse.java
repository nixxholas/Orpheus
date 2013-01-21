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
package net.server.guild;

import tools.PacketCreator;
import net.GamePacket;

public enum GuildInviteResponse {
	NOT_IN_CHANNEL(0x2A), 
	ALREADY_IN_GUILD(0x28), 
	NOT_IN_GUILD(0x2D);
	
	private byte value;

	private GuildInviteResponse(int value) {
		this.value = (byte) value;
	}

	public GamePacket getPacket() {
		return PacketCreator.genericGuildMessage(this.value);
	}
}
