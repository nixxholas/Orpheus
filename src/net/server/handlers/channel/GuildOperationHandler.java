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

import net.server.guild.GuildInviteResponse;
import net.server.guild.Guild;
import client.GameClient;
import net.AbstractPacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;
import java.util.Iterator;
import tools.PacketCreator;
import tools.Output;
import client.GameCharacter;
import net.server.Server;

public final class GuildOperationHandler extends AbstractPacketHandler {
	private boolean isGuildNameAcceptable(String name) {
		if (name.length() < 3 || name.length() > 12) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			if (!Character.isLowerCase(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private void respawnPlayer(GameCharacter player) {
		player.getMap().broadcastMessage(player, PacketCreator.removePlayerFromMap(player.getId()), false);
		player.getMap().broadcastMessage(player, PacketCreator.spawnPlayerMapObject(player), false);
	}

	private class Invited {
		public String name;
		public int gid;
		public long expiration;

		public Invited(String n, int id) {
			name = n.toLowerCase();
			gid = id;
			expiration = System.currentTimeMillis() + 60 * 60 * 1000;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Invited)) {
				return false;
			}
			Invited oth = (Invited) other;
			return (gid == oth.gid && name.equals(oth));
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
			hash = 83 * hash + this.gid;
			return hash;
		}
	}

	private java.util.List<Invited> invited = new java.util.LinkedList<Invited>();
	private long nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		if (System.currentTimeMillis() >= nextPruneTime) {
			Iterator<Invited> itr = invited.iterator();
			Invited inv;
			while (itr.hasNext()) {
				inv = itr.next();
				if (System.currentTimeMillis() >= inv.expiration) {
					itr.remove();
				}
			}
			nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
		}
		GameCharacter player = c.getPlayer();
		byte type = reader.readByte();
		switch (type) {
			case 0x00:
				// c.announce(PacketCreator.showGuildInfo(mc));
				break;
				
			case 0x02:
				if (player.getGuildId() > 0 || player.getMapId() != 200000301) {
					c.getPlayer().dropMessage(1, "You cannot create a new Guild while in one.");
					return;
				}
				if (player.getMeso() < Guild.CREATE_GUILD_COST) {
					c.getPlayer().dropMessage(1, "You do not have enough mesos to create a Guild.");
					return;
				}
				String guildName = reader.readMapleAsciiString();
				if (!isGuildNameAcceptable(guildName)) {
					c.getPlayer().dropMessage(1, "The Guild name you have chosen is not accepted.");
					return;
				}
				int gid;

				gid = Server.getInstance().createGuild(player.getId(), guildName);
				if (gid == 0) {
					c.announce(PacketCreator.genericGuildMessage((byte) 0x1c));
					return;
				}
				player.gainMeso(-Guild.CREATE_GUILD_COST, true, false, true);
				player.setGuildId(gid);
				player.setGuildRank(1);
				player.saveGuildStatus();
				c.announce(PacketCreator.showGuildInfo(player));
				c.getPlayer().dropMessage(1, "You have successfully created a Guild.");
				respawnPlayer(player);
				break;
				
			case 0x05:
				if (player.getGuildId() <= 0 || player.getGuildRank() > 2) {
					return;
				}
				String name = reader.readMapleAsciiString();
				GuildInviteResponse mgr = Guild.sendInvite(c, name);
				if (mgr != null) {
					c.announce(mgr.getPacket());
				} else {
					Invited inv = new Invited(name, player.getGuildId());
					if (!invited.contains(inv)) {
						invited.add(inv);
					}
				}
				break;
				
			case 0x06:
				if (player.getGuildId() > 0) {
					Output.print("[GOH] " + player.getName() + " attempted to join a guild when s/he is already in one.");
					return;
				}
				gid = reader.readInt();
				int cid = reader.readInt();
				if (cid != player.getId()) {
					Output.print("[GOH] " + player.getName() + " attempted to join a guild with a different character id.");
					return;
				}
				name = player.getName().toLowerCase();
				Iterator<Invited> itr = invited.iterator();
				boolean bOnList = false;
				while (itr.hasNext()) {
					Invited inv = itr.next();
					if (gid == inv.gid && name.equals(inv.name)) {
						bOnList = true;
						itr.remove();
						break;
					}
				}
				if (!bOnList) {
					Output.print("[GOH] " + player.getName() + " is trying to join a guild that never invited him/her (or that the invitation has expired)");
					return;
				}
				player.setGuildId(gid); // joins the guild
				player.setGuildRank(5); // start at lowest rank
				int s;

				s = Server.getInstance().addGuildMember(player.getMGC());
				if (s == 0) {
					c.getPlayer().dropMessage(1, "The Guild you are trying to join is already full.");
					player.setGuildId(0);
					return;
				}
				c.announce(PacketCreator.showGuildInfo(player));
				player.saveGuildStatus(); // update database
				respawnPlayer(player);
				break;
				
			case 0x07:
				cid = reader.readInt();
				name = reader.readMapleAsciiString();
				if (cid != player.getId() || !name.equals(player.getName()) || player.getGuildId() <= 0) {
					System.out.println("[hax] " + player.getName() + " tried to quit guild under the name \"" + name + "\" and current guild id of " + player.getGuildId() + ".");
					return;
				}

				Server.getInstance().leaveGuild(player.getMGC());
				c.announce(PacketCreator.showGuildInfo(null));
				player.setGuildId(0);
				player.saveGuildStatus();
				respawnPlayer(player);
				break;
				
			case 0x08:
				cid = reader.readInt();
				name = reader.readMapleAsciiString();
				if (player.getGuildRank() > 2 || player.getGuildId() <= 0) {
					System.out.println("[hax] " + player.getName() + " is trying to expel without rank 1 or 2.");
					return;
				}

				Server.getInstance().expelMember(player.getMGC(), name, cid);
				break;
				
			case 0x0D:
				if (player.getGuildId() <= 0 || player.getGuildRank() != 1) {
					System.out.println("[hax] " + player.getName() + " tried to change guild rank titles when s/he does not have permission.");
					return;
				}
				String ranks[] = new String[5];
				for (int i = 0; i < 5; i++) {
					ranks[i] = reader.readMapleAsciiString();
				}

				Server.getInstance().changeRankTitle(player.getGuildId(), ranks);
				break;
				
			case 0x0E:
				cid = reader.readInt();
				byte newRank = reader.readByte();
				if (player.getGuildRank() > 2 || (newRank <= 2 && player.getGuildRank() != 1) || player.getGuildId() <= 0) {
					System.out.println("[hax] " + player.getName() + " is trying to change rank outside of his/her permissions.");
					return;
				}
				if (newRank <= 1 || newRank > 5) {
					return;
				}
				Server.getInstance().changeRank(player.getGuildId(), cid, newRank);
				break;
				
			case 0x0F:
				if (player.getGuildId() <= 0 || player.getGuildRank() != 1 || player.getMapId() != 200000301) {
					System.out.println("[hax] " + player.getName() + " tried to change guild emblem without being the guild leader.");
					return;
				}
				if (player.getMeso() < Guild.CHANGE_EMBLEM_COST) {
					c.announce(PacketCreator.serverNotice(1, "You do not have enough mesos to create a Guild."));
					return;
				}
				short bg = reader.readShort();
				byte bgcolor = reader.readByte();
				short logo = reader.readShort();
				byte logocolor = reader.readByte();
				Server.getInstance().setGuildEmblem(player.getGuildId(), bg, bgcolor, logo, logocolor);
				player.gainMeso(-Guild.CHANGE_EMBLEM_COST, true, false, true);
				respawnPlayer(player);
				break;
				
			case 0x10:
				if (player.getGuildId() <= 0 || player.getGuildRank() > 2) {
					System.out.println("[hax] " + player.getName() + " tried to change guild notice while not in a guild.");
					return;
				}
				String notice = reader.readMapleAsciiString();
				if (notice.length() > 100) {
					return;
				}
				Server.getInstance().setGuildNotice(player.getGuildId(), notice);
				break;
				
			default:
				System.out.println("Unhandled GUILD_OPERATION packet: \n" + reader.toString());
		}
	}
}
