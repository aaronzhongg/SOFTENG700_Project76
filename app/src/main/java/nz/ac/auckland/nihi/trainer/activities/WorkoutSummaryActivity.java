package nz.ac.auckland.nihi.trainer.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.RCExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.SummaryDataChunk;
import nz.ac.auckland.nihi.trainer.util.LocationUtils;

public class WorkoutSummaryActivity extends FragmentActivity {

    private DatabaseHelper dbHelper;
    private RCExerciseSummary exerciseSummary;
    Dao<RCExerciseSummary, String> exerciseSummariesDAO;
    Route route;

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Polyline routePolyline;
    // A value indicating if we're currently showing the map.
    private boolean isShowingMap = false;

    private TextView timeElapsed;
    private TextView totalDistance;
    private TextView averageSpeed;
    private TextView averageHeartrate;

    /**
     * Lazily creates the {@link #dbHelper} if required, then returns it.
     *
     * @return
     */
    //Make a database helper
    private DatabaseHelper getHelper() {
        if (dbHelper == null) {
            dbHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_summary);
        try {
            // Get RCExerciseSummary object to be displayed
            exerciseSummariesDAO = getHelper().getExerciseSummaryDAO();
            String workoutId = getIntent().getExtras().getString("workout_id");
            exerciseSummary = exerciseSummariesDAO.queryForId(workoutId);

            route = getHelper().getRoutesDAO().queryForId(exerciseSummary.getFollowedRoute().getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.summary_map);
        // Add a custom marker to the map to show the user's location.
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
//                final MarkerOptions markerOpt = new MarkerOptions();
//                markerOpt.icon(BitmapDescriptorFactory.fromResource(R.drawable.nihi_map_marker)).anchor(0.5f, 1f)
//                        .draggable(false).position(new LatLng(0, 0));
//                WorkoutActivity.this.userMapMarker = googleMap.addMarker(markerOpt);
                WorkoutSummaryActivity.this.mMap = googleMap;
                PolylineOptions polyOpt = new PolylineOptions();
                polyOpt.add(LocationUtils.toLatLngArray(route)).color(Color.RED).width(10);
                if (WorkoutSummaryActivity.this.mMap != null) {
                    routePolyline = WorkoutSummaryActivity.this.mMap.addPolyline(polyOpt);
                }

                addMarkers();
                showOnMap(new LatLng(route.getGpsCoordinates().iterator().next().getLatitude(),route.getGpsCoordinates().iterator().next().getLongitude()));
            }
        });

        timeElapsed = (TextView) findViewById(R.id.total_time_elapsed);
        totalDistance = (TextView) findViewById(R.id.total_distance_ran);
        averageSpeed = (TextView) findViewById(R.id.average_speed);
        averageHeartrate = (TextView) findViewById(R.id.average_heartrate);

        setStats();

    }

    private void addMarkers() {
        Collection<SummaryDataChunk> summaryDataChunks = exerciseSummary.getSummaryDataChunks();
        for (SummaryDataChunk s: summaryDataChunks) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(s.getSessionTimestamp());
//            long seconds = TimeUnit.MILLISECONDS.toSeconds(s.getSessionTimestamp());
            mMap.addMarker(new MarkerOptions().position(new LatLng(s.getLatitude(), s.getLongitude())).title("minute: " + minutes)
                    .snippet("Heart rate (bpm): " + s.getHeartRate() + "\nSpeed (km/h): " + s.getSpeed()));
        }

    }

    private void setStats(){
        int duration = exerciseSummary.getDurationInSeconds();
        int minutes = duration / 60;
        int seconds = duration % 60;

        if (seconds < 10) {
            timeElapsed.setText(minutes + ":0" + seconds);
        } else {
            timeElapsed.setText(minutes + ":" + seconds);
        }

        String temp = exerciseSummary.getDistanceInMetres()/1000 + "km";
        totalDistance.setText(temp);

        temp = String.format("%.2f",exerciseSummary.getAvgSpeed()) + "km/hr";
        averageSpeed.setText(temp);

        temp = exerciseSummary.getAvgHeartRate() + "bpm";
        averageHeartrate.setText(temp);

    }

    private void showOnMap(LatLng gmLocation) {
        final double RADIUS_KM = 2; // the distance, in KM, to show around the point.

//        if (!TestHarnessUtils.isTestHarness() && isShowingMap) {
//            LatLng gmLocation = new LatLng(location.getLatitude(), location.getLongitude());

            // For now, just display RADIUS_KM in all directions around the given location.
            LatLng northBounds = LocationUtils.getLocation(gmLocation, 0, RADIUS_KM);
            LatLng southBounds = LocationUtils.getLocation(gmLocation, 180, RADIUS_KM);
            LatLng eastBounds = LocationUtils.getLocation(gmLocation, 90, RADIUS_KM);
            LatLng westBounds = LocationUtils.getLocation(gmLocation, 270, RADIUS_KM);

            LatLngBounds bounds;
            int padding;

            if (routePolyline == null) {
                bounds = LatLngBounds.builder().include(northBounds).include(southBounds).include(eastBounds)
                        .include(westBounds).include(gmLocation).build();
                padding = 0;
            } else {
                LatLngBounds.Builder builder = LatLngBounds.builder().include(gmLocation);
                for (LatLng pt : routePolyline.getPoints()) {
                    builder.include(pt);
                }
                bounds = builder.build();
                padding = 100;
            }

            final CameraUpdate camUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            if (this.mMap != null) {
                try {
                    this.mMap.moveCamera(camUpdate);

                } catch (IllegalStateException ex) {
                    // Map not yet initialized.
//                    logger.info("showOnMap(): IllegalStateException when trying to move camera. Will delegate task to listener.");
                    final View mapView = mapFragment.getView();
                    if (mapView.getViewTreeObserver().isAlive()) {
                        mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @SuppressWarnings("deprecation")
                            @SuppressLint("NewApi")
                            @Override
                            public void onGlobalLayout() {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                    mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                } else {
                                    mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                                WorkoutSummaryActivity.this.mMap.moveCamera(camUpdate);
                            }
                        });
                    }
                }

                mMap.getUiSettings().setScrollGesturesEnabled(false);
            }
//        }
    }
}
