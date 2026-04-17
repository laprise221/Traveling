package com.example.traveling.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.List;

public class TravelPath {

    private String id;
    private String title;
    private String city;
    private String description;
    private String authorId;
    private String authorName;
    private String duration;
    private String budget;
    private String difficulty;
    private String type;
    private int likeCount;
    private int commentCount;
    private boolean isPublic;
    private String imageBase64;
    private List<PathStep> steps;
    @ServerTimestamp
    private Timestamp createdAt;

    // Local-only fields
    private boolean liked;
    private double startLatitude;
    private double startLongitude;
    private int stepsCount;
    private int imageResId;

    /** No-arg constructor required by Firestore */
    public TravelPath() {}

    /** Constructor for SampleData / backward compat */
    public TravelPath(String id, String title, String city, String description,
                      String authorName, double startLatitude, double startLongitude,
                      String duration, String budget, String difficulty,
                      String type, int stepsCount, int likeCount, int imageResId) {
        this.id = id;
        this.title = title;
        this.city = city;
        this.description = description;
        this.authorName = authorName;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.duration = duration;
        this.budget = budget;
        this.difficulty = difficulty;
        this.type = type;
        this.stepsCount = stepsCount;
        this.likeCount = likeCount;
        this.liked = false;
        this.imageResId = imageResId;
    }

    // ---- Firestore-serialized getters ----

    public String getTitle() { return title; }
    public String getCity() { return city; }
    public String getDescription() { return description; }
    public String getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getDuration() { return duration; }
    public String getBudget() { return budget; }
    public String getDifficulty() { return difficulty; }
    public String getType() { return type; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean getIsPublic() { return isPublic; }
    public String getImageBase64() { return imageBase64; }
    public List<PathStep> getSteps() { return steps; }
    public Timestamp getCreatedAt() { return createdAt; }

    // ---- Excluded from Firestore (local-only) ----

    @Exclude public String getId() { return id; }
    @Exclude public boolean isLiked() { return liked; }
    @Exclude public double getStartLatitude() { return startLatitude; }
    @Exclude public double getStartLongitude() { return startLongitude; }
    @Exclude public int getStepsCount() { return stepsCount; }
    @Exclude public int getImageResId() { return imageResId; }

    /** Backward-compat alias for authorName */
    @Exclude
    public String getAuthor() { return authorName; }

    // ---- Setters ----

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCity(String city) { this.city = city; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setBudget(String budget) { this.budget = budget; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setType(String type) { this.type = type; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public void setSteps(List<PathStep> steps) {
        this.steps = steps;
        this.stepsCount = steps != null ? steps.size() : 0;
    }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /** Toggle like locally (adjusts likeCount optimistically) */
    public void setLiked(boolean liked) {
        this.liked = liked;
        this.likeCount += liked ? 1 : -1;
    }

    /** Set liked flag without changing likeCount (for Firestore-loaded data) */
    public void setLikedSilent(boolean liked) {
        this.liked = liked;
    }
}
