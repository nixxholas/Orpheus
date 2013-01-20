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
package scripting.portal;

import client.GameClient;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import scripting.AbstractPlayerInteraction;
import server.Portal;
import tools.DatabaseConnection;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {

	private Portal portal;

	public PortalPlayerInteraction(GameClient c, Portal portal) {
		super(c);
		this.portal = portal;
	}

	public Portal getPortal() {
		return portal;
	}

	public boolean hasLevel30Character() {
		try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `level` FROM `characters` WHERE accountid = ?");) {			
			ps.setInt(1, getPlayer().getAccountId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (rs.getInt("level") >= 30) {
						return true;
					}
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} 
		return false;
	}

	public void blockPortal() {
		c.getPlayer().blockPortal(getPortal().getScriptName());
	}

	public void unblockPortal() {
		c.getPlayer().unblockPortal(getPortal().getScriptName());
	}
}