package com.example.traveling.model;

import java.util.List;

public class TravelPath {

    private String id;
    private String title;
    private String city;
    private String description;
    private String author;
    private double startLatitude;
    private double startLongitude;
    private String duration;       // ex: "3h", "1 journée"
    private String budget;         // ex: "€", "€€", "€€€"
    private String difficulty;     // ex: "Facile", "Modéré", "Difficile"
    private String type;           // économique, équilibré, confort
    private int stepsCount;
    private int likeCount;
    private boolean liked;
    private List<PathStep> steps;
    private int imageResId;

    public TravelPath(String id, String title, String city, String description,
                      String author, double startLatitude, double startLongitude,
                      String duration, String budget, String difficulty,
                      String type, int stepsCount, int likeCount, int imageResId) {
        this.id = id;
        this.title = title;
        this.city = city;
        this.description = description;
        this.author = author;
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

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCity() { return city; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public double getStartLatitude() { return startLatitude; }
    public double getStartLongitude() { return startLongitude; }
    public String getDuration() { return duration; }
    public String getBudget() { return budget; }
    public String getDifficulty() { return difficulty; }
    public String getType() { return type; }
    public int getStepsCount() { return stepsCount; }
    public int getLikeCount() { return likeCount; }
    public boolean isLiked() { return liked; }
    public List<PathStep> getSteps() { return steps; }
    public int getImageResId() { return imageResId; }

    public void setSteps(List<PathStep> steps) { this.steps = steps; }
    public void setLiked(boolean liked) {
        this.liked = liked;
        this.likeCount += liked ? 1 : -1;
    }
}
