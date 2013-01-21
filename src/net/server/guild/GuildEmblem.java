package net.server.guild;

import java.sql.ResultSet;
import java.sql.SQLException;

import tools.data.output.PacketWriter;

public class GuildEmblem {
	
	public final int foregroundId;
	public final int foregroundColor;
	public final int backgroundId;
	public final int backgroundColor;

	public GuildEmblem(ResultSet rs) throws SQLException {
		this.foregroundId = rs.getInt("logo");
		this.foregroundColor = rs.getInt("logoColor");
		this.backgroundId = rs.getInt("logoBG");
		this.backgroundColor = rs.getInt("logoBGColor");
	}
	
	public GuildEmblem(int foregroundId, int foregroundColor, int backgroundId, int backgroundColor) {
		this.foregroundId = foregroundId;
		this.foregroundColor = foregroundColor;
		this.backgroundId = backgroundId;
		this.backgroundColor = backgroundColor;
	}
	
	public void serialize(PacketWriter w) {
		w.writeAsShort(this.backgroundId);
		w.writeAsByte(this.backgroundColor);
		w.writeAsShort(this.foregroundId);
		w.writeAsByte(this.foregroundColor);
	}
}