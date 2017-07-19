package nz.ac.auckland.nihi.trainer.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.GeneratedRouteHelper;
import nz.ac.auckland.nihi.trainer.data.Route;

public class RouteGeneratorActivity extends FragmentActivity {
    /**
     * The helper allowing us to access the database.
     */
    private DatabaseHelper dbHelper = null;

    Button generateBtn;
    EditText elevationText;
    EditText distanceText;
    String latlng;
    TextView routeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_generator);

        routeName = (TextView) findViewById(R.id.txtRouteName);
        generateBtn = (Button) findViewById(R.id.newRouteBtn);
        elevationText = (EditText) findViewById(R.id.txtRouteElevation);
        distanceText = (EditText) findViewById(R.id.txtRouteDistance);

        generateBtn.setOnClickListener(generateButtonClickListener);

        // Get GPS Coordinates of the user
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        latlng = location.getLatitude() + "," + location.getLongitude();
    }
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
                            GeneratedRouteHelper.createRoute(6, responseJsonString, routeName.getText().toString(), routeDao);

                        }
                        finally{
                            urlConnection.disconnect();
                            finish();
                        }
                    }
                    catch(Exception e) {
                        Log.e("ERROR", e.getMessage(), e);

                    }

                }
            });
        }
    };


}
