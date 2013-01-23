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

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Matze
 */
public class ShopFactory {
	private Map<Integer, Shop> shops = new HashMap<Integer, Shop>();
	private Map<Integer, Shop> npcShops = new HashMap<Integer, Shop>();
	private static ShopFactory instance = new ShopFactory();

	public static ShopFactory getInstance() {
		return instance;
	}

	public void reloadShops() {
		shops.clear();
	}

	private Shop loadShop(int id, boolean isShopId) {
		Shop ret = Shop.createFromDB(id, isShopId);
		if (ret != null) {
			shops.put(ret.getId(), ret);
			npcShops.put(ret.getNpcId(), ret);
		} else if (isShopId) {
			shops.put(id, null);
		} else {
			npcShops.put(id, null);
		}
		return ret;
	}

	public Shop getShop(int shopId) {
		if (shops.containsKey(shopId)) {
			return shops.get(shopId);
		}
		return loadShop(shopId, true);
	}

	public Shop getShopForNpc(int npcId) {
		if (npcShops.containsKey(npcId)) {
			npcShops.get(npcId);
		}
		return loadShop(npcId, false);
	}
}
