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
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * 
 * @author kevintjuh93
 */
public class GMPacketCreator {

	public static GamePacket keyResponse(final boolean ok) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
		mplew.writeShort(GMSendOpcode.LOGIN_RESPONSE.getValue());
		mplew.write(ok ? 1 : 0);
		return mplew.getPacket();
	}

	public static GamePacket sendLoginResponse(final byte loginOk, final String login) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.LOGIN_RESPONSE.getValue());
		mplew.write(loginOk);
		if (loginOk == 3) {
			mplew.writeMapleAsciiString(login);
		}
		return mplew.getPacket();
	}

	public static GamePacket chat(final String msg) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.CHAT.getValue());
		mplew.writeMapleAsciiString(msg);
		return mplew.getPacket();
	}

	public static GamePacket sendUserList(final List<String> names) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.GM_LIST.getValue());
		mplew.write(0);
		for (String name : names) {
			mplew.writeMapleAsciiString(name);
		}
		return mplew.getPacket();
	}

	public static GamePacket addUser(final String name) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.GM_LIST.getValue());
		mplew.write(1);
		mplew.writeMapleAsciiString(name);

		return mplew.getPacket();
	}

	public static GamePacket removeUser(final String name) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.GM_LIST.getValue());
		mplew.write(2);
		mplew.writeMapleAsciiString(name);

		return mplew.getPacket();
	}

	public static GamePacket sendPlayerList(final List<String> list) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.SEND_PLAYER_LIST.getValue());
		for (String s : list) {
			mplew.writeMapleAsciiString(s);
		}
		return mplew.getPacket();
	}

	public static GamePacket commandResponse(final byte op) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.COMMAND_RESPONSE.getValue());
		mplew.write(op);
		return mplew.getPacket();
	}

	public static GamePacket playerStats(final String name, final String job, final byte level, final int exp, final short hp, final short mp, final short str, final short dex, final short int_, final short luk, final int meso) {
		MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
		mplew.writeShort(GMSendOpcode.COMMAND_RESPONSE.getValue());
		mplew.write(3);
		mplew.writeMapleAsciiString(name);
		mplew.writeMapleAsciiString(job);
		mplew.write(level);
		mplew.writeInt(exp);
		mplew.writeShort(hp);
		mplew.writeShort(mp);
		mplew.writeShort(str);
		mplew.writeShort(dex);
		mplew.writeShort(int_);
		mplew.writeShort(luk);
		mplew.writeInt(meso);
		return mplew.getPacket();
	}
}
