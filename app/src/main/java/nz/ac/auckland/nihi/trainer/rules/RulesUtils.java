package nz.ac.auckland.nihi.trainer.rules;

import android.speech.tts.TextToSpeech;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.CompositeRule;

import java.sql.Time;

import static org.jeasy.rules.core.RulesEngineBuilder.aNewRulesEngine;

/**
 * Created by alex on 7/13/2017.
 */

public class RulesUtils {
    private Rules rules;
    private Facts facts;
    private RulesEngine rulesEngine;
    private long timestamp;
    private long timeGap;
    private TimeCheck timeCheck;

    public RulesUtils(long timeGap, TextToSpeech tts){
        rulesEngine = aNewRulesEngine()
                .withSkipOnFirstAppliedRule(true)
                .withSilentMode(true)
                .build();
        timestamp = 0;
        this.timeGap = timeGap;
        timeCheck = new TimeCheck(tts);
    }

    public void fireHeartrateRules(int heartRate){
        rules = new Rules();
        rules.register(new HeartrateLow());
        rules.register(new HeartrateNormal());
        rules.register(new HeartrateHigh());
        facts = new Facts();
        facts.put("heartRate", heartRate);
        rulesEngine.fire(rules, facts);
    }

    public void fireSpeedRules(float speed){
        rules = new Rules();
        rules.register(new SpeedLow());
        facts = new Facts();
        facts.put("speed", speed);
        rulesEngine.fire(rules, facts);
    }

    public void fireTimedHeartrateRules(int heartRate, long timeElapsed){
        facts = new Facts();
        facts.put("heartRate", heartRate);
        facts.put("time", timeElapsed);
        facts.put("timeGap", timeGap);

        CompositeRule HearrateTooLow = new CompositeRule("TimedHeartrateLow", "");
        HearrateTooLow.addRule(timeCheck);
        HearrateTooLow.addRule(new HeartrateLow());

        CompositeRule HeartrateTooHigh =  new CompositeRule("TimedHeartrateHigh", "");
        HeartrateTooHigh.addRule(timeCheck);
        HeartrateTooHigh.addRule(new HeartrateHigh());

        CompositeRule HeartrateNormal = new CompositeRule("TimedHeartrateNormal", "");
        HeartrateNormal.addRule(timeCheck);
        HeartrateNormal.addRule(new HeartrateNormal());

        rules = new Rules();
        rules.register(HearrateTooLow);
        rules.register(HeartrateNormal);
        rules.register(HeartrateTooHigh);
        rulesEngine.fire(rules, facts);
    }

    public void fireTimedSpeedRules(float speed, long timeElapsed){
        facts = new Facts();
        facts.put("speed", speed);
        facts.put("time", timeElapsed);
        facts.put("timeGap", timeGap);

        CompositeRule SpeedLow = new CompositeRule("TimedSpeedLow", "");
        SpeedLow.addRule(timeCheck);
        SpeedLow.addRule(new SpeedLow());

        rules = new Rules();
        rules.register(SpeedLow);

        rulesEngine.fire(rules, facts);
    }
}
