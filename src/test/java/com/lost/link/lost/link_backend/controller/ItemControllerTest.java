package com.lost.link.lost.link_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.service.ImageFeatureService;

class ItemControllerTest {

    @Test
    void itemResponsePreservesFrontendFieldsAndImageUrl() {
        Item item = new Item(Map.of("name", "Blue wallet", "location", "Library"));
        item.setId("item-123");
        item.setImageFileId("file-123");

        Map<String, Object> response = item.toResponseMap();

        assertEquals("Blue wallet", response.get("name"));
        assertEquals("Library", response.get("location"));
        assertEquals("/api/items/item-123/image", response.get("imageUrl"));
    }

    @Test
    void imageFeaturesMatchIdenticalImages() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().setColor(Color.BLUE);
        image.getGraphics().fillRect(0, 0, 16, 16);

        ImageFeatureService featureService = new ImageFeatureService();

        assertTrue(featureService.similarity(featureService.extract(image), featureService.extract(image)) > 0.99);
    }
}
