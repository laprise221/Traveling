package com.example.traveling.ui.detail;

import android.graphics.Color;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.ActivePathRegistry;
import com.example.traveling.data.PathRegistry;
import com.example.traveling.model.PathStep;
import com.example.traveling.model.TravelPath;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PathDetailFragment extends Fragment {

    private static final String ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImQ4YWFkZjhlMWY3ZDQ2NjU4YmYxYzM1OTllM2RiN2QwIiwiaCI6Im11cm11cjY0In0=";

    private MapView mapView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext().getApplicationContext();
        org.osmdroid.config.IConfigurationProvider config =
                org.osmdroid.config.Configuration.getInstance();
        config.load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        config.setUserAgentValue(ctx.getPackageName());

        java.io.File osmdroidBasePath = new java.io.File(ctx.getCacheDir(), "osmdroid");
        config.setOsmdroidBasePath(osmdroidBasePath);
        config.setOsmdroidTileCache(new java.io.File(osmdroidBasePath, "tiles"));

        return inflater.inflate(R.layout.fragment_path_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TravelPath path = PathRegistry.get();
        if (path == null) {
            Navigation.findNavController(view).navigateUp();
            return;
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_path_detail);
        toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
        toolbar.setTitle(path.getTitle());

        ((TextView) view.findViewById(R.id.tv_path_title)).setText(path.getTitle());
        ((TextView) view.findViewById(R.id.tv_path_city)).setText(path.getCity());
        ((TextView) view.findViewById(R.id.tv_path_description)).setText(
                path.getDescription() != null && !path.getDescription().isEmpty()
                        ? path.getDescription() : "Aucune description.");

        ((Chip) view.findViewById(R.id.chip_duration)).setText(path.getDuration());
        ((Chip) view.findViewById(R.id.chip_budget)).setText(path.getBudget());
        ((Chip) view.findViewById(R.id.chip_difficulty)).setText(path.getDifficulty());

        mapView = view.findViewById(R.id.map_path_detail);
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        MaterialButton btnStart = view.findViewById(R.id.btn_start_path);
        List<PathStep> startSteps = path.getSteps();
        if (startSteps == null || startSteps.isEmpty()) {
            btnStart.setEnabled(false);
            btnStart.setText("Aucune étape à parcourir");
        } else {
            btnStart.setOnClickListener(v -> {
                ActivePathRegistry.start(path);
                Toast.makeText(requireContext(),
                        "Parcours démarré ! Direction la carte.", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(v).navigate(R.id.navigation_map);
            });
        }

        LinearLayout stepsContainer = view.findViewById(R.id.steps_list_container);

        List<PathStep> steps = path.getSteps();
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                addStepToList(stepsContainer, steps.get(i), i + 1);
            }
            displayStepsOnMap(steps);
            fetchRoute(steps);
        } else {
            stepsContainer.addView(makeTextView("Aucune étape enregistrée."));
            mapView.getController().setZoom(5.0);
            mapView.getController().setCenter(new GeoPoint(46.0, 2.0));
        }
    }

    private void addStepToList(LinearLayout container, PathStep step, int index) {
        View stepView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_step, container, false);

        ((TextView) stepView.findViewById(R.id.tv_step_number)).setText(String.valueOf(index));
        ((TextView) stepView.findViewById(R.id.tv_step_name)).setText(step.getName());
        stepView.findViewById(R.id.btn_remove_step).setVisibility(View.GONE);

        container.addView(stepView);
    }

    private TextView makeTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setPadding(0, 16, 0, 0);
        return tv;
    }

    private void displayStepsOnMap(List<PathStep> steps) {
        List<GeoPoint> points = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            PathStep step = steps.get(i);
            if (step.getLatitude() == 0 && step.getLongitude() == 0) continue;

            GeoPoint point = new GeoPoint(step.getLatitude(), step.getLongitude());
            points.add(point);

            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setTitle((i + 1) + ". " + step.getName());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        }

        if (!points.isEmpty()) {
            zoomToFit(points);
        }
    }

    private void zoomToFit(List<GeoPoint> points) {
        if (points.size() == 1) {
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(points.get(0));
            return;
        }

        double north = -90, south = 90, east = -180, west = 180;
        for (GeoPoint p : points) {
            if (p.getLatitude() > north) north = p.getLatitude();
            if (p.getLatitude() < south) south = p.getLatitude();
            if (p.getLongitude() > east) east = p.getLongitude();
            if (p.getLongitude() < west) west = p.getLongitude();
        }
        double spanLat = north - south;
        double spanLon = east - west;
        double pad = Math.max(0.002, Math.max(spanLat, spanLon) * 0.3);
        BoundingBox box = new BoundingBox(north + pad, east + pad, south - pad, west - pad);
        mapView.post(() -> {
            mapView.zoomToBoundingBox(box, true, 80);
            if (mapView.getZoomLevelDouble() > 18) {
                mapView.getController().setZoom(18.0);
            }
        });
    }

    private void fetchRoute(List<PathStep> steps) {
        Log.d("PathDetail", "fetchRoute: " + steps.size() + " étapes");
        List<PathStep> validSteps = new ArrayList<>();
        for (PathStep s : steps) {
            if (s.getLatitude() != 0 || s.getLongitude() != 0) {
                validSteps.add(s);
            }
        }
        if (validSteps.size() < 2) return;

        executor.execute(() -> {
            try {
                JSONArray coordsArr = new JSONArray();
                for (PathStep s : validSteps) {
                    JSONArray c = new JSONArray();
                    c.put(s.getLongitude());
                    c.put(s.getLatitude());
                    coordsArr.put(c);
                }
                JSONObject body = new JSONObject();
                body.put("coordinates", coordsArr);
                body.put("instructions", false);

                URL url = new URL("https://api.openrouteservice.org/v2/directions/foot-walking/geojson");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", ORS_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json, application/geo+json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int responseCode = conn.getResponseCode();
                Log.d("PathDetail", "ORS response: " + responseCode);
                if (responseCode != 200) {
                    mainHandler.post(() -> drawStraightLine(validSteps));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray features = json.optJSONArray("features");
                if (features == null || features.length() == 0) {
                    mainHandler.post(() -> drawStraightLine(validSteps));
                    return;
                }

                JSONArray coordinates = features.getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                List<GeoPoint> routePoints = new ArrayList<>();
                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray coord = coordinates.getJSONArray(i);
                    routePoints.add(new GeoPoint(coord.getDouble(1), coord.getDouble(0)));
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Polyline polyline = new Polyline(mapView);
                    polyline.setPoints(routePoints);
                    polyline.getOutlinePaint().setColor(Color.BLUE);
                    polyline.getOutlinePaint().setStrokeWidth(10f);
                    polyline.getOutlinePaint().setAntiAlias(true);
                    mapView.getOverlayManager().add(polyline);
                    mapView.invalidate();
                });

            } catch (Exception e) {
                Log.e("PathDetail", "Route fetch error", e);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    drawStraightLine(validSteps);
                });
            }
        });
    }

    private void drawStraightLine(List<PathStep> steps) {
        if (!isAdded()) return;
        List<GeoPoint> points = new ArrayList<>();
        for (PathStep s : steps) {
            points.add(new GeoPoint(s.getLatitude(), s.getLongitude()));
        }
        Polyline polyline = new Polyline(mapView);
        polyline.setPoints(points);
        polyline.getOutlinePaint().setColor(Color.BLUE);
        polyline.getOutlinePaint().setStrokeWidth(10f);
        polyline.getOutlinePaint().setAntiAlias(true);
        mapView.getOverlayManager().add(polyline);
        mapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }
}
