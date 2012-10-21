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

import client.Equip;
import client.IItem;
import client.Item;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleInventoryType;
import tools.DatabaseConnection;
import net.AbstractMaplePacketHandler;
import net.MaplePacket;
import net.server.Channel;
import server.ItemNameEntry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MTSItemInfo;
import tools.MaplePacketCreator;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import net.server.Server;

public final class MTSHandler extends AbstractMaplePacketHandler {
	
	private static enum MtsOperationType {
		INITIATE_SALE(2),
		SEND_OFFER(3),
		LIST_WANTED_ITEM(4),
		SEARCH(5),
		CHANGE_PAGE(6),
		CANCEL_SALE(7),
		TRANSFER_ITEM(8),
		ADD_TO_CART(9),
		REMOVE_FROM_CART(10),
		INITIATE_ITEM_AUCTION(12),
		CANCEL_WANTED_ITEM(13),
		BUY_AUCTION_ITEM(14),
		BUY_ITEM(16),
		BUY_ITEM_FROM_CART(17);
		
		private final byte value;
		
		private MtsOperationType(int value) { 
			this.value = (byte)value;
		}
		
		public static MtsOperationType fromByte(byte b) {
			switch(b) {
			case 2:
				return INITIATE_SALE;
			case 3:
				return SEND_OFFER;
			case 4:
				return LIST_WANTED_ITEM;
			case 5:
				return SEARCH;
			case 6:
				return CHANGE_PAGE;
			case 7:
				return CANCEL_SALE;
			case 8:
				return TRANSFER_ITEM;
			case 9:
				return ADD_TO_CART;
			case 10:
				return REMOVE_FROM_CART;
			case 12:
				return INITIATE_ITEM_AUCTION;
			case 13:
				return CANCEL_WANTED_ITEM;
			case 14:
				return BUY_AUCTION_ITEM;
			case 16:
				return BUY_ITEM;
			case 17:
				return BUY_ITEM_FROM_CART;
				
			default:
				return null;
			}
		}
		
