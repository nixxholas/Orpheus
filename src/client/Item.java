/*
 	OrpheusMS: MapleStory Private Server based on OdinMS
    Copyright (C) 2012 Aaron Weiss

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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

// TODO: I've written this rant somewhere before...
// This class is bad. It does not represent an item. It represents an item container slot.
// For some weird reason (read: people are idiots) it is used for /everything/ item-related.
// Including drops on the ground. Which don't have a position.
public class Item implements IItem {

	private int itemId, cashId, sn;
	
	private byte flag;
	private long expiration = -1;

	private byte position;
	private short quantity;
	private int petId = -1;
	
	private String giftFrom = "";
	private String owner = "";
	
	protected List<String> log;
	
	public Item(int itemId, byte position, short quantity) {
		this.itemId = itemId;
		this.position = position;
		this.quantity = quantity;
		this.flag = 0;

		this.log = new LinkedList<String>();
	}

	public Item(int itemId, byte position, short quantity, int petid) {
		this.itemId = itemId;
		this.position = position;
		this.quantity = quantity;
		this.flag = 0;
		
		this.log = new LinkedList<String>();
	}

	@Override
	public IItem copy() {
		Item ret = new Item(itemId, position, quantity, petId);
		ret.flag = flag;
		ret.owner = owner;
		ret.expiration = expiration;
		ret.log = new LinkedList<String>(log);
		
		return ret;
	}

	@Override
	public void setPosition(byte position) {
		this.position = position;
	}

	@Override
	public void setQuantity(short quantity) {
		this.quantity = quantity;
	}

	@Override
	public int getItemId() {
		return itemId;
	}

	@Override
	public int getCashId() {
		if (cashId == 0) {
			// TODO: ... what!
			cashId = new Random().nextInt(Integer.MAX_VALUE) + 1;
		}
		return cashId;
	}

	@Override
	public byte getPosition() {
		return position;
	}

	@Override
	public short getQuantity() {
		return quantity;
	}

	@Override
	public byte getType() {
		if (this.getPetId() > -1) {
			return IItem.PET;
		} else {
			return IItem.ITEM;
		}
	}

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public int getPetId() {
		return petId;
	}

	@Override
	public void setPetId(int id) {
		this.petId = id;
	}

	@Override
	public int compareTo(IItem other) {
		if (this.itemId < other.getItemId()) {
			return -1;
		} else if (this.itemId > other.getItemId()) {
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "Item: " + itemId + " quantity: " + quantity;
	}

	public List<String> getLog() {
		return Collections.unmodifiableList(log);
	}

	@Override
	public byte getFlag() {
		return flag;
	}

	@Override
	public void setFlag(byte b) {
		this.flag = b;
	}

	@Override
	public long getExpiration() {
		return expiration;
	}

	@Override
	public void setExpiration(long expire) {
		this.expiration = expire;
	}

	@Override
	public int getSN() {
		return sn;
	}

	@Override
	public void setSN(int sn) {
		this.sn = sn;
	}

	@Override
	public String getGiftFrom() {
		return giftFrom;
	}

	@Override
	public void setGiftFrom(String giftFrom) {
		this.giftFrom = giftFrom;
	}
}
