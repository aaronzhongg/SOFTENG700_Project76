package nz.ac.auckland.nihi.trainer.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.services.location.GPSServiceImpl;
import nz.ac.auckland.nihi.trainer.util.LocationUtils;

public class RouteGeneratorActivity extends FragmentActivity {
    Button generateBtn;
    EditText elevationText;
    EditText distanceText;
    String latlng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_generator);

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

                        }
                        finally{
                            urlConnection.disconnect();
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
