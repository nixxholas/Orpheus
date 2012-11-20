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

import client.IItem;
import client.GameCharacter;
import client.GameClient;
import client.Inventory;
import client.InventoryType;
import tools.Output;
import tools.Randomizer;
import java.util.Arrays;
import java.util.List;
import net.AbstractMaplePacketHandler;
import server.InventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.quest.MapleQuest;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class AdminCommandHandler extends AbstractMaplePacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea, GameClient c) {
		final GameCharacter player = c.getPlayer();
		if (!player.isGM()) {
			return;
		}
		byte mode = slea.readByte();
		String victim;
		GameCharacter target;
		switch (mode) {
			case 0x00: // Level1~Level8 & Package1~Package2
				int[][] toSpawn = MapleItemInformationProvider.getInstance().getSummonMobs(slea.readInt());
				for (int z = 0; z < toSpawn.length; z++) {
					int[] toSpawnChild = toSpawn[z];
					if (Randomizer.nextInt(101) <= toSpawnChild[1]) {
						player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(toSpawnChild[0]), player.getPosition());
					}
				}
				c.announce(PacketCreator.enableActions());
				break;
			case 0x01: { // /d (inv)
				final byte typeByte = slea.readByte();
				final InventoryType type = InventoryType.fromByte(typeByte);
				final Inventory in = player.getInventory(type);
				for (byte i = 0; i < in.getSlotLimit(); i++) {
					final IItem item = in.getItem(i);
					if (item != null) {
						InventoryManipulator.removeFromSlot(c, type, i, item.getQuantity(), false);
					}
					return;
				}
				break;
			}
			case 0x02: // Exp
				player.setExp(slea.readInt());
				break;
			case 0x03: // Ban
				victim = slea.readMapleAsciiString();
				String reason = victim + " permanent banned by " + player.getName();
				target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
				if (target != null) {
					String readableTargetName = GameCharacter.makeMapleReadable(target.getName());
					String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
					reason += readableTargetName + " (IP: " + ip + ")";
					target.ban(reason);
					target.sendPolice("You have been blocked by #b" + player.getName() + " #kfor the HACK reason.");
					c.announce(PacketCreator.getGMEffect(4, (byte) 0));
				} else if (GameCharacter.ban(victim, reason, false)) {
					c.announce(PacketCreator.getGMEffect(4, (byte) 0));
				} else {
					c.announce(PacketCreator.getGMEffect(6, (byte) 1));
				}
				break;
			case 0x04: // Block
				victim = slea.readMapleAsciiString();
				slea.readByte(); // type
				int duration = slea.readInt();
				String description = slea.readMapleAsciiString();
				reason = player.getName() + " used /ban to ban";
				target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
				if (target != null) {
					String readableTargetName = GameCharacter.makeMapleReadable(target.getName());
					String ip = target.getClient().getSession().getRemoteAddress().toString().split(":")[0];
					reason += readableTargetName + " (IP: " + ip + ")";
					if (duration == -1) {
						target.ban(description + " " + reason);
					} else {
						// target.tempban(reason, duration, type);
					}
					c.announce(PacketCreator.getGMEffect(4, (byte) 0));
				} else if (GameCharacter.ban(victim, reason, false)) {
					c.announce(PacketCreator.getGMEffect(4, (byte) 0));
				} else {
					c.announce(PacketCreator.getGMEffect(6, (byte) 1));
				}
				break;
			case 0x10: // /h, information by vana
				StringBuilder sb = new StringBuilder("USERS ON THIS MAP: ");
				for (GameCharacter character : player.getMap().getCharacters()) {
					sb.append(character.getName());
					sb.append(" ");
				}
				player.message(sb.toString());
				break;
			case 0x12: // Send
				victim = slea.readMapleAsciiString();
				int mapId = slea.readInt();
				c.getChannelServer().getPlayerStorage().getCharacterByName(victim).changeMap(c.getChannelServer().getMapFactory().getMap(mapId));
				break;
			case 0x15: // Kill
				int mobToKill = slea.readInt();
				int amount = slea.readInt();
				List<MapleMapObject> monsterx = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
				for (int x = 0; x < amount; x++) {
					MapleMonster monster = (MapleMonster) monsterx.get(x);
					if (monster.getId() == mobToKill) {
						player.getMap().killMonster(monster, player, true);
						monster.giveExpToCharacter(player, monster.getExp(), true, 1);
					}
				}
				break;
			case 0x16: // Questreset
				MapleQuest.getInstance(slea.readShort()).reset(player);
				break;
			case 0x17: // Summon
				int mobId = slea.readInt();
				int quantity = slea.readInt();
				for (int i = 0; i < quantity; i++) {
					player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(mobId), player.getPosition());
				}
				break;
			case 0x18: // Maple & Mobhp
				int mobHp = slea.readInt();
				player.dropMessage("Monsters HP");
				List<MapleMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
				for (MapleMapObject mobs : monsters) {
					MapleMonster monster = (MapleMonster) mobs;
					if (monster.getId() == mobHp) {
						player.dropMessage(monster.getName() + ": " + monster.getHp());
					}
				}
				break;
			case 0x1E: // Warn
				victim = slea.readMapleAsciiString();
				String message = slea.readMapleAsciiString();
				target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
				if (target != null) {
					target.getClient().announce(PacketCreator.serverNotice(1, message));
					c.announce(PacketCreator.getGMEffect(0x1E, (byte) 1));
				} else {
					c.announce(PacketCreator.getGMEffect(0x1E, (byte) 0));
				}
				break;
			case 0x77: // Testing purpose
				if (slea.available() == 4)
					Output.print("[ACH] " + slea.readInt());
				else if (slea.available() == 2)
					Output.print("[ACH] " + slea.readShort());
				break;
			default:
				Output.print("New GM packet encountered (MODE : " + mode + ": " + slea.toString());
				break;
		}
	}
}
