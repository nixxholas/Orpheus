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
package net.server.guild;

import client.GameCharacter;
import java.io.Serializable;

public class GuildCharacter implements Serializable {
	private static final long serialVersionUID = -8012634292341191559L;
	private int level;
	private int id;
	private byte worldId, channelId;
	private int jobId;
	private int guildRank;
	private int guildId;
	private int allianceRank;
	private boolean online;
	private String name;

	public GuildCharacter(GameCharacter c) {
		this.name = c.getName();
		this.level = c.getLevel();
		this.id = c.getId();
		this.channelId = c.getClient().getChannel();
		this.worldId = c.getWorldId();
		this.jobId = c.getJob().getId();
		this.guildRank = c.getGuildRank();
		this.guildId = c.getGuildId();
		this.online = true;
		this.allianceRank = c.getAllianceRank();
	}

	public GuildCharacter(int id, int level, String name, byte channelId, byte worldId, int jobId, int rank, int guildId, boolean online, int allianceRank) {
		this.level = level;
		this.id = id;
		this.name = name;
		if (online) {
			this.channelId = channelId;
			this.worldId = worldId;
		}
		this.jobId = jobId;
		this.online = online;
		this.guildRank = rank;
		this.guildId = guildId;
		this.allianceRank = allianceRank;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int l) {
		level = l;
	}

	public int getId() {
		return id;
	}

	public void setChannelId(byte ch) {
		channelId = ch;
	}

	public byte getChannel() {
		return channelId;
	}

	public byte getWorld() {
		return worldId;
	}

	public int getJobId() {
		return jobId;
	}

	public void setJobId(int job) {
		jobId = job;
	}

	public int getGuildId() {
		return guildId;
	}

	public void setGuildId(int gid) {
		guildId = gid;
	}

	public void setGuildRank(int rank) {
		guildRank = rank;
	}

	public int getGuildRank() {
		return guildRank;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean f) {
		online = f;
	}

	public String getName() {
		return name;
	}

	public void setAllianceRank(int rank) {
		allianceRank = rank;
	}

	public int getAllianceRank() {
		return allianceRank;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GuildCharacter)) {
			return false;
		}
		GuildCharacter o = (GuildCharacter) other;
		return (o.getId() == id && o.getName().equals(name));
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 19 * hash + this.id;
		hash = 19 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}
}
