package com.lost.link.lost.link_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.lost.link.lost.link_backend.model.Claim;
import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.model.Notification;
import com.lost.link.lost.link_backend.repository.ClaimRepository;
import com.lost.link.lost.link_backend.repository.NotificationRepository;
import com.lost.link.lost.link_backend.service.ItemService;

class ClaimControllerTest {

    private ClaimRepository claimRepository;
    private NotificationRepository notificationRepository;
    private ItemService itemService;
    private ClaimController claimController;

    @BeforeEach
    void setUp() {
        claimRepository = mock(ClaimRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        itemService = mock(ItemService.class);
        claimController = new ClaimController(claimRepository, notificationRepository, itemService);
    }

    @Test
    void submitClaimGeneratesOtpAndQrToken() {
        Item item = new Item(Map.of("itemName", "Blue Backpack"));
        item.setId("item-1");
        when(itemService.getItem("item-1")).thenReturn(item);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> {
            Claim c = invocation.getArgument(0);
            c.setId("claim-101");
            return c;
        });

        ResponseEntity<Map<String, Object>> response = claimController.submitClaim("item-1", Map.of(
                "claimantName", "Priya",
                "claimantContact", "9876543210",
                "proofDetails", "Has a red keychain inside front zipper"
        ));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        String otp = String.valueOf(response.getBody().get("otp"));
        String qrCode = String.valueOf(response.getBody().get("qrCode"));

        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
        assertTrue(qrCode.startsWith("LL-CLAIM-"));
        assertEquals("PENDING", response.getBody().get("status"));
    }

    @Test
    void verifyClaimWithOtpSucceeds() {
        Item item = new Item(Map.of("itemName", "Blue Backpack"));
        item.setId("item-1");
        when(itemService.getItem("item-1")).thenReturn(item);

        Claim claim = new Claim("item-1", "Priya", "9876543210", "Red keychain", "123456", "LL-CLAIM-TEST-1");
        claim.setId("claim-101");
        when(claimRepository.findByItemId("item-1")).thenReturn(List.of(claim));
        when(claimRepository.save(any(Claim.class))).thenReturn(claim);

        ResponseEntity<Map<String, Object>> response = claimController.verifyClaim("item-1", Map.of("otp", "123456"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("VERIFIED", response.getBody().get("status"));
        assertEquals("CLAIMED", item.getData().get("status"));
    }

    @Test
    void verifyClaimWithWrongOtpFails() {
        Claim claim = new Claim("item-1", "Priya", "9876543210", "Red keychain", "123456", "LL-CLAIM-TEST-1");
        when(claimRepository.findByItemId("item-1")).thenReturn(List.of(claim));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                claimController.verifyClaim("item-1", Map.of("otp", "999999")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void getNotificationsReturnsList() {
        Notification n = new Notification("Test Alert", "Sample item alert", "SYSTEM", "item-1");
        when(notificationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(n));

        ResponseEntity<List<Notification>> response = claimController.getNotifications();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Test Alert", response.getBody().get(0).getTitle());
    }
}

