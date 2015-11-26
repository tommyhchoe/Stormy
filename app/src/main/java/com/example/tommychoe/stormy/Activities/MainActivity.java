package com.example.tommychoe.stormy.Activities;

import android.content.Context;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tommychoe.stormy.models.CurrentWeather;
import com.example.tommychoe.stormy.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
                                                    GoogleApiClient.OnConnectionFailedListener{

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 1000;

    private CurrentWeather mCurrentWeather;
    @Bind(R.id.locationLabel) TextView mLocationLabel;
    @Bind(R.id.timeLabel) TextView mTimeLabel;
    @Bind(R.id.temperatureLabel) TextView mTemperatureLabel;
    @Bind(R.id.humidityValue) TextView mHumidityValue;
    @Bind(R.id.precipValue) TextView mPrecipValue;
    @Bind(R.id.summaryLabel) TextView mSummaryLabel;
    @Bind(R.id.iconImageView) ImageView mIconImageView;
    @Bind(R.id.refreshButton) ImageView mRefreshImageView;
    @Bind(R.id.progressBar) ProgressBar mProgressBar;

    //Google Api client-related member variables
    private GoogleApiClient mGoogleApiClient;

    //Lat/long member
    private double mLatitude;
    private double mLongitude;
    private List<Address> addresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);

        //Create the google map API client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                .addConnectionCallbacks(this)
                                .addOnConnectionFailedListener(this)
                                .addApi(LocationServices.API)
                                .build();
        //Create the LocationRequest object
        LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setInterval(100 * 1000)
                .setFastestInterval(10 * 1000);

        mProgressBar.setVisibility(View.INVISIBLE);

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient != null){
            mGoogleApiClient.connect();
        }
    }

    private void getForecast() {
        String apiKey = "cc487e3a84a1e5b692482a1b0f6078d2";
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + mLatitude + "," + mLongitude;

        if(isNetworkAvailable()) {
            toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else{
            Toast.makeText(this, getString(R.string.network_unavailable), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else{
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        mTemperatureLabel.setText(String.format("%d", mCurrentWeather.getTemperature()));
        mTimeLabel.setText(String.format("At %s it will be", mCurrentWeather.getFormattedTime()));
        mHumidityValue.setText(String.format("%s", mCurrentWeather.getHumidity()));
        mPrecipValue.setText(String.format("%d%%", mCurrentWeather.getPrecipChance()));
        mSummaryLabel.setText(mCurrentWeather.getSummary());
        mLocationLabel.setText(mCurrentWeather.getTimeZone());

        //Obtain city from coordinates
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try{
            addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1);
        } catch(IOException e){
            Log.e(TAG, "Houston, we have a problem.");
        }

        if(addresses.size() > 0){
            String address = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea();
            mLocationLabel.setText(address);
        }

        Drawable drawable = ContextCompat.getDrawable(this, mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {

        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        //Parse JSON data
        JSONObject currently = forecast.getJSONObject("currently");
        //Instantiate CurrentWeather class and set member variables from JSON data.
        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(timezone);
        currentWeather.setSummary(currently.getString("summary"));

        return currentWeather;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(location == null){
            Toast.makeText(this, "Couldn't get the location. Make sure location is enabled.", Toast.LENGTH_LONG).show();
        } else {
            getNewCoordinates(location);
            getForecast();
        }
    }

    private void getNewCoordinates(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(connectionResult.hasResolution()){
            try{
                //Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }catch(IntentSender.SendIntentException e){
                e.printStackTrace();
            }
        } else{
            Log.i(TAG, "Location services connection failed with code " +
                    connectionResult.getErrorCode());
        }
    }
}



