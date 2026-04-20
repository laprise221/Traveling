package com.example.traveling.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Group {
    private String id;
    private String name;
    private String description;
    private String theme;
    private boolean isPublic;
    private String createdBy;
    private String createdByName;
    private int membersCount;
    private List<String> memberIds = new ArrayList<>();
    @ServerTimestamp
    private Date createdAt;

    // Local-only fields
    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;
    private int imageResId;

    public Group() {}

    public Group(String id, String name, String lastMessage, String lastMessageTime,
                 int membersCount, int unreadCount, int imageResId) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.membersCount = membersCount;
        this.unreadCount = unreadCount;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTheme() { return theme; }
    public boolean isPublic() { return isPublic; }
    public boolean getIsPublic() { return isPublic; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    public int getMembersCount() { return membersCount; }
    public List<String> getMemberIds() { return memberIds != null ? memberIds : new ArrayList<>(); }
    public Date getCreatedAt() { return createdAt; }

    @Exclude public String getLastMessage() { return lastMessage; }
    @Exclude public String getLastMessageTime() { return lastMessageTime; }
    @Exclude public int getUnreadCount() { return unreadCount; }
    @Exclude public int getImageResId() { return imageResId; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setTheme(String theme) { this.theme = theme; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public void setMembersCount(int membersCount) { this.membersCount = membersCount; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
}
