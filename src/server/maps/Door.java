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
package server.maps;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import server.Portal;
import tools.PacketCreator;
import client.GameCharacter;
import client.GameClient;

/**
 * 
 * @author Matze
 */
public class Door extends AbstractGameMapObject {
	private GameCharacter owner;
	private GameMap town;
	private Portal townPortal;
	private GameMap target;
	private Point targetPosition;

	public Door(GameCharacter owner, Point targetPosition) {
		super();
		this.owner = owner;
		this.target = owner.getMap();
		this.targetPosition = targetPosition;
		setPosition(this.targetPosition);
		this.town = this.target.getReturnMap();
		this.townPortal = getFreePortal();
	}

	public Door(Door origDoor) {
		super();
		this.owner = origDoor.owner;
		this.town = origDoor.town;
		this.townPortal = origDoor.townPortal;
		this.target = origDoor.target;
		this.targetPosition = origDoor.targetPosition;
		this.townPortal = origDoor.townPortal;
		setPosition(this.townPortal.getPosition());
	}

	private Portal getFreePortal() {
		List<Portal> freePortals = new ArrayList<Portal>();
		for (Portal port : town.getPortals()) {
			if (port.getType() == 6) {
				freePortals.add(port);
			}
		}
		Collections.sort(freePortals, new Comparator<Portal>() {
			public int compare(Portal o1, Portal o2) {
				if (o1.getId() < o2.getId()) {
					return -1;
				} else if (o1.getId() == o2.getId()) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		for (GameMapObject obj : town.getMapObjects()) {
			if (obj instanceof Door) {
				Door door = (Door) obj;
				if (door.getOwner().getParty() != null && owner.getParty().containsMembers(door.getOwner().getPartyCharacter())) {
					freePortals.remove(door.getTownPortal());
				}
			}
		}
		return freePortals.iterator().next();
	}

	public void sendSpawnData(GameClient client) {
		if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer() && owner.getParty() == null) {
			client.getSession().write(PacketCreator.spawnDoor(owner.getId(), town.getId() == client.getPlayer().getMapId() ? townPortal.getPosition() : targetPosition, true));
			if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(client.getPlayer().getPartyCharacter()))) {
				client.getSession().write(PacketCreator.partyPortal(town.getId(), target.getId(), targetPosition));
			}
			client.getSession().write(PacketCreator.spawnPortal(town.getId(), target.getId(), targetPosition));
		}
	}

	public void sendDestroyData(GameClient client) {
		if (target.getId() == client.getPlayer().getMapId() || owner == client.getPlayer() || owner.getParty() != null && owner.getParty().containsMembers(client.getPlayer().getPartyCharacter())) {
			if (owner.getParty() != null && (owner == client.getPlayer() || owner.getParty().containsMembers(client.getPlayer().getPartyCharacter()))) {
				client.getSession().write(PacketCreator.partyPortal(999999999, 999999999, new Point(-1, -1)));
			}
			client.getSession().write(PacketCreator.removeDoor(owner.getId(), false));
			client.getSession().write(PacketCreator.removeDoor(owner.getId(), true));
		}
	}

	public void warp(GameCharacter chr, boolean toTown) {
		if (chr == owner || owner.getParty() != null && owner.getParty().containsMembers(chr.getPartyCharacter())) {
			if (!toTown) {
				chr.changeMap(target, targetPosition);
			} else {
				chr.changeMap(town, townPortal);
			}
		} else {
			chr.getClient().getSession().write(PacketCreator.enableActions());
		}
	}

	public GameCharacter getOwner() {
		return owner;
	}

	public GameMap getTown() {
		return town;
	}

	public Portal getTownPortal() {
		return townPortal;
	}

	public GameMap getTarget() {
		return target;
	}

	public Point getTargetPosition() {
		return targetPosition;
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.DOOR;
	}
}
