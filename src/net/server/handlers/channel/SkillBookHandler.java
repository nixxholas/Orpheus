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

import java.util.Map;
import client.IItem;
import client.GameClient;
import client.InventoryType;
import net.AbstractPacketHandler;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import tools.PacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.GameCharacter;
import client.skills.ISkill;
import client.skills.SkillFactory;
import tools.Randomizer;

public final class SkillBookHandler extends AbstractPacketHandler {

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor reader, GameClient c) {
		if (!c.getPlayer().isAlive()) {
			c.announce(PacketCreator.enableActions());
			return;
		}
		reader.readInt();
		byte slot = (byte) reader.readShort();
		int itemId = reader.readInt();
		GameCharacter player = c.getPlayer();
		IItem toUse = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);
		if (toUse != null && toUse.getQuantity() == 1) {
			if (toUse.getItemId() != itemId) {
				return;
			}
			Map<String, Integer> skilldata = ItemInfoProvider.getInstance().getSkillStats(toUse.getItemId(), c.getPlayer().getJob().getId());
			boolean canuse = false;
			boolean success = false;
			int skill = 0;
			int maxlevel = 0;
			if (skilldata == null) {
				return;
			}
			if (skilldata.get("skillid") == 0) {
				canuse = false;
			} else if (player.getMasterLevel(SkillFactory.getSkill(skilldata.get("skillid"))) >= skilldata.get("reqSkillLevel") || skilldata.get("reqSkillLevel") == 0) {
				canuse = true;
				if (Randomizer.nextInt(101) < skilldata.get("success") && skilldata.get("success") != 0) {
					success = true;
					ISkill skill2 = SkillFactory.getSkill(skilldata.get("skillid"));
					player.changeSkillLevel(skill2, player.getSkillLevel(skill2), Math.max(skilldata.get("masterLevel"), player.getMasterLevel(skill2)), -1);
				} else {
					success = false;
					player.dropMessage("The skill book lights up, but the skill winds up as if nothing happened.");
				}
				InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, (short) 1, false);
			} else {
				canuse = false;
			}
			player.getClient().announce(PacketCreator.skillBookSuccess(player, skill, maxlevel, canuse, success));
		}
	}
}
