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
package net.server.handlers.channel;

import java.util.Collection;
import net.AbstractPacketHandler;
import net.server.MapleParty;
import server.maps.GameMap;
import server.maps.GameMapObject;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.GameCharacter;
import client.GameClient;
import client.MapleJob;

/**
 * 
 * @author XoticStory
 * @author BubblesDev
 */
public class PartySearchStartHandler extends AbstractPacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		int min = slea.readInt();
		int max = slea.readInt();
		slea.readInt(); // members
		int jobs = slea.readInt();
		GameCharacter chr = c.getPlayer();
		GameMap map = chr.getMap();
		Collection<GameMapObject> mapobjs = map.getAllPlayer();
		for (GameMapObject mapobj : mapobjs) {
			if (chr.getParty().getMembers().size() > 5) {
				break;
			}
			if (mapobj instanceof GameCharacter) {
				GameCharacter tchar = (GameCharacter) mapobj;
				int charlvl = tchar.getLevel();
				if (charlvl >= min && charlvl <= max && isValidJob(tchar.getJob(), jobs)) {
					if (c.getPlayer().getParty() == null) {
						// WorldChannelInterface wci =
						// c.getChannelServer().getWorldInterface();
						MapleParty party = c.getPlayer().getParty();
						party = c.getPlayer().getParty();// .getParty(partyid);
						if (party != null) {
							if (party.getMembers().size() < 6) {
								// MaplePartyCharacter partyplayer = tchar.getMPC();
								// wci.updateParty(party.getId(),
								// PartyOperation.JOIN, partyplayer);
								c.getPlayer().receivePartyMemberHP();
								c.getPlayer().updatePartyMemberHP();
							} else {
								c.announce(PacketCreator.partyStatusMessage(17));
							}
						}
					}
				}
			}
		}
	}

	private static boolean isValidJob(MapleJob thejob, int jobs) {
		int jobid = thejob.getId();
		if (jobid == 0) {
			return ((jobs & 2) > 0);
		} else if (jobid == 100) {
			return ((jobs & 4) > 0);
		} else if (jobid > 100 && jobid < 113) {
			return ((jobs & 8) > 0);
		} else if (jobid > 110 && jobid < 123) {
			return ((jobs & 16) > 0);
		} else if (jobid > 120 && jobid < 133) {
			return ((jobs & 32) > 0);
		} else if (jobid == 200) {
			return ((jobs & 64) > 0);
		} else if (jobid > 209 && jobid < 213) {
			return ((jobs & 128) > 0);
		} else if (jobid > 219 && jobid < 223) {
			return ((jobs & 256) > 0);
		} else if (jobid > 229 && jobid < 233) {
			return ((jobs & 512) > 0);
		} else if (jobid == 500) {
			return ((jobs & 1024) > 0);
		} else if (jobid > 509 && jobid < 513) {
			return ((jobs & 2048) > 0);
		} else if (jobid > 519 && jobid < 523) {
			return ((jobs & 4096) > 0);
		} else if (jobid == 400) {
			return ((jobs & 8192) > 0);
		} else if (jobid > 400 && jobid < 413) {
			return ((jobs & 16384) > 0);
		} else if (jobid > 419 && jobid < 423) {
			return ((jobs & 32768) > 0);
		} else if (jobid == 300) {
			return ((jobs & 65536) > 0);
		} else if (jobid > 300 && jobid < 313) {
			return ((jobs & 131072) > 0);
		} else if (jobid > 319 && jobid < 323) {
			return ((jobs & 262144) > 0);
		}
		return false;
	}
}