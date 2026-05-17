package com.example.traveling.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.example.traveling.data.ImageUtils;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.traveling.R;
import com.example.traveling.data.ActivePathRegistry;
import com.example.traveling.data.FirestoreRepository;
import com.example.traveling.data.PathRegistry;
import com.example.traveling.data.PhotoRegistry;
import com.example.traveling.model.PathStep;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
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

    private List<Photo> photos = new ArrayList<>();
    private List<TravelPath> paths = new ArrayList<>();

    private List<Marker> photoMarkers = new ArrayList<>();
    private List<Marker> pathMarkers = new ArrayList<>();

    // Navigation bottom sheet (parcours actif)
    private LinearLayout navBottomSheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView navPathTitle, navStepLabel, navNextStep, navDistance, navTime;
    private LinearLayout navStepCards;

    // Preview card (aperçu au clic sur pin)
    private MaterialCardView pinPreviewCard;
    private ImageView pinPreviewImage;
    private TextView pinPreviewType, pinPreviewTitle, pinPreviewSubtitle;
    private Photo selectedPhoto = null;
    private TravelPath selectedPath = null;

    // Filtre courant
    private boolean showPhotos = true;
    private boolean showPaths = true;

    private FloatingActionButton fabMyLocation;
    private GeoPoint lastKnownUserPos = null;
    private boolean firstLocationFix = true;

    private LocationManager locationManager;
    private Marker userMarker;
    private List<Marker> activePathMarkers = new ArrayList<>();
    private final SparseArray<Marker> stepMarkersByIndex = new SparseArray<>();
    private Polyline activeRoutePolyline;
    private Polyline liveSegmentPolyline;
    private GeoPoint lastLiveRoutePos;
    private boolean liveRouteFetching = false;
    private static final double LIVE_ROUTE_MIN_MOVE_M = 20.0;

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

        navBottomSheet = view.findViewById(R.id.nav_bottom_sheet);
        navPathTitle = view.findViewById(R.id.nav_path_title);
        navStepLabel = view.findViewById(R.id.nav_step_label);
        navNextStep = view.findViewById(R.id.nav_next_step);
        navDistance = view.findViewById(R.id.nav_distance);
        navTime = view.findViewById(R.id.nav_time);
        navStepCards = view.findViewById(R.id.nav_step_cards);

        bottomSheetBehavior = BottomSheetBehavior.from(navBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        pinPreviewCard = view.findViewById(R.id.pin_preview_card);
        pinPreviewImage = view.findViewById(R.id.pin_preview_image);
        pinPreviewType = view.findViewById(R.id.pin_preview_type);
        pinPreviewTitle = view.findViewById(R.id.pin_preview_title);
        pinPreviewSubtitle = view.findViewById(R.id.pin_preview_subtitle);

        navBottomSheet.findViewById(R.id.btn_stop_path).setOnClickListener(v -> stopActivePath());
        pinPreviewCard.setOnClickListener(v -> navigateToSelectedDetail());

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(5.0);
        mapView.getController().setCenter(new GeoPoint(46.0, 2.0));

        // Ferme l'aperçu quand on tape sur la carte vide
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                hidePinPreview();
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(0, eventsOverlay);

        filterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            hidePinPreview();
            if (checkedId == R.id.filter_all) {
                showPhotos = true;
                showPaths = true;
            } else if (checkedId == R.id.filter_photos) {
                showPhotos = true;
                showPaths = false;
            } else if (checkedId == R.id.filter_paths) {
                showPhotos = false;
                showPaths = true;
            }
            showMarkers(showPhotos, showPaths);
        });

        fabMyLocation = view.findViewById(R.id.fab_my_location);
        fabMyLocation.setOnClickListener(v -> centerOnUser());

        loadDataFromFirestore();
        requestLocationPermission();

        if (ActivePathRegistry.isActive()) {
            startActivePathNavigation();
        }
    }

    // ---------- Chargement des données Firestore ----------

    private void loadDataFromFirestore() {
        FirestoreRepository.get().loadPublicPhotos(photoList -> {
            if (!isAdded()) return;
            photos = photoList != null ? photoList : new ArrayList<>();
            rebuildPhotoMarkers();
            showMarkers(showPhotos, showPaths);
        });

        FirestoreRepository.get().loadPublicPaths(pathList -> {
            if (!isAdded()) return;
            paths = pathList != null ? pathList : new ArrayList<>();
            rebuildPathMarkers();
            showMarkers(showPhotos, showPaths);
        });
    }

    // ---------- Aperçu du pin ----------

    private void showPhotoPreview(Photo photo) {
        pinPreviewType.setText("PHOTO · " + photo.getLocationType().toUpperCase());
        pinPreviewTitle.setText(photo.getTitle());
        pinPreviewSubtitle.setText(photo.getLocationName() + "  ·  " + photo.getLikeCount() + " likes");
        setPreviewImage(photo.getImageBitmap() != null ? null : null, photo);
        pinPreviewCard.setVisibility(View.VISIBLE);
    }

    private void setPreviewImage(Object ignored, Photo photo) {
        if (photo.getImageBitmap() != null) {
            pinPreviewImage.setImageBitmap(photo.getImageBitmap());
        } else if (photo.getImageUri() != null) {
            pinPreviewImage.setImageURI(photo.getImageUri());
        } else if (photo.getImageResId() != 0) {
            pinPreviewImage.setImageResource(photo.getImageResId());
        } else {
            pinPreviewImage.setImageResource(R.drawable.ic_marker_photo);
        }
    }

    private void showPathPreview(TravelPath path) {
        pinPreviewType.setText("PARCOURS · " + path.getCity().toUpperCase());
        pinPreviewTitle.setText(path.getTitle());
        pinPreviewSubtitle.setText(path.getDuration() + "  ·  " + path.getDifficulty() + "  ·  " + path.getLikeCount() + " likes");
        if (path.getImageResId() != 0) {
            pinPreviewImage.setImageResource(path.getImageResId());
        } else {
            pinPreviewImage.setImageResource(R.drawable.ic_marker_path);
        }
        pinPreviewCard.setVisibility(View.VISIBLE);
    }

    private void hidePinPreview() {
        pinPreviewCard.setVisibility(View.GONE);
        selectedPhoto = null;
        selectedPath = null;
    }

    private void navigateToSelectedDetail() {
        if (selectedPhoto != null) {
            PhotoRegistry.set(selectedPhoto);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_map_to_photo_detail);
        } else if (selectedPath != null) {
            PathRegistry.set(selectedPath);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_map_to_path_detail);
        }
    }

    // ---------- Création des markers ----------

    private void rebuildPhotoMarkers() {
        for (Marker m : photoMarkers) mapView.getOverlays().remove(m);
        photoMarkers.clear();

        // Only keep photos with valid coordinates
        List<Photo> validPhotos = new ArrayList<>();
        for (Photo photo : photos) {
            if (photo.getLatitude() == 0 && photo.getLongitude() == 0) continue;
            validPhotos.add(photo);
        }

        // Group indices by rounded position (3 decimal places ≈ 111m)
        java.util.Map<String, List<Integer>> posGroups = new java.util.HashMap<>();
        for (int i = 0; i < validPhotos.size(); i++) {
            Photo p = validPhotos.get(i);
            String key = Math.round(p.getLatitude() * 1000) + "," + Math.round(p.getLongitude() * 1000);
            posGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        // Build markers
        for (Photo photo : validPhotos) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(photo.getLatitude(), photo.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_photo);
            if (icon != null) marker.setIcon(icon);
            marker.setOnMarkerClickListener((m, mv) -> {
                selectedPhoto = photo;
                selectedPath = null;
                showPhotoPreview(photo);
                return true;
            });
            photoMarkers.add(marker);
        }

        // Spread overlapping markers in a small circle so each is clickable
        double spreadRadius = 0.0005; // ~55 m — visible at city zoom, discreet at country zoom
        for (List<Integer> group : posGroups.values()) {
            if (group.size() <= 1) continue;
            for (int k = 0; k < group.size(); k++) {
                int idx = group.get(k);
                double baseLat = validPhotos.get(idx).getLatitude();
                double baseLon = validPhotos.get(idx).getLongitude();
                double angle = 2 * Math.PI * k / group.size();
                photoMarkers.get(idx).setPosition(new GeoPoint(
                        baseLat + spreadRadius * Math.cos(angle),
                        baseLon + spreadRadius * Math.sin(angle)));
            }
        }
    }

    private void rebuildPathMarkers() {
        for (Marker m : pathMarkers) mapView.getOverlays().remove(m);
        pathMarkers.clear();

        for (TravelPath path : paths) {
            if (path.getStartLatitude() == 0 && path.getStartLongitude() == 0) continue;

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(path.getStartLatitude(), path.getStartLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_path);
            if (icon != null) marker.setIcon(icon);

            marker.setOnMarkerClickListener((m, mv) -> {
                selectedPath = path;
                selectedPhoto = null;
                showPathPreview(path);
                return true;
            });
            pathMarkers.add(marker);
        }
    }

    private void showMarkers(boolean showPhotoMarkers, boolean showPathMarkers) {
        for (Marker m : photoMarkers) mapView.getOverlays().remove(m);
        for (Marker m : pathMarkers) mapView.getOverlays().remove(m);

        if (showPhotoMarkers) mapView.getOverlays().addAll(photoMarkers);
        if (showPathMarkers) mapView.getOverlays().addAll(pathMarkers);
        mapView.invalidate();
    }

    // ---------- Parcours actif ----------

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        loadDataFromFirestore();
        if (ActivePathRegistry.isActive() && navBottomSheet.getVisibility() != View.VISIBLE) {
            startActivePathNavigation();
        }
        // Toujours démarrer la localisation pour afficher la position de l'utilisateur
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        stopLocationUpdates();
    }

    private void startActivePathNavigation() {
        TravelPath path = ActivePathRegistry.getActivePath();
        if (path == null) return;

        hidePinPreview();
        navBottomSheet.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        navPathTitle.setText(path.getTitle());

        for (Marker m : activePathMarkers) mapView.getOverlays().remove(m);
        activePathMarkers.clear();
        stepMarkersByIndex.clear();
        if (activeRoutePolyline != null) {
            mapView.getOverlays().remove(activeRoutePolyline);
            activeRoutePolyline = null;
        }
        if (liveSegmentPolyline != null) {
            mapView.getOverlays().remove(liveSegmentPolyline);
            liveSegmentPolyline = null;
        }
        lastLiveRoutePos = null;

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
            stepMarkersByIndex.put(i, marker);
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
        stepMarkersByIndex.clear();
        if (activeRoutePolyline != null) {
            mapView.getOverlays().remove(activeRoutePolyline);
            activeRoutePolyline = null;
        }
        if (liveSegmentPolyline != null) {
            mapView.getOverlays().remove(liveSegmentPolyline);
            liveSegmentPolyline = null;
        }
        lastLiveRoutePos = null;
        if (userMarker != null) {
            mapView.getOverlays().remove(userMarker);
            userMarker = null;
        }
        navStepCards.removeAllViews();
        navBottomSheet.setVisibility(View.GONE);
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

    private static final String ORS_API_KEY =
            "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImQ4YWFkZjhlMWY3ZDQ2NjU4YmYxYzM1OTllM2RiN2QwIiwiaCI6Im11cm11cjY0In0=";

    private void fetchAndDrawRoute(List<PathStep> steps) {
        List<PathStep> validSteps = new ArrayList<>();
        for (PathStep s : steps) {
            if (s.getLatitude() != 0 || s.getLongitude() != 0) validSteps.add(s);
        }
        if (validSteps.size() < 2) return;

        executor.execute(() -> {
            List<GeoPoint> routePoints = new ArrayList<>();
            try {
                // Same API as PathDetailFragment: OpenRouteService foot-walking
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

                URL url = new URL(
                        "https://api.openrouteservice.org/v2/directions/foot-walking/geojson");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", ORS_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json, application/geo+json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray features = json.optJSONArray("features");
                    if (features != null && features.length() > 0) {
                        JSONArray coordinates = features.getJSONObject(0)
                                .getJSONObject("geometry").getJSONArray("coordinates");
                        for (int i = 0; i < coordinates.length(); i++) {
                            JSONArray c = coordinates.getJSONArray(i);
                            routePoints.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("MapFragment", "ORS route fetch error", e);
            }

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
                activeRoutePolyline.getOutlinePaint().setColor(Color.parseColor("#5B5CF6"));
                activeRoutePolyline.getOutlinePaint().setStrokeWidth(10f);
                activeRoutePolyline.getOutlinePaint().setAntiAlias(true);
                mapView.getOverlayManager().add(0, activeRoutePolyline);
                mapView.invalidate();
            });
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
        lastKnownUserPos = userPos;

        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            userMarker.setTitle("Vous êtes ici");
            Drawable userIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_location_marker);
            if (userIcon != null) userMarker.setIcon(userIcon);
            mapView.getOverlays().add(userMarker);
        }
        userMarker.setPosition(userPos);

        // Premier fix GPS : centrer la carte sur l'utilisateur au niveau ville
        if (firstLocationFix) {
            firstLocationFix = false;
            mapView.getController().animateTo(userPos);
            mapView.getController().setZoom(14.0);
        }

        mapView.invalidate();

        if (ActivePathRegistry.isActive()) {
            updateNavigationOverlay(userPos);
            maybeRefreshLiveRoute(userPos);
        }
    }

    /** Centre la carte sur la dernière position connue de l'utilisateur. */
    private void centerOnUser() {
        if (lastKnownUserPos != null) {
            mapView.getController().animateTo(lastKnownUserPos);
            mapView.getController().setZoom(15.0);
        } else {
            Toast.makeText(requireContext(),
                    "Position non disponible, vérifiez le GPS", Toast.LENGTH_SHORT).show();
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
            navStepCards.removeAllViews();
            return;
        }

        PathStep target = steps.get(idx);
        navStepLabel.setText("Étape " + (idx + 1) + "/" + steps.size() + " · Prochaine destination :");
        navNextStep.setText(target.getName());

        if (userPos == null) {
            navDistance.setText("En attente du GPS...");
            navTime.setText("--");
            updateNavStepCards(steps, idx);
            return;
        }

        double distMeters = distanceMeters(
                userPos.getLatitude(), userPos.getLongitude(),
                target.getLatitude(), target.getLongitude());

        if (distMeters <= STEP_REACHED_THRESHOLD_M) {
            Toast.makeText(requireContext(),
                    "Étape atteinte : " + target.getName(), Toast.LENGTH_SHORT).show();
            Marker doneMarker = stepMarkersByIndex.get(idx);
            if (doneMarker != null) {
                mapView.getOverlays().remove(doneMarker);
                activePathMarkers.remove(doneMarker);
                stepMarkersByIndex.remove(idx);
                mapView.invalidate();
            }
            lastLiveRoutePos = null;
            ActivePathRegistry.advanceStep();
            int newIdx = ActivePathRegistry.getCurrentStepIndex();
            if (activeRoutePolyline != null) {
                mapView.getOverlays().remove(activeRoutePolyline);
                activeRoutePolyline = null;
            }
            if (newIdx == idx) {
                navStepLabel.setText("Parcours terminé !");
                navNextStep.setText("Bravo 🎉");
                navDistance.setText("--");
                navTime.setText("--");
                navStepCards.removeAllViews();
                return;
            }
            fetchAndDrawRoute(steps.subList(newIdx, steps.size()));
            updateNavigationOverlay(userPos);
            return;
        }

        navDistance.setText(formatDistance(distMeters));
        navTime.setText(formatDuration(distMeters));
        updateNavStepCards(steps, idx);
    }

    private void updateNavStepCards(List<PathStep> steps, int currentIdx) {
        navStepCards.removeAllViews();
        for (int i = currentIdx + 1; i < steps.size(); i++) {
            PathStep step = steps.get(i);
            View card = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_step, navStepCards, false);
            ((TextView) card.findViewById(R.id.tv_step_number)).setText(String.valueOf(i + 1));
            ((TextView) card.findViewById(R.id.tv_step_name)).setText(step.getName());
            card.findViewById(R.id.btn_remove_step).setVisibility(View.GONE);

            String desc = step.getDescription();
            if (desc != null && !desc.isEmpty()) {
                TextView tvDesc = card.findViewById(R.id.tv_step_desc);
                tvDesc.setText(desc);
                tvDesc.setVisibility(View.VISIBLE);
            }

            String imageUrl = step.getImageUrl();
            String b64 = step.getImageBase64();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                ImageView imgView = card.findViewById(R.id.img_step_photo);
                View cardPhoto = card.findViewById(R.id.card_step_photo);
                View tvNum = card.findViewById(R.id.tv_step_number);
                String finalUrl = imageUrl;
                executor.execute(() -> {
                    try {
                        URL u = new URL(finalUrl);
                        HttpURLConnection c = (HttpURLConnection) u.openConnection();
                        c.setRequestProperty("User-Agent", "TravelingApp/1.0");
                        c.setConnectTimeout(8000);
                        c.setReadTimeout(8000);
                        if (c.getResponseCode() != 200) return;
                        android.graphics.Bitmap bmp =
                                android.graphics.BitmapFactory.decodeStream(c.getInputStream());
                        c.getInputStream().close();
                        if (bmp == null) return;
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            imgView.setImageBitmap(bmp);
                            cardPhoto.setVisibility(View.VISIBLE);
                            tvNum.setVisibility(View.GONE);
                        });
                    } catch (Exception ignored) {}
                });
            } else if (b64 != null && !b64.isEmpty()) {
                Bitmap bmp = ImageUtils.base64ToBitmap(b64);
                if (bmp != null) {
                    ((ImageView) card.findViewById(R.id.img_step_photo)).setImageBitmap(bmp);
                    card.findViewById(R.id.card_step_photo).setVisibility(View.VISIBLE);
                    card.findViewById(R.id.tv_step_number).setVisibility(View.GONE);
                }
            }

            navStepCards.addView(card);
        }
    }

    // ---------- Tracé live position → prochaine étape ----------

    private void maybeRefreshLiveRoute(GeoPoint userPos) {
        if (liveRouteFetching) return;
        TravelPath path = ActivePathRegistry.getActivePath();
        if (path == null) return;
        int idx = ActivePathRegistry.getCurrentStepIndex();
        List<PathStep> steps = path.getSteps();
        if (idx >= steps.size()) return;
        PathStep target = steps.get(idx);
        if (target.getLatitude() == 0 && target.getLongitude() == 0) return;

        if (lastLiveRoutePos != null) {
            double moved = distanceMeters(
                    lastLiveRoutePos.getLatitude(), lastLiveRoutePos.getLongitude(),
                    userPos.getLatitude(), userPos.getLongitude());
            if (moved < LIVE_ROUTE_MIN_MOVE_M) return;
        }

        lastLiveRoutePos = userPos;
        liveRouteFetching = true;
        fetchLiveSegment(userPos, target);
    }

    private void fetchLiveSegment(GeoPoint from, PathStep to) {
        executor.execute(() -> {
            try {
                String urlStr = "https://router.project-osrm.org/route/v1/foot/"
                        + from.getLongitude() + "," + from.getLatitude() + ";"
                        + to.getLongitude() + "," + to.getLatitude()
                        + "?overview=full&geometries=geojson";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                List<GeoPoint> pts = new ArrayList<>();
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
                            JSONArray coords = routes.getJSONObject(0)
                                    .getJSONObject("geometry").getJSONArray("coordinates");
                            for (int i = 0; i < coords.length(); i++) {
                                JSONArray c = coords.getJSONArray(i);
                                pts.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
                            }
                        }
                    }
                }

                if (pts.isEmpty()) {
                    pts.add(from);
                    pts.add(new GeoPoint(to.getLatitude(), to.getLongitude()));
                }

                List<GeoPoint> finalPts = pts;
                mainHandler.post(() -> {
                    liveRouteFetching = false;
                    if (!isAdded()) return;
                    if (liveSegmentPolyline != null) {
                        mapView.getOverlays().remove(liveSegmentPolyline);
                    }
                    liveSegmentPolyline = new Polyline(mapView);
                    liveSegmentPolyline.setPoints(finalPts);
                    liveSegmentPolyline.getOutlinePaint().setColor(Color.parseColor("#FF6B35"));
                    liveSegmentPolyline.getOutlinePaint().setStrokeWidth(12f);
                    liveSegmentPolyline.getOutlinePaint().setAntiAlias(true);
                    mapView.getOverlayManager().add(liveSegmentPolyline);
                    mapView.invalidate();
                });
            } catch (Exception e) {
                Log.e("MapFragment", "Live segment fetch error", e);
                mainHandler.post(() -> liveRouteFetching = false);
            }
        });
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
            startLocationUpdates();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }
}
