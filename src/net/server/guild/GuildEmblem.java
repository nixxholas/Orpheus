package net.server.guild;

public class GuildEmblem {
	
	private int fgId;
	private int fgColor;
	private int bgId;
	private int bgColor;

	public GuildEmblem(int foregroundId, int foregroundColor, int backgroundId, int backgroundColor) {
		this.fgId = foregroundId;
		this.fgColor = foregroundColor;
		this.bgId = backgroundId;
		this.bgColor = backgroundColor;
	}

	public int getForegroundId() {
		return fgId;
	}

	public int getForegroundColor() {
		return fgColor;
	}

	public int getBackgroundId() {
		return bgId;
	}
	
	public int getBackgroundColor() {
		return bgColor;
	}
}