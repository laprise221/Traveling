package com.example.traveling.model;

public class PathStep {

    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private String duration;
    private String timeSlot;
    private String imageBase64;
    private String imageUrl;
    private String xid;

    public PathStep() {}

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
    public String getImageBase64() { return imageBase64; }
    public String getImageUrl() { return imageUrl; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getXid() { return xid; }
    public void setXid(String xid) { this.xid = xid; }
}
