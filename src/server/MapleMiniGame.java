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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import client.GameCharacter;
import client.GameClient;
import net.GamePacket;
import server.maps.AbstractMapleMapObject;
import server.maps.MapleMapObjectType;
import tools.PacketCreator;

/**
 * 
 * @author Matze
 */
public class MapleMiniGame extends AbstractMapleMapObject {
	private GameCharacter owner;
	private GameCharacter visitor;
	private String GameType = null;
	private int[] piece = new int[250];
	private List<Integer> list4x3 = new ArrayList<Integer>();
	private List<Integer> list5x4 = new ArrayList<Integer>();
	private List<Integer> list6x5 = new ArrayList<Integer>();
	private String description;
	private int loser = 1;
	private int piecetype;
	private int firstslot = 0;
	private int visitorpoints = 0;
	private int ownerpoints = 0;
	private int matchestowin = 0;

	public MapleMiniGame(GameCharacter owner, String description) {
		this.owner = owner;
		this.description = description;
	}

	public boolean hasFreeSlot() {
		return visitor == null;
	}

	public boolean isOwner(GameCharacter c) {
		return owner.equals(c);
	}

	public void addVisitor(GameCharacter challenger) {
		visitor = challenger;
		if (GameType.equals("omok")) {
			this.getOwner().getClient().announce(PacketCreator.getMiniGameNewVisitor(challenger, 1));
			this.getOwner().getMap().broadcastMessage(PacketCreator.addOmokBox(owner, 2, 0));
		}
		if (GameType.equals("matchcard")) {
			this.getOwner().getClient().announce(PacketCreator.getMatchCardNewVisitor(challenger, 1));
			this.getOwner().getMap().broadcastMessage(PacketCreator.addMatchCardBox(owner, 2, 0));
		}
	}

	public void removeVisitor(GameCharacter challenger) {
		if (visitor == challenger) {
			visitor = null;
			this.getOwner().getClient().announce(PacketCreator.getMiniGameRemoveVisitor());
			if (GameType.equals("omok")) {
				this.getOwner().getMap().broadcastMessage(PacketCreator.addOmokBox(owner, 1, 0));
			}
			if (GameType.equals("matchcard")) {
				this.getOwner().getMap().broadcastMessage(PacketCreator.addMatchCardBox(owner, 1, 0));
			}
		}
	}

	public boolean isVisitor(GameCharacter challenger) {
		return visitor == challenger;
	}

	public void broadcastToVisitor(GamePacket packet) {
		if (visitor != null) {
			visitor.getClient().announce(packet);
		}
	}

	public void setFirstSlot(int type) {
		firstslot = type;
	}

	public int getFirstSlot() {
		return firstslot;
	}

	public void setOwnerPoints() {
		ownerpoints++;
		if (ownerpoints + visitorpoints == matchestowin) {
			if (ownerpoints == visitorpoints) {
				this.broadcast(PacketCreator.getMatchCardTie(this));
			} else if (ownerpoints > visitorpoints) {
				this.broadcast(PacketCreator.getMatchCardOwnerWin(this));
			} else {
				this.broadcast(PacketCreator.getMatchCardVisitorWin(this));
			}
			ownerpoints = 0;
			visitorpoints = 0;
		}
	}

	public void setVisitorPoints() {
		visitorpoints++;
		if (ownerpoints + visitorpoints == matchestowin) {
			if (ownerpoints > visitorpoints) {
				this.broadcast(PacketCreator.getMiniGameOwnerWin(this));
			} else if (visitorpoints > ownerpoints) {
				this.broadcast(PacketCreator.getMiniGameVisitorWin(this));
			} else {
				this.broadcast(PacketCreator.getMiniGameTie(this));
			}
			ownerpoints = 0;
			visitorpoints = 0;
		}
	}

	public void setMatchesToWin(int type) {
		matchestowin = type;
	}

	public void setPieceType(int type) {
		piecetype = type;
	}

	public int getPieceType() {
		return piecetype;
	}

	public void setGameType(String game) {
		GameType = game;
		if (game.equals("matchcard")) {
			if (matchestowin == 6) {
				for (int i = 0; i < 6; i++) {
					list4x3.add(i);
					list4x3.add(i);
				}
			} else if (matchestowin == 10) {
				for (int i = 0; i < 10; i++) {
					list5x4.add(i);
					list5x4.add(i);
				}
			} else {
				for (int i = 0; i < 15; i++) {
					list6x5.add(i);
					list6x5.add(i);
				}
			}
		}
	}

