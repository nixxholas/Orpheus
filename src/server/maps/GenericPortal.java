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
package server.maps;

import java.awt.Point;
import client.GameClient;
import scripting.portal.PortalScriptManager;
import server.Portal;
import server.FourthJobQuestsPortalHandler;
import tools.PacketCreator;

public class GenericPortal implements Portal {

	private String name;
	private String target;
	private Point position;
	private int targetmap;
	private int type;
	private boolean status = true;
	private int id;
	private String scriptName;
	private boolean portalState;

	public GenericPortal(int type) {
		this.type = type;
	}

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Point getPosition() {
		return position;
	}

	@Override
	public String getTarget() {
		return target;
	}

	@Override
	public void setPortalStatus(boolean newStatus) {
		this.status = newStatus;
	}

	@Override
	public boolean getPortalStatus() {
		return status;
	}

	@Override
	public int getTargetMapId() {
		return targetmap;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public String getScriptName() {
		return scriptName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPosition(Point position) {
		this.position = position;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setTargetMapId(int targetmapid) {
		this.targetmap = targetmapid;
	}

	@Override
	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}

	@Override
	public void enterPortal(GameClient c) {
		boolean changed = false;
		if (getScriptName() != null) {
			if (!FourthJobQuestsPortalHandler.handlePortal(getScriptName(), c.getPlayer())) {
				changed = PortalScriptManager.getInstance().executePortalScript(this, c);
			}
		} else if (getTargetMapId() != 999999999) {
			GameMap to = c.getPlayer().getEventInstance() == null ? c.getChannelServer().getMapFactory().getMap(getTargetMapId()) : c.getPlayer().getEventInstance().getMapInstance(getTargetMapId());
			Portal pto = to.getPortal(getTarget());
			if (pto == null) {// fallback for missing portals - no real life
								// case anymore - intresting for not implemented
								// areas
				pto = to.getPortal(0);
			}
			c.getPlayer().changeMap(to, pto); // late resolving makes this
												// harder but prevents us from
												// loading the whole world at
												// once
			changed = true;
		}
		if (!changed) {
			c.getSession().write(PacketCreator.enableActions());
		}
	}

	@Override
	public void setPortalState(boolean state) {
		this.portalState = state;
	}

	@Override
	public boolean getPortalState() {
		return portalState;
	}
}
