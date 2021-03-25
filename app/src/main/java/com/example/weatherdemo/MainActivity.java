package com.example.weatherdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.MutableLong;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    //location stuff
    private FusedLocationProviderClient fusedLocationClient;

    //UI stuff
    EditText editText;
    TextView mainWeather;
    TextView weatherD;
    TextView tempView;
    TextView tempMNView;
    TextView phs;
    TextView cityView;
    DownloadTask task;
    Button button2;
    //Image Stuff
    ImageView image1;
    ImageView image2;
    boolean isImage1 = true;
    //property to be used everywhere in the code
    String city;

    //class for Gathering info using API setting the respective fields(background +post execution)
    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection connection;
            try {
                url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();

                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    result += (char) data;
                    data = reader.read();
                }

                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                //for main and desc
                String weatherInfo = jsonObject.getString("weather");
                JSONArray w = new JSONArray(weatherInfo);

                JSONObject jsonPartWeather = w.getJSONObject(0);
                String main = jsonPartWeather.getString("main");
                String description = jsonPartWeather.getString("description");

                if (!main.equals("")) {
                    mainWeather.setText(main);
                    ImageView temp = chooseImageView(isImage1);
                    setImageOfWeather(main, temp);
                }
                if (!description.equals("")) {
                    description = description.substring(0, 1).toUpperCase() + description.substring(1);
                    weatherD.setText(description);
                }

                //for temp pressure humidity tempmin max
                String mainInfo = jsonObject.getString("main");
                JSONObject m = new JSONObject(mainInfo);


                String temp = m.getString("temp");
                String pressure = m.getString("pressure");
                String humidity = m.getString("humidity");
                String temp_min = m.getString("temp_min");
                String temp_max = m.getString("temp_max");

                double t = Double.parseDouble(temp);
                temp = "" + (int) t;
                double tm = Double.parseDouble(temp_max);
                temp_max = "" + (int) tm;
                double tn = Double.parseDouble(temp_min);
                temp_min = "" + (int) tn;

                if (!temp.equals("")) {
                    tempView.setText(temp + "°");
                }
                if (!temp_max.equals("") && !temp_min.equals("")) {
                    tempMNView.setText(temp_max + " / " + temp_min + "°C");
                }
                String p = "";
                if (!pressure.equals("")) {
                    p += "Pressure : " + pressure + "\n";
                }
                if (!humidity.equals("")) {
                    p += "Humidity : " + humidity + "\n";
                }

                //for windspeed
                String windInfo = jsonObject.getString("wind");
                JSONObject wind = new JSONObject(windInfo);


                String speed = wind.getString("speed");
                if (!speed.equals("")) {
                    p += "Wind Speed : " + speed + "\n";
                }
                phs.setText(p);


            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Could Not Find Weather :(", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Main work for location listening
    public void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        }
    }
    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }
        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                //Initialise location
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                        List<Address> add=geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        String[] a=add.get(0).getAddressLine(0).split(", ");

                        city = add.get(0).getLocality();
                        updateWeatherOfCity(city);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    //execution of the download task and update the city
    public void updateWeatherOfCity(String c){
        String result = null;
        task = new DownloadTask();

        try {
            String encodedCityName= URLEncoder.encode(c,"UTF-8");
            result = task.execute("https://openweathermap.org/data/2.5/weather?q=" + encodedCityName + "&appid=439d4b804bc8187953eb36d2a8c26a02").get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result != null) {

            String[] temp = c.split(" ");
            c = "";
            for (String s : temp)
                c += s.substring(0, 1).toUpperCase() + s.substring(1) + " ";
            Log.i("c===>",c);

            city=c;
            Log.i("city===>",city);
            cityView.setText(city);
        }
    }

    //choosing image for transition(One--->two or two--->one)
    public ImageView chooseImageView(boolean isImg) {
        if (isImg) {
            image1.animate().alpha(0).setDuration(1000);
            isImage1 = false;
            return image2;
        } else {
            image2.animate().alpha(0).setDuration(1000);
            isImage1 = true;
            return image1;
        }

    }

    //setting the appropriate image according to the weather
    public void setImageOfWeather(String curr, ImageView temp) {
        if (curr.equals("Clear"))
            temp.setImageResource(R.drawable.clear);
        else if (curr.equals("Smoke") || curr.equals("Dust") || curr.equals("Sand") || curr.equals("Ash") || curr.equals("Squall"))
            temp.setImageResource(R.drawable.smoke);
        else if (curr.equals("Rain"))
            temp.setImageResource(R.drawable.rain1);
        else if (curr.equals("Haze") || curr.equals("Fog") || curr.equals("Mist"))
            temp.setImageResource(R.drawable.haze);
        else if (curr.equals("Drizzle"))
            temp.setImageResource(R.drawable.drizzle);
        else if (curr.equals("Snow"))
            temp.setImageResource(R.drawable.snow);
        else if (curr.equals("Tornado"))
            temp.setImageResource(R.drawable.tornado);
        else if (curr.equals("Thunderstorm"))
            temp.setImageResource(R.drawable.thunderstorm);
        else if (curr.equals("Clouds"))
            temp.setImageResource(R.drawable.cloudy);
        else
            temp.setImageResource(R.drawable.back1);

        temp.animate().alpha(1).setDuration(1000);

    }

    //onClick of the UPDATE button
    public void getWeather(View view) {
        //for hiding the keyboard when you press the button to show the full screen
        InputMethodManager mgr = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(editText.getWindowToken(), 0);


        String prev = "";
        try {
            prev = cityView.getText().toString();
            if (!editText.getText().toString().isEmpty()){
                updateWeatherOfCity(editText.getText().toString());
                button2.setVisibility(View.VISIBLE);
            }
            else{
                startListening();
            }

        } catch (Exception e) {
            e.printStackTrace();
            cityView.setText(prev);
            Toast.makeText(getApplicationContext(), "Could Not Find Weather :(", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setting up the fields
        editText = findViewById(R.id.editText);
        mainWeather = findViewById(R.id.mainWeather);
        weatherD = findViewById(R.id.weatherD);
        tempView = findViewById(R.id.tempView);
        tempMNView = findViewById(R.id.tempMNView);
        phs = findViewById(R.id.phs);
        cityView = findViewById(R.id.cityView);
        image1 = findViewById(R.id.imageView);
        image2 = findViewById(R.id.imageView2);
        button2=findViewById(R.id.button2);

        // add gibberish in city to check if the location listeners are working
        city = "pune";
        Log.i("CITY0:",city);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //trying to update the weather info
        try {
            startListening();
        }catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Could Not Find Weather :(", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==1){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                    startListening();

                }
            }
        }
    }

    public void clear(View view){
        button2.setVisibility(View.INVISIBLE);
        editText.setText("");
    }
}


