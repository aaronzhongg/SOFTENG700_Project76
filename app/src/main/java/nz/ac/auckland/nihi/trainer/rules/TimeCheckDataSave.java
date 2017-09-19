package nz.ac.auckland.nihi.trainer.rules;

import android.speech.tts.TextToSpeech;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Priority;
import org.jeasy.rules.annotation.Rule;

import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.services.workout.WorkoutService;

/**
 * Created by alex on 7/26/2017.
 */

@Rule(name = "TimeCheckDataSave", description = "check if a certain amount of time has passed. Used for saving summary data at this current point")
public class TimeCheckDataSave {
    private int timeStampMultiplier = 1;

    //for debugging
    private TextToSpeech tts;

    public TimeCheckDataSave(TextToSpeech tts){
        this.tts = tts;
    }
    @Condition
    public boolean timeCheck(@Fact("time") long timeElapsed, @Fact("timeGap") long timeGap){
        if(timeElapsed - (timeGap * timeStampMultiplier) > 0){ //longer than time gap
            timeStampMultiplier ++;
            //For debugging
//            tts.speak(timeStampMultiplier + "",TextToSpeech.QUEUE_FLUSH, null);
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            return true;
        }else{
            return false;
        }
    }

    @Action
    public void trigger(@Fact("timeGap") long timeGap, @Fact("time") long timeElapsed){
        WorkoutService.shouldSaveSummaryChunk = true;
    }

    @Priority
    public int getPriority() {
        return 12;
    }

}
