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
package net.server.handlers.login;

import client.AutoRegister;
import client.GameClient;
import java.util.Calendar;
import net.MaplePacketHandler;
import net.server.Server;
import server.TimerManager;
import tools.DateUtil;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class LoginPasswordHandler implements MaplePacketHandler {
	
	@Override
	public boolean validateState(GameClient c) {
		return !c.isLoggedIn();
	}

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int loginok = 0;
		String login = slea.readMapleAsciiString();
		String pwd = slea.readMapleAsciiString();
		c.setAccountName(login);
		
		if (AutoRegister.getAccountExists(login)) {
			loginok = c.login(login, pwd);
		} else {
			final boolean autoRegisterSuccess = AutoRegister.createAccount(login, pwd, c.getSession().getRemoteAddress().toString());
			if (autoRegisterSuccess) {
				loginok = c.login(login, pwd);
			}
		}

		if (c.hasBannedIP() || c.hasBannedMac()) {
			c.announce(MaplePacketCreator.getLoginFailed(3));
		}
		Calendar tempban = c.getTempBanCalendar();
		if (tempban != null) {
			if (tempban.getTimeInMillis() > System.currentTimeMillis()) {
				long till = DateUtil.getFileTimestamp(tempban.getTimeInMillis());
				c.announce(MaplePacketCreator.getTempBan(till, c.getGReason()));
				return;
			}
		}
		if (loginok == 3) {
			c.announce(MaplePacketCreator.getPermBan(c.getGReason()));
			return;
		} else if (loginok != 0) {
			c.announce(MaplePacketCreator.getLoginFailed(loginok));
			return;
		}
		if (!c.isDeveloper() && Server.getInstance().isDebugging()) {
			c.announce(MaplePacketCreator.getLoginFailed(7));
		}
		if (c.finishLogin() == 0) {
			c.announce(MaplePacketCreator.getAuthSuccess(c));
			final GameClient client = c;
			client.saveLastKnownIP();
			c.setIdleTask(TimerManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					client.disconnect();
				}
			}, 600000));
		} else {
			c.announce(MaplePacketCreator.getLoginFailed(7));
		}
	}
}
