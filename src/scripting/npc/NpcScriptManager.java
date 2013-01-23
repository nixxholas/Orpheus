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

import java.util.HashMap;
import java.util.Map;
import javax.script.Invocable;
import client.GameClient;
import client.GameCharacter;
import java.lang.reflect.UndeclaredThrowableException;
import scripting.AbstractScriptManager;
import tools.Output;

/**
 * 
 * @author Matze
 */
public class NpcScriptManager extends AbstractScriptManager {
	private Map<GameClient, NpcConversationManager> cms = new HashMap<GameClient, NpcConversationManager>();
	private Map<GameClient, NpcScript> scripts = new HashMap<GameClient, NpcScript>();
	private static NpcScriptManager instance = new NpcScriptManager();

	public synchronized static NpcScriptManager getInstance() {
		return instance;
	}

	public void start(GameClient c, int npc, String filename, GameCharacter chr) {
		try {
			NpcConversationManager cm = new NpcConversationManager(c, npc);
			if (cms.containsKey(c)) {
				Output.print("FUU D:");
				dispose(c);
				return;
			}
			cms.put(c, cm);
			Invocable iv = null;
			if (filename != null) {
				iv = getInvocable("npc/world" + c.getWorldId() + "/" + filename + ".js", c);
			}
			if (iv == null) {
				iv = getInvocable("npc/world" + c.getWorldId() + "/" + npc + ".js", c);
			}
			if (iv == null || NpcScriptManager.getInstance() == null) {
				dispose(c);
				return;
			}
			engine.put("cm", cm);
			NpcScript ns = iv.getInterface(NpcScript.class);
			scripts.put(c, ns);
			if (chr == null) {
				ns.start();
			} else {
				ns.start(chr);
			}
		} catch (UndeclaredThrowableException ute) {
			ute.printStackTrace();
			Output.print("Error: NPC " + npc + ". UndeclaredThrowableException.");
			dispose(c);
			cms.remove(c);
			notice(c, npc);
		} catch (Exception e) {
			Output.print("Error: NPC " + npc + ".");
			dispose(c);
			cms.remove(c);
			notice(c, npc);
		}
	}

	public void action(GameClient c, byte mode, byte type, int selection) {
		NpcScript ns = scripts.get(c);
		if (ns != null) {
			try {
				ns.action(mode, type, selection);
			} catch (UndeclaredThrowableException ute) {
				ute.printStackTrace();
				Output.print("Error: NPC " + getConversationManager(c).getNpc() + ". UndeclaredThrowableException.");
				dispose(c);
				notice(c, getConversationManager(c).getNpc());
			} catch (Exception e) {
				Output.print("Error: NPC " + getConversationManager(c).getNpc() + ".");
				dispose(c);
				notice(c, getConversationManager(c).getNpc());
			}
		}
	}

	public void dispose(NpcConversationManager cm) {
		GameClient c = cm.getClient();
		cms.remove(c);
		scripts.remove(c);
		resetContext("npc/world" + c.getWorldId() + "/" + cm.getNpc() + ".js", c);
	}

	public void dispose(GameClient c) {
		if (cms.get(c) != null) {
			dispose(cms.get(c));
		}
	}

	public NpcConversationManager getConversationManager(GameClient c) {
		return cms.get(c);
	}

	private void notice(GameClient c, int id) {
		c.getPlayer().dropMessage(1, "This NPC is not working properly. Please report it. NPCID: " + id);
	}
}
