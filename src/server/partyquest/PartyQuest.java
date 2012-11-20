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
package server.partyquest;

import client.GameCharacter;
import java.util.ArrayList;
import java.util.List;
import net.server.Party;
import net.server.PartyCharacter;
import net.server.Server;

/**
 * 
 * @author kevintjuh93
 */
public class PartyQuest {
	byte channel, world;
	Party party;
	List<GameCharacter> participants = new ArrayList<GameCharacter>();

	public PartyQuest(Party party) {
		this.party = party;
		PartyCharacter leader = party.getLeader();
		channel = leader.getChannel();
		world = leader.getWorld();
		int mapid = leader.getMapId();
		for (PartyCharacter pchr : party.getMembers()) {
			if (pchr.getChannel() == channel && pchr.getMapId() == mapid) {
				GameCharacter chr = Server.getInstance().getWorld(world).getChannel(channel).getPlayerStorage().getCharacterById(pchr.getId());
				if (chr != null)
					this.participants.add(chr);
			}
		}
	}

	public Party getParty() {
		return party;
	}

	public List<GameCharacter> getParticipants() {
		return participants;
	}

	public void removeParticipant(GameCharacter chr) throws Throwable {
		synchronized (participants) {
			participants.remove(chr);
			chr.setPartyQuest(null);
			if (participants.isEmpty())
				super.finalize();
			// System.gc();
		}
	}
}
