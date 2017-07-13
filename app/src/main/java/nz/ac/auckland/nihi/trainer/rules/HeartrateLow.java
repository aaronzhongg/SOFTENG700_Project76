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

@Rule(name = "HeartrateLow", description = "heart rate below 100")
public class HeartrateLow {
    private int heart;
    @Condition
    public boolean heartRateTooLowCond(@Fact("heartRate") int heartRate){
        heart = heartRate;
        if(heartRate <= 100){
            return true;
        }else{
            return false;
        }
    }

    @Action
    public void giveEncouragement(){
        WorkoutService.setFeedback("Lets put more effort in to it!! Heart Rate is " + heart);
    }

    @Priority
    public int getPriority() {
        return 4;
    }

}
