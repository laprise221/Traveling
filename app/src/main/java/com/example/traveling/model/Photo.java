package com.example.traveling.model;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class Photo {

    private String id;
    private String title;
    private String description;
    private String author;
    private String locationName;
    private double latitude;
    private double longitude;
    private String date;
    private String locationType;
    private int likeCount;
    private boolean liked;
    private int imageResId;
    private Uri imageUri;
    private Bitmap imageBitmap;
    private final List<Comment> comments = new ArrayList<>(); // pour les photos prises avec l'appareil photo

    public Photo(String id, String title, String description, String author,
                 String locationName, double latitude, double longitude,
                 String date, String locationType, int likeCount, int imageResId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.locationType = locationType;
        this.likeCount = likeCount;
        this.liked = false;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getLocationName() { return locationName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDate() { return date; }
    public String getLocationType() { return locationType; }
    public int getLikeCount() { return likeCount; }
    public boolean isLiked() { return liked; }
    public int getImageResId() { return imageResId; }

    public Uri getImageUri() { return imageUri; }
    public void setImageUri(Uri uri) { this.imageUri = uri; }
    public Bitmap getImageBitmap() { return imageBitmap; }
    public void setImageBitmap(Bitmap bitmap) { this.imageBitmap = bitmap; }

    public List<Comment> getComments() { return comments; }
    public void addComment(Comment c) { comments.add(c); }

    public void setLiked(boolean liked) {
        this.liked = liked;
        this.likeCount += liked ? 1 : -1;
    }
}
