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
package server.life;

import client.GameClient;
import server.MapleShopFactory;
import server.maps.GameMapObjectType;
import tools.PacketCreator;

public class MapleNPC extends AbstractLoadedMapleLife {
	private MapleNPCStats stats;

	public MapleNPC(int id, MapleNPCStats stats) {
		super(id);
		this.stats = stats;
	}

	public boolean hasShop() {
		return MapleShopFactory.getInstance().getShopForNPC(getId()) != null;
	}

	public void sendShop(GameClient c) {
		MapleShopFactory.getInstance().getShopForNPC(getId()).sendShop(c);
	}

	@Override
	public void sendSpawnData(GameClient client) {
		if (!this.isHidden()) {
			if (this.getId() > 9010010 && this.getId() < 9010014) {
				client.getSession().write(PacketCreator.spawnNPCRequestController(this, false));
			} else {
				client.getSession().write(PacketCreator.spawnNPC(this));
				client.getSession().write(PacketCreator.spawnNPCRequestController(this, true));
			}
		}
	}

	@Override
	public void sendDestroyData(GameClient client) {
		client.getSession().write(PacketCreator.removeNPC(getObjectId()));
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.NPC;
	}

	public String getName() {
		return stats.getName();
	}
}
