package nz.ac.auckland.nihi.trainer.activities;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.RCExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.SummaryDataChunk;
import nz.ac.auckland.nihi.trainer.util.LocationUtils;

public class WorkoutSummaryActivity extends FragmentActivity {

    private DatabaseHelper dbHelper;
    private RCExerciseSummary s;
    Dao<RCExerciseSummary, String> exerciseSummariesDAO;
    Route route;

    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private Polyline routePolyline;

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
            s = exerciseSummariesDAO.queryForId(workoutId);

            route = getHelper().getRoutesDAO().queryForId(s.getFollowedRoute().getId());
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

            }
        });


    }
}
