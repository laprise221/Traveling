package com.example.traveling.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.traveling.R;
import com.example.traveling.data.ActivePathRegistry;
import com.example.traveling.data.SampleData;
import com.example.traveling.model.PathStep;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment implements LocationListener {

    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final double WALKING_SPEED_KMH = 5.0;
    private static final double STEP_REACHED_THRESHOLD_M = 30.0;

    private MapView mapView;
    private RadioGroup filterGroup;

    private List<Photo> photos;
    private List<TravelPath> paths;

    private List<Marker> photoMarkers = new ArrayList<>();
    private List<Marker> pathMarkers = new ArrayList<>();

    private MaterialCardView navOverlay;
    private TextView navPathTitle, navStepLabel, navNextStep, navDistance, navTime;

    private LocationManager locationManager;
    private Marker userMarker;
    private List<Marker> activePathMarkers = new ArrayList<>();
    private Polyline activeRoutePolyline;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext().getApplicationContext();
        IConfigurationProvider config = Configuration.getInstance();
        config.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        config.setUserAgentValue(ctx.getPackageName());

        File osmdroidBasePath = new File(ctx.getCacheDir(), "osmdroid");
        config.setOsmdroidBasePath(osmdroidBasePath);
        File osmdroidTileCache = new File(osmdroidBasePath, "tiles");
        config.setOsmdroidTileCache(osmdroidTileCache);

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.map_view);
        filterGroup = view.findViewById(R.id.filter_group_map);

        navOverlay = view.findViewById(R.id.nav_overlay);
        navPathTitle = view.findViewById(R.id.nav_path_title);
        navStepLabel = view.findViewById(R.id.nav_step_label);
        navNextStep = view.findViewById(R.id.nav_next_step);
        navDistance = view.findViewById(R.id.nav_distance);
        navTime = view.findViewById(R.id.nav_time);

        view.findViewById(R.id.btn_stop_path).setOnClickListener(v -> stopActivePath());

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(5.0);
        mapView.getController().setCenter(new GeoPoint(46.0, 2.0));

        photos = SampleData.getSamplePhotos();
        paths = SampleData.getSamplePaths();

        createPhotoMarkers();
        createPathMarkers();

        filterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_all) {
                showMarkers(true, true);
            } else if (checkedId == R.id.filter_photos) {
                showMarkers(true, false);
            } else if (checkedId == R.id.filter_paths) {
                showMarkers(false, true);
            }
        });

        showMarkers(true, true);
        requestLocationPermission();

        if (ActivePathRegistry.isActive()) {
            startActivePathNavigation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (ActivePathRegistry.isActive() && navOverlay.getVisibility() != View.VISIBLE) {
            startActivePathNavigation();
        }
        if (ActivePathRegistry.isActive()) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        stopLocationUpdates();
    }

    // ---------- Parcours actif ----------

    private void startActivePathNavigation() {
        TravelPath path = ActivePathRegistry.getActivePath();
        if (path == null) return;

        navOverlay.setVisibility(View.VISIBLE);
        navPathTitle.setText(path.getTitle());

        for (Marker m : activePathMarkers) mapView.getOverlays().remove(m);
        activePathMarkers.clear();
        if (activeRoutePolyline != null) {
            mapView.getOverlays().remove(activeRoutePolyline);
            activeRoutePolyline = null;
        }

        List<PathStep> steps = path.getSteps();
        List<GeoPoint> points = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            PathStep s = steps.get(i);
            if (s.getLatitude() == 0 && s.getLongitude() == 0) continue;
            GeoPoint pt = new GeoPoint(s.getLatitude(), s.getLongitude());
            points.add(pt);

            Marker marker = new Marker(mapView);
            marker.setPosition(pt);
            marker.setTitle((i + 1) + ". " + s.getName());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_path);
            if (icon != null) marker.setIcon(icon);
            mapView.getOverlays().add(marker);
            activePathMarkers.add(marker);
        }

        if (!points.isEmpty()) {
            zoomToFit(points);
        }

        fetchAndDrawRoute(steps);
        updateNavigationOverlay(null);
        startLocationUpdates();
    }

    private void stopActivePath() {
        ActivePathRegistry.stop();
        stopLocationUpdates();

        for (Marker m : activePathMarkers) mapView.getOverlays().remove(m);
        activePathMarkers.clear();
        if (activeRoutePolyline != null) {
            mapView.getOverlays().remove(activeRoutePolyline);
            activeRoutePolyline = null;
        }
        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
            userMarker = null;
        }
        navOverlay.setVisibility(View.GONE);
        mapView.invalidate();
        Toast.makeText(requireContext(), "Parcours arrêté", Toast.LENGTH_SHORT).show();
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

    private void fetchAndDrawRoute(List<PathStep> steps) {
        List<PathStep> validSteps = new ArrayList<>();
        for (PathStep s : steps) {
            if (s.getLatitude() != 0 || s.getLongitude() != 0) validSteps.add(s);
        }
        if (validSteps.size() < 2) return;

        executor.execute(() -> {
            try {
                StringBuilder coords = new StringBuilder();
                for (int i = 0; i < validSteps.size(); i++) {
                    if (i > 0) coords.append(";");
                    coords.append(validSteps.get(i).getLongitude())
                          .append(",").append(validSteps.get(i).getLatitude());
                }
                String urlStr = "https://router.project-osrm.org/route/v1/foot/"
                        + coords + "?overview=full&geometries=geojson";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                List<GeoPoint> routePoints = new ArrayList<>();
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    if ("Ok".equals(json.optString("code", ""))) {
                        JSONArray routes = json.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONArray coordinates = routes.getJSONObject(0)
                                    .getJSONObject("geometry").getJSONArray("coordinates");
                            for (int i = 0; i < coordinates.length(); i++) {
                                JSONArray c = coordinates.getJSONArray(i);
                                routePoints.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                            }
                        }
                    }
                }

                // Fallback : ligne droite
                if (routePoints.isEmpty()) {
                    for (PathStep s : validSteps) {
                        routePoints.add(new GeoPoint(s.getLatitude(), s.getLongitude()));
                    }
                }

                List<GeoPoint> finalPoints = routePoints;
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    activeRoutePolyline = new Polyline(mapView);
                    activeRoutePolyline.setPoints(finalPoints);
                    activeRoutePolyline.getOutlinePaint().setColor(Color.parseColor("#1976D2"));
                    activeRoutePolyline.getOutlinePaint().setStrokeWidth(10f);
                    activeRoutePolyline.getOutlinePaint().setAntiAlias(true);
                    mapView.getOverlayManager().add(0, activeRoutePolyline);
                    mapView.invalidate();
                });
            } catch (Exception e) {
                Log.e("MapFragment", "Route fetch error", e);
            }
        });
    }

    // ---------- Localisation ----------

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (locationManager == null) {
            locationManager = (LocationManager) requireContext()
                    .getSystemService(Context.LOCATION_SERVICE);
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 2000, 5, this);
                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last != null) onLocationChanged(last);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 2000, 5, this);
            }
        } catch (SecurityException e) {
            Log.e("MapFragment", "Location permission error", e);
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException ignored) {}
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (!isAdded()) return;
        GeoPoint userPos = new GeoPoint(location.getLatitude(), location.getLongitude());

        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            userMarker.setTitle("Vous êtes ici");
            mapView.getOverlays().add(userMarker);
        }
        userMarker.setPosition(userPos);
        mapView.invalidate();

        if (ActivePathRegistry.isActive()) {
            updateNavigationOverlay(userPos);
        }
    }

    private void updateNavigationOverlay(@Nullable GeoPoint userPos) {
        TravelPath path = ActivePathRegistry.getActivePath();
        if (path == null || path.getSteps() == null) return;

        List<PathStep> steps = path.getSteps();
        int idx = ActivePathRegistry.getCurrentStepIndex();
        if (idx >= steps.size()) {
            navStepLabel.setText("Parcours terminé !");
            navNextStep.setText("Bravo 🎉");
            navDistance.setText("--");
            navTime.setText("--");
            return;
        }

        PathStep target = steps.get(idx);
        navStepLabel.setText("Étape " + (idx + 1) + "/" + steps.size()
                + " · Prochaine destination :");
        navNextStep.setText(target.getName());

        if (userPos == null) {
            navDistance.setText("En attente du GPS...");
            navTime.setText("--");
            return;
        }

        double distMeters = distanceMeters(
                userPos.getLatitude(), userPos.getLongitude(),
                target.getLatitude(), target.getLongitude());

        if (distMeters <= STEP_REACHED_THRESHOLD_M) {
            Toast.makeText(requireContext(),
                    "Étape atteinte : " + target.getName(), Toast.LENGTH_SHORT).show();
            ActivePathRegistry.advanceStep();
            int newIdx = ActivePathRegistry.getCurrentStepIndex();
            if (newIdx == idx) {
                navStepLabel.setText("Parcours terminé !");
                navNextStep.setText("Bravo 🎉");
                navDistance.setText("--");
                navTime.setText("--");
                return;
            }
            updateNavigationOverlay(userPos);
            return;
        }

        navDistance.setText(formatDistance(distMeters));
        navTime.setText(formatDuration(distMeters));
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String formatDistance(double meters) {
        if (meters < 1000) return Math.round(meters) + " m";
        return String.format("%.2f km", meters / 1000.0);
    }

    private String formatDuration(double meters) {
        double minutes = (meters / 1000.0) / WALKING_SPEED_KMH * 60;
        if (minutes < 1) return "< 1 min";
        if (minutes < 60) return Math.round(minutes) + " min";
        long h = (long) (minutes / 60);
        long m = Math.round(minutes - h * 60);
        return h + " h " + m + " min";
    }

    // ---------- Markers existants ----------

    private void createPhotoMarkers() {
        for (Photo photo : photos) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(photo.getLatitude(), photo.getLongitude()));
            marker.setTitle(photo.getTitle());
            marker.setSnippet(photo.getLocationName() + "\n" + photo.getAuthor());
            marker.setSubDescription("Likes: " + photo.getLikeCount());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_photo);
            if (icon != null) marker.setIcon(icon);

            marker.setOnMarkerClickListener((m, mv) -> {
                Toast.makeText(requireContext(),
                        photo.getTitle() + "\n" + photo.getLocationName(),
                        Toast.LENGTH_SHORT).show();
                m.showInfoWindow();
                return true;
            });
            photoMarkers.add(marker);
        }
    }

    private void createPathMarkers() {
        for (TravelPath path : paths) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(path.getStartLatitude(), path.getStartLongitude()));
            marker.setTitle(path.getTitle());
            marker.setSnippet(path.getCity() + " | " + path.getDuration() + " | " + path.getBudget());
            marker.setSubDescription(path.getType() + " - " + path.getDifficulty());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_path);
            if (icon != null) marker.setIcon(icon);

            marker.setOnMarkerClickListener((m, mv) -> {
                Toast.makeText(requireContext(),
                        path.getTitle() + " - " + path.getCity(),
                        Toast.LENGTH_SHORT).show();
                m.showInfoWindow();
                return true;
            });
            pathMarkers.add(marker);
        }
    }

    private void showMarkers(boolean showPhotos, boolean showPaths) {
        for (Marker m : photoMarkers) mapView.getOverlays().remove(m);
        for (Marker m : pathMarkers) mapView.getOverlays().remove(m);

        if (showPhotos) mapView.getOverlays().addAll(photoMarkers);
        if (showPaths) mapView.getOverlays().addAll(pathMarkers);
        mapView.invalidate();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivePathRegistry.isActive()) startLocationUpdates();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }
}