	public String getGameType() {
		return GameType;
	}

	public void shuffleList() {
		if (matchestowin == 6) {
			Collections.shuffle(list4x3);
		} else if (matchestowin == 10) {
			Collections.shuffle(list5x4);
		} else {
			Collections.shuffle(list6x5);
		}
	}

	public int getCardId(int slot) {
		int cardid = 0;
		if (matchestowin == 6) {
			cardid = list4x3.get(slot - 1);
		} else if (matchestowin == 10) {
			cardid = list5x4.get(slot - 1);
		} else {
			cardid = list6x5.get(slot - 1);
		}
		return cardid;
	}

	public int getMatchesToWin() {
		return matchestowin;
	}

	public void setLoser(int type) {
		loser = type;
	}

	public int getLoser() {
		return loser;
	}

	public void broadcast(GamePacket packet) {
		if (owner.getClient() != null && owner.getClient().getSession() != null) {
			owner.getClient().announce(packet);
		}
		broadcastToVisitor(packet);
	}

	public void chat(GameClient c, String chat) {
		broadcast(PacketCreator.getPlayerShopChat(c.getPlayer(), chat, isOwner(c.getPlayer())));
	}

	public void sendOmok(GameClient c, int type) {
		c.announce(PacketCreator.getMiniGame(c, this, isOwner(c.getPlayer()), type));
	}

	public void sendMatchCard(GameClient c, int type) {
		c.announce(PacketCreator.getMatchCard(c, this, isOwner(c.getPlayer()), type));
	}

	public GameCharacter getOwner() {
		return owner;
	}

	public GameCharacter getVisitor() {
		return visitor;
	}

	public void setPiece(int move1, int move2, int type, GameCharacter chr) {
		int slot = move2 * 15 + move1 + 1;
		if (piece[slot] == 0) {
			piece[slot] = type;
			this.broadcast(PacketCreator.getMiniGameMoveOmok(this, move1, move2, type));
			for (int y = 0; y < 15; y++) {
				for (int x = 0; x < 11; x++) {
					if (searchCombo(x, y, type)) {
						if (this.isOwner(chr)) {
							this.broadcast(PacketCreator.getMiniGameOwnerWin(this));
							this.setLoser(0);
						} else {
							this.broadcast(PacketCreator.getMiniGameVisitorWin(this));
							this.setLoser(1);
						}
						for (int y2 = 0; y2 < 15; y2++) {
							for (int x2 = 0; x2 < 15; x2++) {
								int slot2 = (y2 * 15 + x2 + 1);
								piece[slot2] = 0;
							}
						}
					}
				}
			}
			for (int y = 0; y < 15; y++) {
				for (int x = 4; x < 15; x++) {
					if (searchCombo2(x, y, type)) {
						if (this.isOwner(chr)) {
							this.broadcast(PacketCreator.getMiniGameOwnerWin(this));
							this.setLoser(0);
						} else {
							this.broadcast(PacketCreator.getMiniGameVisitorWin(this));
							this.setLoser(1);
						}
						for (int y2 = 0; y2 < 15; y2++) {
							for (int x2 = 0; x2 < 15; x2++) {
								int slot2 = (y2 * 15 + x2 + 1);
								piece[slot2] = 0;
							}
						}
					}
				}
			}
		}
	}

	private boolean searchCombo(int x, int y, int type) {
		int slot = y * 15 + x + 1;
		for (int i = 0; i < 5; i++) {
			if (piece[slot + i] == type) {
				if (i == 4) {
					return true;
				}
			} else {
				break;
			}
		}
		for (int j = 15; j < 17; j++) {
			for (int i = 0; i < 5; i++) {
				if (piece[slot + i * j] == type) {
					if (i == 4) {
						return true;
					}
				} else {
					break;
				}
			}
		}
		return false;
	}

	private boolean searchCombo2(int x, int y, int type) {
		int slot = y * 15 + x + 1;
		for (int j = 14; j < 15; j++) {
			for (int i = 0; i < 5; i++) {
				if (piece[slot + i * j] == type) {
					if (i == 4) {
						return true;
					}
				} else {
					break;
				}
			}
		}
		return false;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public void sendDestroyData(GameClient client) {
		return;
	}

	@Override
	public void sendSpawnData(GameClient client) {
		return;
	}

	@Override
	public MapleMapObjectType getType() {
		return MapleMapObjectType.MINI_GAME;
	}
}
