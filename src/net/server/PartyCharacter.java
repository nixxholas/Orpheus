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
package net.server;

import java.awt.Point;
import java.io.Serializable;
import client.Job;
import client.GameCharacter;

public class PartyCharacter implements Serializable {
	private static final long serialVersionUID = -6460122214407438511L;
	
	private int id;
	private String name;
	private byte channelId, worldId;
	private int mapId;
	private boolean online;

	private Job job;
	private int jobId;
	
	private int level;
	
	private int doorTown = 999999999;
	private int doorTarget = 999999999;
	private Point doorPosition = new Point(0, 0);

	public PartyCharacter(GameCharacter character) {
		this.id = character.getId();
		this.name = character.getName();
		this.worldId = character.getClient().getWorldId();
		this.channelId = character.getClient().getChannelId();
		this.mapId = character.getMapId();

		this.online = true;

		this.jobId = character.getJob().getId();
		this.job = character.getJob();
		
		this.level = character.getLevel();
		
		if (character.getDoors().size() > 0) {
			this.doorTown = character.getDoors().get(0).getTown().getId();
			this.doorTarget = character.getDoors().get(0).getTarget().getId();
			this.doorPosition = character.getDoors().get(0).getTargetPosition();
		}
	}

	public PartyCharacter() {
		this.name = "";
	}

	public Job getJob() {
		return job;
	}

	public int getLevel() {
		return level;
	}

	public byte getChannelId() {
		return channelId;
	}

	public void setChannelId(byte channel) {
		this.channelId = channel;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public int getMapId() {
		return mapId;
	}

	public void setMapId(int mapid) {
		this.mapId = mapid;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public int getJobId() {
		return jobId;
	}

	public int getDoorTown() {
		return doorTown;
	}

	public int getDoorTarget() {
		return doorTarget;
	}

	public Point getDoorPosition() {
		return doorPosition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final PartyCharacter other = (PartyCharacter) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	public byte getWorldId() {
		return worldId;
	}
}
