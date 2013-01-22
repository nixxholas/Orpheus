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
package scripting.npc;

import client.Equip;
import client.IItem;
import client.ISkill;
import client.ItemFactory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import constants.ExpTable;
import constants.ServerConstants;
import client.GameCharacter;
import client.GameClient;
import client.Inventory;
import client.InventoryType;
import client.Job;
import client.Pet;
import client.SkinColor;
import client.Stat;
import client.SkillFactory;
import tools.Randomizer;
import java.io.File;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import net.server.Channel;
import tools.DatabaseConnection;
import net.server.Party;
import net.server.PartyCharacter;
import net.server.Server;
import net.server.guild.Alliance;
import net.server.guild.Guild;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import scripting.AbstractPlayerInteraction;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.ShopFactory;
import server.StatEffect;
import server.events.gm.GameEvent;
import server.expeditions.Expedition;
import server.maps.GameMap;
import server.maps.GameMapFactory;
import server.partyquest.Pyramid;
import server.partyquest.Pyramid.PyramidMode;
import server.quest.Quest;
import tools.PacketCreator;

/**
 * 
 * @author Matze
 */
public class NPCConversationManager extends AbstractPlayerInteraction {

	private int npc;
	private String getText;

	public NPCConversationManager(GameClient c, int npc) {
		super(c);
		this.npc = npc;
	}

	public int getNpc() {
		return npc;
	}

	public void dispose() {
		NPCScriptManager.getInstance().dispose(this);
	}

