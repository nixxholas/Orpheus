/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import tools.DatabaseConnection;

/**
 * 
 * @author Danny
 */
public class Ring implements Comparable<Ring> {
	private int ringId;
	private int ringId2;
	private int partnerId;
	private int itemId;
	private String partnerName;
	private boolean equipped = false;

	public Ring(int id, int id2, int partnerId, int itemid, String partnername) {
		this.ringId = id;
		this.ringId2 = id2;
		this.partnerId = partnerId;
		this.itemId = itemid;
		this.partnerName = partnername;
	}

	public static Ring loadFromDb(int ringId) {
		Ring ring = null;
		Connection connection = DatabaseConnection.getConnection(); 
		try (
				PreparedStatement ps = getSelectCommand(connection, ringId);
				ResultSet rs = ps.executeQuery();) {
			
			if (rs.next()) {
				final int partnerRingId = rs.getInt("partnerRingId");
				final int partnerCharacterId = rs.getInt("partnerChrId");
				final int ringItemId = rs.getInt("itemid");
				final String partnerCharacterName = rs.getString("partnerName");
				ring = new Ring(ringId, partnerRingId, partnerCharacterId, ringItemId, partnerCharacterName);
			}

			return ring;
		} catch (SQLException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private static PreparedStatement getSelectCommand(Connection connection, int ringId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT * FROM `rings` WHERE `id` = ?"); 
		ps.setInt(1, ringId);
		return ps;
	}

	// TODO: Extract partner IDs and names into a parameter object. Yuck.
	public static int createRing(int itemid, final GameCharacter partner1, final GameCharacter partner2) {
		if (partner1 == null) {
			return -2;
		} else if (partner2 == null) {
			return -1;
		}
		int[] ringID = new int[2];

		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("INSERT INTO `rings` (`itemid`, `partnerChrId`, `partnername`) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, itemid);
			ps.setInt(2, partner2.getId());
			ps.setString(3, partner2.getName());
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			ringID[0] = rs.getInt(1); // ID.
			rs.close();
			ps.close();
			ps = con.prepareStatement("INSERT INTO `rings` (`itemid`, `partnerRingId`, `partnerChrId`, `partnername`) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, itemid);
			ps.setInt(2, ringID[0]);
			ps.setInt(3, partner1.getId());
			ps.setString(4, partner1.getName());
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			rs.next();
			ringID[1] = rs.getInt(1);
			rs.close();
			ps.close();
			ps = con.prepareStatement("UPDATE `rings` SET `partnerRingId` = ? WHERE `id` = ?");
			ps.setInt(1, ringID[1]);
			ps.setInt(2, ringID[0]);
			ps.executeUpdate();
			ps.close();
			return ringID[0];
		} catch (SQLException ex) {
			ex.printStackTrace();
			return -1;
		}
	}

	public int getRingId() {
		return ringId;
	}

	public int getPartnerRingId() {
		return ringId2;
	}

	public int getPartnerChrId() {
		return partnerId;
	}

	public int getItemId() {
		return itemId;
	}

	public String getPartnerName() {
		return partnerName;
	}

	public boolean equipped() {
		return equipped;
	}

	public void equip() {
		this.equipped = true;
	}

	public void unequip() {
		this.equipped = false;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Ring) {
			if (((Ring) o).getRingId() == getRingId()) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + this.ringId;
		return hash;
	}

	@Override
	public int compareTo(Ring other) {
		if (ringId < other.getRingId()) {
			return -1;
		} else if (ringId == other.getRingId()) {
			return 0;
		}
		return 1;
	}
}
