package com.example.traveling.model;

public class Comment {
    private String id;
    private String authorId;
    private String author;
    private String text;
    private String date;

    /** No-arg constructor required by Firestore */
    public Comment() {}

    public Comment(String id, String author, String text, String date) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.date = date;
    }

    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthor() { return author; }
    public String getText() { return text; }
    public String getDate() { return date; }

    public void setId(String id) { this.id = id; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setAuthor(String author) { this.author = author; }
    public void setText(String text) { this.text = text; }
    public void setDate(String date) { this.date = date; }
}
