package com.example.traveling.model;

public class PathStep {

    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private String duration;
    private String timeSlot; // matin, après-midi, soir

    public PathStep(String name, String description, double latitude, double longitude,
                    String duration, String timeSlot) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.duration = duration;
        this.timeSlot = timeSlot;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDuration() { return duration; }
    public String getTimeSlot() { return timeSlot; }
}
