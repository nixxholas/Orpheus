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

import java.io.File;
import java.io.FileReader;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import client.GameClient;

/**
 * 
 * @author Matze
 */
public abstract class AbstractScriptManager {
	protected ScriptEngine engine;
	private ScriptEngineManager sem;

	protected AbstractScriptManager() {
		sem = new ScriptEngineManager();
	}

	protected Invocable getInvocable(String path, GameClient c) {
		try {
			path = "scripts/" + path;
			engine = null;
			if (c != null) {
				engine = c.getScriptEngine(path);
			}
			if (engine == null) {
				File scriptFile = new File(path);
				if (!scriptFile.exists()) {
					return null;
				}
				engine = sem.getEngineByName("JavaScript");
				if (c != null) {
					c.setScriptEngine(path, engine);
				}
				FileReader fr = new FileReader(scriptFile);
				engine.eval(fr);
				fr.close();
			}
			return (Invocable) engine;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected void resetContext(String path, GameClient c) {
		c.removeScriptEngine("scripts/" + path);
	}
}
