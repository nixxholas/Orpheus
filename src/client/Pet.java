/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss

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
package client;

import com.mysql.jdbc.Statement;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import tools.DatabaseConnection;
import server.ItemInfoProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;

/**
 * 
 * @author Matze
 */
public class Pet extends Item {
	private String name;
	private int uniqueId;
	private int closeness = 0;
	private byte level = 1;
	private int fullness = 100;
	private int foothold;
	private Point pos;
	private int stance;
	private boolean summoned;

	private Pet(int id, byte position, int uniqueId) {
		super(id, position, (short) 1);
		this.uniqueId = uniqueId;
	}
	
	public static Pet loadFromDb(IItem petItem) {
		int petId = petItem.getPetId();
		final Pet pet = new Pet(petItem.getItemId(), petItem.getPosition(), petId);

		// Get pet details...
		final Connection connection = DatabaseConnection.getConnection();
		try (
				PreparedStatement ps = getSelectCommand(petId, connection);
				ResultSet rs = ps.executeQuery();) {
			
			rs.next();
			pet.name = rs.getString("name");
			pet.closeness = Math.min(rs.getInt("closeness"), 30000);
			pet.level = (byte) Math.min(rs.getByte("level"), 30);
			pet.fullness = Math.min(rs.getInt("fullness"), 100);
			pet.summoned = rs.getInt("summoned") == 1;

			return pet;
		} catch (SQLException e) {
			return null;
		}
	}

	private static PreparedStatement getSelectCommand(int petId, final Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT `name`, `level`, `closeness`, `fullness`, `summoned` FROM `pets` WHERE `petid` = ?"); 
		ps.setInt(1, petId);
		return ps;
	}

	public void saveToDb() {
		final Connection connection = DatabaseConnection.getConnection();
		try (final PreparedStatement ps = getUpdateCommand(connection);) {
			ps.executeUpdate();			
		} catch (SQLException e) {
		}
	}

	private PreparedStatement getUpdateCommand(final Connection connection) throws SQLException {
		final PreparedStatement ps = connection.prepareStatement("UPDATE `pets` SET `name` = ?, `level` = ?, `closeness` = ?, `fullness` = ?, `summoned` = ? WHERE `petid` = ?");
		ps.setString(1, this.getName());
		ps.setInt(2, this.getLevel());
		ps.setInt(3, this.getCloseness());
		ps.setInt(4, this.getFullness());
		ps.setInt(5, this.isSummoned() ? 1 : 0);
		ps.setInt(6, this.getUniqueId());
		return ps;
	}

	public static int createPet(int itemId) {
		final Connection connection = DatabaseConnection.getConnection();
		try {
			final PreparedStatement ps = connection.prepareStatement("INSERT INTO `pets` (`name`, `level`, `closeness`, `fullness`, `summoned`) VALUES (?, 1, 0, 100, 0)", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, ItemInfoProvider.getInstance().getName(itemId));
			ps.executeUpdate();
			final ResultSet rs = ps.getGeneratedKeys();
			int result = -1;
			if (rs.next()) {
				result = rs.getInt(1);
			}

			return result;
		} catch (SQLException e) {
			return -1;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(int id) {
		this.uniqueId = id;
	}

	public int getCloseness() {
		return closeness;
	}

	public void setCloseness(int closeness) {
		this.closeness = closeness;
	}

	public void gainCloseness(int x) {
		this.closeness += x;
	}

	public byte getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public int getFullness() {
		return fullness;
	}

	public void setFullness(int fullness) {
		this.fullness = fullness;
	}

	public int getFoothold() {
		return foothold;
	}

	public void setFoothold(int foothold) {
		this.foothold = foothold;
	}

	public Point getPos() {
		return pos;
	}

	public void setPos(Point pos) {
		this.pos = pos;
	}

	public int getStance() {
		return stance;
	}

	public void setStance(int stance) {
		this.stance = stance;
	}

	public boolean isSummoned() {
		return summoned;
	}

	public void setSummoned(boolean value) {
		this.summoned = value;
	}

	public boolean canConsume(int itemId) {
		for (int petId : ItemInfoProvider.getInstance().petsCanConsume(itemId)) {
			if (petId == this.getItemId()) {
				return true;
			}
		}		
		return false;
	}

	public void updatePosition(List<LifeMovementFragment> movement) {
		for (LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				final LifeMovement lifeMove = (LifeMovement) move;
				if (lifeMove instanceof AbsoluteLifeMovement) {
					this.setPos(lifeMove.getPosition());
				}
				
				this.setStance(lifeMove.getNewstate());
			}
		}
	}
}