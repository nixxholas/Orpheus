package server;

import java.util.List;
import java.util.Map;

import server.partyquest.Pyramid;
import client.GameCharacter;
import client.ISkill;
import client.SkillFactory;
import client.autoban.AutobanType;

public class AttackInfo {

    public int numAttacked, numDamage, numAttackedAndDamage, skill, skilllevel, stance, direction, rangedirection, charge, display;
    public Map<Integer, List<Integer>> allDamage;
    public boolean isHH = false;
    public int speed = 4;

    public StatEffect getAttackEffect(GameCharacter chr, ISkill theSkill) {
        ISkill mySkill = theSkill;
        if (mySkill == null) {
            mySkill = SkillFactory.getSkill(this.skill);
        }
        int skillLevel = chr.getSkillLevel(mySkill);
        if (mySkill.getId() % 10000000 == 1020) {
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
            if (!theSkill.getAction()) {
            	chr.getAutobanManager().autoban(AutobanType.FAST_ATTACK, "WZ Edit; adding action to a skill: " + this.display);
                return null;
            }
        }
        return mySkill.getEffect(skillLevel);
    }
}