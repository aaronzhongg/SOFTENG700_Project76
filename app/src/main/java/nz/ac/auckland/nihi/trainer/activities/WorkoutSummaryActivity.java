package nz.ac.auckland.nihi.trainer.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nz.ac.auckland.cs.odin.android.api.services.testharness.TestHarnessUtils;
import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.RCExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.SummaryDataChunk;
import nz.ac.auckland.nihi.trainer.util.LocationUtils;
import nz.ac.auckland.nihi.trainer.views.CustomMapFragment;

public class WorkoutSummaryActivity extends FragmentActivity {

    private DatabaseHelper dbHelper;
    private RCExerciseSummary exerciseSummary;
    Dao<RCExerciseSummary, String> exerciseSummariesDAO;
    Route route;

    private ScrollView scrollView;
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Polyline routePolyline;
    // A value indicating if we're currently showing the map.
    private boolean isShowingMap = false;

    private TextView timeElapsed;
    private TextView totalDistance;
    private TextView averageSpeed;
    private TextView averageHeartrate;

    private GraphView heartrateGraph;
    private GraphView speedGraph;
    LineGraphSeries<DataPoint> heartRatePoints;
    LineGraphSeries<DataPoint> speedPoints;

    int runDuration;

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

        timeElapsed = (TextView) findViewById(R.id.total_time_elapsed);
        totalDistance = (TextView) findViewById(R.id.total_distance_ran);
        averageSpeed = (TextView) findViewById(R.id.average_speed);
        averageHeartrate = (TextView) findViewById(R.id.average_heartrate);


        setStats();

