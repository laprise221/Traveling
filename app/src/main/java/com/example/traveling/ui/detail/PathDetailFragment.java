package com.example.traveling.ui.detail;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.traveling.data.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;

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
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private static final String OTM_API_KEY = "5ae2e3f221c38a28845f05b64ceacf3f82755ce118bd16981c7984e8";

    private MapView mapView;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<View> stepViewList = new ArrayList<>();

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

        view.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportToPdf(path));

        LinearLayout stepsContainer = view.findViewById(R.id.steps_list_container);

        List<PathStep> steps = path.getSteps();
        if (steps != null && !steps.isEmpty()) {
            stepViewList.clear();
            for (int i = 0; i < steps.size(); i++) {
                addStepToList(stepsContainer, steps.get(i), i + 1);
            }
            enrichMissingImages(steps);
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

        String desc = step.getDescription();
        if (desc != null && !desc.isEmpty()) {
            TextView tvDesc = stepView.findViewById(R.id.tv_step_desc);
            tvDesc.setText(desc);
            tvDesc.setVisibility(View.VISIBLE);
        }

        // Priority: imageUrl (API-fetched, saved as URL) > imageBase64 (manually picked)
        String imageUrl = step.getImageUrl();
        String b64 = step.getImageBase64();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            ImageView imgView = stepView.findViewById(R.id.img_step_photo);
            View cardPhoto = stepView.findViewById(R.id.card_step_photo);
            View tvNum = stepView.findViewById(R.id.tv_step_number);
            executor.execute(() -> {
                Bitmap bmp = downloadBitmap(imageUrl);
                mainHandler.post(() -> {
                    if (!isAdded() || bmp == null) return;
                    imgView.setImageBitmap(bmp);
                    cardPhoto.setVisibility(View.VISIBLE);
                    tvNum.setVisibility(View.GONE);
                });
            });
        } else if (b64 != null && !b64.isEmpty()) {
            Bitmap bmp = ImageUtils.base64ToBitmap(b64);
            if (bmp != null) {
                ((ImageView) stepView.findViewById(R.id.img_step_photo)).setImageBitmap(bmp);
                stepView.findViewById(R.id.card_step_photo).setVisibility(View.VISIBLE);
                stepView.findViewById(R.id.tv_step_number).setVisibility(View.GONE);
            }
        }

        stepView.setOnClickListener(v -> showStepDetailSheet(step, index));

        stepViewList.add(stepView);
        container.addView(stepView);
    }

    private void enrichMissingImages(List<PathStep> steps) {
        for (int i = 0; i < steps.size(); i++) {
            PathStep step = steps.get(i);
            if ((step.getImageUrl() != null && !step.getImageUrl().isEmpty())
                    || (step.getImageBase64() != null && !step.getImageBase64().isEmpty())) {
                continue; // image déjà disponible
            }
            int idx = i;
            executor.execute(() -> {
                // 1. OpenTripMap par coordonnées (meilleure couverture POI touristique)
                String url = null;
                if (step.getLatitude() != 0 || step.getLongitude() != 0) {
                    url = fetchImageUrlFromOTM(step.getLatitude(), step.getLongitude());
                }
                // 2. Wikipedia par nom (fallback)
                if (url == null) {
                    url = fetchImageUrlFromWikipedia(step.getName());
                }
                if (url == null) return;

                step.setImageUrl(url); // mémorisé pour le bottom sheet
                Bitmap bmp = downloadBitmap(url);
                if (bmp == null) return;

                mainHandler.post(() -> {
                    if (!isAdded() || idx >= stepViewList.size()) return;
                    View sv = stepViewList.get(idx);
                    ((ImageView) sv.findViewById(R.id.img_step_photo)).setImageBitmap(bmp);
                    sv.findViewById(R.id.card_step_photo).setVisibility(View.VISIBLE);
                    sv.findViewById(R.id.tv_step_number).setVisibility(View.GONE);
                });
            });
        }
    }

    private String fetchImageUrlFromOTM(double lat, double lon) {
        try {
            URL url = new URL("https://api.opentripmap.com/0.1/en/places/radius"
                    + "?radius=80&lon=" + lon + "&lat=" + lat
                    + "&format=json&limit=5&apikey=" + OTM_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            if (conn.getResponseCode() != 200) return null;

            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) sb.append(l);
            r.close();

            JSONArray features = new JSONArray(sb.toString());
            for (int i = 0; i < features.length(); i++) {
                JSONObject props = features.getJSONObject(i).optJSONObject("properties");
                if (props == null) continue;
                String xid = props.optString("xid", "");
                if (xid.isEmpty()) continue;

                URL xidUrl = new URL("https://api.opentripmap.com/0.1/en/places/xid/"
                        + xid + "?apikey=" + OTM_API_KEY);
                HttpURLConnection xidConn = (HttpURLConnection) xidUrl.openConnection();
                xidConn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                xidConn.setConnectTimeout(6000);
                xidConn.setReadTimeout(6000);
                if (xidConn.getResponseCode() != 200) continue;

                BufferedReader xr = new BufferedReader(
                        new InputStreamReader(xidConn.getInputStream()));
                StringBuilder xsb = new StringBuilder();
                String xl;
                while ((xl = xr.readLine()) != null) xsb.append(xl);
                xr.close();

                JSONObject detail = new JSONObject(xsb.toString());
                String imgUrl = null;
                if (detail.has("preview")) {
                    imgUrl = detail.getJSONObject("preview").optString("source", null);
                }
                if (imgUrl == null || imgUrl.isEmpty()) {
                    String img = detail.optString("image", "");
                    if (img.startsWith("http")) imgUrl = img;
                }
                if (imgUrl != null && !imgUrl.isEmpty()) return imgUrl;
            }
        } catch (Exception e) {
            Log.d("PathDetail", "OTM image fetch failed: " + e.getMessage());
        }
        return null;
    }

    private String fetchImageUrlFromWikipedia(String name) {
        try {
            String encoded = java.net.URLEncoder.encode(name, "UTF-8");
            for (String lang : new String[]{"fr", "en"}) {
                URL searchUrl = new URL("https://" + lang
                        + ".wikipedia.org/w/api.php?action=query"
                        + "&generator=search&gsrsearch=" + encoded
                        + "&gsrlimit=1&prop=pageimages&piprop=thumbnail"
                        + "&pithumbsize=600&format=json");
                HttpURLConnection conn = (HttpURLConnection) searchUrl.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                if (conn.getResponseCode() != 200) continue;

                BufferedReader r = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                r.close();

                JSONObject data = new JSONObject(sb.toString());
                JSONObject query = data.optJSONObject("query");
                if (query == null) continue;
                JSONObject pages = query.optJSONObject("pages");
                if (pages == null || pages.length() == 0) continue;

                JSONObject page = pages.getJSONObject(pages.keys().next());
                JSONObject thumbnail = page.optJSONObject("thumbnail");
                if (thumbnail == null) continue;
                String imgUrl = thumbnail.optString("source", null);
                if (imgUrl != null && !imgUrl.isEmpty()) return imgUrl;
            }
        } catch (Exception e) {
            Log.d("PathDetail", "Wikipedia image fetch failed: " + e.getMessage());
        }
        return null;
    }

    private void showStepDetailSheet(PathStep step, int index) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_step_detail, null);
        dialog.setContentView(sheet);

        ((TextView) sheet.findViewById(R.id.sheet_step_number)).setText(String.valueOf(index));
        ((TextView) sheet.findViewById(R.id.sheet_step_name)).setText(step.getName());

        // --- Prix et description ---
        String rawDesc = step.getDescription() != null ? step.getDescription() : "";
        String price = "—";
        String cleanDesc = rawDesc;

        if (rawDesc.startsWith("Entrée") || rawDesc.startsWith("entrée")) {
            int nl = rawDesc.indexOf('\n');
            if (nl > 0) {
                price = rawDesc.substring(0, nl).trim();
                cleanDesc = rawDesc.substring(nl + 1).trim();
            } else {
                price = rawDesc.trim();
                cleanDesc = "";
            }
        }

        ((TextView) sheet.findViewById(R.id.sheet_price_value)).setText(price);

        TextView tvDesc = sheet.findViewById(R.id.sheet_step_desc);
        TextView tvNoDesc = sheet.findViewById(R.id.sheet_no_desc);
        if (!cleanDesc.isEmpty()) {
            tvDesc.setText(cleanDesc);
            tvDesc.setVisibility(View.VISIBLE);
        } else {
            tvNoDesc.setVisibility(View.VISIBLE);
        }

        // --- Image ---
        ImageView imgView = sheet.findViewById(R.id.sheet_img);
        String imageUrl = step.getImageUrl();
        String b64 = step.getImageBase64();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // URL déjà disponible (enrichissement effectué avant publication)
            executor.execute(() -> {
                Bitmap bmp = downloadBitmap(imageUrl);
                mainHandler.post(() -> {
                    if (!isAdded() || bmp == null) return;
                    imgView.setImageBitmap(bmp);
                });
            });
        } else if (b64 != null && !b64.isEmpty()) {
            // Photo choisie manuellement
            Bitmap bmp = ImageUtils.base64ToBitmap(b64);
            if (bmp != null) imgView.setImageBitmap(bmp);
        } else {
            // Pas encore enrichi : chercher OTM puis Wikipedia à la demande
            executor.execute(() -> {
                String url = null;
                if (step.getLatitude() != 0 || step.getLongitude() != 0) {
                    url = fetchImageUrlFromOTM(step.getLatitude(), step.getLongitude());
                }
                if (url == null) url = fetchImageUrlFromWikipedia(step.getName());
                if (url != null) step.setImageUrl(url);
                Bitmap bmp = url != null ? downloadBitmap(url) : null;
                mainHandler.post(() -> {
                    if (!isAdded() || bmp == null) return;
                    imgView.setImageBitmap(bmp);
                });
            });
        }

        // --- Horaires via Overpass ---
        TextView tvHours = sheet.findViewById(R.id.sheet_hours_value);
        if (step.getLatitude() != 0 || step.getLongitude() != 0) {
            executor.execute(() -> {
                String hours = fetchOpeningHours(step.getLatitude(), step.getLongitude());
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    tvHours.setText(hours != null ? hours : "Non renseigné");
                });
            });
        } else {
            tvHours.setText("Non renseigné");
        }

        // --- Bouton Maps ---
        sheet.findViewById(R.id.sheet_btn_maps).setOnClickListener(v -> {
            Uri uri = Uri.parse("geo:" + step.getLatitude() + "," + step.getLongitude()
                    + "?q=" + Uri.encode(step.getName()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(requireContext().getPackageManager()) == null) {
                intent.setPackage(null);
            }
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Aucune application de carte disponible",
                        Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String fetchOpeningHours(double lat, double lon) {
        try {
            String query = "[out:json][timeout:8];"
                    + "(node(around:100," + lat + "," + lon + ")[\"opening_hours\"];"
                    + "way(around:100," + lat + "," + lon + ")[\"opening_hours\"];);"
                    + "out tags;";
            URL url = new URL("https://overpass-api.de/api/interpreter?data="
                    + java.net.URLEncoder.encode(query, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) return null;

            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) sb.append(l);
            r.close();

            JSONArray elements = new JSONObject(sb.toString()).optJSONArray("elements");
            if (elements == null || elements.length() == 0) return null;

            for (int i = 0; i < elements.length(); i++) {
                JSONObject tags = elements.getJSONObject(i).optJSONObject("tags");
                if (tags == null) continue;
                String oh = tags.optString("opening_hours", "");
                if (!oh.isEmpty()) return oh;
            }
        } catch (Exception e) {
            Log.d("PathDetail", "Opening hours fetch failed: " + e.getMessage());
        }
        return null;
    }

    private Bitmap downloadBitmap(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) return null;
            java.io.InputStream is = conn.getInputStream();
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
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

                // Cache the route so MapFragment can reuse it without a second ORS request
                List<double[]> routeCache = new ArrayList<>();
                for (GeoPoint p : routePoints) {
                    routeCache.add(new double[]{p.getLatitude(), p.getLongitude()});
                }
                com.example.traveling.data.ActivePathRegistry.cacheRoute(routeCache);

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

    private void exportToPdf(TravelPath path) {
        Toast.makeText(requireContext(), "Génération du PDF…", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                final int W = 595, H = 842;
                final float M = 40f;
                final float CW = W - 2 * M;

                PdfDocument doc = new PdfDocument();
                int pageNum = 1;
                PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNum).create());
                Canvas cv = page.getCanvas();
                float y = M;

                // --- Paints ---
                Paint appLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                appLabelPaint.setTextSize(9f);
                appLabelPaint.setColor(0xFF9CA3AF);
                appLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                appLabelPaint.setLetterSpacing(0.1f);

                Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                titlePaint.setTextSize(22f);
                titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                titlePaint.setColor(0xFF5B5CF6);

                Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                subtitlePaint.setTextSize(12f);
                subtitlePaint.setColor(0xFF6B7280);

                Paint dividerPaint = new Paint();
                dividerPaint.setColor(0xFFE5E7EB);
                dividerPaint.setStrokeWidth(1f);

                Paint tagBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                tagBgPaint.setColor(0xFFEEF2FF);

                Paint tagTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                tagTextPaint.setTextSize(10f);
                tagTextPaint.setColor(0xFF5B5CF6);
                tagTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                sectionPaint.setTextSize(14f);
                sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                sectionPaint.setColor(0xFF1E1B4B);

                TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                bodyPaint.setTextSize(11f);
                bodyPaint.setColor(0xFF374151);

                Paint stepBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                stepBgPaint.setColor(0xFF5B5CF6);

                Paint stepNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                stepNumPaint.setTextSize(10f);
                stepNumPaint.setColor(0xFFFFFFFF);
                stepNumPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                stepNumPaint.setTextAlign(Paint.Align.CENTER);

                Paint stepNamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                stepNamePaint.setTextSize(12f);
                stepNamePaint.setColor(0xFF1E1B4B);

                TextPaint stepDescPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                stepDescPaint.setTextSize(10f);
                stepDescPaint.setColor(0xFF6B7280);

                Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                footerPaint.setTextSize(8f);
                footerPaint.setColor(0xFF9CA3AF);
                footerPaint.setTextAlign(Paint.Align.CENTER);

                // --- Contenu ---

                // App label
                cv.drawText("TRAVELING", M, y + 9f, appLabelPaint);
                y += 18f;

                // Titre
                cv.drawText(path.getTitle(), M, y + 22f, titlePaint);
                y += 32f;

                // Sous-titre : ville + auteur
                String subtitle = path.getCity() != null ? path.getCity() : "";
                if (path.getAuthorName() != null && !path.getAuthorName().isEmpty()) {
                    subtitle += "  ·  Par " + path.getAuthorName();
                }
                cv.drawText(subtitle, M, y + 12f, subtitlePaint);
                y += 22f;

                // Divider
                cv.drawLine(M, y, W - M, y, dividerPaint);
                y += 12f;

                // Tags (duration, budget, difficulty)
                String[] tags = {path.getDuration(), path.getBudget(), path.getDifficulty()};
                float tagX = M;
                float tagH = 20f;
                for (String tag : tags) {
                    if (tag == null || tag.isEmpty()) continue;
                    float tagW = tagTextPaint.measureText(tag) + 14f;
                    cv.drawRoundRect(new RectF(tagX, y, tagX + tagW, y + tagH), 5f, 5f, tagBgPaint);
                    cv.drawText(tag, tagX + 7f, y + 14f, tagTextPaint);
                    tagX += tagW + 6f;
                }
                y += tagH + 16f;

                // Divider
                cv.drawLine(M, y, W - M, y, dividerPaint);
                y += 12f;

                // Description
                String desc = path.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    cv.drawText("Description", M, y + 14f, sectionPaint);
                    y += 22f;

                    StaticLayout descLayout = StaticLayout.Builder
                            .obtain(desc, 0, desc.length(), bodyPaint, (int) CW)
                            .setLineSpacing(2f, 1f)
                            .build();

                    if (y + descLayout.getHeight() > H - M - 20) {
                        doc.finishPage(page);
                        page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, ++pageNum).create());
                        cv = page.getCanvas();
                        y = M;
                    }
                    cv.save();
                    cv.translate(M, y);
                    descLayout.draw(cv);
                    cv.restore();
                    y += descLayout.getHeight() + 14f;

                    cv.drawLine(M, y, W - M, y, dividerPaint);
                    y += 12f;
                }

                // Étapes
                List<PathStep> steps = path.getSteps();
                if (steps != null && !steps.isEmpty()) {
                    cv.drawText("Étapes  (" + steps.size() + ")", M, y + 14f, sectionPaint);
                    y += 24f;

                    float circleR = 12f;
                    float nameX = M + circleR * 2 + 10f;

                    for (int i = 0; i < steps.size(); i++) {
                        PathStep step = steps.get(i);
                        float rowH = 36f;

                        String stepDesc2 = step.getDescription();
                        StaticLayout sdLayout = null;
                        if (stepDesc2 != null && !stepDesc2.isEmpty()) {
                            sdLayout = StaticLayout.Builder
                                    .obtain(stepDesc2, 0, stepDesc2.length(), stepDescPaint,
                                            (int) (CW - circleR * 2 - 10f))
                                    .setLineSpacing(1f, 1f)
                                    .build();
                            rowH += sdLayout.getHeight();
                        }

                        if (y + rowH > H - M - 20) {
                            doc.finishPage(page);
                            page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, ++pageNum).create());
                            cv = page.getCanvas();
                            y = M;
                        }

                        float circleY = y + circleR;
                        cv.drawCircle(M + circleR, circleY, circleR, stepBgPaint);
                        cv.drawText(String.valueOf(i + 1), M + circleR, circleY + 4f, stepNumPaint);

                        String name = step.getName() != null ? step.getName() : "";
                        cv.drawText(name, nameX, y + 16f, stepNamePaint);

                        if (sdLayout != null) {
                            cv.save();
                            cv.translate(nameX, y + 22f);
                            sdLayout.draw(cv);
                            cv.restore();
                        }
                        y += rowH;
                    }
                }

                // Footer
                cv.drawLine(M, H - 28f, W - M, H - 28f, dividerPaint);
                cv.drawText("Exporté depuis Traveling", W / 2f, H - 16f, footerPaint);

                doc.finishPage(page);

                // Écriture du fichier
                String safeName = path.getTitle().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                File dir = requireContext().getExternalFilesDir(null);
                if (dir == null) dir = requireContext().getCacheDir();
                File pdfFile = new File(dir, "parcours_" + safeName + ".pdf");
                FileOutputStream fos = new FileOutputStream(pdfFile);
                doc.writeTo(fos);
                fos.close();
                doc.close();

                File finalFile = pdfFile;
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Uri uri = FileProvider.getUriForFile(
                            requireContext(), "com.example.traveling.fileprovider", finalFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("application/pdf");
                        share.putExtra(Intent.EXTRA_STREAM, uri);
                        share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(share, "Partager le PDF"));
                    }
                });

            } catch (Exception e) {
                Log.e("PathDetail", "PDF export error", e);
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Erreur lors de l'export PDF", Toast.LENGTH_SHORT).show();
                });
            }
        });
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
