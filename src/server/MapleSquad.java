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
package server;

import client.GameCharacter;
import java.util.LinkedList;
import java.util.List;
import net.MaplePacket;
import tools.MaplePacketCreator;

/**
 * 
 * @author Danny
 */
public class MapleSquad {
	private GameCharacter leader;
	private List<GameCharacter> members = new LinkedList<GameCharacter>();
	private List<GameCharacter> bannedMembers = new LinkedList<GameCharacter>();
	private int ch;
	private int status = 0;

	public MapleSquad(int ch, GameCharacter leader) {
		this.leader = leader;
		this.members.add(leader);
		this.ch = ch;
		this.status = 1;
	}

	public GameCharacter getLeader() {
		return leader;
	}

	public boolean containsMember(GameCharacter member) {
		for (GameCharacter mmbr : members) {
			if (mmbr.getId() == member.getId()) {
				return true;
			}
		}
		return false;
	}

	public boolean isBanned(GameCharacter member) {
		for (GameCharacter banned : bannedMembers) {
			if (banned.getId() == member.getId()) {
				return true;
			}
		}
		return false;
	}

	public List<GameCharacter> getMembers() {
		return members;
	}

	public int getSquadSize() {
		return members.size();
	}

	public boolean addMember(GameCharacter member) {
		if (isBanned(member)) {
			return false;
		} else {
			members.add(member);
			MaplePacket packet = MaplePacketCreator.serverNotice(5, member.getName() + " has joined the fight!");
			getLeader().getClient().getSession().write(packet);
			return true;
		}
	}

	public void banMember(GameCharacter member, boolean ban) {
		int index = -1;
		for (GameCharacter mmbr : members) {
			if (mmbr.getId() == member.getId()) {
				index = members.indexOf(mmbr);
			}
		}
		members.remove(index);
		if (ban) {
			bannedMembers.add(member);
		}
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public boolean equals(MapleSquad other) {
		if (other.ch == ch) {
			if (other.leader.getId() == leader.getId()) {
				return true;
			}
		}
		return false;
	}
}
