package nz.ac.auckland.nihi.trainer.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.odin.android.services.LocalBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.GeneratedRouteHelper;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.services.location.GPSServiceImpl;
import nz.ac.auckland.nihi.trainer.services.location.GPSServiceListener;
import nz.ac.auckland.nihi.trainer.services.location.IGPSService;

public class RouteGeneratorActivity extends FragmentActivity implements GPSServiceListener {
    /**
     * The helper allowing us to access the database.
     */
    private DatabaseHelper dbHelper = null;

    // The GPS service used to filter by location and also to view the distance between the user and a route.
    private LocalBinder<IGPSService> gpsService;

    Button generateBtn;
    EditText elevationText;
    EditText distanceText;
    String latlng;
    TextView routeName;
    LocationManager locationManager;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_generator);

        routeName = (TextView) findViewById(R.id.txtRouteName);
        generateBtn = (Button) findViewById(R.id.newRouteBtn);
        elevationText = (EditText) findViewById(R.id.txtRouteElevation);
        distanceText = (EditText) findViewById(R.id.txtRouteDistance);

        generateBtn.setOnClickListener(generateButtonClickListener);
        dialog =  new ProgressDialog(RouteGeneratorActivity.this);
        getLocation();
    }

    protected void getLocation() {
        dialog.setMessage("Retrieving current location...");
        dialog.show();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

//        if (location != null) {
//            latlng = location.getLatitude() + "," + location.getLongitude();
//            dialog.dismiss();
//        } else {
            bindService(new Intent(this, GPSServiceImpl.class), gpsConn, BIND_AUTO_CREATE);
//        }
    }
//    protected void getLocation() {
//        dialog.setMessage("Retrieving current location...");
//        dialog.show();
//        // Get GPS Coordinates of the user
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
////        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
////        Criteria criteria = new Criteria();
////        String bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
//    }

    //Make a database helper
    private DatabaseHelper getHelper() {
        if (dbHelper == null) {
            dbHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }


    private View.OnClickListener generateButtonClickListener = new View.OnClickListener() {

        // Call API to generate new route based on the inputs
        @Override
        public void onClick(View view) {
            if (!routeName.getText().toString().trim().equals("") && !distanceText.getText().toString().trim().equals("") && !elevationText.getText().toString().trim().equals("")) {
                dialog.setMessage("Generating new route...");
                dialog.show();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            URL url = new URL("https://project76.azurewebsites.net/api/directions/generateroute?inputDistance=" + distanceText.getText().toString() + "&latlng=" + latlng + "&inputElevation=" + elevationText.getText().toString());
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            try {
                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                                StringBuilder stringBuilder = new StringBuilder();
                                String line;
                                while ((line = bufferedReader.readLine()) != null) {
                                    stringBuilder.append(line).append("\n");
                                }
                                bufferedReader.close();

                                String responseJsonString = stringBuilder.toString();
                                Dao<Route, String> routeDao = getHelper().getRoutesDAO();
                                GeneratedRouteHelper.createRoute(routeDao.countOf() + 1, responseJsonString, routeName.getText().toString(), routeDao);

                            } finally {
                                urlConnection.disconnect();
                                dialog.dismiss();
                                finish();
                            }
                        } catch (Exception e) {
                            Log.e("ERROR", e.getMessage(), e);

                        }

                    }

                });
            } else {

                if (routeName.getText().toString().trim().equals(""))
                {
                    routeName.setError("Route name is required");
                }

                if (elevationText.getText().toString().trim().equals("")) {
                    elevationText.setError("Elevation amount is required");
                }

                if (distanceText.getText().toString().trim().equals("")) {
                    distanceText.setError("Distance amount is required");
                }
            }
        }
    };


    private final ServiceConnection gpsConn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            gpsService = (LocalBinder<IGPSService>) service;
            gpsService.getService().setGPSServiceListener(RouteGeneratorActivity.this);
        }
    };

    @Override
    public void newLocationReceived(Location location) {
        latlng = location.getLatitude() + "," + location.getLongitude();
        if (gpsService != null) {
            gpsService.getService().setGPSServiceListener(null);
            unbindService(gpsConn);
        }
        dialog.dismiss();
    }

    @Override
    public void gpsConnectivityChanged(boolean isConnected) {

    }
}
