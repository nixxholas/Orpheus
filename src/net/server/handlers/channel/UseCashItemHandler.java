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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import constants.ExpTable;
import client.IEquip;
import client.IItem;
import client.ISkill;
import client.GameCharacter;
import client.GameClient;
import client.InventoryType;
import client.MapleJob;
import client.Pet;
import client.MapleStat;
import client.MapleStatDelta;
import client.SkillFactory;
import constants.ItemConstants;
import constants.skills.Aran;
import java.sql.SQLException;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import scripting.npc.NPCScriptManager;
import server.InventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShop;
import server.MapleShopFactory;
import server.maps.MapleMap;
import server.maps.MapleTVEffect;
import tools.PacketCreator;
import tools.Output;
import tools.data.input.SeekableLittleEndianAccessor;

public final class UseCashItemHandler extends AbstractMaplePacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		GameCharacter player = c.getPlayer();
		if (System.currentTimeMillis() - player.getLastUsedCashItem() < 3000) {
			return;
		}
		player.setLastUsedCashItem(System.currentTimeMillis());
		MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
		slea.readShort();
		int itemId = slea.readInt();
		int itemType = itemId / 10000;
		IItem toUse = c.getPlayer().getInventory(InventoryType.CASH).getItem(c.getPlayer().getInventory(InventoryType.CASH).findById(itemId).getPosition());
		String medal = "";
		IItem medalItem = c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem((byte) -49);
		if (medalItem != null) {
			medal = "<" + ii.getName(medalItem.getItemId()) + "> ";
		}
		if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		if (itemType == 505) { // AP/SP reset
			if (itemId > 5050000) {
				int SPTo = slea.readInt();
				int SPFrom = slea.readInt();
				ISkill skillSPTo = SkillFactory.getSkill(SPTo);
				ISkill skillSPFrom = SkillFactory.getSkill(SPFrom);
				byte curLevel = player.getSkillLevel(skillSPTo);
				byte curLevelSPFrom = player.getSkillLevel(skillSPFrom);
				if ((curLevel < skillSPTo.getMaxLevel()) && curLevelSPFrom > 0) {
					player.changeSkillLevel(skillSPFrom, (byte) (curLevelSPFrom - 1), player.getMasterLevel(skillSPFrom), -1);
					player.changeSkillLevel(skillSPTo, (byte) (curLevel + 1), player.getMasterLevel(skillSPTo), -1);
					if (SPFrom == Aran.FULL_SWING) {
						ISkill hidden1 = SkillFactory.getSkill(Aran.HIDDEN_FULL_DOUBLE);
						ISkill hidden2 = SkillFactory.getSkill(Aran.HIDDEN_FULL_TRIPLE);
						player.changeSkillLevel(hidden1, (byte) (curLevelSPFrom - 1), player.getMasterLevel(hidden1), -1);
						player.changeSkillLevel(hidden2, (byte) (curLevelSPFrom - 1), player.getMasterLevel(hidden2), -1);
					} else if (SPFrom == Aran.OVER_SWING) {
						ISkill hidden1 = SkillFactory.getSkill(Aran.HIDDEN_OVER_DOUBLE);
						ISkill hidden2 = SkillFactory.getSkill(Aran.HIDDEN_OVER_TRIPLE);
						player.changeSkillLevel(hidden1, (byte) (curLevelSPFrom - 1), player.getMasterLevel(hidden1), -1);
						player.changeSkillLevel(hidden2, (byte) (curLevelSPFrom - 1), player.getMasterLevel(hidden2), -1);
					}
				}
			} else {
				List<MapleStatDelta> statupdate = new ArrayList<MapleStatDelta>(2);
				int APTo = slea.readInt();
				int APFrom = slea.readInt();
				switch (APFrom) {
					case 64: // str
						if (player.getStr() < 5) {
							return;
						}
						player.addStat(1, -1);
						break;
					case 128: // dex
						if (player.getDex() < 5) {
							return;
						}
						player.addStat(2, -1);
						break;
					case 256: // int
						if (player.getInt() < 5) {
							return;
						}
						player.addStat(3, -1);
						break;
					case 512: // luk
						if (player.getLuk() < 5) {
							return;
						}
						player.addStat(4, -1);
						break;
					case 2048: // HP
						int hplose = 0;
						final int jobid = player.getJob().getId();
						if (jobid == 0 || jobid == 1000 || jobid == 2000 || jobid >= 1200 && jobid <= 1211) { // Beginner
							hplose -= 12;
						} else if (jobid >= 100 && jobid <= 132) { // Warrior
							ISkill improvinghplose = SkillFactory.getSkill(1000001);
							int improvinghploseLevel = c.getPlayer().getSkillLevel(improvinghplose);
							hplose -= 24;
							if (improvinghploseLevel >= 1) {
								hplose -= improvinghplose.getEffect(improvinghploseLevel).getY();
							}
						} else if (jobid >= 200 && jobid <= 232) { // Magician
							hplose -= 10;
						} else if (jobid >= 500 && jobid <= 522) { // Pirate
							ISkill improvinghplose = SkillFactory.getSkill(5100000);
							int improvinghploseLevel = c.getPlayer().getSkillLevel(improvinghplose);
							hplose -= 22;
							if (improvinghploseLevel > 0) {
								hplose -= improvinghplose.getEffect(improvinghploseLevel).getY();
							}
						} else if (jobid >= 1100 && jobid <= 1111) { // Soul
																		// Master
							ISkill improvinghplose = SkillFactory.getSkill(11000000);
							int improvinghploseLevel = c.getPlayer().getSkillLevel(improvinghplose);
							hplose -= 27;
							if (improvinghploseLevel >= 1) {
								hplose -= improvinghplose.getEffect(improvinghploseLevel).getY();
							}
						} else if ((jobid >= 1300 && jobid <= 1311) || (jobid >= 1400 && jobid <= 1411)) { // Wind
																											// Breaker
																											// and
																											// Night
																											// Walker
							hplose -= 17;
						} else if (jobid >= 300 && jobid <= 322 || jobid >= 400 && jobid <= 422 || jobid >= 2000 && jobid <= 2112) { // Aran
							hplose -= 20;
						} else { // GameMaster
							hplose -= 20;
						}
						player.setHp(player.getHp() + hplose);
						player.setMaxHp(player.getMaxHp() + hplose);
						statupdate.add(new MapleStatDelta(MapleStat.HP, player.getHp()));
						statupdate.add(new MapleStatDelta(MapleStat.MAXHP, player.getMaxHp()));
						break;
					case 8192: // MP
						int mp = player.getMp();
						int level = player.getLevel();
						MapleJob job = player.getJob();
						boolean canWash = true;
						if (job.isA(MapleJob.SPEARMAN) && mp < 4 * level + 156) {
							canWash = false;
						} else if (job.isA(MapleJob.FIGHTER) && mp < 4 * level + 56) {
							canWash = false;
						} else if (job.isA(MapleJob.THIEF) && job.getId() % 100 > 0 && mp < level * 14 - 4) {
							canWash = false;
						} else if (mp < level * 14 + 148) {
							canWash = false;
						}
						if (canWash) {
							int minmp = 0;
							if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
								minmp += 4;
							} else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
								minmp += 36;
							} else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
								minmp += 12;
							} else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
								minmp += 16;
							} else {
								minmp += 8;
							}

							player.setMp(player.getMp() - minmp);
							player.setMaxMp(player.getMaxMp() - minmp);
							statupdate.add(new MapleStatDelta(MapleStat.MP, player.getMp()));
							statupdate.add(new MapleStatDelta(MapleStat.MAXMP, player.getMaxMp()));
							break;
						}
					default:
						c.announce(PacketCreator.updatePlayerStats(PacketCreator.EMPTY_STATUPDATE, true));
						return;
				}
				DistributeAPHandler.addStat(c, APTo);
				c.announce(PacketCreator.updatePlayerStats(statupdate, true));
			}
			remove(c, itemId);
		} else if (itemType == 506) {
			IItem eq = null;
			if (itemId == 5060000) { // Item tag.
				int equipSlot = slea.readShort();
				if (equipSlot == 0) {
					return;
				}
				eq = player.getInventory(InventoryType.EQUIPPED).getItem((byte) equipSlot);
				eq.setOwner(player.getName());
			} else if (itemId == 5060001 || itemId == 5061000 || itemId == 5061001 || itemId == 5061002 || itemId == 5061003) { // Sealing
																																// lock
				InventoryType type = InventoryType.fromByte((byte) slea.readInt());
				IItem item = c.getPlayer().getInventory(type).getItem((byte) slea.readInt());
				if (item == null) { // Check if the type is EQUIPMENT?
					return;
				}
				byte flag = item.getFlag();
				flag |= ItemConstants.LOCK;
				if (item.getExpiration() > -1) {
					return; // No perma items pls
				}
				item.setFlag(flag);

				long period = 0;
				if (itemId == 5061000) {
					period = 7;
				} else if (itemId == 5061001) {
					period = 30;
				} else if (itemId == 5061002) {
					period = 90;
				} else if (itemId == 5061003) {
					period = 365;
				}

				if (period > 0) {
					item.setExpiration(System.currentTimeMillis() + (period * 60 * 60 * 24 * 1000));
				}

				c.announce(PacketCreator.updateSlot(item));

				remove(c, itemId);
			} else if (itemId == 5060002) { // Incubator
				byte inventory2 = (byte) slea.readInt();
				byte slot2 = (byte) slea.readInt();
				IItem item2 = c.getPlayer().getInventory(InventoryType.fromByte(inventory2)).getItem(slot2);
				if (item2 == null) // hacking
				{
					return;
				}
				if (getIncubatedItem(c, itemId)) {
					InventoryManipulator.removeFromSlot(c, InventoryType.fromByte(inventory2), slot2, (short) 1, false);
					remove(c, itemId);
				}
				return;
			}
			slea.readInt(); // time stamp
			c.announce(PacketCreator.updateSlot(eq));
			remove(c, itemId);
		} else if (itemType == 507) {
			boolean whisper;
			switch (itemId / 1000 % 10) {
				case 1: // Megaphone
					if (player.getLevel() > 9) {
						player.getClient().getChannelServer().broadcastPacket(PacketCreator.serverNotice(2, medal + player.getName() + " : " + slea.readMapleAsciiString()));
					} else {
						player.dropMessage(1, "You may not use this until you're level 10.");
					}
					break;
				case 2: // Super megaphone
					Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + slea.readMapleAsciiString(), (slea.readByte() != 0)));
					break;
				case 5: // Maple TV
					int tvType = itemId % 10;
					boolean megassenger = false;
					boolean ear = false;
					GameCharacter victim = null;
					if (tvType != 1) {
						if (tvType >= 3) {
							megassenger = true;
							if (tvType == 3) {
								slea.readByte();
							}
							ear = 1 == slea.readByte();
						} else if (tvType != 2) {
							slea.readByte();
						}
						if (tvType != 4) {
							victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
						}
					}
					List<String> messages = new LinkedList<String>();
					StringBuilder builder = new StringBuilder();
					for (int i = 0; i < 5; i++) {
						String message = slea.readMapleAsciiString();
						if (megassenger) {
							builder.append(" ").append(message);
						}
						messages.add(message);
					}
					slea.readInt();
					if (megassenger) {
						Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.serverNotice(3, c.getChannel(), medal + player.getName() + " : " + builder.toString(), ear));
					}
					if (!MapleTVEffect.isActive()) {
						new MapleTVEffect(player, victim, messages, tvType, c.getWorld());
						remove(c, itemId);
					} else {
						player.dropMessage(1, "MapleTV is already in use.");
						return;
					}
					break;
				case 6: // item megaphone
					String msg = medal + c.getPlayer().getName() + " : " + slea.readMapleAsciiString();
					whisper = slea.readByte() == 1;
					IItem item = null;
					if (slea.readByte() == 1) { // item
						item = c.getPlayer().getInventory(InventoryType.fromByte((byte) slea.readInt())).getItem((byte) slea.readInt());
						if (item == null) // hack
						{
							return;
						} else if (ii.isDropRestricted(item.getItemId())) { // Lol?
							player.dropMessage(1, "You cannot trade this item.");
							c.announce(PacketCreator.enableActions());
							return;
						}
					}
					Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.itemMegaphone(msg, whisper, c.getChannel(), item));
					break;
				case 7: // triple megaphone
					int lines = slea.readByte();
					if (lines < 1 || lines > 3) // hack
					{
						return;
					}
					String[] msg2 = new String[lines];
					for (int i = 0; i < lines; i++) {
						msg2[i] = medal + c.getPlayer().getName() + " : " + slea.readMapleAsciiString();
					}
					whisper = slea.readByte() == 1;
					Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.getMultiMegaphone(msg2, c.getChannel(), whisper));
					break;
			}
			remove(c, itemId);
		} else if (itemType == 508) { // graduation banner
			slea.readMapleAsciiString(); // message, sepearated by 0A for lines
			c.announce(PacketCreator.enableActions());
		} else if (itemType == 509) {
			String sendTo = slea.readMapleAsciiString();
			String msg = slea.readMapleAsciiString();
			try {
				player.sendNote(sendTo, msg, (byte) 0);
			} catch (SQLException e) {
			}
			remove(c, itemId);
		} else if (itemType == 510) {
			player.getMap().broadcastMessage(PacketCreator.musicChange("Jukebox/Congratulation"));
			remove(c, itemId);
		} else if (itemType == 512) {
			if (ii.getStateChangeItem(itemId) != 0) {
				for (GameCharacter character : c.getPlayer().getMap().getCharacters()) {
					ii.getItemEffect(ii.getStateChangeItem(itemId)).applyTo(character);
				}
			}
			player.getMap().startMapEffect(ii.getMsg(itemId).replaceFirst("%s", c.getPlayer().getName()).replaceFirst("%s", slea.readMapleAsciiString()), itemId);
			remove(c, itemId);
		} else if (itemType == 517) {
			Pet pet = player.getPet(0);
			if (pet == null) {
				c.announce(PacketCreator.enableActions());
				return;
			}
			IItem item = player.getInventory(InventoryType.CASH).getItem(pet.getPosition());
			String newName = slea.readMapleAsciiString();
			pet.setName(newName);
			pet.saveToDb();
			c.announce(PacketCreator.updateSlot(item));
			player.getMap().broadcastMessage(player, PacketCreator.changePetName(player, newName, 1), true);
			c.announce(PacketCreator.enableActions());
			remove(c, itemId);
		} else if (itemType == 504) { // vip teleport rock
			String error1 = "Either the player could not be found or you were trying to teleport to an illegal location.";
			boolean vip = slea.readByte() == 1;
			remove(c, itemId);
			if (!vip) {
				int mapId = slea.readInt();
				if (c.getChannelServer().getMapFactory().getMap(mapId).getForcedReturnId() == 999999999) {
					player.changeMap(c.getChannelServer().getMapFactory().getMap(mapId));
				} else {
					InventoryManipulator.addById(c, itemId, (short) 1);
					c.getPlayer().dropMessage(1, error1);
					c.announce(PacketCreator.enableActions());
				}
			} else {
				String name = slea.readMapleAsciiString();
				GameCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
				boolean success = false;
				if (victim != null) {
					MapleMap target = victim.getMap();
					if (c.getChannelServer().getMapFactory().getMap(victim.getMapId()).getForcedReturnId() == 999999999 || victim.getMapId() < 100000000) {
						if (victim.gmLevel() <= player.gmLevel()) {
							if (itemId == 5041000 || victim.getMapId() / player.getMapId() == 1) { // viprock
																									// &
																									// same
																									// continent
								player.changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
								success = true;
							} else {
								player.dropMessage(1, error1);
							}
						} else {
							player.dropMessage(1, error1);
						}
					} else {
						player.dropMessage(1, "You cannot teleport to this map.");
					}
				} else {
					player.dropMessage(1, "Player could not be found in this channel.");
				}
				if (!success) {
					InventoryManipulator.addById(c, itemId, (short) 1);
					c.announce(PacketCreator.enableActions());
				}
			}
		} else if (itemType == 520) {
			player.gainMeso(ii.getMeso(itemId), true, false, true);
			remove(c, itemId);
			c.announce(PacketCreator.enableActions());
		} else if (itemType == 524) {
			for (byte i = 0; i < 3; i++) {
				Pet pet = player.getPet(i);
				if (pet != null) {
					if (pet.canConsume(itemId)) {
						pet.setFullness(100);
						if (pet.getCloseness() + 100 > 30000) {
							pet.setCloseness(30000);
						} else {
							pet.gainCloseness(100);
						}

						while (pet.getCloseness() >= ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
							pet.setLevel((byte) (pet.getLevel() + 1));
							byte index = player.getPetIndex(pet);
							c.announce(PacketCreator.showOwnPetLevelUp(index));
							player.getMap().broadcastMessage(PacketCreator.showPetLevelUp(c.getPlayer(), index));
						}
						IItem item = player.getInventory(InventoryType.CASH).getItem(pet.getPosition());
						c.announce(PacketCreator.updateSlot(item));
						player.getMap().broadcastMessage(c.getPlayer(), PacketCreator.commandResponse(player.getId(), i, 1, true), true);
						remove(c, itemId);
						break;
					}
				} else {
					break;
				}
			}
			c.announce(PacketCreator.enableActions());
		} else if (itemType == 530) {
			ii.getItemEffect(itemId).applyTo(player);
			remove(c, itemId);
		} else if (itemType == 533) {
			NPCScriptManager.getInstance().start(c, 9010009, null, null);
		} else if (itemType == 537) {
			player.setChalkboard(slea.readMapleAsciiString());
			player.getMap().broadcastMessage(PacketCreator.useChalkboard(player, false));
			player.getClient().announce(PacketCreator.enableActions());
		} else if (itemType == 539) {
			List<String> lines = new LinkedList<String>();
			for (int i = 0; i < 4; i++) {
				lines.add(slea.readMapleAsciiString());
			}
			Server.getInstance().broadcastMessage(c.getWorld(), PacketCreator.getAvatarMega(c.getPlayer(), medal, c.getChannel(), itemId, lines, (slea.readByte() != 0)));
			remove(c, itemId);
		} else if (itemType == 545) { // MiuMiu's travel store
			if (player.getShop() == null) {
				MapleShop shop = MapleShopFactory.getInstance().getShop(1338);
				if (shop != null) {
					shop.sendShop(c);
					remove(c, itemId);
				}
			} else {
				c.announce(PacketCreator.enableActions());
			}
		} else if (itemType == 550) { // Extend item expiration
			c.announce(PacketCreator.enableActions());
		} else if (itemType == 552) {
			InventoryType type = InventoryType.fromByte((byte) slea.readInt());
			byte slot = (byte) slea.readInt();
			IItem item = c.getPlayer().getInventory(type).getItem(slot);
			if (item == null || item.getQuantity() <= 0 || (item.getFlag() & ItemConstants.KARMA) > 0 && ii.isKarmaAble(item.getItemId())) {
				c.announce(PacketCreator.enableActions());
				return;
			}
			if (type.equals(InventoryType.USE)) {
				item.setFlag((byte) ItemConstants.SPIKES);
			} else {
				item.setFlag((byte) ItemConstants.KARMA);
			}

			c.getPlayer().forceUpdateItem(type, item);
			remove(c, itemId);
			c.announce(PacketCreator.enableActions());
		} else if (itemType == 552) { // DS EGG THING
			c.announce(PacketCreator.enableActions());
		} else if (itemType == 557) {
			slea.readInt();
			int itemSlot = slea.readInt();
			slea.readInt();
			final IEquip equip = (IEquip) c.getPlayer().getInventory(InventoryType.EQUIP).getItem((byte) itemSlot);
			if (equip.getVicious() == 2 || c.getPlayer().getInventory(InventoryType.CASH).findById(5570000) == null) {
				return;
			}
			equip.setVicious(equip.getVicious() + 1);
			equip.setUpgradeSlots(equip.getUpgradeSlots() + 1);
			remove(c, itemId);
			c.announce(PacketCreator.enableActions());
			c.announce(PacketCreator.sendHammerData(equip.getVicious()));
			c.announce(PacketCreator.hammerItem(equip));
		} else if (itemType == 561) { // VEGA'S SPELL
			c.announce(PacketCreator.enableActions());
		} else {
			Output.print("NEW CASH ITEM: " + itemType + "\n" + slea.toString());
			c.announce(PacketCreator.enableActions());
		}
	}

	private static void remove(GameClient c, int itemId) {
		InventoryManipulator.removeById(c, InventoryType.CASH, itemId, 1, true, false);
	}

	private static boolean getIncubatedItem(GameClient c, int id) {
		final int[] ids = {1012070, 1302049, 1302063, 1322027, 2000004, 2000005, 2020013, 2020015, 2040307, 2040509, 2040519, 2040521, 2040533, 2040715, 2040717, 2040810, 2040811, 2070005, 2070006, 4020009,};
		final int[] quantitys = {1, 1, 1, 1, 240, 200, 200, 200, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3};
		int amount = 0;
		for (int i = 0; i < ids.length; i++) {
			if (i == id) {
				amount = quantitys[i];
			}
		}
		if (c.getPlayer().getInventory(InventoryType.fromByte((byte) (id / 1000000))).isFull()) {
			return false;
		}
		InventoryManipulator.addById(c, id, (short) amount);
		return true;
	}
}
