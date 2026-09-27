package com.lost.link.lost.link_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.service.GeminiImageMatchingService;
import com.lost.link.lost.link_backend.service.ImageFeatureService;
import com.lost.link.lost.link_backend.service.ImageStorageService;
import com.lost.link.lost.link_backend.service.ItemService;
import com.lost.link.lost.link_backend.service.NotificationService;

class ItemControllerMatchNotificationTest {

    private ItemService itemService;
    private GeminiImageMatchingService matchingService;
    private NotificationService notificationService;
    private ItemController controller;
    private Item candidate;

    @BeforeEach
    void setUp() {
        itemService = mock(ItemService.class);
        matchingService = mock(GeminiImageMatchingService.class);
        notificationService = mock(NotificationService.class);
        controller = new ItemController(itemService, mock(ImageStorageService.class),
                mock(ImageFeatureService.class), mock(GridFsTemplate.class), matchingService, notificationService);

        candidate = new Item(Map.of("itemName", "Blue wallet", "userId", "owner-42"));
        candidate.setId("lost-item-42");
        when(itemService.getFoundItems()).thenReturn(List.of(candidate));
    }

    @Test
    void createsOneNotificationForAConfirmedHundredPercentMatch() {
        when(matchingService.findMatches(any(), eq(List.of(candidate)), eq(0.65))).thenReturn(List.of(
                match(1.0, 100.0)));

        var response = controller.matchLostItem(image(), "lost", 0.65, "owner-42", "owner@example.com");

        assertEquals(200, response.getStatusCode().value());
        verify(notificationService).handleMatchNotification(candidate, "lost", "owner-42", "owner@example.com", 100.0);
    }

    @Test
    void doesNotNotifyForAResultRoundedToNinetyNinePointNinetyNinePercent() {
        when(matchingService.findMatches(any(), eq(List.of(candidate)), eq(0.65))).thenReturn(List.of(
                match(0.9999, 99.99)));

        var response = controller.matchLostItem(image(), "lost", 0.65, "owner-42", "owner@example.com");

        assertEquals(200, response.getStatusCode().value());
        verify(notificationService, never()).handleMatchNotification(any(), any(), any(), any(), anyDouble());
    }

    @Test
    void notificationFailureDoesNotFailMatchingResponse() {
        when(matchingService.findMatches(any(), eq(List.of(candidate)), eq(0.65))).thenReturn(List.of(
                match(1.0, 100.0)));
        when(notificationService.handleMatchNotification(candidate, "lost", "owner-42", "owner@example.com", 100.0))
                .thenThrow(new IllegalStateException("Mongo unavailable"));

        var response = controller.matchLostItem(image(), "lost", 0.65, "owner-42", "owner@example.com");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, ((List<?>) response.getBody().get("matches")).size());
    }

    private Map<String, Object> match(double similarity, double percentage) {
        return Map.of("item", candidate.toResponseMap(), "similarity", similarity,
                "similarityPercentage", percentage, "result", "MATCH");
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "item.png", "image/png", new byte[] { 1 });
    }
}