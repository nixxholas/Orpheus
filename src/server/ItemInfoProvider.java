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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import client.Equip;
import client.IEquip;
import client.IItem;
import client.GameCharacter;
import client.GameClient;
import client.Inventory;
import client.InventoryType;
import client.Job;
import client.WeaponType;
import client.SkillFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import constants.ItemConstants;
import java.util.Collection;
import java.util.LinkedList;
import tools.Randomizer;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.DatabaseConnection;

/**
 * 
 * @author Matze
 * 
 */
public class ItemInfoProvider {
	private static ItemInfoProvider instance = null;
	protected MapleDataProvider itemData;
	protected MapleDataProvider equipData;
	protected MapleDataProvider stringData;
	protected MapleData cashStringData;
	protected MapleData consumeStringData;
	protected MapleData eqpStringData;
	protected MapleData etcStringData;
	protected MapleData insStringData;
	protected MapleData petStringData;
	protected Map<Integer, InventoryType> inventoryTypeCache = new HashMap<Integer, InventoryType>();
	protected Map<Integer, Short> slotMaxCache = new HashMap<Integer, Short>();
	protected Map<Integer, StatEffect> itemEffects = new HashMap<Integer, StatEffect>();
	protected Map<Integer, Map<String, Integer>> equipStatsCache = new HashMap<Integer, Map<String, Integer>>();
	protected Map<Integer, Equip> equipCache = new HashMap<Integer, Equip>();
	protected Map<Integer, Double> priceCache = new HashMap<Integer, Double>();
	protected Map<Integer, Integer> wholePriceCache = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> projectileWatkCache = new HashMap<Integer, Integer>();
	protected Map<Integer, String> nameCache = new HashMap<Integer, String>();
	protected Map<Integer, String> descCache = new HashMap<Integer, String>();
	protected Map<Integer, String> msgCache = new HashMap<Integer, String>();
	protected Map<Integer, Boolean> dropRestrictionCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, Boolean> pickupRestrictionCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, Integer> getMesoCache = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> monsterBookID = new HashMap<Integer, Integer>();
	protected Map<Integer, Boolean> onEquipUntradableCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, ScriptedItem> scriptedItemCache = new HashMap<Integer, ScriptedItem>();
	protected Map<Integer, Boolean> karmaCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, Integer> triggerItemCache = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> expCache = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> levelCache = new HashMap<Integer, Integer>();
	protected Map<Integer, RewardInfo> rewardCache = new HashMap<Integer, RewardInfo>();
	protected List<ItemNameEntry> itemNameCache = new ArrayList<ItemNameEntry>();
	protected Map<Integer, Boolean> consumeOnPickupCache = new HashMap<Integer, Boolean>();
	protected Map<Integer, Boolean> isQuestItemCache = new HashMap<Integer, Boolean>();

	private ItemInfoProvider() {
		loadCardIdData();
		itemData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Item.wz"));
		equipData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Character.wz"));
		stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz"));
		cashStringData = stringData.getData("Cash.img");
		consumeStringData = stringData.getData("Consume.img");
		eqpStringData = stringData.getData("Eqp.img");
		etcStringData = stringData.getData("Etc.img");
		insStringData = stringData.getData("Ins.img");
		petStringData = stringData.getData("Pet.img");
	}

	public static ItemInfoProvider getInstance() {
		if (instance == null) {
			instance = new ItemInfoProvider();
		}
		return instance;
	}

