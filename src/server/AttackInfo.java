package server;

import java.util.List;
import java.util.Map;

import server.partyquest.Pyramid;
import client.GameCharacter;
import client.ISkill;
import client.SkillFactory;
import client.autoban.AutobanType;

public class AttackInfo {

	public static class AttackSkillInfo {
		public int id;
		public int level;
		
		private AttackSkillInfo() { }
	}
	
	public AttackSkillInfo skill;
	
    public int numAttacked, numDamage, numAttackedAndDamage, stance, direction, rangedirection, charge, display;
    public Map<Integer, List<Integer>> allDamage;
    public boolean isHH = false;
    public int speed = 4;

    public StatEffect getAttackEffect(GameCharacter chr, ISkill skill) {
        ISkill copy = skill;
        if (copy == null) {
            copy = SkillFactory.getSkill(this.skill.id);
        }
        int skillLevel = chr.getSkillLevel(copy);
        if (copy.getId() % 10000000 == 1020) {
            if (chr.getPartyQuest() instanceof Pyramid) {
                if (((Pyramid) chr.getPartyQuest()).useSkill()) {
                    skillLevel = 1;
                }
            }
        }
        if (skillLevel == 0) {
            return null;
        }
        if (this.display > 80) { 
        	// Hmm
            if (!skill.getAction()) {
            	chr.getAutobanManager().autoban(AutobanType.FAST_ATTACK, "WZ Edit; adding action to a skill: " + this.display);
                return null;
            }
        }
        
        return copy.getEffect(skillLevel);
    }
}