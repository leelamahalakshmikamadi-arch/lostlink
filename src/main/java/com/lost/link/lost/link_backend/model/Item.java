package com.lost.link.lost.link_backend.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lost_items")
public class Item {

    @Id
    private String id;

    private Map<String, Object> data = new LinkedHashMap<>();

    private String itemType = "lost";

    private String imageFileId;

    private String imageFilename;

    private String imageContentType;

    private List<Double> imageFeatures;

    public Item() {
    }

    public Item(Map<String, Object> data) {
        this.data = new LinkedHashMap<>(data);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = new LinkedHashMap<>(data);
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getImageFileId() {
        return imageFileId;
    }

    public void setImageFileId(String imageFileId) {
        this.imageFileId = imageFileId;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public List<Double> getImageFeatures() {
        return imageFeatures;
    }

    public void setImageFeatures(List<Double> imageFeatures) {
        this.imageFeatures = imageFeatures;
    }

    public Map<String, Object> toResponseMap() {
        Map<String, Object> response = new LinkedHashMap<>(data);
        response.put("id", id);
        response.put("itemType", itemType);
        if (imageFileId != null) {
            response.put("imageUrl", "/api/items/" + id + "/image");
        }
        return response;
    }
}