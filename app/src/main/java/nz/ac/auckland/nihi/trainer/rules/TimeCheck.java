package nz.ac.auckland.nihi.trainer.rules;

import android.speech.tts.TextToSpeech;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Priority;
import org.jeasy.rules.annotation.Rule;

import nz.ac.auckland.nihi.trainer.services.workout.WorkoutService;

/**
 * Created by alex on 7/13/2017.
 */


@Rule(name = "TimeCheck", description = "check if a certain amount of time has passed")
public class TimeCheck {
    private TextToSpeech tts;
    private int timeStampMultiplier = 1;

    public TimeCheck(TextToSpeech tts){
        this.tts = tts;
    }

    @Condition
    public boolean timeCheck(@Fact("time") long timeElapsed, @Fact("timeGap") long timeGap){
        if(timeElapsed - (timeGap * timeStampMultiplier) > 0){ //longer than time gap
            timeStampMultiplier ++;
            return true;
        }else{
            return false;
        }
    }

    @Action
    public void trigger(@Fact("timeGap") long timeGap, @Fact("time") long timeElapsed){
        //say the allocated feedback
        tts.speak(WorkoutService.getFeedback(),TextToSpeech.QUEUE_ADD, null);
//        tts.speak(timeGap * timeStampMultiplier + "is" + timeElapsed,TextToSpeech.QUEUE_FLUSH, null);
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    @Priority
    public int getPriority() {
        return 11;
    }
}
