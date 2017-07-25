package nz.ac.auckland.nihi.trainer.data;

import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by alex on 7/25/2017.
 */

@DatabaseTable(
        tableName = "summary_data_chunks"
)
public class summaryDataChunk {
    @DatabaseField(
            id = true
    )
    private String id;

    @DatabaseField(
            canBeNull = false
    )
    private long sessionTimestamp;

    @DatabaseField(
            canBeNull = false
    )
    private double latitude;

    @DatabaseField(
            canBeNull = false
    )
    private double longitude;

    @DatabaseField(
            canBeNull = false
    )
    private float speed;

    @DatabaseField(
            canBeNull = false
    )
    private int heartRate;

    public summaryDataChunk( long timestamp,LatLng latLng, float speed, int heartRate){
        this.sessionTimestamp = timestamp;
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
        this.speed = speed;
        this.heartRate = heartRate;
    }

    public long getSessionTimestamp() {
        return sessionTimestamp;
    }

    public void setSessionTimestamp(long sessionDate) {
        this.sessionTimestamp = sessionDate;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }
}
