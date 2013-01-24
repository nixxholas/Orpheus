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

import java.util.Calendar;
import client.IItem;

/**
 * 
 * @author Traitor
 */
public class MtsItemInfo {
	private int price;
	private IItem item;
	private String seller;
	private int id;
	private int year, month, day = 1;

	public MtsItemInfo(IItem item, int price, int id, int cid, String seller, String date) {
		this.item = item;
		this.price = price;
		this.seller = seller;
		this.id = id;
		this.year = Integer.parseInt(date.substring(0, 4));
		this.month = Integer.parseInt(date.substring(5, 7));
		this.day = Integer.parseInt(date.substring(8, 10));
	}

	public IItem getItem() {
		return item;
	}

	public int getPrice() {
		return price;
	}

	public int getTaxes() {
		return 100 + price / 10;
	}

	public int getID() {
		return id;
	}

	public long getEndingDate() {
		Calendar now = Calendar.getInstance();
		now.set(year, month - 1, day);
		return now.getTimeInMillis();
	}

	public String getSeller() {
		return seller;
	}
}
