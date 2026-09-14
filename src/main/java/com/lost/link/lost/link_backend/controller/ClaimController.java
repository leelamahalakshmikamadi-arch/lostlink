package com.lost.link.lost.link_backend.controller;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lost.link.lost.link_backend.model.Claim;
import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.model.Notification;
import com.lost.link.lost.link_backend.repository.ClaimRepository;
import com.lost.link.lost.link_backend.repository.NotificationRepository;
import com.lost.link.lost.link_backend.service.ItemService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174",
    "http://127.0.0.1:5173",
    "http://127.0.0.1:5174"
}, allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS })
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final NotificationRepository notificationRepository;
    private final ItemService itemService;
    private final SecureRandom random = new SecureRandom();

    public ClaimController(ClaimRepository claimRepository,
                           NotificationRepository notificationRepository,
                           ItemService itemService) {
        this.claimRepository = claimRepository;
        this.notificationRepository = notificationRepository;
        this.itemService = itemService;
    }

    @PostMapping("/items/{id}/claim")
    public ResponseEntity<Map<String, Object>> submitClaim(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        Map<String, String> safe = payload != null ? payload : Map.of();
        String claimantName = safe.getOrDefault("claimantName", "").trim();
        String claimantContact = safe.getOrDefault("claimantContact", "").trim();
        String proofDetails = safe.getOrDefault("proofDetails", "").trim();

        if (claimantName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Claimant name is required.");
        }
        if (claimantContact.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contact details are required.");
        }

        Item item = itemService.getItem(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found.");
        }

        // Generate 6-digit numeric OTP and QR token
        String otp = String.format("%06d", random.nextInt(1_000_000));
        String qrToken = "LL-CLAIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + id;

        Claim claim = new Claim(id, claimantName, claimantContact, proofDetails, otp, qrToken);
        Claim savedClaim = claimRepository.save(claim);

        // Record notification
        String itemName = String.valueOf(item.getData().getOrDefault("itemName", "item #" + id));
        Notification notification = new Notification(
                "New Claim Submitted",
                claimantName + " submitted a claim with OTP for " + itemName + ".",
                "CLAIM",
                id
        );
        notificationRepository.save(notification);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Claim submitted successfully. Please verify using your 6-digit OTP or show the QR code at the Help Desk.");
        response.put("claimId", savedClaim.getId());
        response.put("itemId", id);
        response.put("otp", otp);
        response.put("qrCode", qrToken);
        response.put("status", savedClaim.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/items/{id}/verify-claim")
    public ResponseEntity<Map<String, Object>> verifyClaim(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        Map<String, String> safe = payload != null ? payload : Map.of();
        String inputOtp = safe.getOrDefault("otp", "").trim();
        String inputQr = safe.getOrDefault("qrCode", "").trim();

        if (inputOtp.isEmpty() && inputQr.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either OTP or QR Code is required for verification.");
        }

        List<Claim> claims = claimRepository.findByItemId(id);
        Claim matchingClaim = null;

        for (Claim c : claims) {
            if (!inputOtp.isEmpty() && inputOtp.equals(c.getOtp())) {
                matchingClaim = c;
                break;
            }
            if (!inputQr.isEmpty() && inputQr.equalsIgnoreCase(c.getQrCode())) {
                matchingClaim = c;
                break;
            }
        }

        if (matchingClaim == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP or QR code for this item.");
        }

        matchingClaim.setStatus("VERIFIED");
        matchingClaim.setVerifiedAt(LocalDateTime.now());
        claimRepository.save(matchingClaim);

        // Update item status
        Item item = itemService.getItem(id);
        if (item != null) {
            item.getData().put("status", "CLAIMED");
            item.getData().put("claimedBy", matchingClaim.getClaimantName());
            itemService.updateItem(item);
        }

        Notification notification = new Notification(
                "Claim Verified Successfully",
                "Item #" + id + " has been verified via OTP/QR and marked as CLAIMED.",
                "CLAIM",
                id
        );
        notificationRepository.save(notification);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Claim verified successfully! Item has been marked as CLAIMED.");
        response.put("claimId", matchingClaim.getId());
        response.put("status", "VERIFIED");
        response.put("verifiedAt", matchingClaim.getVerifiedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/items/{id}/claims")
    public ResponseEntity<List<Claim>> getItemClaims(@PathVariable String id) {
        return ResponseEntity.ok(claimRepository.findByItemId(id));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications() {
        List<Notification> list = notificationRepository.findAllByOrderByCreatedAtDesc();
        if (list.isEmpty()) {
            // Seed welcome and campus alerts so the drawer is never completely blank
            Notification welcome = new Notification(
                    "Welcome to LostLink",
                    "LostLink campus lost-and-found system is active. Report lost or found items anytime!",
                    "SYSTEM",
                    null
            );
            Notification desk = new Notification(
                    "Campus Help Desk Alert",
                    "Physical handovers and QR verifications are supported at the Main Block Help Desk.",
                    "SYSTEM",
                    null
            );
            notificationRepository.save(welcome);
            notificationRepository.save(desk);
            list = notificationRepository.findAllByOrderByCreatedAtDesc();
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<Map<String, Object>> markAllNotificationsRead() {
        List<Notification> list = notificationRepository.findAll();
        for (Notification n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }
}

