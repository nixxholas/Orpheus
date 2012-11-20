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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Messenger {
	private List<MessengerCharacter> members = new ArrayList<MessengerCharacter>(3);
	private int id;
	private boolean[] pos = new boolean[3];

	public Messenger(int id, MessengerCharacter chrfor) {
		this.members.add(chrfor);
		chrfor.setPosition(getLowestPosition());
		this.id = id;
	}

	public void addMember(MessengerCharacter member) {
		members.add(member);
		member.setPosition(getLowestPosition());
	}

	public void removeMember(MessengerCharacter member) {
		pos[member.getPosition()] = true;
		members.remove(member);
	}

	public void silentRemoveMember(MessengerCharacter member) {
		members.remove(member);
	}

	public void silentAddMember(MessengerCharacter member, int position) {
		members.add(member);
		member.setPosition(position);
	}

	public Collection<MessengerCharacter> getMembers() {
		return Collections.unmodifiableList(members);
	}

	public int getLowestPosition() {// (:
		for (byte b = 0; b < 3; b++) {
			if (pos[b]) {
				pos[b] = false;
				return b;
			}
		}
		return -1;
	}

	public int getPositionByName(String name) {
		for (MessengerCharacter messengerchar : members) {
			if (messengerchar.getName().equals(name)) {
				return messengerchar.getPosition();
			}
		}
		return 4;
	}

	public int getId() {
		return id;
	}
}
