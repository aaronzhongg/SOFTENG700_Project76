package nz.ac.auckland.nihi.trainer.data;

/**
 * Created by Aaron on 31/07/17.
 */
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

@DatabaseTable(
        tableName = "exercise_summary"
)
public class RCExerciseSummary {
    @DatabaseField(
            id = true
    )
    private String id;
    @DatabaseField(
            canBeNull = false,
            index = true
    )
    private long userId;
    @DatabaseField(
            canBeNull = false
    )
    private int durationInSeconds;
    @DatabaseField(
            canBeNull = false
    )
    private int distanceInMetres;
    @DatabaseField(
            canBeNull = false
    )
    private float minSpeed;
    @DatabaseField(
            canBeNull = false
    )
    private float maxSpeed;
    @DatabaseField(
            canBeNull = false
    )
    private float avgSpeed;
    @DatabaseField(
            canBeNull = false
    )
    private int minHeartRate;
    @DatabaseField(
            canBeNull = false
    )
    private int maxHeartRate;
    @DatabaseField(
            canBeNull = false
    )
    private int avgHeartRate;
    @DatabaseField(
            canBeNull = false
    )
    private double trainingLoad;
    @DatabaseField(
            canBeNull = false,
            index = true
    )
    private Date date;
    @DatabaseField(
            canBeNull = true,
            foreign = true,
            foreignAutoCreate = false
    )
    private Route followedRoute;
//    @ForeignCollectionField(
//            eager = true,
//            foreignFieldName = "summary"
//    )
//    @JsonIgnore
//    private ForeignCollection<SymptomEntry> symptoms;
//    @ForeignCollectionField(
//            eager = true,
//            foreignFieldName = "summary"
//    )
//    @JsonIgnore
//    private ForeignCollection<ExerciseNotification> notifications;
//

    @ForeignCollectionField(
            eager = true,
            foreignFieldName = "summary"
    )
    private ForeignCollection<SummaryDataChunk> summaryDataChunks;

    public RCExerciseSummary() {
    }

    public RCExerciseSummary(int durationInSeconds, int distanceInMetres, float minSpeed, float avgSpeed, float maxSpeed, int minHeartRate, int avgHeartRate, int maxHeartRate, int energy) {
        this.durationInSeconds = durationInSeconds;
        this.distanceInMetres = distanceInMetres;
        this.minSpeed = minSpeed;
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
        this.minHeartRate = minHeartRate;
        this.avgHeartRate = avgHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.trainingLoad = (double)energy;
    }

    public RCExerciseSummary(Collection<ExerciseSummary> summaries) {
        this(0, 0, 0.0F, 0.0F, 0.0F, 0, 0, 0, 0);

        RCExerciseSummary summary;
        for(Iterator var2 = summaries.iterator(); var2.hasNext(); this.trainingLoad += summary.trainingLoad) {
            summary = (RCExerciseSummary)var2.next();
            this.durationInSeconds += summary.durationInSeconds;
            this.distanceInMetres += summary.distanceInMetres;
            this.minSpeed = Math.min(this.minSpeed, summary.minSpeed);
            this.avgSpeed += summary.avgSpeed;
            this.maxSpeed = Math.max(this.maxSpeed, summary.maxSpeed);
            this.minHeartRate = Math.min(this.minHeartRate, summary.minHeartRate);
            this.avgHeartRate += summary.avgHeartRate;
            this.maxHeartRate = Math.max(this.maxHeartRate, summary.maxHeartRate);
        }

        if(summaries.size() > 0) {
            this.avgHeartRate /= summaries.size();
            this.avgSpeed /= (float)summaries.size();
        }

    }

    public int getDurationInSeconds() {
        return this.durationInSeconds;
    }

    public void setDurationInSeconds(int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public int getDistanceInMetres() {
        return this.distanceInMetres;
    }

    public void setDistanceInMetres(int distanceInMetres) {
        this.distanceInMetres = distanceInMetres;
    }

    public float getMinSpeed() {
        return this.minSpeed;
    }

    public void setMinSpeed(float minSpeed) {
        this.minSpeed = minSpeed;
    }

    public int getMinHeartRate() {
        return this.minHeartRate;
    }

    public void setMinHeartRate(int minHeartRate) {
        this.minHeartRate = minHeartRate;
    }

    public double getTrainingLoad() {
        return this.trainingLoad;
    }

    public void setCumulativeImpulse(double ci) {
        this.trainingLoad = ci;
    }

    public int getMaxHeartRate() {
        return this.maxHeartRate;
    }

    public void setMaxHeartRate(int maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public int getAvgHeartRate() {
        return this.avgHeartRate;
    }

    public void setAvgHeartRate(int avgHeartRate) {
        this.avgHeartRate = avgHeartRate;
    }

    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public float getAvgSpeed() {
        return this.avgSpeed;
    }

    public void setAvgSpeed(float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public String getId() {
        return this.id;
    }
//
//    public ForeignCollection<SymptomEntry> getSymptoms() {
//        return this.symptoms;
//    }
//
//    public void setSymptoms(ForeignCollection<SymptomEntry> symptoms) {
//        this.symptoms = symptoms;
//    }
    public ForeignCollection<SummaryDataChunk> getSummaryDataChunks() {
        return this.summaryDataChunks;
    }

    public void setSummaryDataChunks(ForeignCollection<SummaryDataChunk> summaryDataChunks) {
        this.summaryDataChunks = summaryDataChunks;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
        this.updateId();
    }

    private void updateId() {
        long uid = this.userId;
        long date = this.date == null?0L:this.date.getTime();
        this.id = "ES" + uid + "." + date;
    }

//    public ForeignCollection<ExerciseNotification> getNotifications() {
//        return this.notifications;
//    }
//
//    public void setNotifications(ForeignCollection<ExerciseNotification> notifications) {
//        this.notifications = notifications;
//    }

    public Route getFollowedRoute() {
        return this.followedRoute;
    }

    public void setFollowedRoute(Route followedRoute) {
        this.followedRoute = followedRoute;
    }

    public long getUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
        this.updateId();
    }
}
