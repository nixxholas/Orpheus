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
package gm;

import java.util.List;
import net.GamePacket;
import tools.data.output.PacketWriter;

/**
 * 
 * @author kevintjuh93
 */
public class GMPacketCreator {

	public static GamePacket keyResponse(final boolean ok) {
		PacketWriter w = new PacketWriter(3);
		w.writeShort(GMSendOpcode.LOGIN_RESPONSE.getValue());
		w.write(ok ? 1 : 0);
		return w.getPacket();
	}

	public static GamePacket sendLoginResponse(final byte loginOk, final String login) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.LOGIN_RESPONSE.getValue());
		w.write(loginOk);
		if (loginOk == 3) {
			w.writeMapleAsciiString(login);
		}
		return w.getPacket();
	}

	public static GamePacket chat(final String msg) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.CHAT.getValue());
		w.writeMapleAsciiString(msg);
		return w.getPacket();
	}

	public static GamePacket sendUserList(final List<String> names) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.GM_LIST.getValue());
		w.write(0);
		for (String name : names) {
			w.writeMapleAsciiString(name);
		}
		return w.getPacket();
	}

	public static GamePacket addUser(final String name) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.GM_LIST.getValue());
		w.write(1);
		w.writeMapleAsciiString(name);

		return w.getPacket();
	}

	public static GamePacket removeUser(final String name) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.GM_LIST.getValue());
		w.write(2);
		w.writeMapleAsciiString(name);

		return w.getPacket();
	}

	public static GamePacket sendPlayerList(final List<String> list) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.SEND_PLAYER_LIST.getValue());
		for (String s : list) {
			w.writeMapleAsciiString(s);
		}
		return w.getPacket();
	}

	public static GamePacket commandResponse(final byte op) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.COMMAND_RESPONSE.getValue());
		w.write(op);
		return w.getPacket();
	}

	public static GamePacket playerStats(final String name, final String job, final byte level, final int exp, final short hp, final short mp, final short str, final short dex, final short int_, final short luk, final int meso) {
		PacketWriter w = new PacketWriter();
		w.writeShort(GMSendOpcode.COMMAND_RESPONSE.getValue());
		w.write(3);
		w.writeMapleAsciiString(name);
		w.writeMapleAsciiString(job);
		w.write(level);
		w.writeInt(exp);
		w.writeShort(hp);
		w.writeShort(mp);
		w.writeShort(str);
		w.writeShort(dex);
		w.writeShort(int_);
		w.writeShort(luk);
		w.writeInt(meso);
		return w.getPacket();
	}
}
