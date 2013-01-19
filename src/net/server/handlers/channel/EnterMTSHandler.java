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

import client.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import constants.ServerConstants;
import net.AbstractPacketHandler;
import net.server.Server;
import server.MTSItemInfo;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.DatabaseConnection;

public final class EnterMTSHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter chr = c.getPlayer();
		if (!chr.isAlive()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		if (chr.getLevel() < 10) {
			c.announce(PacketCreator.blockedMessage2(5));
			c.announce(PacketCreator.enableActions());
			return;
		}
		if (ServerConstants.USE_MTS_AS_FM_WARP) {
			chr.changeMap(910000000);
		} else {
			Server.getInstance().getPlayerBuffStorage().addBuffsToStorage(chr.getId(), chr.getAllBuffs());
			chr.cancelExpirationTask();
			chr.saveToDB(true);
			chr.getMap().removePlayer(c.getPlayer());
			try {
				c.announce(PacketCreator.openCashShop(c, true));
			} catch (Exception ex) {
			}
			chr.getCashShop().open(true);// xD
			c.announce(PacketCreator.enableCSUse());
			c.announce(PacketCreator.MTSWantedListingOver(0, 0));
			c.announce(PacketCreator.showMTSCash(c.getPlayer()));
			List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
			int pages = 0;
			try {
				PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM mts_items WHERE tab = 1 AND transfer = 0 ORDER BY id DESC LIMIT 16, 16");
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					if (rs.getInt("type") != 1) {
						Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
						i.setOwner(rs.getString("owner"));
						items.add(new MTSItemInfo(i, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
					} else {
						Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
						equip.setOwner(rs.getString("owner"));
						equip.setQuantity((short) 1);
						equip.setAcc((short) rs.getInt("acc"));
						equip.setAvoid((short) rs.getInt("avoid"));
						equip.setDex((short) rs.getInt("dex"));
						equip.setHands((short) rs.getInt("hands"));
						equip.setHp((short) rs.getInt("hp"));
						equip.setInt((short) rs.getInt("int"));
						equip.setJump((short) rs.getInt("jump"));
						equip.setVicious((short) rs.getInt("vicious"));
						equip.setFlag((byte) rs.getInt("flag"));
						equip.setLuk((short) rs.getInt("luk"));
						equip.setMatk((short) rs.getInt("matk"));
						equip.setMdef((short) rs.getInt("mdef"));
						equip.setMp((short) rs.getInt("mp"));
						equip.setSpeed((short) rs.getInt("speed"));
						equip.setStr((short) rs.getInt("str"));
						equip.setWatk((short) rs.getInt("watk"));
						equip.setWdef((short) rs.getInt("wdef"));
						equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
						equip.setLevel((byte) rs.getInt("level"));
						items.add(new MTSItemInfo((IItem) equip, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
					}
				}
				rs.close();
				ps.close();
				ps = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM mts_items");
				rs = ps.executeQuery();
				if (rs.next()) {
					pages = (int) Math.ceil(rs.getInt(1) / 16);
				}
				rs.close();
				ps.close();
			} catch (SQLException e) {
			}
			c.announce(PacketCreator.sendMts(items, 1, 0, 0, pages));
			c.announce(PacketCreator.transferInventory(getTransfer(chr.getId())));
			c.announce(PacketCreator.notYetSoldInv(getNotYetSold(chr.getId())));
		}
	}

	private List<MTSItemInfo> getNotYetSold(int cid) {
		List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC");
			ps.setInt(1, cid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getInt("type") != 1) {
					Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
					i.setOwner(rs.getString("owner"));
					items.add(new MTSItemInfo((IItem) i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
				} else {
					Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
					equip.setOwner(rs.getString("owner"));
					equip.setQuantity((short) 1);
					equip.setAcc((short) rs.getInt("acc"));
					equip.setAvoid((short) rs.getInt("avoid"));
					equip.setDex((short) rs.getInt("dex"));
					equip.setHands((short) rs.getInt("hands"));
					equip.setHp((short) rs.getInt("hp"));
					equip.setInt((short) rs.getInt("int"));
					equip.setJump((short) rs.getInt("jump"));
					equip.setVicious((short) rs.getInt("vicious"));
					equip.setLuk((short) rs.getInt("luk"));
					equip.setMatk((short) rs.getInt("matk"));
					equip.setMdef((short) rs.getInt("mdef"));
					equip.setMp((short) rs.getInt("mp"));
					equip.setSpeed((short) rs.getInt("speed"));
					equip.setStr((short) rs.getInt("str"));
					equip.setWatk((short) rs.getInt("watk"));
					equip.setWdef((short) rs.getInt("wdef"));
					equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
					equip.setLevel((byte) rs.getInt("level"));
					equip.setFlag((byte) rs.getInt("flag"));
					items.add(new MTSItemInfo((IItem) equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
		}
		return items;
	}

	private List<MTSItemInfo> getTransfer(int cid) {
		List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC");
			ps.setInt(1, cid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (rs.getInt("type") != 1) {
					Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
					i.setOwner(rs.getString("owner"));
					items.add(new MTSItemInfo((IItem) i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
				} else {
					Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
					equip.setOwner(rs.getString("owner"));
					equip.setQuantity((short) 1);
					equip.setAcc((short) rs.getInt("acc"));
					equip.setAvoid((short) rs.getInt("avoid"));
					equip.setDex((short) rs.getInt("dex"));
					equip.setHands((short) rs.getInt("hands"));
					equip.setHp((short) rs.getInt("hp"));
					equip.setInt((short) rs.getInt("int"));
					equip.setJump((short) rs.getInt("jump"));
					equip.setVicious((short) rs.getInt("vicious"));
					equip.setLuk((short) rs.getInt("luk"));
					equip.setMatk((short) rs.getInt("matk"));
					equip.setMdef((short) rs.getInt("mdef"));
					equip.setMp((short) rs.getInt("mp"));
					equip.setSpeed((short) rs.getInt("speed"));
					equip.setStr((short) rs.getInt("str"));
					equip.setWatk((short) rs.getInt("watk"));
					equip.setWdef((short) rs.getInt("wdef"));
					equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
					equip.setLevel((byte) rs.getInt("level"));
					equip.setFlag((byte) rs.getInt("flag"));
					items.add(new MTSItemInfo((IItem) equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
		}
		return items;
	}
}