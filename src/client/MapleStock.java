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
package client;

/**
 * @author Aaron Weiss
 */
public class MapleStock {
	private String name;
	private String ticker;
	private StockEntry entry;

	public MapleStock(String ticker, int count, int value, int change) {
		this(null, ticker, new StockEntry(count, value, change));
	}
	
	public MapleStock(String name, String ticker, int count, int value, int change) {
		this(name, ticker, new StockEntry(count, value, change));
	}
	
	public MapleStock(String name, String ticker, StockEntry entry) {
		if (name != null) {
			this.name = name;
		} else {
			this.name = "Unknown";
		}
		this.ticker = ticker;
		this.entry = entry;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTicker() {
		return ticker;
	}
	
	public int getCount() {
		return entry.count;
	}
	
	public int getValue() {
		return entry.value;
	}
	
	public int getChange() {
		return entry.change;
	}
	
	public void update(int change) {
		this.update(new StockEntry(this.getCount(), this.getValue(), change));
	}
	
	public void update(int value, int change) {
		this.update(new StockEntry(this.getCount(), value, change));
	}
	
	public void update(int count, int value, int change) {
		this.update(new StockEntry(count, value, change));
	}
	
	public void update(StockEntry entry) {
		this.entry = entry;
	}
	
	public boolean equals(Object o) {
		if (this.getClass() != o.getClass()) return false;
		if (this == (MapleStock) o) return true;
		MapleStock c = (MapleStock) o;
		return (c.getName() == this.getName() && c.getTicker() == this.getTicker());
	}
}