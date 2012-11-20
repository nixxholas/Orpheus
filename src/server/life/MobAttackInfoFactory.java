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
package server.life;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.StringUtil;

/**
 * 
 * @author Danny (Leifde)
 */
public class MobAttackInfoFactory {
	private static Map<String, MobAttackInfo> mobAttacks = new HashMap<String, MobAttackInfo>();
	private static MapleDataProvider dataSource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Mob.wz"));

	public static MobAttackInfo getMobAttackInfo(Monster mob, int attack) {
		MobAttackInfo ret = mobAttacks.get(mob.getId() + "" + attack);
		if (ret != null) {
			return ret;
		}
		synchronized (mobAttacks) {
			ret = mobAttacks.get(mob.getId() + "" + attack);
			if (ret == null) {
				MapleData mobData = dataSource.getData(StringUtil.getLeftPaddedStr(Integer.toString(mob.getId()) + ".img", '0', 11));
				if (mobData != null) {
					// MapleData infoData = mobData.getChildByPath("info");
					String linkedmob = MapleDataTool.getString("link", mobData, "");
					if (!linkedmob.equals("")) {
						mobData = dataSource.getData(StringUtil.getLeftPaddedStr(linkedmob + ".img", '0', 11));
					}
					MapleData attackData = mobData.getChildByPath("attack" + (attack + 1) + "/info");
					if (attackData != null) {
						MapleData deadlyAttack = attackData.getChildByPath("deadlyAttack");
						int mpBurn = MapleDataTool.getInt("mpBurn", attackData, 0);
						int disease = MapleDataTool.getInt("disease", attackData, 0);
						int level = MapleDataTool.getInt("level", attackData, 0);
						int mpCon = MapleDataTool.getInt("conMP", attackData, 0);
						ret = new MobAttackInfo(mob.getId(), attack);
						ret.setDeadlyAttack(deadlyAttack != null);
						ret.setMpBurn(mpBurn);
						ret.setDiseaseSkill(disease);
						ret.setDiseaseLevel(level);
						ret.setMpCon(mpCon);
					}
				}
				mobAttacks.put(mob.getId() + "" + attack, ret);
			}
			return ret;
		}
	}
}
