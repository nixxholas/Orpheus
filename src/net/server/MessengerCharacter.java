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

import client.GameCharacter;

public class MessengerCharacter {
	private String name;
	private int id, position;
	private byte channel;
	private boolean online;

	public MessengerCharacter(GameCharacter character) {
		this.name = character.getName();
		this.channel = character.getClient().getChannelId();
		this.id = character.getId();
		this.online = true;
		this.position = 0;
	}

	public MessengerCharacter(GameCharacter character, int position) {
		this.name = character.getName();
		this.channel = character.getClient().getChannelId();
		this.id = character.getId();
		this.online = true;
		this.position = position;
	}

	public int getId() {
		return id;
	}

	public byte getChannel() {
		return channel;
	}

	public String getName() {
		return name;
	}

	public boolean isOnline() {
		return online;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
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
		final MessengerCharacter other = (MessengerCharacter) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}
