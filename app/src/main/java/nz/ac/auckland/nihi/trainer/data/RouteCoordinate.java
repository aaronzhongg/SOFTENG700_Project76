package nz.ac.auckland.nihi.trainer.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by alex on 6/13/2017.
 */
@DatabaseTable(tableName = "route_coords")
public class RouteCoordinate {

    @DatabaseField(canBeNull = false, foreign = true)
    @JsonIgnore
    private Route route;

    @DatabaseField(canBeNull = false)
    private double latitude;

    @DatabaseField(canBeNull = false)
    private double longitude;

    public RouteCoordinate() {
    }

    public RouteCoordinate(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }
}
