package com.example.traveling.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.traveling.R;
import com.example.traveling.data.SampleData;
import com.example.traveling.model.Photo;
import com.example.traveling.model.TravelPath;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private static final int REQUEST_LOCATION_PERMISSION = 100;

    private MapView mapView;
    private RadioGroup filterGroup;

    private List<Photo> photos;
    private List<TravelPath> paths;

    private List<Marker> photoMarkers = new ArrayList<>();
    private List<Marker> pathMarkers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Configurer osmdroid avec cache et user agent
        Context ctx = requireContext().getApplicationContext();
        IConfigurationProvider config = Configuration.getInstance();
        config.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        config.setUserAgentValue(ctx.getPackageName());

        // Définir le répertoire de cache des tuiles
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

        // Configurer la carte
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(5.0);
        mapView.getController().setCenter(new GeoPoint(46.0, 2.0)); // Centre sur la France

        // Charger les données
        photos = SampleData.getSamplePhotos();
        paths = SampleData.getSamplePaths();

        // Créer les marqueurs
        createPhotoMarkers();
        createPathMarkers();

        // Filtre
        filterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_all) {
                showMarkers(true, true);
            } else if (checkedId == R.id.filter_photos) {
                showMarkers(true, false);
            } else if (checkedId == R.id.filter_paths) {
                showMarkers(false, true);
            }
        });

        // Par défaut : tout afficher
        showMarkers(true, true);

        // Demander la permission de localisation
        requestLocationPermission();
    }

    private void createPhotoMarkers() {
        for (Photo photo : photos) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(photo.getLatitude(), photo.getLongitude()));
            marker.setTitle(photo.getTitle());
            marker.setSnippet(photo.getLocationName() + "\n" + photo.getAuthor());
            marker.setSubDescription("Likes: " + photo.getLikeCount());
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            // Icône bleue pour les photos
            Drawable icon = ContextCompat.getDrawable(requireContext(),
                    R.drawable.ic_marker_photo);
            if (icon != null) {
                marker.setIcon(icon);
            }

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

            // Icône verte pour les parcours
            Drawable icon = ContextCompat.getDrawable(requireContext(),
                    R.drawable.ic_marker_path);
            if (icon != null) {
                marker.setIcon(icon);
            }

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
        mapView.getOverlays().clear();

        if (showPhotos) {
            mapView.getOverlays().addAll(photoMarkers);
        }
        if (showPaths) {
            mapView.getOverlays().addAll(pathMarkers);
        }

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
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
