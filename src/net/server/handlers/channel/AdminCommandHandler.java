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
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.life.LifeFactory;
import server.life.Monster;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.quest.Quest;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class AdminCommandHandler extends AbstractPacketHandler {
	
	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		final GameCharacter player = c.getPlayer();
		if (!player.isGM()) {
			return;
		}
		byte mode = reader.readByte();
		String victim;
		GameCharacter target;
		switch (mode) {
		
			case 0x00: 
				// Level1~Level8 & Package1~Package2
				int[][] toSpawn = ItemInfoProvider.getInstance().getSummonMobs(reader.readInt());
				for (int z = 0; z < toSpawn.length; z++) {
					int[] toSpawnChild = toSpawn[z];
					if (Randomizer.nextInt(101) <= toSpawnChild[1]) {
						player.getMap().spawnMonsterOnGroudBelow(LifeFactory.getMonster(toSpawnChild[0]), player.getPosition());
					}
				}
				c.announce(PacketCreator.enableActions());
				break;
				
			case 0x01: { 
				// /d (inv)
				final byte typeByte = reader.readByte();
				final InventoryType type = InventoryType.fromByte(typeByte);
				final Inventory in = player.getInventory(type);
				
				// TODO: This for loop is equivalent to checking for a first item and removing it.
				// You sure you wanna return in the end?				
				for (byte i = 0; i < in.getCapacity(); i++) {
					final IItem item = in.getItem(i);
					if (item != null) {
						InventoryManipulator.removeFromSlot(c, type, i, item.getQuantity(), false);
					}
					return;
				}
				break;
			}
			
			case 0x02: 
				// Exp
				player.setExp(reader.readInt());
				break;
				
			case 0x03: 
				// Ban
				victim = reader.readMapleAsciiString();
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
				
			case 0x04: 
				// Block
				victim = reader.readMapleAsciiString();
				reader.readByte(); // type
				int duration = reader.readInt();
				String description = reader.readMapleAsciiString();
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
				
			case 0x10: 
				// /h, information by vana
				StringBuilder sb = new StringBuilder("USERS ON THIS MAP: ");
				for (GameCharacter character : player.getMap().getCharacters()) {
					sb.append(character.getName());
					sb.append(" ");
				}
				player.message(sb.toString());
				break;
				
			case 0x12: 
				// Send
				victim = reader.readMapleAsciiString();
				int mapId = reader.readInt();
				c.getChannelServer().getPlayerStorage().getCharacterByName(victim).changeMap(c.getChannelServer().getMapFactory().getMap(mapId));
				break;
				
			case 0x15: 
				// Kill
				int mobToKill = reader.readInt();
				int amount = reader.readInt();
				List<GameMapObject> monsterx = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.MONSTER));
				for (int x = 0; x < amount; x++) {
					Monster monster = (Monster) monsterx.get(x);
					if (monster.getId() == mobToKill) {
						player.getMap().killMonster(monster, player, true);
						monster.giveExpToCharacter(player, monster.getExp(), true, 1);
					}
				}
				break;
				
			case 0x16: 
				// Questreset
				Quest.getInstance(reader.readShort()).reset(player);
				break;
				
			case 0x17: 
				// Summon
				int mobId = reader.readInt();
				int quantity = reader.readInt();
				for (int i = 0; i < quantity; i++) {
					player.getMap().spawnMonsterOnGroudBelow(LifeFactory.getMonster(mobId), player.getPosition());
				}
				break;
				
			case 0x18: 
				// Maple & Mobhp
				int mobHp = reader.readInt();
				player.dropMessage("Monsters HP");
				List<GameMapObject> monsters = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(GameMapObjectType.MONSTER));
				for (GameMapObject mobs : monsters) {
					Monster monster = (Monster) mobs;
					if (monster.getId() == mobHp) {
						player.dropMessage(monster.getName() + ": " + monster.getHp());
					}
				}
				break;
				
			case 0x1E: 
				// Warn
				victim = reader.readMapleAsciiString();
				String message = reader.readMapleAsciiString();
				target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
				if (target != null) {
					target.getClient().announce(PacketCreator.serverNotice(1, message));
					c.announce(PacketCreator.getGMEffect(0x1E, (byte) 1));
				} else {
					c.announce(PacketCreator.getGMEffect(0x1E, (byte) 0));
				}
				break;
				
			case 0x77: 
				// Testing purpose
				if (reader.available() == 4)
					Output.print("[ACH] " + reader.readInt());
				else if (reader.available() == 2)
					Output.print("[ACH] " + reader.readShort());
				break;
				
			default:				
				Output.print("New GM packet encountered (MODE : " + mode + ": " + reader.toString());
				break;
		}
	}
}
