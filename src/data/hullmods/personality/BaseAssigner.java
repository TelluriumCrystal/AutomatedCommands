package data.hullmods.personality;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import data.hullmods.AutomatedHullMod;
import data.hullmods.Util;

import java.text.MessageFormat;
import java.util.Objects;

public class BaseAssigner extends AutomatedHullMod {
    private static final MessageFormat PERSONALITY_UNAPPLICABLE_REASON = Util.applyTemplate(Util.MOD_KEY +":PERSONALITY_UNAPPLICABLE_REASON");
    private static final MessageFormat PERSONALITY_DISABLED_REASON = Util.applyTemplate(Util.MOD_KEY +":PERSONALITY_DISABLED_REASON");
    private static final MessageFormat PERSONALITY_APPLIED = Util.applyTemplate(Util.MOD_KEY +":PERSONALITY_APPLIED");
    private static final MessageFormat PERSONALITY_APPLIED_FLAGSHIP = Util.applyTemplate(Util.MOD_KEY +":PERSONALITY_APPLIED_FLAGSHIP");

    private static final String PREFIX = "automated_personality_";
    private static final String CATEGORY = "personality";
    protected static final String TOKEN = "";

    enum Personality {
        TIMID,
        CAUTIOUS,
        STEADY,
        AGGRESSIVE,
        RECKLESS;
        final String id;
        final String value;

        Personality() {
            value = Global.getSettings().getString(Util.MOD_KEY, this.toString());
            id = PREFIX + value;
        }

        String withArticle() {
            return ((this == AGGRESSIVE) ? "an " : "a ") + this.value;
        }
    }

    private final Personality personality;

    public BaseAssigner(Personality personality) {
        Objects.requireNonNull(personality);
        this.personality = personality;
    }

    private String message(MessageFormat formatter, ShipAPI ship, Personality personality) {
        return formatter.format(new Object[]{ship, personality});
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return message(PERSONALITY_UNAPPLICABLE_REASON, ship, personality);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return !shipHasOtherModInCategory(ship, personality.id, CATEGORY);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0)
            return personality.value;
        return null;
    }

    protected String message(ShipAPI ship) {
        return message(PERSONALITY_APPLIED, ship, personality);
        //"has been assigned " + personality.withArticle() + " personality.";
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCurrentState() != GameState.COMBAT) return;
        String tag = ship.getId() + "_" + personality.id;
        if (Global.getCombatEngine().getCustomData().containsKey(tag)) return;
        Global.getCombatEngine().getCustomData().put(tag, TOKEN);
        overrideAI(ship);
    }

    protected void overrideAI(ShipAPI ship) {
        if (!ship.getCaptain().isDefault()) {
            formatShipMessage(ship, message(PERSONALITY_DISABLED_REASON, ship, personality));
            return; //Ship has officer, do not apply customAI
        }
        if (ship.getShipAI() == null) { // Ship is not currently autopilotted
            ShipAIConfig config = new ShipAIConfig();
            Global.getSettings().createDefaultShipAI(ship, config);
            formatShipMessage(ship, message(PERSONALITY_APPLIED_FLAGSHIP, ship, personality));
            return;
        }
        ship.getShipAI().getConfig().personalityOverride = personality.value;
        formatShipMessage(ship, message(PERSONALITY_APPLIED, ship, personality));
    }
}