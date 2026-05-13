package com.example.traveling.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeocodingUtils {

    private static final String TAG = "GeocodingUtils";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface GeocodingCallback {
        void onResult(double latitude, double longitude);
        void onFailure();
    }

    /** Geocode a city/place name to coordinates using Nominatim (OpenStreetMap). */
    public static void geocode(String placeName, GeocodingCallback callback) {
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(placeName, "UTF-8");
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" + encoded
                        + "&format=json&limit=1&accept-language=fr";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() != 200) {
                    mainHandler.post(callback::onFailure);
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray results = new JSONArray(sb.toString());
                if (results.length() == 0) {
                    mainHandler.post(callback::onFailure);
                    return;
                }

                JSONObject first = results.getJSONObject(0);
                double lat = first.getDouble("lat");
                double lon = first.getDouble("lon");
                mainHandler.post(() -> callback.onResult(lat, lon));

            } catch (Exception e) {
                Log.e(TAG, "Geocoding failed for: " + placeName, e);
                mainHandler.post(callback::onFailure);
            }
        });
    }
}
