package com.example.traveling.model;

public class Comment {
    private final String id;
    private final String author;
    private final String text;
    private final String date;

    public Comment(String id, String author, String text, String date) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.date = date;
    }

    public String getId() { return id; }
    public String getAuthor() { return author; }
    public String getText() { return text; }
    public String getDate() { return date; }
}