		@SuppressWarnings("unused")
		public byte asByte() {
			return this.value;
		}
	}
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
		if (!c.getPlayer().getCashShop().isOpened()) {
			return;
		}
		if (slea.available() > 0) {
			final byte typeByte = slea.readByte();
			final MtsOperationType operationType = MtsOperationType.fromByte(typeByte);
			switch (operationType) {
			case INITIATE_SALE:
				doInitiateSale(slea, c);
				break;
			case SEND_OFFER:
				break;
			case LIST_WANTED_ITEM:
				doListWantedItem(slea);
				break;
			case CHANGE_PAGE:
				doChangePage(slea, c);
				break;
			case SEARCH:
				doSearch(slea, c);
				break;
			case CANCEL_SALE:
				doCancelSale(slea, c);
				break;
			case TRANSFER_ITEM:
				doTransferItem(slea, c);
				break;
			case ADD_TO_CART:
				doAddToCart(slea, c);
				break;
			case REMOVE_FROM_CART:
				doRemoveFromCart(slea, c);
				break;
			case INITIATE_ITEM_AUCTION:
				break;
			case CANCEL_WANTED_ITEM:
				break;
			case BUY_AUCTION_ITEM:
				break;
			case BUY_ITEM:
				doBuyItem(slea, c);
				break;
			case BUY_ITEM_FROM_CART:
				doBuyItemFromCart(slea, c);
				break;
			default:
				System.out.println("Unhandled OP(MTS): " + typeByte + " Packet: " + slea.toString());
				break;
			}
		} else {
			c.announce(MaplePacketCreator.showMTSCash(c.getPlayer()));
		}
	}

	private void doBuyItemFromCart(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		int id = slea.readInt(); // id of the item
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		try {
			ps = con.prepareStatement("SELECT * FROM mts_items WHERE id = ? ORDER BY id DESC");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next()) {
				int price = rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1);
				if (c.getPlayer().getCashShop().getCash(4) >= price) {
					for (Channel cserv : Server.getInstance().getAllChannels()) {
						MapleCharacter victim = cserv.getPlayerStorage().getCharacterById(rs.getInt("seller"));
						if (victim != null) {
							victim.getCashShop().gainCash(4, rs.getInt("price"));
						} else {
							PreparedStatement pse = con.prepareStatement("SELECT accountid FROM characters WHERE id = ?");
							pse.setInt(1, rs.getInt("seller"));
							ResultSet rse = pse.executeQuery();
							if (rse.next()) {
								PreparedStatement psee = con.prepareStatement("UPDATE accounts SET nxPrepaid = nxPrepaid + ? WHERE id = ?");
								psee.setInt(1, rs.getInt("price"));
								psee.setInt(2, rse.getInt("accountid"));
								psee.executeUpdate();
								psee.close();
							}
							pse.close();
							rse.close();
						}
					}
					PreparedStatement pse = con.prepareStatement("UPDATE mts_items SET seller = ?, transfer = 1 WHERE id = ?");
					pse.setInt(1, c.getPlayer().getId());
					pse.setInt(2, id);
					pse.executeUpdate();
					pse.close();
					pse = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ?");
					pse.setInt(1, id);
					pse.executeUpdate();
					pse.close();
					c.getPlayer().getCashShop().gainCash(4, -price);
					c.announce(getCart(c.getPlayer().getId()));
					c.announce(MaplePacketCreator.enableCSUse());
					c.announce(MaplePacketCreator.MTSConfirmBuy());
					c.announce(MaplePacketCreator.showMTSCash(c.getPlayer()));
					c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
					c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
				} else {
					c.announce(MaplePacketCreator.MTSFailBuy());
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			c.announce(MaplePacketCreator.MTSFailBuy());
		}
	}

	private void doBuyItem(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		int id = slea.readInt(); // id of the item
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		try {
			ps = con.prepareStatement("SELECT * FROM mts_items WHERE id = ? ORDER BY id DESC");
			ps.setInt(1, id);
			rs = ps.executeQuery();
			if (rs.next()) {
				int price = rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1); // taxes
				if (c.getPlayer().getCashShop().getCash(4) >= price) { // FIX
					boolean alwaysnull = true;
					for (Channel cserv : Server.getInstance().getAllChannels()) {
						MapleCharacter victim = cserv.getPlayerStorage().getCharacterById(rs.getInt("seller"));
						if (victim != null) {
							victim.getCashShop().gainCash(4, rs.getInt("price"));
							alwaysnull = false;
						}
					}
					if (alwaysnull) {
						PreparedStatement pse = con.prepareStatement("SELECT accountid FROM characters WHERE id = ?");
						pse.setInt(1, rs.getInt("seller"));
						ResultSet rse = pse.executeQuery();
						if (rse.next()) {
							PreparedStatement psee = con.prepareStatement("UPDATE accounts SET nxPrepaid = nxPrepaid + ? WHERE id = ?");
							psee.setInt(1, rs.getInt("price"));
							psee.setInt(2, rse.getInt("accountid"));
							psee.executeUpdate();
							psee.close();
						}
						pse.close();
						rse.close();
					}
					PreparedStatement pse = con.prepareStatement("UPDATE mts_items SET seller = ?, transfer = 1 WHERE id = ?");
					pse.setInt(1, c.getPlayer().getId());
					pse.setInt(2, id);
					pse.executeUpdate();
					pse.close();
					pse = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ?");
					pse.setInt(1, id);
					pse.executeUpdate();
					pse.close();
					c.getPlayer().getCashShop().gainCash(4, -price);
					c.announce(MaplePacketCreator.enableCSUse());
					c.announce(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
					c.announce(MaplePacketCreator.MTSConfirmBuy());
					c.announce(MaplePacketCreator.showMTSCash(c.getPlayer()));
					c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
					c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
					c.announce(MaplePacketCreator.enableActions());
				} else {
					c.announce(MaplePacketCreator.MTSFailBuy());
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			c.announce(MaplePacketCreator.MTSFailBuy());
		}
	}

	private void doRemoveFromCart(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		int id = slea.readInt(); // id of the item
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ? AND cid = ?");
			ps.setInt(1, id);
			ps.setInt(2, c.getPlayer().getId());
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		c.announce(getCart(c.getPlayer().getId()));
		c.announce(MaplePacketCreator.enableCSUse());
		c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
		c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
	}

	private void doAddToCart(SeekableLittleEndianAccessor slea, MapleClient c) {
		int id = slea.readInt(); // id of the item
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps1 = con.prepareStatement("SELECT * FROM mts_items WHERE id = ? AND seller <> ?");
			ps1.setInt(1, id);// Previene que agregues al cart tus
								// propios items
			ps1.setInt(2, c.getPlayer().getId());
			ResultSet rs1 = ps1.executeQuery();
			if (rs1.next()) {
				PreparedStatement ps = con.prepareStatement("SELECT * FROM mts_cart WHERE cid = ? AND itemid = ?");
				ps.setInt(1, c.getPlayer().getId());
				ps.setInt(2, id);
				ResultSet rs = ps.executeQuery();
				if (!rs.next()) {
					PreparedStatement pse = con.prepareStatement("INSERT INTO mts_cart (cid, itemid) VALUES (?, ?)");
					pse.setInt(1, c.getPlayer().getId());
					pse.setInt(2, id);
					pse.executeUpdate();
					pse.close();
				}
				rs.close();
			}
			rs1.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		c.announce(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
		c.announce(MaplePacketCreator.enableCSUse());
		c.announce(MaplePacketCreator.enableActions());
		c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
		c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
	}

	private void doTransferItem(SeekableLittleEndianAccessor slea, MapleClient c) {
		int id = slea.readInt(); // id of the item
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		try {
			ps = con.prepareStatement("SELECT * FROM mts_items WHERE seller = ? AND transfer = 1  AND id= ? ORDER BY id DESC");
			ps.setInt(1, c.getPlayer().getId());
			ps.setInt(2, id);
			rs = ps.executeQuery();
			if (rs.next()) {
				IItem i;
				if (rs.getInt("type") != 1) {
					Item ii = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
					ii.setOwner(rs.getString("owner"));
					ii.setPosition(c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(rs.getInt("itemid"))).getNextFreeSlot());
					i = ii.copy();
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
					equip.setVicious((byte) rs.getInt("vicious"));
					equip.setFlag((byte) rs.getInt("flag"));
					equip.setPosition(c.getPlayer().getInventory(MapleItemInformationProvider.getInstance().getInventoryType(rs.getInt("itemid"))).getNextFreeSlot());
					i = equip.copy();
				}
				PreparedStatement pse = con.prepareStatement("DELETE FROM mts_items WHERE id = ? AND seller = ? AND transfer = 1");
				pse.setInt(1, id);
				pse.setInt(2, c.getPlayer().getId());
				pse.executeUpdate();
				pse.close();
				MapleInventoryManipulator.addFromDrop(c, i, false);
				c.announce(MaplePacketCreator.enableCSUse());
				c.announce(getCart(c.getPlayer().getId()));
				c.announce(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
				c.announce(MaplePacketCreator.MTSConfirmTransfer(i.getQuantity(), i.getPosition()));
				c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			Output.print("MTS Transfer error: " + e);
		}
	}

	private void doCancelSale(SeekableLittleEndianAccessor slea, MapleClient c) {
		int id = slea.readInt(); // id of the item
		Connection con = DatabaseConnection.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("UPDATE mts_items SET transfer = 1 WHERE id = ? AND seller = ?");
			ps.setInt(1, id);
			ps.setInt(2, c.getPlayer().getId());
			ps.executeUpdate();
			ps.close();
			ps = con.prepareStatement("DELETE FROM mts_cart WHERE itemid = ?");
			ps.setInt(1, id);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		c.announce(MaplePacketCreator.enableCSUse());
		c.announce(getMTS(c.getPlayer().getCurrentTab(), c.getPlayer().getCurrentType(), c.getPlayer().getCurrentPage()));
		c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
		c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
	}

	private void doSearch(SeekableLittleEndianAccessor slea, MapleClient c) {
		int tab = slea.readInt();
		int type = slea.readInt();
		slea.readInt();
		int ci = slea.readInt();
		String search = slea.readMapleAsciiString();
		c.getPlayer().setSearch(search);
		c.getPlayer().changeTab(tab);
		c.getPlayer().changeType(type);
		c.getPlayer().changeCI(ci);
		c.announce(MaplePacketCreator.enableCSUse());
		c.announce(MaplePacketCreator.enableActions());
		c.announce(getMTSSearch(tab, type, ci, search, c.getPlayer().getCurrentPage()));
		c.announce(MaplePacketCreator.showMTSCash(c.getPlayer()));
		c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
		c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
	}

	private void doChangePage(SeekableLittleEndianAccessor slea, MapleClient c) {
		int tab = slea.readInt();
		int type = slea.readInt();
		int page = slea.readInt();
		c.getPlayer().changePage(page);
		if (tab == 4 && type == 0) {
			c.announce(getCart(c.getPlayer().getId()));
		} else if (tab == c.getPlayer().getCurrentTab() && type == c.getPlayer().getCurrentType() && c.getPlayer().getSearch() != null) {
			c.announce(getMTSSearch(tab, type, c.getPlayer().getCurrentCI(), c.getPlayer().getSearch(), page));
		} else {
			c.getPlayer().setSearch(null);
			c.announce(getMTS(tab, type, page));
		}
		c.getPlayer().changeTab(tab);
		c.getPlayer().changeType(type);
		c.announce(MaplePacketCreator.enableCSUse());
		c.announce(MaplePacketCreator.transferInventory(getTransfer(c.getPlayer().getId())));
		c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(c.getPlayer().getId())));
	}

	private void doListWantedItem(SeekableLittleEndianAccessor slea) {
		slea.readInt();
		slea.readInt();
		slea.readInt();
		slea.readShort();
		slea.readMapleAsciiString();
	}

	private void doInitiateSale(SeekableLittleEndianAccessor slea, MapleClient c) {
		byte itemtype = slea.readByte();
		int itemid = slea.readInt();
		slea.readShort();
		slea.skip(7);
		short stars = 1;
		if (itemtype == 1) {
			slea.skip(32);
		} else {
			stars = slea.readShort();
		}
		slea.readMapleAsciiString(); // another useless thing (owner)
		if (itemtype == 1) {
			slea.skip(32);
		} else {
			slea.readShort();
		}
		byte slot = 1;
		short quantity = 1;
		if (itemtype != 1) {
			if (itemid / 10000 == 207 || itemid / 10000 == 233) {
				slea.skip(8);
			}
			slot = (byte) slea.readInt();
		} else {
			slot = (byte) slea.readInt();
		}
		if (itemtype != 1) {
			if (itemid / 10000 == 207 || itemid / 10000 == 233) {
				quantity = stars;
				slea.skip(4);
			} else {
				quantity = (short) slea.readInt();
			}
		} else {
			quantity = (byte) slea.readInt();
		}
		int price = slea.readInt();
		if (itemtype == 1) {
			quantity = 1;
		}
		if (quantity < 0 || price < 110 || c.getPlayer().getItemQuantity(itemid, false) < quantity) {
			return;
		}
		MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
		IItem i = c.getPlayer().getInventory(type).getItem(slot).copy();
		if (i != null && c.getPlayer().getMeso() >= 5000) {
			Connection con = DatabaseConnection.getConnection();
			final int playerId = c.getPlayer().getId();
			try (PreparedStatement ps = getSelectItemsBySellerId(con, playerId);
					ResultSet rs = ps.executeQuery();) {
				
				if (rs.next()) {
					if (rs.getInt(1) > 10) { 
						// They have more than 10 items up for sale already!
						c.getPlayer().dropMessage(1, "You already have 10 items up for auction!");
						c.announce(getMTS(1, 0, 0));
						c.announce(MaplePacketCreator.transferInventory(getTransfer(playerId)));
						c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(playerId)));
						return;
					}
				}
			} catch (SQLException e) {
				return;
			}
			
			Calendar calendar = Calendar.getInstance();
			int year = 2008;
			int month = 6;
			int day = 17;
			int oldmax = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			int oldday = calendar.get(Calendar.DAY_OF_MONTH) + 7;
			if (oldmax < oldday) {
				if (calendar.get(Calendar.MONTH) + 2 > 12) {
					year = calendar.get(Calendar.YEAR) + 1;
					month = 1;
					calendar.set(year, month, 1);
					day = oldday - oldmax;
				} else {
					month = calendar.get(Calendar.MONTH) + 2;
					year = calendar.get(Calendar.YEAR);
					calendar.set(year, month, 1);
					day = oldday - oldmax;
				}
			} else {
				day = calendar.get(Calendar.DAY_OF_MONTH) + 7;
				month = calendar.get(Calendar.MONTH);
				year = calendar.get(Calendar.YEAR);
			}
			String date = year + "-";
			if (month < 10) {
				date += "0" + month + "-";
			} else {
				date += month + "-";
			}
			if (day < 10) {
				date += "0" + day;
			} else {
				date += day + "";
			}
			
			try(PreparedStatement ps = getInsertStatement(c, quantity, price, type, i, con, playerId, date);) {
				
				ps.executeUpdate();
				MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
				
			} catch (SQLException e) {
			}
			
			c.getPlayer().gainMeso(-5000, false);
			c.announce(MaplePacketCreator.MTSConfirmSell());
			c.announce(getMTS(1, 0, 0));
			c.announce(MaplePacketCreator.enableCSUse());
			c.announce(MaplePacketCreator.transferInventory(getTransfer(playerId)));
			c.announce(MaplePacketCreator.notYetSoldInv(getNotYetSold(playerId)));
		}
	}

	private PreparedStatement getInsertStatement(MapleClient c, short quantity, int price,
			MapleInventoryType type, IItem i, Connection con,
			final int playerId, String date) throws SQLException {
		if (i.getType() == 2) {
			return getInsertMtsItem(c, quantity, price, type, i, con, playerId, date);
		} else {
			return getInsertMtsEquip(c, quantity, price, type, i, con, playerId, date);
		}
	}

	private PreparedStatement getInsertMtsEquip(MapleClient c, short quantity,
			int price, MapleInventoryType type, IItem i, Connection con,
			final int playerId, String date) throws SQLException {
		PreparedStatement pse;
		Equip equip = (Equip) i;
		pse = con.prepareStatement("INSERT INTO mts_items (tab, type, itemid, quantity, seller, price, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, locked, owner, sellername, sell_ends, vicious, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		pse.setInt(1, 1);
		pse.setInt(2, (int) type.asByte());
		pse.setInt(3, equip.getItemId());
		pse.setInt(4, quantity);
		pse.setInt(5, playerId);
		pse.setInt(6, price);
		pse.setInt(7, equip.getUpgradeSlots());
		pse.setInt(8, equip.getLevel());
		pse.setInt(9, equip.getStr());
		pse.setInt(10, equip.getDex());
		pse.setInt(11, equip.getInt());
		pse.setInt(12, equip.getLuk());
		pse.setInt(13, equip.getHp());
		pse.setInt(14, equip.getMp());
		pse.setInt(15, equip.getWatk());
		pse.setInt(16, equip.getMatk());
		pse.setInt(17, equip.getWdef());
		pse.setInt(18, equip.getMdef());
		pse.setInt(19, equip.getAcc());
		pse.setInt(20, equip.getAvoid());
		pse.setInt(21, equip.getHands());
		pse.setInt(22, equip.getSpeed());
		pse.setInt(23, equip.getJump());
		pse.setInt(24, 0);
		pse.setString(25, equip.getOwner());
		pse.setString(26, c.getPlayer().getName());
		pse.setString(27, date);
		pse.setInt(28, equip.getVicious());
		pse.setInt(29, equip.getFlag());
		return pse;
	}

	private PreparedStatement getInsertMtsItem(MapleClient c, short quantity, int price,
			MapleInventoryType type, IItem i, Connection con,
			final int playerId, String date) throws SQLException {
		
		Item item = (Item) i;
		PreparedStatement pse = con.prepareStatement("INSERT INTO mts_items (tab, type, itemid, quantity, seller, price, owner, sellername, sell_ends) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		pse.setInt(1, 1);
		pse.setInt(2, (int) type.asByte());
		pse.setInt(3, item.getItemId());
		pse.setInt(4, quantity);
		pse.setInt(5, playerId);
		pse.setInt(6, price);
		pse.setString(7, item.getOwner());
		pse.setString(8, c.getPlayer().getName());
		pse.setString(9, date);
		
		return pse;
	}

	private PreparedStatement getSelectItemsBySellerId(Connection con,
			final int playerId) throws SQLException {
		PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items WHERE seller = ?");
		ps.setInt(1, playerId);
		return ps;
	}

	public List<MTSItemInfo> getNotYetSold(int cid) {
		List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		try {
			ps = con.prepareStatement("SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC");
			ps.setInt(1, cid);
			rs = ps.executeQuery();
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

	public MaplePacket getCart(int cid) {
		List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		int pages = 0;
		try {
			ps = con.prepareStatement("SELECT * FROM mts_cart WHERE cid = ? ORDER BY id DESC");
			ps.setInt(1, cid);
			rs = ps.executeQuery();
			while (rs.next()) {
				PreparedStatement pse = con.prepareStatement("SELECT * FROM mts_items WHERE id = ?");
				pse.setInt(1, rs.getInt("itemid"));
				ResultSet rse = pse.executeQuery();
				if (rse.next()) {
					if (rse.getInt("type") != 1) {
						Item i = new Item(rse.getInt("itemid"), (byte) 0, (short) rse.getInt("quantity"));
						i.setOwner(rse.getString("owner"));
						items.add(new MTSItemInfo((IItem) i, rse.getInt("price"), rse.getInt("id"), rse.getInt("seller"), rse.getString("sellername"), rse.getString("sell_ends")));
					} else {
						Equip equip = new Equip(rse.getInt("itemid"), (byte) rse.getInt("position"), -1);
						equip.setOwner(rse.getString("owner"));
						equip.setQuantity((short) 1);
						equip.setAcc((short) rse.getInt("acc"));
						equip.setAvoid((short) rse.getInt("avoid"));
						equip.setDex((short) rse.getInt("dex"));
						equip.setHands((short) rse.getInt("hands"));
						equip.setHp((short) rse.getInt("hp"));
						equip.setInt((short) rse.getInt("int"));
						equip.setJump((short) rse.getInt("jump"));
						equip.setVicious((short) rse.getInt("vicious"));
						equip.setLuk((short) rse.getInt("luk"));
						equip.setMatk((short) rse.getInt("matk"));
						equip.setMdef((short) rse.getInt("mdef"));
						equip.setMp((short) rse.getInt("mp"));
						equip.setSpeed((short) rse.getInt("speed"));
						equip.setStr((short) rse.getInt("str"));
						equip.setWatk((short) rse.getInt("watk"));
						equip.setWdef((short) rse.getInt("wdef"));
						equip.setUpgradeSlots((byte) rse.getInt("upgradeslots"));
						equip.setLevel((byte) rse.getInt("level"));
						equip.setFlag((byte) rs.getInt("flag"));
						items.add(new MTSItemInfo((IItem) equip, rse.getInt("price"), rse.getInt("id"), rse.getInt("seller"), rse.getString("sellername"), rse.getString("sell_ends")));
					}
				}
				pse.close();
			}
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT COUNT(*) FROM mts_cart WHERE cid = ?");
			ps.setInt(1, cid);
			rs = ps.executeQuery();
			if (rs.next()) {
				pages = rs.getInt(1) / 16;
				if (rs.getInt(1) % 16 > 0) {
					pages += 1;
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
		}
		return MaplePacketCreator.sendMTS(items, 4, 0, 0, pages);
	}

	public List<MTSItemInfo> getTransfer(int cid) {
		List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		try {
			ps = con.prepareStatement("SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC");
			ps.setInt(1, cid);
			rs = ps.executeQuery();
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

	private static MaplePacket getMTS(int tab, int type, int page) {
		List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		int pages = 0;
		try {
			if (type != 0) {
				ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = ? AND type = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
			} else {
				ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
			}
			ps.setInt(1, tab);
			if (type != 0) {
				ps.setInt(2, type);
				ps.setInt(3, page * 16);
			} else {
				ps.setInt(2, page * 16);
			}
			rs = ps.executeQuery();
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
			ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items WHERE tab = ? " + (type != 0 ? "AND type = ?" : "") + "AND transfer = 0");
			ps.setInt(1, tab);
			if (type != 0) {
				ps.setInt(2, type);
			}
			rs = ps.executeQuery();
			if (rs.next()) {
				pages = rs.getInt(1) / 16;
				if (rs.getInt(1) % 16 > 0) {
					pages++;
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
		}  
		return MaplePacketCreator.sendMTS(items, tab, type, page, pages); // resniff
	}

	public MaplePacket getMTSSearch(int tab, int type, int cOi, String search, int page) {
		List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
		MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
		String listaitems = "";
		if (cOi != 0) {
			List<String> retItems = new ArrayList<String>();
			for (ItemNameEntry itemPair : ii.getAllItems()) {
				if (itemPair.name.toLowerCase().contains(search.toLowerCase())) {
					retItems.add(" itemid=" + itemPair.itemId + " OR ");
				}
			}
			listaitems += " AND (";
			if (retItems != null && retItems.size() > 0) {
				for (String singleRetItem : retItems) {
					listaitems += singleRetItem;
				}
				listaitems += " itemid=0 )";
			}
		} else {
			listaitems = " AND sellername LIKE CONCAT('%','" + search + "', '%')";
		}
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		ResultSet rs;
		int pages = 0;
		try {
			if (type != 0) {
				ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = ? " + listaitems + " AND type = ? AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
			} else {
				ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = ? " + listaitems + " AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
			}
			ps.setInt(1, tab);
			if (type != 0) {
				ps.setInt(2, type);
				ps.setInt(3, page * 16);
			} else {
				ps.setInt(2, page * 16);
			}
			rs = ps.executeQuery();
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
			if (type != 0) {
				ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items WHERE tab = ? " + listaitems + " AND type = ? AND transfer = 0");
			} else {
				ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items WHERE tab = ? " + listaitems + " AND transfer = 0");
				ps.setInt(1, tab);
				if (type != 0) {
					ps.setInt(2, type);
				}
				rs = ps.executeQuery();
				if (rs.next()) {
					pages = rs.getInt(1) / 16;
					if (rs.getInt(1) % 16 > 0) {
						pages++;
					}
				}
				rs.close();
				ps.close();
			}
		} catch (SQLException e) {
		}
		return MaplePacketCreator.sendMTS(items, tab, type, page, pages);
	}
}