	public void sendNext(String text) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", (byte) 0));
	}

	public void sendPrev(String text) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", (byte) 0));
	}

	public void sendNextPrev(String text) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", (byte) 0));
	}

	public void sendOk(String text) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
	}

	public void sendYesNo(String text) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 1, text, "", (byte) 0));
	}

	public void sendAcceptDecline(String text) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0x0C, text, "", (byte) 0));
	}

	public void sendSimple(String text) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 4, text, "", (byte) 0));
	}

	public void sendNext(String text, byte speaker) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 01", speaker));
	}

	public void sendPrev(String text, byte speaker) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 00", speaker));
	}

	public void sendNextPrev(String text, byte speaker) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "01 01", speaker));
	}

	public void sendOk(String text, byte speaker) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0, text, "00 00", speaker));
	}

	public void sendYesNo(String text, byte speaker) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 1, text, "", speaker));
	}

	public void sendAcceptDecline(String text, byte speaker) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 0x0C, text, "", speaker));
	}

	public void sendSimple(String text, byte speaker) {
		getClient().announce(PacketCreator.getNPCTalk(npc, (byte) 4, text, "", speaker));
	}

	public void sendStyle(String text, int styles[]) {
		getClient().announce(PacketCreator.getNPCTalkStyle(npc, text, styles));
	}

	public void sendGetNumber(String text, int def, int min, int max) {
		getClient().announce(PacketCreator.getNPCTalkNum(npc, text, def, min, max));
	}

	public void sendGetText(String text) {
		getClient().announce(PacketCreator.getNPCTalkText(npc, text, ""));
	}

	/*
	 * 0 = ariant colliseum 1 = Dojo 2 = Carnival 1 3 = Carnival 2 4 = Ghost
	 * Ship PQ? 5 = Pyramid PQ 6 = Kerning Subway
	 */
	public void sendDimensionalMirror(String text) {
		getClient().announce(PacketCreator.getDimensionalMirror(text));
	}

	public void setGetText(String text) {
		this.getText = text;
	}

	public String getText() {
		return this.getText;
	}

	public int getJobId() {
		return getPlayer().getJob().getId();
	}

	public void startQuest(short id) {
		try {
			Quest.getInstance(id).forceStart(getPlayer(), npc);
		} catch (NullPointerException ex) {
		}
	}

	public void completeQuest(short id) {
		try {
			Quest.getInstance(id).forceComplete(getPlayer(), npc);
		} catch (NullPointerException ex) {
		}
	}

	public int getMeso() {
		return getPlayer().getMeso();
	}

	public void gainMeso(int gain) {
		getPlayer().gainMeso(gain, true, false, true);
	}

	public void gainExp(int gain) {
		getPlayer().gainExp(gain, true, true);
	}

	public int getLevel() {
		return getPlayer().getLevel();
	}

	public void showEffect(String effect) {
		getPlayer().getMap().broadcastMessage(PacketCreator.environmentChange(effect, 3));
	}

	public void playSound(String sound) {
		getPlayer().getMap().broadcastMessage(PacketCreator.environmentChange(sound, 4));
	}

	public void setHair(int hair) {
		getPlayer().setHair(hair);
		getPlayer().updateSingleStat(Stat.HAIR, hair);
		getPlayer().equipChanged();
	}

	public void setFace(int face) {
		getPlayer().setFace(face);
		getPlayer().updateSingleStat(Stat.FACE, face);
		getPlayer().equipChanged();
	}

	public void setSkin(int color) {
		getPlayer().setSkinColor(SkinColor.getById(color));
		getPlayer().updateSingleStat(Stat.SKIN, color);
		getPlayer().equipChanged();
	}

	public int itemQuantity(int itemid) {
		return getPlayer().getInventory(ItemInfoProvider.getInstance().getInventoryType(itemid)).countById(itemid);
	}

	public void displayGuildRanks() {
		Guild.displayGuildRanks(getClient(), npc);
	}

	@Override
	public Party getParty() {
		return getPlayer().getParty();
	}

	@Override
	public void resetMap(int mapid) {
		getClient().getChannelServer().getMapFactory().getMap(mapid).resetReactors();
	}

	public void gainCloseness(int closeness) {
		for (Pet pet : getPlayer().getPets()) {
			if (pet.getCloseness() > 30000) {
				pet.setCloseness(30000);
				return;
			}
			pet.gainCloseness(closeness);
			while (pet.getCloseness() > ExpTable.getClosenessNeededForLevel(pet.getLevel())) {
				pet.setLevel((byte) (pet.getLevel() + 1));
				byte index = getPlayer().getPetIndex(pet);
				getClient().announce(PacketCreator.showOwnPetLevelUp(index));
				getPlayer().getMap().broadcastMessage(getPlayer(), PacketCreator.showPetLevelUp(getPlayer(), index));
			}
			IItem petz = getPlayer().getInventory(InventoryType.CASH).getItem(pet.getSlot());
			getPlayer().getClient().announce(PacketCreator.updateSlot(petz));
		}
	}

	public String getName() {
		return getPlayer().getName();
	}

	public int getGender() {
		return getPlayer().getGender();
	}

	public void changeJobById(int a) {
		getPlayer().changeJob(Job.getById(a));
	}

	public void addRandomItem(int id) {
		ItemInfoProvider i = ItemInfoProvider.getInstance();
		InventoryManipulator.addFromDrop(getClient(), i.randomizeStats((Equip) i.getEquipById(id)), true);
	}

	public Job getJobName(int id) {
		return Job.getById(id);
	}

	public StatEffect getItemEffect(int itemId) {
		return ItemInfoProvider.getInstance().getItemEffect(itemId);
	}

	public void resetStats() {
		getPlayer().resetStats();
	}

	public void maxMastery() {
		// TODO: Uh... do we not cache this?
		for (MapleData skillData : MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img").getChildren()) {
            try {
                ISkill skill = SkillFactory.getSkill(Integer.parseInt(skillData.getName()));
                getPlayer().changeSkillLevel(skill, (byte) skill.getMaxLevel(), skill.getMaxLevel(), -1);
            } catch (NumberFormatException nfe) {
            	// TODO: Dude. C'mon.
                break;
            } catch (NullPointerException npe) {
            	// TODO: Really? REALLY?
                continue;
            }
        }
	}

	public void processGachapon(int[] id, boolean remote) {
		int[] gacMap = {100000000, 101000000, 102000000, 103000000, 105040300, 800000000, 809000101, 809000201, 600000000, 120000000};
		int itemid = id[Randomizer.nextInt(id.length)];
		addRandomItem(itemid);
		if (!remote) {
			gainItem(5220000, (short) -1);
		}
		sendNext("You have obtained a #b#t" + itemid + "##k.");
		if (ServerConstants.BROADCAST_GACHAPON_ITEMS) {
			getClient().getChannelServer().broadcastPacket(PacketCreator.gachaponMessage(getPlayer().getInventory(InventoryType.fromByte((byte) (itemid / 1000000))).findById(itemid), c.getChannelServer().getMapFactory().getMap(gacMap[(getNpc() != 9100117 && getNpc() != 9100109) ? (getNpc() - 9100100) : getNpc() == 9100109 ? 8 : 9]).getMapName(), getPlayer()));
		}
	}

	public void disbandAlliance(GameClient c, int allianceId) {
		PreparedStatement ps = null;
		try {
			ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM `alliance` WHERE id = ?");
			ps.setInt(1, allianceId);
			ps.executeUpdate();
			ps.close();
			Server.getInstance().allianceMessage(c.getPlayer().getGuild().getAllianceId(), PacketCreator.disbandAlliance(allianceId), -1, -1);
			Server.getInstance().disbandAlliance(allianceId);
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
				}
			} catch (SQLException ex) {
			}
		}
	}

	public boolean canBeUsedAllianceName(String name) {
		if (name.contains(" ") || name.length() > 12) {
			return false;
		}
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM alliance WHERE name = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				ps.close();
				rs.close();
				return false;
			}
			ps.close();
			rs.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Alliance createAlliance(GameCharacter chr1, GameCharacter chr2, String name) {
		int id = 0;
		int guild1 = chr1.getGuildId();
		int guild2 = chr2.getGuildId();
		try {
			PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO `alliance` (`name`, `guild1`, `guild2`) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setInt(2, guild1);
			ps.setInt(3, guild2);
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			id = rs.getInt(1);
			rs.close();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		Alliance alliance = new Alliance(name, id, guild1, guild2);
		try {
			Server.getInstance().setGuildAllianceId(guild1, id);
			Server.getInstance().setGuildAllianceId(guild2, id);
			chr1.setAllianceRank(1);
			chr1.saveGuildStatus();
			chr2.setAllianceRank(2);
			chr2.saveGuildStatus();
			Server.getInstance().addAlliance(id, alliance);
			Server.getInstance().allianceMessage(id, PacketCreator.makeNewAlliance(alliance, chr1.getClient()), -1, -1);
		} catch (Exception e) {
			return null;
		}
		return alliance;
	}

	public List<GameCharacter> getPartyMembers() {
		if (getPlayer().getParty() == null) {
			return null;
		}
		List<GameCharacter> chars = new LinkedList<GameCharacter>();
		for (Channel channel : Server.getInstance().getChannelsFromWorld(getPlayer().getWorldId())) {
			for (GameCharacter chr : channel.getPartyMembers(getPlayer().getParty())) {
				if (chr != null) {
					chars.add(chr);
				}
			}
		}
		return chars;
	}

	public void warpParty(int id) {
		for (GameCharacter player : getPartyMembers()) {
			if (id == 925020100) {
				player.getDojoState().setDojoParty(true);
			}
			player.changeMap(getWarpMap(id));
		}
	}

	public boolean hasMerchant() {
		return getPlayer().hasMerchant();
	}

	public boolean hasMerchantItems() {
		try {
			if (!ItemFactory.MERCHANT.loadItems(getPlayer().getId(), false).isEmpty()) {
				return true;
			}
		} catch (SQLException e) {
			return false;
		}
		if (getPlayer().getMerchantMeso() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public void showFredrick() {
		c.announce(PacketCreator.getFredrick(getPlayer()));
	}

	public int partyMembersInMap() {
		int inMap = 0;
		for (GameCharacter char2 : getPlayer().getMap().getCharacters()) {
			if (char2.getParty() == getPlayer().getParty()) {
				inMap++;
			}
		}
		return inMap;
	}

	public GameEvent getEvent() {
		return c.getChannelServer().getEvent();
	}

	public void divideTeams() {
		if (getEvent() != null) {
			getPlayer().setTeam(getEvent().getLimit() % 2); // muhaha :D
		}
	}

	public Expedition createExpedition(String type, byte min) {
		Party party = getPlayer().getParty();
		if (party == null || party.getMembers().size() < min)
			return null;
		return new Expedition(getPlayer());
	}

	public boolean createPyramid(String mode, boolean party) {// lol
		PyramidMode mod = PyramidMode.valueOf(mode);

		Party partyz = getPlayer().getParty();
		GameMapFactory mf = c.getChannelServer().getMapFactory();

		GameMap map = null;
		int mapid = 926010100;
		if (party) {
			mapid += 10000;
		}
		mapid += (mod.getMode() * 1000);

		for (byte b = 0; b < 5; b++) {// They cannot warp to the next map before
										// the timer ends (:
			map = mf.getMap(mapid + b);
			if (map.getCharacters().size() > 0) {
				map = null;
				continue;
			} else {
				break;
			}
		}

		if (map == null) {
			return false;
		}

		if (!party) {
			partyz = new Party(-1, new PartyCharacter(getPlayer()));
		}
		Pyramid py = new Pyramid(partyz, mod, map.getId());
		getPlayer().setPartyQuest(py);
		py.warp(mapid);
		dispose();
		return true;
	}
	
	public void openShop(int id) {
		dispose();
		ShopFactory.getInstance().getShop(id).sendShop(c);
	}
	
	public void warp(int id) {
		dispose();
		getPlayer().changeMap(id);
	}
	
	public void warp(int id, int portal) {
		dispose();
		getPlayer().changeMap(id, portal);
	}
	
	public String getServerName() {
		return ServerConstants.SERVER_NAME;
	}
	
	public int getWorld() {
		return getPlayer().getWorldId();
	}
	
	public String listEquips() {
		StringBuilder sb = new StringBuilder();
		Inventory mi = getPlayer().getInventory(InventoryType.EQUIP);
		for (IItem i : mi.list()) {
			sb.append("#L" + i.getSlot() + "##v" + i.getItemId() + "##l");
		}
		return sb.toString();
	}
	
	public boolean hasItem(int itemid) {
		return getPlayer().haveItem(itemid);
	}
	
	public boolean hasItem(int itemid, int quantity) {
		return (getPlayer().getItemQuantity(itemid, false) > quantity);
	}
	
	public int getItemId(byte slot) {
		return getPlayer().getInventory(InventoryType.EQUIP).getItem(slot).getItemId();
	}
	
	public void setItemOwner(byte slot) {
		Inventory equip = getPlayer().getInventory(InventoryType.EQUIP);
        Equip eu = (Equip) equip.getItem(slot);
        eu.setOwner(getName());
	}
	
	public void makeItemEpic(byte slot) {
		Inventory equip = getPlayer().getInventory(InventoryType.EQUIP);
        Equip eu = (Equip) equip.getItem(slot);
		eu.setStr(Short.MAX_VALUE);
		eu.setDex(Short.MAX_VALUE);
		eu.setInt(Short.MAX_VALUE);
		eu.setLuk(Short.MAX_VALUE);
		eu.setAcc(Short.MAX_VALUE);
		eu.setAvoid(Short.MAX_VALUE);
		eu.setWatk(Short.MAX_VALUE);
		eu.setWdef(Short.MAX_VALUE);
		eu.setMatk(Short.MAX_VALUE);
		eu.setMdef(Short.MAX_VALUE);
		eu.setHp(Short.MAX_VALUE);
		eu.setMp(Short.MAX_VALUE);
		eu.setJump(Short.MAX_VALUE);
		eu.setSpeed(Short.MAX_VALUE);
		eu.setOwner(getName());
		getPlayer().equipChanged();
	}
	
	public void modifyItem(byte slot, String stat, short value) {
		Inventory equip = getPlayer().getInventory(InventoryType.EQUIP);
        Equip eu = (Equip) equip.getItem(slot);
		if (stat.equalsIgnoreCase("str") || stat.equalsIgnoreCase("strength")) {
	        eu.setStr(value);
		} else if (stat.equalsIgnoreCase("dex") || stat.equalsIgnoreCase("dexterity")) {
	        eu.setDex(value);
		} else if (stat.equalsIgnoreCase("int") || stat.equalsIgnoreCase("intellect")) {
	        eu.setInt(value);
		} else if (stat.equalsIgnoreCase("luk") || stat.equalsIgnoreCase("luck")) {
	        eu.setLuk(value);
		} else if (stat.equalsIgnoreCase("hp") || stat.equalsIgnoreCase("maxhp")) {
	        eu.setHp(value);
		} else if (stat.equalsIgnoreCase("mp") || stat.equalsIgnoreCase("maxmp")) {
	        eu.setMp(value);
		} else if (stat.equalsIgnoreCase("acc") || stat.equalsIgnoreCase("accuracy")) {
	        eu.setAcc(value);
		} else if (stat.equalsIgnoreCase("avoid") || stat.equalsIgnoreCase("avoidability")) {
	        eu.setAvoid(value);
		} else if (stat.equalsIgnoreCase("watk") || stat.equalsIgnoreCase("wattack")) {
	        eu.setWatk(value);
		} else if (stat.equalsIgnoreCase("matk") || stat.equalsIgnoreCase("mattack")) {
	        eu.setMatk(value);
		} else if (stat.equalsIgnoreCase("wdef") || stat.equalsIgnoreCase("wdefense")) {
	        eu.setWdef(value);
		} else if (stat.equalsIgnoreCase("mdef") || stat.equalsIgnoreCase("mdefense")) {
	        eu.setMdef(value);
		} else if (stat.equalsIgnoreCase("jump")) {
	        eu.setJump(value);
		} else if (stat.equalsIgnoreCase("speed")) {
			eu.setSpeed(value);
		} else if (stat.equalsIgnoreCase("upgrades")) {
			eu.setUpgradeSlots(value);
		}
		getPlayer().equipChanged();
	}
}
