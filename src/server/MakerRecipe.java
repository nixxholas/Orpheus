package server;

import java.util.ArrayList;
import java.util.List;

public class MakerRecipe {
	private final int reqLevel, reqMakerLevel;
	private final int cost;
	private final int toGive;

	private final List<MakerRecipeIngredient> ingredients; 

	MakerRecipe(int cost, int reqLevel, int reqMakerLevel, int toGive, List<MakerRecipeIngredient> ingredients) {
		this.cost = cost;
		this.reqLevel = reqLevel;
		this.reqMakerLevel = reqMakerLevel;
		this.toGive = toGive;
		this.ingredients = new ArrayList<MakerRecipeIngredient>(ingredients);
	}

	public int getRewardAmount() {
		return toGive;
	}

	public List<MakerRecipeIngredient> getIngredients() {
		return ingredients;
	}

	public int getReqLevel() {
		return reqLevel;
	}

	public int getReqSkillLevel() {
		return reqMakerLevel;
	}

	public int getCost() {
		return cost;
	}
}