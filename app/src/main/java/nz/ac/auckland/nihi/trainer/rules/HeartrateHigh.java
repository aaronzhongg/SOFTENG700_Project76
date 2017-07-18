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

@Rule(name = "HeartrateHigh", description = "heart rate above 170")
public class HeartrateHigh {
    private int heart;
    @Condition
    public boolean heartRateTooHighCond(@Fact("heartRate") int heartRate){
        heart = heartRate;
        if(heartRate >= 170){
            return true;
        }else{
            return false;
        }
    }

    @Action
    public void giveEncouragement(){
        WorkoutService.setFeedback("Lets take it easy for a while!! Heart Rate is " + heart);
    }

    @Priority
    public int getPriority() {
        return 3;
    }

}
