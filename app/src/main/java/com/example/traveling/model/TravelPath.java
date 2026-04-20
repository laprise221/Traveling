package com.example.traveling.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class TravelPath {

    private String id;
    private String title;
    private String city;
    private String description;
    private String authorName;
    private String authorId;
    private double startLatitude;
    private double startLongitude;
    private String duration;
    private String budget;
    private String difficulty;
    private String type;
    private int stepsCount;
    private int likeCount;
    private boolean liked;
    private boolean favorited;
    private boolean isPublic;
    private List<PathStep> steps;
    private String imageBase64;
    private int imageResId;
    @ServerTimestamp
    private Date createdAt;

    public TravelPath() {}

    public TravelPath(String id, String title, String city, String description,
                      String author, double startLatitude, double startLongitude,
                      String duration, String budget, String difficulty,
                      String type, int stepsCount, int likeCount, int imageResId) {
        this.id = id;
        this.title = title;
        this.city = city;
        this.description = description;
        this.authorName = author;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.duration = duration;
        this.budget = budget;
        this.difficulty = difficulty;
        this.type = type;
        this.stepsCount = stepsCount;
        this.likeCount = likeCount;
        this.liked = false;
        this.isPublic = true;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCity() { return city; }
    public String getDescription() { return description; }
    public String getAuthorName() { return authorName; }
    public String getAuthorId() { return authorId; }
    public double getStartLatitude() { return startLatitude; }
    public double getStartLongitude() { return startLongitude; }
    public String getDuration() { return duration; }
    public String getBudget() { return budget; }
    public String getDifficulty() { return difficulty; }
    public String getType() { return type; }
    public int getStepsCount() { return stepsCount; }
    public int getLikeCount() { return likeCount; }
    public boolean getIsPublic() { return isPublic; }
    public String getImageBase64() { return imageBase64; }
    @Exclude
    public boolean isLiked() { return liked; }
    public boolean isPublic() { return isPublic; }
    public List<PathStep> getSteps() { return steps; }
    @Exclude
    public int getImageResId() { return imageResId; }
    public Date getCreatedAt() { return createdAt; }

    @Exclude
    public String getAuthor() { return authorName; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCity(String city) { this.city = city; }
    public void setDescription(String description) { this.description = description; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setAuthor(String author) { this.authorName = author; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setStartLatitude(double startLatitude) { this.startLatitude = startLatitude; }
    public void setStartLongitude(double startLongitude) { this.startLongitude = startLongitude; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setBudget(String budget) { this.budget = budget; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setType(String type) { this.type = type; }
    public void setStepsCount(int stepsCount) { this.stepsCount = stepsCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setLiked(boolean liked) {
        this.liked = liked;
        this.likeCount += liked ? 1 : -1;
    }
    public void setLikedSilent(boolean liked) { this.liked = liked; }
    @Exclude public boolean isFavorited() { return favorited; }
    public void setFavoritedSilent(boolean favorited) { this.favorited = favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setSteps(List<PathStep> steps) { this.steps = steps; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
