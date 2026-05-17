package com.example.traveling.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class AppNotification {

    private String id;
    private String type;        // "like" | "comment" | "favorite" | "group_post"
    private String senderId;
    private String senderName;
    private String contentId;   // photoId, pathId, or groupId
    private String contentType; // "photo" | "path" | "group"
    private String contentTitle;
    private String message;
    private boolean read;
    @ServerTimestamp
    private Date createdAt;

    public AppNotification() {}

    @Exclude public String getId() { return id; }
    public String getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContentId() { return contentId; }
    public String getContentType() { return contentType; }
    public String getContentTitle() { return contentTitle; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public Date getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setContentTitle(String contentTitle) { this.contentTitle = contentTitle; }
    public void setMessage(String message) { this.message = message; }
    public void setRead(boolean read) { this.read = read; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    /** Human-readable summary for display in the notification list. */
    @Exclude
    public String buildText() {
        String name = senderName != null ? senderName : "Quelqu'un";
        String title = contentTitle != null && !contentTitle.isEmpty() ? " \"" + contentTitle + "\"" : "";
        switch (type != null ? type : "") {
            case "like":     return name + " a aimé votre publication" + title;
            case "comment":  return name + " a commenté" + title + (message != null && !message.isEmpty() ? " : " + message : "");
            case "favorite": return name + " a mis en favori votre publication" + title;
            case "group_post": return name + " a partagé un message dans " + title;
            case "tag_follow": return "Nouvelle publication dans la catégorie \"" + (message != null && !message.isEmpty() ? message : contentTitle) + "\"";
            default:         return name + " a interagi avec votre contenu";
        }
    }
}
