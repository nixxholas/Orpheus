package client;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LinkedCharacterInfo {
	public final int level;
	public final String name;
	
	public LinkedCharacterInfo(ResultSet rs) throws SQLException {
		this.level = rs.getInt("level");
		this.name = rs.getString("name");
	}
}
