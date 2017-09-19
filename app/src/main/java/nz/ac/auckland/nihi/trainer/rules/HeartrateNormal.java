package nz.ac.auckland.nihi.trainer.rules;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Priority;
import org.jeasy.rules.annotation.Rule;

import nz.ac.auckland.nihi.trainer.services.workout.WorkoutService;

/**
 * Created by alex on 7/12/2017.
 */

@Rule(name = "HeartrateNormal", description = "heart rate between 100 and 170")
public class HeartrateNormal {
    private int heart;
    @Condition
    public boolean heartRateNormalCond(@Fact("heartRate") int heartRate){
        heart = heartRate;
        if(heartRate < 170 && heartRate > 100){
            return true;
        }else{
            return false;
        }
    }

    @Action
    public void giveEncouragement(){
        WorkoutService.setFeedback("You are doing fine!! Heart Rate is " + heart);
    }

    @Priority
    public int getPriority() {
        return 10;
    }
}