	public InventoryType getInventoryType(int itemId) {
		if (inventoryTypeCache.containsKey(itemId)) {
			return inventoryTypeCache.get(itemId);
		}
		InventoryType ret;
		String idStr = "0" + String.valueOf(itemId);
		MapleDataDirectoryEntry root = itemData.getRoot();
		for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
					ret = InventoryType.getByWZName(topDir.getName());
					inventoryTypeCache.put(itemId, ret);
					return ret;
				} else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
					ret = InventoryType.getByWZName(topDir.getName());
					inventoryTypeCache.put(itemId, ret);
					return ret;
				}
			}
		}
		root = equipData.getRoot();
		for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr + ".img")) {
					ret = InventoryType.EQUIP;
					inventoryTypeCache.put(itemId, ret);
					return ret;
				}
			}
		}
		ret = InventoryType.UNDEFINED;
		inventoryTypeCache.put(itemId, ret);
		return ret;
	}

	public List<ItemNameEntry> getAllItems() {
		// BUG: If you never add anything to this, it'll always be empty, duh?
		if (!itemNameCache.isEmpty()) {
			return itemNameCache; 
		}
		
		List<ItemNameEntry> entries = new ArrayList<ItemNameEntry>();
		MapleData itemsData;
		
		itemsData = stringData.getData("Cash.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			entries.add(getItemNameEntry(itemFolder));
		}
		
		itemsData = stringData.getData("Consume.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			entries.add(getItemNameEntry(itemFolder));
		}
		
		itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
		for (MapleData eqpType : itemsData.getChildren()) {
			for (MapleData itemFolder : eqpType.getChildren()) {
				entries.add(getItemNameEntry(itemFolder));
			}
		}
		
		itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
		for (MapleData itemFolder : itemsData.getChildren()) {
			entries.add(getItemNameEntry(itemFolder));
		}
		
		itemsData = stringData.getData("Ins.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			entries.add(getItemNameEntry(itemFolder));
		}
		
		itemsData = stringData.getData("Pet.img");
		for (MapleData itemFolder : itemsData.getChildren()) {
			entries.add(getItemNameEntry(itemFolder));
		}
		
		return entries;
	}

	private static ItemNameEntry getItemNameEntry(MapleData itemFolder) {
		return new ItemNameEntry(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME"));
	}

	private MapleData getStringData(int itemId) {
		String category = "null";
		MapleData data;
		if (5010000 <= itemId) {
			data = cashStringData;
		} else if (2000000 <= itemId && itemId < 3000000) {
			data = consumeStringData;
		} else if ((1010000 <= itemId && itemId < 1040000) 
				|| (1122000 <= itemId && itemId < 1123000) 
				|| (1142000 <= itemId && itemId < 1143000)) {
			data = eqpStringData;
			category = "Eqp/Accessory";
		} else if (1000000 <= itemId && itemId < 1010000) {
			data = eqpStringData;
			category = "Eqp/Cap";
		} else if (1102000 <= itemId && itemId < 1103000) {
			data = eqpStringData;
			category = "Eqp/Cape";
		} else if (1040000 <= itemId && itemId < 1050000) {
			data = eqpStringData;
			category = "Eqp/Coat";
		} else if (20000 <= itemId && itemId < 22000) {
			data = eqpStringData;
			category = "Eqp/Face";
		} else if (1080000 <= itemId && itemId < 1090000) {
			data = eqpStringData;
			category = "Eqp/Glove";
		} else if (30000 <= itemId && itemId < 32000) {
			data = eqpStringData;
			category = "Eqp/Hair";
		} else if (1050000 <= itemId && itemId < 1060000) {
			data = eqpStringData;
			category = "Eqp/Longcoat";
		} else if (1060000 <= itemId && itemId < 1070000) {
			data = eqpStringData;
			category = "Eqp/Pants";
		} else if (1802000 <= itemId && itemId < 1810000) {
			data = eqpStringData;
			category = "Eqp/PetEquip";
		} else if (1112000 <= itemId && itemId < 1120000) {
			data = eqpStringData;
			category = "Eqp/Ring";
		} else if (1092000 <= itemId && itemId < 1100000) {
			data = eqpStringData;
			category = "Eqp/Shield";
		} else if (1070000 <= itemId && itemId < 1080000) {
			data = eqpStringData;
			category = "Eqp/Shoes";
		} else if (1900000 <= itemId && itemId < 2000000) {
			data = eqpStringData;
			category = "Eqp/Taming";
		} else if (1300000 <= itemId && itemId < 1800000) {
			data = eqpStringData;
			category = "Eqp/Weapon";
		} else if (4000000 <= itemId && itemId < 5000000) {
			data = etcStringData;
		} else if (3000000 <= itemId && itemId < 4000000) {
			data = insStringData;
		} else if (5000000 <= itemId && itemId < 5010000) {
			data = petStringData;
		} else {
			return null;
		}
		
		if (category.equalsIgnoreCase("null")) {
			return data.getChildByPath(String.valueOf(itemId));
		} else {
			return data.getChildByPath(category + "/" + itemId);
		}
	}

	public boolean noCancelMouse(int itemId) {
		MapleData item = getItemData(itemId);
		if (item == null)
			return false;
		return MapleDataTool.getIntConvert("info/noCancelMouse", item, 0) == 1;
	}

	private MapleData getItemData(int itemId) {
		MapleData ret = null;
		String idStr = "0" + String.valueOf(itemId);
		MapleDataDirectoryEntry root = itemData.getRoot();
		for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
					ret = itemData.getData(topDir.getName() + "/" + iFile.getName());
					if (ret == null) {
						return null;
					}
					ret = ret.getChildByPath(idStr);
					return ret;
				} else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
					return itemData.getData(topDir.getName() + "/" + iFile.getName());
				}
			}
		}
		root = equipData.getRoot();
		for (MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
			for (MapleDataFileEntry iFile : topDir.getFiles()) {
				if (iFile.getName().equals(idStr + ".img")) {
					return equipData.getData(topDir.getName() + "/" + iFile.getName());
				}
			}
		}
		return ret;
	}

	public short getSlotMax(GameClient c, int itemId) {
		if (slotMaxCache.containsKey(itemId)) {
			return slotMaxCache.get(itemId);
		}
		short ret = 0;
		MapleData item = getItemData(itemId);
		if (item != null) {
			MapleData smEntry = item.getChildByPath("info/slotMax");
			if (smEntry == null) {
				if (getInventoryType(itemId).asByte() == InventoryType.EQUIP.asByte()) {
					ret = 1;
				} else {
					ret = 100;
				}
			} else {
				if (ItemConstants.isRechargable(itemId) || (MapleDataTool.getInt(smEntry) == 0)) {
					ret = 1;
				}
				ret = (short) MapleDataTool.getInt(smEntry);
				if (ItemConstants.isThrowingStar(itemId)) {
					ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(4100000)) * 10;
				} else {
					ret += c.getPlayer().getSkillLevel(SkillFactory.getSkill(5200000)) * 10;
				}
			}
		}
		if (!ItemConstants.isRechargable(itemId)) {
			slotMaxCache.put(itemId, ret);
		}
		return ret;
	}

	public int getMeso(int itemId) {
		if (getMesoCache.containsKey(itemId)) {
			return getMesoCache.get(itemId);
		}
		MapleData item = getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry = 0;
		MapleData pData = item.getChildByPath("info/meso");
		if (pData == null) {
			return -1;
		}
		pEntry = MapleDataTool.getInt(pData);
		getMesoCache.put(itemId, pEntry);
		return pEntry;
	}

	public int getWholePrice(int itemId) {
		if (wholePriceCache.containsKey(itemId)) {
			return wholePriceCache.get(itemId);
		}
		MapleData item = getItemData(itemId);
		if (item == null) {
			return -1;
		}
		int pEntry = 0;
		MapleData pData = item.getChildByPath("info/price");
		if (pData == null) {
			return -1;
		}
		pEntry = MapleDataTool.getInt(pData);
		wholePriceCache.put(itemId, pEntry);
		return pEntry;
	}

	public double getPrice(int itemId) {
		if (priceCache.containsKey(itemId)) {
			return priceCache.get(itemId);
		}
		MapleData item = getItemData(itemId);
		if (item == null) {
			return -1;
		}
		double pEntry = 0.0;
		MapleData pData = item.getChildByPath("info/unitPrice");
		if (pData != null) {
			try {
				pEntry = MapleDataTool.getDouble(pData);
			} catch (Exception e) {
				pEntry = (double) MapleDataTool.getInt(pData);
			}
		} else {
			pData = item.getChildByPath("info/price");
			if (pData == null) {
				return -1;
			}
			pEntry = (double) MapleDataTool.getInt(pData);
		}
		priceCache.put(itemId, pEntry);
		return pEntry;
	}

	protected Map<String, Integer> getEquipStats(int itemId) {
		if (equipStatsCache.containsKey(itemId)) {
			return equipStatsCache.get(itemId);
		}
		
		Map<String, Integer> result = new LinkedHashMap<String, Integer>();
		MapleData item = getItemData(itemId);
		if (item == null) {
			return null;
		}
		MapleData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		for (MapleData data : info.getChildren()) {
			if (data.getName().startsWith("inc"))
				result.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
			/*
			 * else if (data.getName().startsWith("req"))
			 * ret.put(data.getName(), MapleDataTool.getInt(data.getName(),
			 * info, 0));
			 */
		}
		result.put("reqJob", MapleDataTool.getInt("reqJob", info, 0));
		result.put("reqLevel", MapleDataTool.getInt("reqLevel", info, 0));
		result.put("reqDEX", MapleDataTool.getInt("reqDEX", info, 0));
		result.put("reqSTR", MapleDataTool.getInt("reqSTR", info, 0));
		result.put("reqINT", MapleDataTool.getInt("reqINT", info, 0));
		result.put("reqLUK", MapleDataTool.getInt("reqLUK", info, 0));
		result.put("reqPOP", MapleDataTool.getInt("reqPOP", info, 0));
		result.put("cash", MapleDataTool.getInt("cash", info, 0));
		result.put("tuc", MapleDataTool.getInt("tuc", info, 0));
		result.put("cursed", MapleDataTool.getInt("cursed", info, 0));
		result.put("success", MapleDataTool.getInt("success", info, 0));
		result.put("fs", MapleDataTool.getInt("fs", info, 0));
		equipStatsCache.put(itemId, result);
		return result;
	}

	public List<Integer> getScrollReqs(int itemId) {
		List<Integer> result = new ArrayList<Integer>();
		MapleData data = getItemData(itemId);
		data = data.getChildByPath("req");
		if (data == null) {
			return result;
		}
		for (MapleData req : data.getChildren()) {
			result.add(MapleDataTool.getInt(req));
		}
		return result;
	}

	public WeaponType getWeaponType(int itemId) {
		int partialId = (itemId / 10000) % 100;

		WeaponType type = WeaponType.fromPartialId(partialId);
		return type;
	}

	private boolean isCleanSlate(int scrollId) {
		return scrollId > 2048999 && scrollId < 2049004;
	}

	// NOTE: Oi, GM nibs, play fair!
