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
		w.writeAsShort(GMSendOpcode.LOGIN_RESPONSE.getValue());
		w.writeAsByte(ok);
		return w.getPacket();
	}

	public static GamePacket sendLoginResponse(final byte loginResult, final String login) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.LOGIN_RESPONSE.getValue());
		w.writeAsByte(loginResult);
		if (loginResult == 3) {
			w.writeLengthString(login);
		}
		return w.getPacket();
	}

	public static GamePacket chat(final String msg) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.CHAT.getValue());
		w.writeLengthString(msg);
		return w.getPacket();
	}

	public static GamePacket sendUserList(final List<String> names) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.GM_LIST.getValue());
		w.writeAsByte(0);
		for (String name : names) {
			w.writeLengthString(name);
		}
		return w.getPacket();
	}

	public static GamePacket addUser(final String name) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.GM_LIST.getValue());
		w.writeAsByte(1);
		w.writeLengthString(name);

		return w.getPacket();
	}

	public static GamePacket removeUser(final String name) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.GM_LIST.getValue());
		w.writeAsByte(2);
		w.writeLengthString(name);

		return w.getPacket();
	}

	public static GamePacket sendPlayerList(final List<String> list) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.SEND_PLAYER_LIST.getValue());
		for (String s : list) {
			w.writeLengthString(s);
		}
		return w.getPacket();
	}

	public static GamePacket commandResponse(final byte op) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.COMMAND_RESPONSE.getValue());
		w.writeAsByte(op);
		return w.getPacket();
	}

	public static GamePacket playerStats(final String name, final String job, final byte level, final int exp, final short hp, final short mp, final short str, final short dex, final short int_, final short luk, final int meso) {
		PacketWriter w = new PacketWriter();
		w.writeAsShort(GMSendOpcode.COMMAND_RESPONSE.getValue());
		w.writeAsByte(3);
		w.writeLengthString(name);
		w.writeLengthString(job);
		w.writeAsByte(level);
		w.writeInt(exp);
		w.writeAsShort(hp);
		w.writeAsShort(mp);
		w.writeAsShort(str);
		w.writeAsShort(dex);
		w.writeAsShort(int_);
		w.writeAsShort(luk);
		w.writeInt(meso);
		return w.getPacket();
	}
}
