package com.lost.link.lost.link_backend.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String notificationId;

    private String userId;

    private String title;

    private String message;

    private String type = "SYSTEM"; // MATCH, CLAIM, SYSTEM

    private Double matchPercentage;

    private String itemId;

    private String relatedItemId;

    private boolean read = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {
    }

    public Notification(String title, String message, String type, String itemId) {
        this(title, message, type, itemId, null, null);
    }

    public Notification(String title, String message, String type, String itemId, String userId, Double matchPercentage) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.itemId = itemId;
        this.relatedItemId = itemId;
        this.userId = userId;
        this.matchPercentage = matchPercentage;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id != null ? id : notificationId;
    }

    public void setId(String id) {
        this.id = id;
        if (this.notificationId == null) {
            this.notificationId = id;
        }
    }

    public String getNotificationId() {
        return notificationId != null ? notificationId : id;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
        if (this.id == null) {
            this.id = notificationId;
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getMatchPercentage() {
        return matchPercentage;
    }

    public void setMatchPercentage(Double matchPercentage) {
        this.matchPercentage = matchPercentage;
    }

    public String getRelatedItemId() {
        return relatedItemId != null ? relatedItemId : itemId;
    }

    public void setRelatedItemId(String relatedItemId) {
        this.relatedItemId = relatedItemId;
        if (this.itemId == null) {
            this.itemId = relatedItemId;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getItemId() {
        return itemId != null ? itemId : relatedItemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
        if (this.relatedItemId == null) {
            this.relatedItemId = itemId;
        }
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