        scrollView = (ScrollView) findViewById(R.id.map_scroll_view);
        mapFragment = (CustomMapFragment) getSupportFragmentManager().findFragmentById(R.id.summary_map);
        ((CustomMapFragment) getSupportFragmentManager().findFragmentById(R.id.summary_map)).setListener(new CustomMapFragment.OnTouchListener() {
            @Override
            public void onTouch() {
                scrollView.requestDisallowInterceptTouchEvent(true);
            }
        });
        // Add a custom marker to the map to show the user's location.
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
//                final MarkerOptions markerOpt = new MarkerOptions();
//                markerOpt.icon(BitmapDescriptorFactory.fromResource(R.drawable.nihi_map_marker)).anchor(0.5f, 1f)
//                        .draggable(false).position(new LatLng(0, 0));
//                WorkoutActivity.this.userMapMarker = googleMap.addMarker(markerOpt);
                WorkoutSummaryActivity.this.mMap = googleMap;
                WorkoutSummaryActivity.this.mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                    @Override
                    public View getInfoWindow(Marker marker) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        View v = getLayoutInflater().inflate(R.layout.marker_popup, null);
                        TextView tv = (TextView) v.findViewById(R.id.markertitle);
                        tv.setText(marker.getTitle());
                        tv = (TextView) v.findViewById(R.id.markersnippet);
                        tv.setText(marker.getSnippet());
                        return v;
                    }
                });
                PolylineOptions polyOpt = new PolylineOptions();
                polyOpt.add(LocationUtils.toLatLngArray(route)).color(Color.RED).width(10);
                if (WorkoutSummaryActivity.this.mMap != null) {
                    routePolyline = WorkoutSummaryActivity.this.mMap.addPolyline(polyOpt);
                }

                heartrateGraph = (GraphView) findViewById(R.id.heartrate_graph);
                speedGraph = (GraphView) findViewById(R.id.speed_graph);
                addData();
                showOnMap(new LatLng(route.getGpsCoordinates().iterator().next().getLatitude(),route.getGpsCoordinates().iterator().next().getLongitude()));
            }
        });



    }

    private void addData() {
        Collection<SummaryDataChunk> summaryDataChunks = exerciseSummary.getSummaryDataChunks();
        List<DataPoint> hrDp = new ArrayList<DataPoint>();
        List<DataPoint> sDp = new ArrayList<DataPoint>();
        int minHeart = 999;
        int maxHeart = 0;
        float minSpeed = 999f;
        float maxSpeed = 0f;

        for (SummaryDataChunk s: summaryDataChunks) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(s.getSessionTimestamp());
//            long seconds = TimeUnit.MILLISECONDS.toSeconds(s.getSessionTimestamp());
            int heart = s.getHeartRate();
            float speed = s.getSpeed();
            mMap.addMarker(new MarkerOptions().position(new LatLng(s.getLatitude(), s.getLongitude())).title("minute: " + minutes)
                    .snippet("Heart rate (bpm): " + heart + "\nSpeed (km/h): " + speed));

            // Add data to plot on graph
            hrDp.add(new DataPoint(minutes, heart));
            sDp.add(new DataPoint(minutes, speed));

            if (minSpeed > speed) {
                minSpeed = speed;
            } else if (maxSpeed < speed) {
                maxSpeed = speed;
            }

            if(minHeart > heart) {
                minHeart = heart;
            } else if (maxHeart < heart) {
                maxHeart = heart;
            }

        }

        DataPoint[] hrtemp = new DataPoint[ hrDp.size() ];
        DataPoint[] stemp = new DataPoint[ sDp.size() ];
        hrDp.toArray(hrtemp);
        sDp.toArray(stemp);

        heartrateGraph.getViewport().setYAxisBoundsManual(true);
        heartrateGraph.getViewport().setMinY(minHeart);
        heartrateGraph.getViewport().setMaxY(maxHeart);

        heartrateGraph.getViewport().setXAxisBoundsManual(true);
        heartrateGraph.getViewport().setMinX(0);
        heartrateGraph.getViewport().setMaxX(runDuration + 1);

        speedGraph.getViewport().setYAxisBoundsManual(true);
        speedGraph.getViewport().setMinY(minSpeed);
        speedGraph.getViewport().setMaxY(maxSpeed);

        speedGraph.getViewport().setXAxisBoundsManual(true);
        speedGraph.getViewport().setMinX(0);
        speedGraph.getViewport().setMaxX(runDuration + 1);

        heartRatePoints = new LineGraphSeries<>(hrtemp);
        speedPoints = new LineGraphSeries<>(stemp);
        heartRatePoints.setTitle("Heart rate");
        speedPoints.setTitle("Speed");
        heartrateGraph.addSeries(heartRatePoints);
        speedGraph.addSeries(speedPoints);

        //StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(heartrateGraph);
        //staticLabelsFormatter.setHorizontalLabels(new String[] {"Time", "(min)"});
        //staticLabelsFormatter.setVerticalLabels(new String[] {"Heart rate", "(bpm)"});
        heartrateGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time(min)");
        heartrateGraph.getGridLabelRenderer().setVerticalAxisTitle("Heart rate (bpm)");

        //StaticLabelsFormatter staticLabelsFormatter1 = new StaticLabelsFormatter(speedGraph);
        //staticLabelsFormatter1.setHorizontalLabels(new String[] {"Time ", "(min)"});
        //staticLabelsFormatter1.setVerticalLabels(new String[] {"Speed ", "(km/h)"});
        speedGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time(min)");
        speedGraph.getGridLabelRenderer().setVerticalAxisTitle("Speed (km/h)");
    }

    private void setStats(){
        int duration = exerciseSummary.getDurationInSeconds();
        int minutes = duration / 60;
        int seconds = duration % 60;

        runDuration = minutes;

        if (seconds < 10) {
            timeElapsed.setText(minutes + ":0" + seconds);
        } else {
            timeElapsed.setText(minutes + ":" + seconds);
        }

        String temp = ((float)exerciseSummary.getDistanceInMetres())/1000 + "km";
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

//                mMap.getUiSettings().setScrollGesturesEnabled(false);
            }
//        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        startActivity(new Intent(WorkoutSummaryActivity.this, WorkoutSummaryListActivity.class));
        finish();

    }
}