//	public IItem scrollEquipWithId(IItem equip, int scrollId, boolean usingWhiteScroll, boolean isGM) {
	public IItem scrollEquipWithId(IItem equip, int scrollId, boolean usingWhiteScroll) {
		if (equip instanceof Equip) {
			Equip nEquip = (Equip) equip;
			Map<String, Integer> stats = this.getEquipStats(scrollId);
			Map<String, Integer> eqstats = this.getEquipStats(equip.getItemId());
			
			if (((nEquip.getUpgradeSlots() > 0 || isCleanSlate(scrollId)) && Math.ceil(Math.random() * 100.0) <= stats.get("success"))) {
				short flag = nEquip.getFlag();
				switch (scrollId) {
					case 2040727:
						flag |= ItemConstants.SPIKES;
						nEquip.setFlag((byte) flag);
						return equip;
						
					case 2041058:
						flag |= ItemConstants.COLD;
						nEquip.setFlag((byte) flag);
						return equip;
						
					case 2049000:
					case 2049001:
					case 2049002:
					case 2049003:
						if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats.get("tuc")) {
							nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
						}
						break;
						
					case 2049100:
					case 2049101:
					case 2049102:
						int inc = 1;
						if (Randomizer.nextInt(2) == 0) {
							inc = -1;
						}
						if (nEquip.getStr() > 0) {
							nEquip.setStr((short) Math.max(0, (nEquip.getStr() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getDex() > 0) {
							nEquip.setDex((short) Math.max(0, (nEquip.getDex() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getInt() > 0) {
							nEquip.setInt((short) Math.max(0, (nEquip.getInt() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getLuk() > 0) {
							nEquip.setLuk((short) Math.max(0, (nEquip.getLuk() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getWatk() > 0) {
							nEquip.setWatk((short) Math.max(0, (nEquip.getWatk() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getWdef() > 0) {
							nEquip.setWdef((short) Math.max(0, (nEquip.getWdef() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getMatk() > 0) {
							nEquip.setMatk((short) Math.max(0, (nEquip.getMatk() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getMdef() > 0) {
							nEquip.setMdef((short) Math.max(0, (nEquip.getMdef() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getAcc() > 0) {
							nEquip.setAcc((short) Math.max(0, (nEquip.getAcc() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getAvoid() > 0) {
							nEquip.setAvoid((short) Math.max(0, (nEquip.getAvoid() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getSpeed() > 0) {
							nEquip.setSpeed((short) Math.max(0, (nEquip.getSpeed() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getJump() > 0) {
							nEquip.setJump((short) Math.max(0, (nEquip.getJump() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getHp() > 0) {
							nEquip.setHp((short) Math.max(0, (nEquip.getHp() + Randomizer.nextInt(6) * inc)));
						}
						if (nEquip.getMp() > 0) {
							nEquip.setMp((short) Math.max(0, (nEquip.getMp() + Randomizer.nextInt(6) * inc)));
						}
						break;
						
					default:
						for (Entry<String, Integer> entry : stats.entrySet()) {
							final String name = entry.getKey();
							final int value = entry.getValue().intValue();
							if (name.equals("STR")) {
								nEquip.setStr((short) (nEquip.getStr() + value));
							} else if (name.equals("DEX")) {
								nEquip.setDex((short) (nEquip.getDex() + value));
							} else if (name.equals("INT")) {
								nEquip.setInt((short) (nEquip.getInt() + value));
							} else if (name.equals("LUK")) {
								nEquip.setLuk((short) (nEquip.getLuk() + value));
							} else if (name.equals("PAD")) {
								nEquip.setWatk((short) (nEquip.getWatk() + value));
							} else if (name.equals("PDD")) {
								nEquip.setWdef((short) (nEquip.getWdef() + value));
							} else if (name.equals("MAD")) {
								nEquip.setMatk((short) (nEquip.getMatk() + value));
							} else if (name.equals("MDD")) {
								nEquip.setMdef((short) (nEquip.getMdef() + value));
							} else if (name.equals("ACC")) {
								nEquip.setAcc((short) (nEquip.getAcc() + value));
							} else if (name.equals("EVA")) {
								nEquip.setAvoid((short) (nEquip.getAvoid() + value));
							} else if (name.equals("Speed")) {
								nEquip.setSpeed((short) (nEquip.getSpeed() + value));
							} else if (name.equals("Jump")) {
								nEquip.setJump((short) (nEquip.getJump() + value));
							} else if (name.equals("MHP")) {
								nEquip.setHp((short) (nEquip.getHp() + value));
							} else if (name.equals("MMP")) {
								nEquip.setMp((short) (nEquip.getMp() + value));
							} else if (name.equals("afterImage")) {
							}
						}
						break;
				}
				if (!isCleanSlate(scrollId)) {
					nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
					nEquip.setLevel((byte) (nEquip.getLevel() + 1));
				}
			} else {
				if (!usingWhiteScroll && !isCleanSlate(scrollId)) {
					nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - 1));
				}
				if (Randomizer.nextInt(101) < stats.get("cursed")) {
					return null;
				}
			}
		}
		return equip;
	}

	public IItem getEquipById(int equipId) {
		return getEquipById(equipId, -1);
	}

	IItem getEquipById(int equipId, int ringId) {
		final Equip equip = new Equip(equipId, (byte) 0, ringId);
		
		equip.setQuantity((short) 1);
		
		Map<String, Integer> stats = this.getEquipStats(equipId);
		if (stats != null) {
			for (Entry<String, Integer> stat : stats.entrySet()) {
				final String name = stat.getKey();
				final int value = stat.getValue().intValue();
				if (name.equals("STR")) {
					equip.setStr((short) value);
				} else if (name.equals("DEX")) {
					equip.setDex((short) value);
				} else if (name.equals("INT")) {
					equip.setInt((short) value);
				} else if (name.equals("LUK")) {
					equip.setLuk((short) value);
				} else if (name.equals("PAD")) {
					equip.setWatk((short) value);
				} else if (name.equals("PDD")) {
					equip.setWdef((short) value);
				} else if (name.equals("MAD")) {
					equip.setMatk((short) value);
				} else if (name.equals("MDD")) {
					equip.setMdef((short) value);
				} else if (name.equals("ACC")) {
					equip.setAcc((short) value);
				} else if (name.equals("EVA")) {
					equip.setAvoid((short) value);
				} else if (name.equals("Speed")) {
					equip.setSpeed((short) value);
				} else if (name.equals("Jump")) {
					equip.setJump((short) value);
				} else if (name.equals("MHP")) {
					equip.setHp((short) value);
				} else if (name.equals("MMP")) {
					equip.setMp((short) value);
				} else if (name.equals("tuc")) {
					equip.setUpgradeSlots((byte) value);
				} else if (isDropRestricted(equipId)) {
					byte flag = equip.getFlag();
					flag |= ItemConstants.UNTRADEABLE;
					equip.setFlag(flag);
				} else if (stats.get("fs") > 0) {
					byte flag = equip.getFlag();
					flag |= ItemConstants.SPIKES;
					equip.setFlag(flag);
					equipCache.put(equipId, equip);
				}
			}
		}
		return equip.copy();
	}

	private static short getRandStat(short defaultValue, int maxRange) {
		if (defaultValue == 0) {
			return 0;
		}
		int lMaxRange = (int) Math.min(Math.ceil(defaultValue * 0.1), maxRange);
		return (short) ((defaultValue - lMaxRange) + Math.floor(Randomizer.nextDouble() * (lMaxRange * 2 + 1)));
	}

	public Equip randomizeStats(Equip equip) {
		equip.setStr(getRandStat(equip.getStr(), 5));
		equip.setDex(getRandStat(equip.getDex(), 5));
		equip.setInt(getRandStat(equip.getInt(), 5));
		equip.setLuk(getRandStat(equip.getLuk(), 5));
		equip.setMatk(getRandStat(equip.getMatk(), 5));
		equip.setWatk(getRandStat(equip.getWatk(), 5));
		equip.setAcc(getRandStat(equip.getAcc(), 5));
		equip.setAvoid(getRandStat(equip.getAvoid(), 5));
		equip.setJump(getRandStat(equip.getJump(), 5));
		equip.setSpeed(getRandStat(equip.getSpeed(), 5));
		equip.setWdef(getRandStat(equip.getWdef(), 10));
		equip.setMdef(getRandStat(equip.getMdef(), 10));
		equip.setHp(getRandStat(equip.getHp(), 10));
		equip.setMp(getRandStat(equip.getMp(), 10));
		return equip;
	}

	public StatEffect getItemEffect(int itemId) {
		StatEffect ret = itemEffects.get(Integer.valueOf(itemId));
		if (ret == null) {
			MapleData item = getItemData(itemId);
			if (item == null) {
				return null;
			}
			MapleData spec = item.getChildByPath("spec");
			ret = StatEffect.loadItemEffectFromData(spec, itemId);
			itemEffects.put(Integer.valueOf(itemId), ret);
		}
		return ret;
	}

	public int[][] getSummonMobs(int itemId) {
		MapleData data = getItemData(itemId);
		int theInt = data.getChildByPath("mob").getChildren().size();
		int[][] mobs2spawn = new int[theInt][2];
		for (int x = 0; x < theInt; x++) {
			mobs2spawn[x][0] = MapleDataTool.getIntConvert("mob/" + x + "/id", data);
			mobs2spawn[x][1] = MapleDataTool.getIntConvert("mob/" + x + "/prob", data);
		}
		return mobs2spawn;
	}

	public int getWatkForProjectile(int itemId) {
		Integer atk = projectileWatkCache.get(itemId);
		if (atk != null) {
			return atk.intValue();
		}
		MapleData data = getItemData(itemId);
		atk = Integer.valueOf(MapleDataTool.getInt("info/incPAD", data, 0));
		projectileWatkCache.put(itemId, atk);
		return atk.intValue();
	}

	public String getName(int itemId) {
		if (nameCache.containsKey(itemId)) {
			return nameCache.get(itemId);
		}
		MapleData strings = getStringData(itemId);
		if (strings == null) {
			return null;
		}
		String ret = MapleDataTool.getString("name", strings, null);
		nameCache.put(itemId, ret);
		return ret;
	}

	public String getMsg(int itemId) {
		if (msgCache.containsKey(itemId)) {
			return msgCache.get(itemId);
		}
		MapleData strings = getStringData(itemId);
		if (strings == null) {
			return null;
		}
		String ret = MapleDataTool.getString("msg", strings, null);
		msgCache.put(itemId, ret);
		return ret;
	}

	public boolean isDropRestricted(int itemId) {
		if (dropRestrictionCache.containsKey(itemId)) {
			return dropRestrictionCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		boolean bRestricted = MapleDataTool.getIntConvert("info/tradeBlock", data, 0) == 1;
		if (!bRestricted)
			bRestricted = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
		dropRestrictionCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public boolean isPickupRestricted(int itemId) {
		if (pickupRestrictionCache.containsKey(itemId)) {
			return pickupRestrictionCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		boolean bRestricted = MapleDataTool.getIntConvert("info/only", data, 0) == 1;
		pickupRestrictionCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public Map<String, Integer> getSkillStats(int itemId, double playerJob) {
		Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
		MapleData item = getItemData(itemId);
		if (item == null) {
			return null;
		}
		MapleData info = item.getChildByPath("info");
		if (info == null) {
			return null;
		}
		for (MapleData data : info.getChildren()) {
			if (data.getName().startsWith("inc")) {
				ret.put(data.getName().substring(3), MapleDataTool.getIntConvert(data));
			}
		}
		ret.put("masterLevel", MapleDataTool.getInt("masterLevel", info, 0));
		ret.put("reqSkillLevel", MapleDataTool.getInt("reqSkillLevel", info, 0));
		ret.put("success", MapleDataTool.getInt("success", info, 0));
		MapleData skill = info.getChildByPath("skill");
		int curskill = 1;
		for (int i = 0; i < skill.getChildren().size(); i++) {
			curskill = MapleDataTool.getInt(Integer.toString(i), skill, 0);
			if (curskill == 0) {
				break;
			}
			if (curskill / 10000 == playerJob) {
				ret.put("skillid", curskill);
				break;
			}
		}
		if (ret.get("skillid") == null) {
			ret.put("skillid", 0);
		}
		return ret;
	}

	public List<Integer> petsCanConsume(int itemId) {
		List<Integer> ret = new ArrayList<Integer>();
		MapleData data = getItemData(itemId);
		int curPetId = 0;
		for (int i = 0; i < data.getChildren().size(); i++) {
			curPetId = MapleDataTool.getInt("spec/" + Integer.toString(i), data, 0);
			if (curPetId == 0) {
				break;
			}
			ret.add(Integer.valueOf(curPetId));
		}
		return ret;
	}

	public boolean isQuestItem(int itemId) {
		if (isQuestItemCache.containsKey(itemId)) {
			return isQuestItemCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		boolean questItem = MapleDataTool.getIntConvert("info/quest", data, 0) == 1;
		isQuestItemCache.put(itemId, questItem);
		return questItem;
	}

	public int getQuestIdFromItem(int itemId) {
		MapleData data = getItemData(itemId);
		int questItem = MapleDataTool.getIntConvert("info/quest", data, 0);
		return questItem;
	}

	private void loadCardIdData() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement("SELECT cardid, mobid FROM monstercarddata");
			rs = ps.executeQuery();
			while (rs.next()) {
				monsterBookID.put(rs.getInt(1), rs.getInt(2));
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (ps != null) {
					ps.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	public int getCardMobId(int id) {
		return monsterBookID.get(id);
	}

	public boolean isUntradeableOnEquip(int itemId) {
		if (onEquipUntradableCache.containsKey(itemId)) {
			return onEquipUntradableCache.get(itemId);
		}
		boolean untradableOnEquip = MapleDataTool.getIntConvert("info/equipTradeBlock", getItemData(itemId), 0) > 0;
		onEquipUntradableCache.put(itemId, untradableOnEquip);
		return untradableOnEquip;
	}

	public ScriptedItem getScriptedItemInfo(int itemId) {
		if (scriptedItemCache.containsKey(itemId)) {
			return scriptedItemCache.get(itemId);
		}
		if ((itemId / 10000) != 243) {
			return null;
		}
		ScriptedItem script = new ScriptedItem(MapleDataTool.getInt("spec/npc", getItemData(itemId), 0), MapleDataTool.getString("spec/script", getItemData(itemId), ""), MapleDataTool.getInt("spec/runOnPickup", getItemData(itemId), 0) == 1);
		scriptedItemCache.put(itemId, script);
		return scriptedItemCache.get(itemId);
	}

	public boolean isKarmaAble(int itemId) {
		if (karmaCache.containsKey(itemId)) {
			return karmaCache.get(itemId);
		}
		boolean bRestricted = MapleDataTool.getIntConvert("info/tradeAvailable", getItemData(itemId), 0) > 0;
		karmaCache.put(itemId, bRestricted);
		return bRestricted;
	}

	public int getStateChangeItem(int itemId) {
		if (triggerItemCache.containsKey(itemId)) {
			return triggerItemCache.get(itemId);
		} else {
			int triggerItem = MapleDataTool.getIntConvert("info/stateChangeItem", getItemData(itemId), 0);
			triggerItemCache.put(itemId, triggerItem);
			return triggerItem;
		}
	}

	public int getExpById(int itemId) {
		if (expCache.containsKey(itemId)) {
			return expCache.get(itemId);
		} else {
			int exp = MapleDataTool.getIntConvert("spec/exp", getItemData(itemId), 0);
			expCache.put(itemId, exp);
			return exp;
		}
	}

	public int getMaxLevelById(int itemId) {
		if (levelCache.containsKey(itemId)) {
			return levelCache.get(itemId);
		} else {
			int level = MapleDataTool.getIntConvert("info/maxLevel", getItemData(itemId), 256);
			levelCache.put(itemId, level);
			return level;
		}
	}

	public RewardInfo getItemReward(int itemId) {
		// Thanks Celino, used some stuffs :)
		if (rewardCache.containsKey(itemId)) {
			return rewardCache.get(itemId);
		}

		List<RewardItem> rewards = new ArrayList<RewardItem>();
		for (MapleData child : getItemData(itemId).getChildByPath("reward").getChildren()) {
			RewardItem reward = new RewardItem(child);

			rewards.add(reward);
		}
		RewardInfo info = new RewardInfo(rewards);
		rewardCache.put(itemId, info);
		return info;
	}
	
	public boolean isConsumeOnPickup(int itemId) {
		if (consumeOnPickupCache.containsKey(itemId)) {
			return consumeOnPickupCache.get(itemId);
		}
		MapleData data = getItemData(itemId);
		boolean consume = MapleDataTool.getIntConvert("spec/consumeOnPickup", data, 0) == 1 || MapleDataTool.getIntConvert("specEx/consumeOnPickup", data, 0) == 1;
		consumeOnPickupCache.put(itemId, consume);
		return consume;
	}

	public final boolean isTwoHanded(int itemId) {
		switch (getWeaponType(itemId)) {
			case AXE2H:
			case BLUNT2H:
			case BOW:
			case CLAW:
			case CROSSBOW:
			case POLE_ARM:
			case SPEAR:
			case SWORD2H:
			case GUN:
			case KNUCKLE:
				return true;
			default:
				return false;
		}
	}

	public boolean isCash(int itemId) {
		return itemId / 1000000 == 5 || getEquipStats(itemId).get("cash") == 1;
	}

	public Collection<IItem> canWearEquipment(GameCharacter chr, Collection<IItem> items) {
		Inventory inv = chr.getInventory(InventoryType.EQUIPPED);
		if (inv.checked())
			return items;
		Collection<IItem> allowedItems = new LinkedList<IItem>();
		if (chr.getJob() == Job.SUPERGM || chr.getJob() == Job.GM) {
			for (IItem item : items) {
				IEquip equip = (IEquip) item;
				equip.wear(true);
				allowedItems.add(item);
			}
			return allowedItems;
		}
		
		boolean highfivestamp = false;
		
		// Removed because players shouldn't even get this, and gm's should just be gm job. 
//		try { 
//			for (ItemInventoryEntry entry : ItemFactory.INVENTORY.loadItems(chr.getId(), false)) { 
//				if (entry.type == InventoryType.CASH) { 
//					if (entry.item.getItemId() == 5590000) { 
//						highfivestamp = true; 
//					} 
//				} 
//			} 
//		} catch (SQLException ex) { 
//		}
		
		// Initialize with base stat values.
		int totalStr = chr.getStr();
		int totalDex = chr.getDex();
		int totalInt = chr.getInt();
		int totalLuk = chr.getLuk();
		
		if (chr.getJob() != Job.SUPERGM || chr.getJob() != Job.GM) { 
			for (IItem item : inv.list()) {
				IEquip equip = (IEquip) item;
				totalStr += equip.getStr();
				totalDex += equip.getDex();
				totalLuk += equip.getLuk();
				totalInt += equip.getInt();
			}
		}
		for (IItem item : items) {
			IEquip equip = (IEquip) item;
			final Map<String, Integer> stats = getEquipStats(equip.getItemId());
			int reqLevel = stats.get("reqLevel");
			if (highfivestamp) {
				reqLevel -= 5;
				if (reqLevel < 0) {
					reqLevel = 0;
				}
			}
			
			// Really hard check, and not really needed 
			// In this one GMs should just be GM job, 
			// and players cannot change jobs.
//			int reqJob = getEquipStats(equip.getItemId()).get("reqJob"); 
//			if (reqJob != 0) {
//			}
			
			if (reqLevel > chr.getLevel()) {
				continue;			
			} else if (stats.get("reqDEX") > totalDex) {
				continue;
			} else if (stats.get("reqSTR") > totalStr) {
				continue;
			} else if (stats.get("reqLUK") > totalLuk) {
				continue;
			} else if (stats.get("reqINT") > totalInt) {
				continue;
			}
			
			int reqPOP = stats.get("reqPOP");
			int fame = chr.getFame();
			if (reqPOP > 0) {
				if (stats.get("reqPOP") > fame)
					continue;
			}
			equip.wear(true);
			allowedItems.add(equip);
		}
		inv.checked(true);
		return allowedItems;
	}

	public boolean canWearEquipment(GameCharacter chr, Equip equip) {
		if (chr.getJob() == Job.SUPERGM || chr.getJob() == Job.GM) {
			equip.wear(true);
			return true;
		}
		
		boolean highfivestamp = false;
		// Removed because players shouldn't even get this, and gm's should just be gm job. 
//		try { 
//			for (ItemInventoryEntry entry : ItemFactory.INVENTORY.loadItems(chr.getId(), false)) { 
//				if (entry.type == InventoryType.CASH) { 
//					if (entry.item.getItemId() == 5590000) { 
//						highfivestamp = true; 
//					} 
//				} 
//			} 
//		} catch (SQLException ex) { 
//		}
		
		int totalStr = chr.getStr();
		int totalDex = chr.getDex();
		int totalInt = chr.getInt();
		int totalLuk = chr.getLuk();

		for (IItem item : chr.getInventory(InventoryType.EQUIPPED).list()) {
			IEquip eq = (IEquip) item;
			totalStr += eq.getStr();
			totalDex += eq.getDex();
			totalLuk += eq.getLuk();
			totalInt += eq.getInt();
		}
		final Map<String, Integer> stats = getEquipStats(equip.getItemId());
		int reqLevel = stats.get("reqLevel");
		if (highfivestamp) {
			reqLevel -= 5;
		}

		// Removed job check. Shouldn't really be needed.
		
		boolean match = true;
		
		if (reqLevel > chr.getLevel()) {
			match = false;
		} else if (stats.get("reqDEX") > totalDex) {
			match = false;
		} else if (stats.get("reqSTR") > totalStr) {
			match = false;
		} else if (stats.get("reqLUK") > totalLuk) {
			match = false;
		} else if (stats.get("reqINT") > totalInt) {
			match = false;
		}
		
		int reqPOP = stats.get("reqPOP");
		if (reqPOP > 0) {
			if (stats.get("reqPOP") > chr.getFame())
				match = false;
		}

		equip.wear(match);
		return match;
	}

	public List<EquipLevelUpStat> getItemLevelupStats(int itemId, int level, boolean timeless) {
		List<EquipLevelUpStat> list = new LinkedList<EquipLevelUpStat>();
		MapleData data = getItemData(itemId);
		MapleData data1 = data.getChildByPath("info").getChildByPath("level");
		/*
		 * if ((timeless && level == 5) || (!timeless && level == 3)) {
		 * MapleData skilldata =
		 * data1.getChildByPath("case").getChildByPath("1")
		 * .getChildByPath(timeless ? "6" : "4"); if (skilldata != null) {
		 * List<MapleData> skills =
		 * skilldata.getChildByPath("Skill").getChildren(); for (int i = 0; i <
		 * skilldata.getChildByPath("Skill").getChildren().size(); i++) {
		 * System.
		 * out.println(MapleDataTool.getInt(skills.get(i).getChildByPath("id"
		 * ))); if (Math.random() < 0.1) list.add(new Pair<String,
		 * Integer>("Skill" + 0,
		 * MapleDataTool.getInt(skills.get(i).getChildByPath("id")))); } } }
		 */
		if (data1 != null) {
			MapleData data2 = data1.getChildByPath("info").getChildByPath(Integer.toString(level));
			if (data2 != null) {
				for (MapleData da : data2.getChildren()) {
					if (Math.random() < 0.9) {
						final String name = da.getName();
						final int lowerBound = MapleDataTool.getInt(da);
						if (name.startsWith("incDEXMin")) {
							list.add(new EquipLevelUpStat("incDEX", Randomizer.rand(lowerBound, MapleDataTool.getInt("incDEXMax", data2))));
						} else if (name.startsWith("incSTRMin")) {
							list.add(new EquipLevelUpStat("incSTR", Randomizer.rand(lowerBound, MapleDataTool.getInt("incSTRMax", data2))));
						} else if (name.startsWith("incINTMin")) {
							list.add(new EquipLevelUpStat("incINT", Randomizer.rand(lowerBound, MapleDataTool.getInt("incINTMax", data2))));
						} else if (name.startsWith("incLUKMin")) {
							list.add(new EquipLevelUpStat("incLUK", Randomizer.rand(lowerBound, MapleDataTool.getInt("incLUKMax", data2))));
						} else if (name.startsWith("incMHPMin")) {
							list.add(new EquipLevelUpStat("incMHP", Randomizer.rand(lowerBound, MapleDataTool.getInt("incMHPMax", data2))));
						} else if (name.startsWith("incMMPMin")) {
							list.add(new EquipLevelUpStat("incMMP", Randomizer.rand(lowerBound, MapleDataTool.getInt("incMMPMax", data2))));
						} else if (name.startsWith("incPADMin")) {
							list.add(new EquipLevelUpStat("incPAD", Randomizer.rand(lowerBound, MapleDataTool.getInt("incPADMax", data2))));
						} else if (name.startsWith("incMADMin")) {
							list.add(new EquipLevelUpStat("incMAD", Randomizer.rand(lowerBound, MapleDataTool.getInt("incMADMax", data2))));
						} else if (name.startsWith("incPDDMin")) {
							list.add(new EquipLevelUpStat("incPDD", Randomizer.rand(lowerBound, MapleDataTool.getInt("incPDDMax", data2))));
						} else if (name.startsWith("incMDDMin")) {
							list.add(new EquipLevelUpStat("incMDD", Randomizer.rand(lowerBound, MapleDataTool.getInt("incMDDMax", data2))));
						} else if (name.startsWith("incACCMin")) {
							list.add(new EquipLevelUpStat("incACC", Randomizer.rand(lowerBound, MapleDataTool.getInt("incACCMax", data2))));
						} else if (name.startsWith("incEVAMin")) {
							list.add(new EquipLevelUpStat("incEVA", Randomizer.rand(lowerBound, MapleDataTool.getInt("incEVAMax", data2))));
						} else if (name.startsWith("incSpeedMin")) {
							list.add(new EquipLevelUpStat("incSpeed", Randomizer.rand(lowerBound, MapleDataTool.getInt("incSpeedMax", data2))));
						} else if (name.startsWith("incJumpMin")) {
							list.add(new EquipLevelUpStat("incJump", Randomizer.rand(lowerBound, MapleDataTool.getInt("incJumpMax", data2))));
						}
					}
				}
			}
		}

		return list;
	}

	public class ScriptedItem {
		public final boolean runOnPickup;
		public final int npcId;
		public final String script;

		public ScriptedItem(int npc, String script, boolean rop) {
			this.npcId = npc;
			this.script = script;
			this.runOnPickup = rop;
		}
	}
}