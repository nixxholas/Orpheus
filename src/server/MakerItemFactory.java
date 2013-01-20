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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.DatabaseConnection;

/**
 * 
 * @author Jay Estrella
 */
public class MakerItemFactory {
	private static Map<Integer, MakerRecipe> recipeCache = new HashMap<Integer, MakerRecipe>();

	public static MakerRecipe getRecipe(int toCreate) {
		if (recipeCache.get(toCreate) != null) {
			return recipeCache.get(toCreate);
		}

		try {
			Connection con = DatabaseConnection.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT req_level, req_maker_level, req_meso, quantity FROM makercreatedata WHERE itemid = ?");
			ps.setInt(1, toCreate);
			ResultSet rs = ps.executeQuery();
			int reqLevel = 0;
			int reqMakerLevel = 0;
			int cost = 0;
			int toGive = 0;
			if (rs.next()) {
				reqLevel = rs.getInt("req_level");
				reqMakerLevel = rs.getInt("req_maker_level");
				cost = rs.getInt("req_meso");
				toGive = rs.getInt("quantity");
			}
			ps.close();
			rs.close();
			ps = con.prepareStatement("SELECT req_item, count FROM makerrecipedata WHERE itemid = ?");
			ps.setInt(1, toCreate);
			rs = ps.executeQuery();
			List<MakerRecipeIngredient> ingredients = new ArrayList<MakerRecipeIngredient>();
			while (rs.next()) {
				MakerRecipeIngredient entry = new MakerRecipeIngredient(
						rs.getInt("req_item"), rs.getInt("count"));
				ingredients.add(entry);
			}
			rs.close();
			ps.close();
			MakerRecipe result = new MakerRecipe(cost, reqLevel, reqMakerLevel, toGive, ingredients);
			recipeCache.put(toCreate, result);
		} catch (SQLException sqle) {
		}

		return recipeCache.get(toCreate);
	}
}
