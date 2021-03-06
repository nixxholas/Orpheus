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

import client.GameCharacter;
import client.GameClient;
import client.InventoryType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import server.Portal;
import server.maps.GameMap;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ChangeMapHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		GameCharacter chr = c.getPlayer();

		if (chr.isBanned()) {
			return;
		}

		if (reader.available() == 0) { 
			// Cash Shop :)
			String[] socket = c.getChannelServer().getIP().split(":");
			chr.saveToDb(true);
			chr.getCashShop().open(false);
			c.getChannelServer().removePlayer(chr);
			c.updateLoginState(GameClient.State.TRANSITION);
			try {
				c.announce(PacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
			} catch (UnknownHostException ex) {
			}
		} else {
			try {				
				// 1 = from dying 
				// 0 = regular portals
				reader.readByte();
				
				int targetid = reader.readInt();
				String startwp = reader.readMapleAsciiString();
				Portal portal = chr.getMap().getPortal(startwp);
				reader.readByte();
				boolean wheel = reader.readShort() > 0;
				if (targetid != -1 && !chr.isAlive()) {
					boolean executeStandardPath = true;
					if (chr.getEventInstance() != null) {
						executeStandardPath = chr.getEventInstance().revivePlayer(chr);
					}
					if (executeStandardPath) {
						GameMap to = chr.getMap();
						if (wheel && chr.getItemQuantity(5510000, false) > 0) {
							InventoryManipulator.removeById(c, InventoryType.CASH, 5510000, 1, true, false);
							chr.announce(PacketCreator.showWheelsLeft(chr.getItemQuantity(5510000, false)));
						} else {
							chr.cancelAllBuffs(false);
							to = chr.getMap().getReturnMap();
							chr.setStance(0);
						}
						chr.setHp(50);
						chr.changeMap(to, to.getPortal(0));
					}
				} else if (targetid != -1 && chr.isGM()) {
					GameMap to = c.getChannelServer().getMapFactory().getMap(targetid);
					chr.changeMap(to, to.getPortal(0));
				} else if (targetid != -1 && !chr.isGM()) {
					// Thanks celino for saving me some time (:
					final int divi = chr.getMapId() / 100;
					boolean warp = false;
					if (divi == 0) {
						if (targetid == 10000) {
							warp = true;
						}
					} else if (divi == 20100) {
						if (targetid == 104000000) {
							warp = true;
						}
					} else if (divi == 9130401) {
						// Only allow warp if player is already in Intro map, or else = hack
						if (targetid == 130000000 || targetid / 100 == 9130401) {
							// Cygnus introduction
							warp = true;
						}
					} else if (divi == 9140900) {
						// Aran Introduction
						if (targetid == 914090011 || targetid == 914090012
								|| targetid == 914090013
								|| targetid == 140090000) {
							warp = true;
						}
					} else if (divi / 10 == 1020) { 
						// Adventurer movie clip Intro
						if (targetid == 1020000) {
							warp = true;
						}
					}
					if (warp) {
						final GameMap to = c.getChannelServer().getMapFactory().getMap(targetid);
						chr.changeMap(to, to.getPortal(0));
					}
				}
				if (portal != null && !portal.getPortalStatus()) {
					c.announce(PacketCreator.blockedMessage(1));
					c.announce(PacketCreator.enableActions());
					return;
				}
				if (chr.getMapId() == 109040004) {
					chr.getFitness().resetTimes();
				}
				if (chr.getMapId() == 109030003 || chr.getMapId() == 109030103) {
					chr.getOla().resetTimes();
				}
				if (portal != null) {
					portal.enterPortal(c);
				} else {
					c.announce(PacketCreator.enableActions());
				}
				chr.refreshRates();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
