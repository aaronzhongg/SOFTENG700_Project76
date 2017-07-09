package nz.ac.auckland.nihi.trainer.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by alex on 6/13/2017.
 */

public class Route {
    @DatabaseField(id = true)
    private String id;

    @DatabaseField(canBeNull = false, index = true, uniqueCombo = true)
    private long userId;

    @DatabaseField(canBeNull = false, index = true)
    private String name;

    @DatabaseField(canBeNull = false)
    private boolean isFavorite;

    @DatabaseField(canBeNull = false)
    private double length;

    @DatabaseField(canBeNull = false)
    private double elevation;

    @DatabaseField(canBeNull = true)
    private String thumbnailFileName;

    @ForeignCollectionField(eager = true, foreignFieldName = "route")
    @JsonIgnore
    private ForeignCollection<RouteCoordinate> gpsCoordinates;

    @DatabaseField(canBeNull = false)
    private String creatorName;

    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private long createdTimestamp;

    public Route(){

    }

    public void setLength(double length){
        this.length = length; //why is this not a thing
    }
    public void setElevation(double elevation){
        this.elevation = elevation;
    }

    public String getId() {
        return id;
    }

    private void updateId() {
        this.id = "RT" + this.userId + "." + Long.toHexString(this.createdTimestamp);
    }

    public ForeignCollection<RouteCoordinate> getGpsCoordinates() {
        return gpsCoordinates;
    }

    public void setGpsCoordinates(ForeignCollection<RouteCoordinate> gpsCoordinates) {
        this.gpsCoordinates = gpsCoordinates;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public String getThumbnailFileName() {
        return thumbnailFileName;
    }

    public void setThumbnailFileName(String thumbnailFileName) {
        this.thumbnailFileName = thumbnailFileName;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
        updateId();
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
        updateId();
    }

    @Override
    public String toString(){
        return this.name + ", " + this.creatorName;
    }
}
