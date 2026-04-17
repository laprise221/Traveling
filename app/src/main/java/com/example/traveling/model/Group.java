package com.example.traveling.model;

public class Group {
    private String id;
    private String name;
    private String lastMessage;
    private String lastMessageTime;
    private int membersCount;
    private int unreadCount;
    private int imageResId;

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
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageTime() { return lastMessageTime; }
    public int getMembersCount() { return membersCount; }
    public int getUnreadCount() { return unreadCount; }
    public int getImageResId() { return imageResId; }
}
