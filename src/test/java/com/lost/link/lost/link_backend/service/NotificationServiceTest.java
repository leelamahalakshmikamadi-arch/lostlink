package com.lost.link.lost.link_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.model.Notification;
import com.lost.link.lost.link_backend.model.User;
import com.lost.link.lost.link_backend.repository.NotificationRepository;
import com.lost.link.lost.link_backend.repository.UserRepository;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = new NotificationService(notificationRepository, userRepository);
    }

    @Test
    void test100PercentMatchCreatesNotificationForLostItemOwner() {
        Item lostItem = new Item();
        lostItem.setId("lost-item-1");
        Map<String, Object> data = new HashMap<>();
        data.put("itemName", "Black Dell Laptop");
        data.put("userId", "user-100");
        lostItem.setData(data);

        when(notificationRepository.existsByItemIdAndUserIdAndType("lost-item-1", "user-100", "MATCH"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) {
                n.setId("notif-999");
            }
            return n;
        });

        Notification result = notificationService.handleMatchNotification(
                lostItem, "found", null, null, 100.0);

        assertNotNull(result);
        assertEquals("user-100", result.getUserId());
        assertEquals("MATCH", result.getType());
        assertEquals(100.0, result.getMatchPercentage());
        assertEquals("lost-item-1", result.getItemId());
        assertEquals("Possible match found! Your lost item has a 100% similarity match with a found item.", result.getMessage());
        assertFalse(result.isRead());

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testDuplicate100PercentMatchDoesNotCreateSecondNotification() {
        Item lostItem = new Item();
        lostItem.setId("lost-item-1");
        Map<String, Object> data = new HashMap<>();
        data.put("itemName", "Black Dell Laptop");
        data.put("userId", "user-100");
        lostItem.setData(data);

        // Simulate notification already exists
        when(notificationRepository.existsByItemIdAndUserIdAndType("lost-item-1", "user-100", "MATCH"))
                .thenReturn(true);

        Notification result = notificationService.handleMatchNotification(
                lostItem, "found", null, null, 100.0);

        assertNull(result);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void test100PercentMatchResolvesUserIdFromCandidateEmail() {
        Item lostItem = new Item();
        lostItem.setId("lost-item-2");
        Map<String, Object> data = new HashMap<>();
        data.put("itemName", "Red Water Bottle");
        data.put("userEmail", "student@campus.edu");
        lostItem.setData(data);

        User resolvedUser = new User();
        resolvedUser.setId("user-555");
        resolvedUser.setEmail("student@campus.edu");

        when(userRepository.findByEmailIgnoreCase("student@campus.edu"))
                .thenReturn(Optional.of(resolvedUser));
        when(notificationRepository.existsByItemIdAndUserIdAndType("lost-item-2", "user-555", "MATCH"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) {
                n.setId("notif-888");
            }
            return n;
        });

        Notification result = notificationService.handleMatchNotification(
                lostItem, "found", null, null, 100.0);

        assertNotNull(result);
        assertEquals("user-555", result.getUserId());
        assertEquals("lost-item-2", result.getItemId());
    }

    @Test
    void testMarkAsRead() {
        Notification notification = new Notification("Title", "Message", "MATCH", "item-1", "user-1", 100.0);
        notification.setId("notif-1");
        notification.setRead(false);

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification updated = notificationService.markAsRead("notif-1");
        assertNotNull(updated);
        assertTrue(updated.isRead());
    }
}
