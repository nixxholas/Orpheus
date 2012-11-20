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
package net.server;

import java.util.Collection;
import client.GameCharacter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PlayerStorage {
	private final ReentrantReadWriteLock locks = new ReentrantReadWriteLock();
	private final Lock rlock = locks.readLock();
	private final Lock wlock = locks.writeLock();
	private final Map<Integer, GameCharacter> storage = new LinkedHashMap<Integer, GameCharacter>();

	public void addPlayer(GameCharacter chr) {
		wlock.lock();
		try {
			storage.put(chr.getId(), chr);
		} finally {
			wlock.unlock();
		}
	}

	public GameCharacter removePlayer(int chr) {
		wlock.lock();
		try {
			return storage.remove(chr);
		} finally {
			wlock.unlock();
		}
	}

	public GameCharacter getCharacterByName(String name) {
		rlock.lock();
		try {
			for (GameCharacter chr : storage.values()) {
				if (chr.getName().toLowerCase().equals(name.toLowerCase()))
					return chr;
			}
			return null;
		} finally {
			rlock.unlock();
		}
	}

	public GameCharacter getCharacterById(int id) {
		rlock.lock();
		try {
			return storage.get(id);
		} finally {
			rlock.unlock();
		}
	}

	public Collection<GameCharacter> getAllCharacters() {
		rlock.lock();
		try {
			return storage.values();
		} finally {
			rlock.unlock();
		}
	}

	public final void disconnectAll() {
		wlock.lock();
		try {
			final Iterator<GameCharacter> chrit = storage.values().iterator();
			while (chrit.hasNext()) {
				chrit.next().getClient().disconnect();
				chrit.remove();
			}
		} finally {
			wlock.unlock();
		}
	}
}