package com.example.traveling.model;

import android.graphics.Bitmap;
import android.net.Uri;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;

public class Photo {

    private String id;
    private String title;
    private String description;
    private String authorId;
    private String authorName;
    private String locationName;
    private double latitude;
    private double longitude;
    private String date;
    private String locationType;
    private int likeCount;
    private int commentCount;
    private int favoriteCount;
    private boolean isPublic;
    private String groupId;
    private String imageBase64;
    @ServerTimestamp
    private Timestamp createdAt;

    // Local-only fields (not stored in Firestore)
    private boolean liked;
    private boolean favorited;
    private int imageResId;
    private Uri imageUri;
    private Bitmap imageBitmap;
    private final List<Comment> comments = new ArrayList<>();

    /** No-arg constructor required by Firestore */
    public Photo() {}

    /** Constructor for SampleData / backward compat */
    public Photo(String id, String title, String description, String authorName,
                 String locationName, double latitude, double longitude,
                 String date, String locationType, int likeCount, int imageResId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.authorName = authorName;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.locationType = locationType;
        this.likeCount = likeCount;
        this.liked = false;
        this.imageResId = imageResId;
    }

    // ---- Firestore-serialized getters ----

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getLocationName() { return locationName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDate() { return date; }
    public String getLocationType() { return locationType; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public int getFavoriteCount() { return favoriteCount; }
    public boolean getIsPublic() { return isPublic; }
    public String getGroupId() { return groupId; }
    public String getImageBase64() { return imageBase64; }
    public Timestamp getCreatedAt() { return createdAt; }

    // ---- Excluded from Firestore (local-only) ----

    @Exclude public String getId() { return id; }
    @Exclude public boolean isLiked() { return liked; }
    @Exclude public int getImageResId() { return imageResId; }
    @Exclude public Uri getImageUri() { return imageUri; }
    @Exclude public Bitmap getImageBitmap() { return imageBitmap; }
    @Exclude public List<Comment> getComments() { return comments; }

    /** Backward-compat alias for authorName */
    @Exclude
    public String getAuthor() { return authorName; }

    // ---- Setters ----

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setDate(String date) { this.date = date; }
    public void setLocationType(String locationType) { this.locationType = locationType; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public void setImageUri(Uri uri) { this.imageUri = uri; }
    public void setImageBitmap(Bitmap bitmap) { this.imageBitmap = bitmap; }
    public void addComment(Comment c) { comments.add(c); }

    /** Toggle like locally (adjusts likeCount optimistically) */
    public void setLiked(boolean liked) {
        this.liked = liked;
        this.likeCount += liked ? 1 : -1;
    }

    /** Set liked flag without changing likeCount (for Firestore-loaded data) */
    public void setLikedSilent(boolean liked) {
        this.liked = liked;
    }

    @Exclude public boolean isFavorited() { return favorited; }
    public void setFavoritedSilent(boolean favorited) { this.favorited = favorited; }
    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
        this.favoriteCount += favorited ? 1 : -1;
    }
}
