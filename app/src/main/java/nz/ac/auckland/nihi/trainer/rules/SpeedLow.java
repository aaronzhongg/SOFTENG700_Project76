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

@Rule(name = "SpeedLow", description = "Speed drops below 7km/hr")
public class SpeedLow {
    private float speed;
    @Condition
    public boolean heartRateTooLowCond(@Fact("speed") float runSpeed){
        speed = runSpeed;
        if(speed <= 7){
            return true;
        }else{
            return false;
        }
    }

    @Action
    public void giveEncouragement(){
        WorkoutService.setFeedback("Speed is reaching walking levels!! Consider picking up the pace or resting. Speed is " + speed);
    }

    @Priority
    public int getPriority() {
        return 9;
    }
}
