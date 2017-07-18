package nz.ac.auckland.nihi.trainer.rules;

import android.speech.tts.TextToSpeech;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Priority;
import org.jeasy.rules.annotation.Rule;

import nz.ac.auckland.nihi.trainer.services.workout.WorkoutService;

/**
 * Created by alex on 7/11/2017.
 */

@Rule(name = "test rule", description = "testing some stuff")
public class ExampleRules {

    private int heart;
    @Condition
    public boolean heartRateTooLowCond(@Fact("heartRate") int heartRate){
        heart = heartRate;
        if(heartRate < 130){
            return true;
        }else{
            return false;
        }
    }

    @Action
    public void giveEncouragement(){
        WorkoutService.setFeedback("LETS Go CHAMP Heart Rate is " + heart);
    }

    @Priority
    public int getPriority() {
        return 1;
    }
}
