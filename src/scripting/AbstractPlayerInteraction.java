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
package scripting;

import java.util.Arrays;
import java.util.List;
import client.Equip;
import client.IItem;
import client.ISkill;
import client.GameCharacter;
import client.GameClient;
import client.Inventory;
import client.InventoryType;
import client.Pet;
import client.QuestCompletionState;
import client.SkillFactory;
import constants.ItemConstants;
import java.awt.Point;
import net.server.Party;
import net.server.Server;
import net.server.guild.Guild;
import scripting.event.EventManager;
import scripting.npc.NPCScriptManager;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.life.LifeFactory;
import server.life.Monster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.GameMap;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.partyquest.Pyramid;
import server.quest.Quest;
import tools.PacketCreator;

public class AbstractPlayerInteraction {
	public GameClient c;

	public AbstractPlayerInteraction(GameClient c) {
		this.c = c;
	}

	public GameClient getClient() {
		return c;
	}

	public GameCharacter getPlayer() {
		return c.getPlayer();
	}

	public void warp(int map) {
		getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(0));
	}

	public void warp(int map, int portal) {
		getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
	}

	public void warp(int map, String portal) {
		getPlayer().changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal));
	}

	public void warpMap(int map) {
		for (GameCharacter player : getPlayer().getMap().getCharacters()) {
			player.changeMap(getWarpMap(map), getWarpMap(map).getPortal(0));
		}
	}

	protected GameMap getWarpMap(int map) {
		GameMap target;
		if (getPlayer().getEventInstance() == null) {
			target = c.getChannelServer().getMapFactory().getMap(map);
		} else {
			target = getPlayer().getEventInstance().getMapInstance(map);
		}
		return target;
	}

	public GameMap getMap(int map) {
		return getWarpMap(map);
	}

	public EventManager getEventManager(String event) {
		return getClient().getChannelServer().getEventSM().getEventManager(event);
	}

	public boolean haveItem(int itemid) {
		return haveItem(itemid, 1);
	}

	public boolean haveItem(int itemid, int quantity) {
		return getPlayer().getItemQuantity(itemid, false) >= quantity;
	}

	public boolean canHold(int itemid) {
		return getPlayer().getInventory(ItemInfoProvider.getInstance().getInventoryType(itemid)).getNextFreeSlot() > -1;
	}

	public void openNpc(int npcid) {
		NPCScriptManager.getInstance().dispose(c);
		NPCScriptManager.getInstance().start(c, npcid, null, null);
	}

	public void updateQuest(int questid, String status) {
		c.announce(PacketCreator.updateQuest((short) questid, status));
	}

	public QuestCompletionState getQuestStatus(int id) {
		return c.getPlayer().getQuest(Quest.getInstance(id)).getCompletionState();
	}

	public boolean isQuestCompleted(int quest) {
		try {
			return getQuestStatus(quest) == QuestCompletionState.COMPLETED;
		} catch (NullPointerException e) {
			return false;
		}
	}

	public boolean isQuestStarted(int quest) {
		try {
			return getQuestStatus(quest) == QuestCompletionState.STARTED;
		} catch (NullPointerException e) {
			return false;
		}
	}

	public int getQuestProgress(int qid) {
		return Integer.parseInt(getPlayer().getQuest(Quest.getInstance(29932)).getProgress().get(0));
	}

	public void gainItem(int id, short quantity) {
		gainItem(id, quantity, false);
	}

	public void gainItem(int id) {
		gainItem(id, (short) 1, false);
	}

	public void gainItem(int id, short quantity, boolean randomStats) {
		if (ItemConstants.isPet(id)) {
			final int uniqueId = Pet.createPet(id);
			InventoryManipulator.addById(c, id, (short) 1, null, uniqueId, -1);
		}
		if (quantity >= 0) {
			ItemInfoProvider ii = ItemInfoProvider.getInstance();
			IItem item = ii.getEquipById(id);
			if (!InventoryManipulator.checkSpace(c, id, quantity, "")) {
				c.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + ii.getInventoryType(id).name() + " inventory.");
				return;
			}
			if (ii.getInventoryType(id).equals(InventoryType.EQUIP) && !ItemConstants.isRechargable(item.getItemId())) {
				if (randomStats) {
					InventoryManipulator.addFromDrop(c, ii.randomizeStats((Equip) item), false);
				} else {
					InventoryManipulator.addFromDrop(c, (Equip) item, false);
				}
			} else {
				InventoryManipulator.addById(c, id, quantity);
			}
		} else {
			InventoryManipulator.removeById(c, ItemInfoProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
		}
		c.announce(PacketCreator.getShowItemGain(id, quantity, true));
	}

	public void changeMusic(String songName) {
		getPlayer().getMap().broadcastMessage(PacketCreator.musicChange(songName));
	}

	public void playerMessage(int type, String message) {
		c.announce(PacketCreator.serverNotice(type, message));
	}

	public void message(String message) {
		getPlayer().message(message);
	}

	public void mapMessage(int type, String message) {
		getPlayer().getMap().broadcastMessage(PacketCreator.serverNotice(type, message));
	}

	public void mapEffect(String path) {
		c.announce(PacketCreator.mapEffect(path));
	}

	public void mapSound(String path) {
		c.announce(PacketCreator.mapSound(path));
	}

	public void showIntro(String path) {
		c.announce(PacketCreator.showIntro(path));
	}

	public void showInfo(String path) {
		c.announce(PacketCreator.showInfo(path));
		c.announce(PacketCreator.enableActions());
	}

	public void guildMessage(int type, String message) {
		if (getGuild() != null) {
			getGuild().guildMessage(PacketCreator.serverNotice(type, message));
		}
	}

	public Guild getGuild() {
		try {
			return Server.getInstance().getGuild(getPlayer().getGuildId(), null);
		} catch (Exception e) {
		}
		return null;
	}

	public Party getParty() {
		return getPlayer().getParty();
	}

	public boolean isLeader() {
		return getParty().getLeader().equals(getPlayer().getMPC());
	}

	public void givePartyItems(int id, short quantity, List<GameCharacter> party) {
		for (GameCharacter chr : party) {
			GameClient cl = chr.getClient();
			if (quantity >= 0) {
				InventoryManipulator.addById(cl, id, quantity);
			} else {
				InventoryManipulator.removeById(cl, ItemInfoProvider.getInstance().getInventoryType(id), id, -quantity, true, false);
			}
			cl.announce(PacketCreator.getShowItemGain(id, quantity, true));
		}
	}

	public void givePartyExp(int amount, List<GameCharacter> party) {
		for (GameCharacter chr : party) {
			chr.gainExp((int) (amount * chr.rates().exp()), true, true);
		}
	}

	public void removeFromParty(int id, List<GameCharacter> party) {
		for (GameCharacter chr : party) {
			GameClient cl = chr.getClient();
			InventoryType type = ItemInfoProvider.getInstance().getInventoryType(id);
			Inventory iv = cl.getPlayer().getInventory(type);
			int possesed = iv.countById(id);
			if (possesed > 0) {
				InventoryManipulator.removeById(c, ItemInfoProvider.getInstance().getInventoryType(id), id, possesed, true, false);
				cl.announce(PacketCreator.getShowItemGain(id, (short) -possesed, true));
			}
		}
	}

	public void removeAll(int id) {
		removeAll(id, c);
	}

	public void removeAll(int id, GameClient cl) {
		int possessed = cl.getPlayer().getInventory(ItemInfoProvider.getInstance().getInventoryType(id)).countById(id);
		if (possessed > 0) {
			InventoryManipulator.removeById(cl, ItemInfoProvider.getInstance().getInventoryType(id), id, possessed, true, false);
			cl.announce(PacketCreator.getShowItemGain(id, (short) -possessed, true));
		}
	}

	public int getMapId() {
		return c.getPlayer().getMap().getId();
	}

	public int getPlayerCount(int mapid) {
		return c.getChannelServer().getMapFactory().getMap(mapid).getCharacters().size();
	}

	public void showInstruction(String msg, int width, int height) {
		c.announce(PacketCreator.sendHint(msg, width, height));
		c.announce(PacketCreator.enableActions());
	}

	public void disableMinimap() {
		c.announce(PacketCreator.disableMinimap());
	}

	public void resetMap(int mapid) {
		getMap(mapid).resetReactors();
		getMap(mapid).killAllMonsters();
		for (GameMapObject i : getMap(mapid).getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.ITEM))) {
			getMap(mapid).removeMapObject(i);
			getMap(mapid).broadcastMessage(PacketCreator.removeItemFromMap(i.getObjectId(), 0, c.getPlayer().getId()));
		}
	}

	public void sendClock(GameClient d, int time) {
		d.announce(PacketCreator.getClock((int) (time - System.currentTimeMillis()) / 1000));
	}

	public void useItem(int id) {
		ItemInfoProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer());
		c.announce(PacketCreator.getItemMessage(id));// Useful shet :3
	}

	public void giveTutorialSkills() {
		if (getPlayer().getMapId() == 914000100) {
			ISkill skill = SkillFactory.getSkill(20000018);
			ISkill skill0 = SkillFactory.getSkill(20000017);
			getPlayer().changeSkillLevel(skill, (byte) 1, 1, -1);
			getPlayer().changeSkillLevel(skill0, (byte) 1, 1, -1);
		} else if (getPlayer().getMapId() == 914000200) {
			ISkill skill = SkillFactory.getSkill(20000015);
			ISkill skill0 = SkillFactory.getSkill(20000014);
			getPlayer().changeSkillLevel(skill, (byte) 1, 1, -1);
			getPlayer().changeSkillLevel(skill0, (byte) 1, 1, -1);
		} else if (getPlayer().getMapId() == 914000210) {
			ISkill skill = SkillFactory.getSkill(20000016);
			getPlayer().changeSkillLevel(skill, (byte) 1, 1, -1);
		}
	}

	public void removeAranPoleArm() {
		IItem tempItem = c.getPlayer().getInventory(InventoryType.EQUIPPED).getItem((byte) -11);
		InventoryManipulator.removeFromSlot(c.getPlayer().getClient(), InventoryType.EQUIPPED, (byte) -11, tempItem.getQuantity(), false, true);
	}

	public void spawnMonster(int id, int x, int y) {
		Monster monster = LifeFactory.getMonster(id);
		monster.setPosition(new Point(x, y));
		getPlayer().getMap().spawnMonster(monster);
	}

	public void spawnGuide() {
		c.announce(PacketCreator.spawnGuide(true));
	}

	public void removeGuide() {
		c.announce(PacketCreator.spawnGuide(false));
	}

	public void displayGuide(int num) {
		c.announce(PacketCreator.showInfo("UI/tutorial.img/" + num));
	}

	public void talkGuide(String message) {
		c.announce(PacketCreator.talkGuide(message));
	}

	public void guideHint(int hint) {
		c.announce(PacketCreator.guideHint(hint));
	}

	public void updateAranIntroState(String mode) {
		c.getPlayer().addAreaData(21002, mode);
		c.announce(PacketCreator.updateAreaInfo(mode, 21002));
	}

	public void updateAranIntroState2(String mode) {
		c.getPlayer().addAreaData(21019, mode);
		c.announce(PacketCreator.updateAreaInfo(mode, 21019));
	}

	public boolean getAranIntroState(String mode) {
		if (c.getPlayer().area_data.contains(mode)) {
			return true;
		}
		return false;
	}

	public void updateCygnusIntroState(String mode) {
		c.getPlayer().addAreaData(20021, mode);
		c.announce(PacketCreator.updateAreaInfo(mode, 20021));
	}

	public boolean getCygnusIntroState(String mode) {
		if (c.getPlayer().area_data.contains(mode)) {
			return true;
		}
		return false;
	}

	public MobSkill getMobSkill(int skill, int level) {
		return MobSkillFactory.getMobSkill(skill, level);
	}

	public void earnTitle(String msg) {
		c.announce(PacketCreator.earnTitleMessage(msg));
	}

	public void showInfoText(String msg) {
		c.announce(PacketCreator.showInfoText(msg));
	}

	public void openUI(byte ui) {
		c.announce(PacketCreator.openUI(ui));
	}

	public void lockUI() {
		c.announce(PacketCreator.disableUI(true));
		c.announce(PacketCreator.lockUI(true));
	}

	public void unlockUI() {
		c.announce(PacketCreator.disableUI(false));
		c.announce(PacketCreator.lockUI(false));
	}

	public void environmentChange(String env, int mode) {
		getPlayer().getMap().broadcastMessage(PacketCreator.environmentChange(env, mode));
	}

	public Pyramid getPyramid() {
		return (Pyramid) getPlayer().getPartyQuest();
	}
}
