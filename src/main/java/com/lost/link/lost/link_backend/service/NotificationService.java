package com.lost.link.lost.link_backend.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.model.Notification;
import com.lost.link.lost.link_backend.model.User;
import com.lost.link.lost.link_backend.repository.NotificationRepository;
import com.lost.link.lost.link_backend.repository.UserRepository;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Notification handleMatchNotification(Item candidateItem, String uploadedItemType,
                                               String currentUserId, String currentUserEmail,
                                               double matchPercentage) {
        if (candidateItem == null) {
            return null;
        }

        String targetUserId = null;

        if ("found".equalsIgnoreCase(uploadedItemType)) {
            // A found item was uploaded to match against lost items.
            // Candidate is the lost item -> notify the owner of the lost item.
            Map<String, Object> data = candidateItem.getData();
            if (data != null) {
                Object uid = data.get("userId");
                if (uid != null && !uid.toString().isBlank()) {
                    targetUserId = uid.toString().trim();
                } else {
                    Object emailObj = data.get("userEmail");
                    if (emailObj == null) emailObj = data.get("email");
                    if (emailObj == null) emailObj = data.get("contactEmail");
                    if (emailObj != null && !emailObj.toString().isBlank()) {
                        Optional<User> uOpt = userRepository.findByEmailIgnoreCase(emailObj.toString().trim());
                        if (uOpt.isPresent()) {
                            targetUserId = uOpt.get().getId();
                        }
                    }
                }
            }
        } else {
            // A lost item was uploaded to match against found items.
            // The uploader is the owner of the lost item -> notify uploader.
            if (currentUserId != null && !currentUserId.isBlank()) {
                targetUserId = currentUserId.trim();
            } else if (currentUserEmail != null && !currentUserEmail.isBlank()) {
                Optional<User> userOpt = userRepository.findByEmailIgnoreCase(currentUserEmail.trim());
                if (userOpt.isPresent()) {
                    targetUserId = userOpt.get().getId();
                }
            }
        }

        String candidateId = candidateItem.getId();
        if (targetUserId == null || targetUserId.isBlank() || candidateId == null || candidateId.isBlank()) {
            logger.warn("Skipping match notification because its recipient or candidate item could not be resolved.");
            return null;
        }

        // Prevent duplicate notifications
        boolean exists = notificationRepository.existsByItemIdAndUserIdAndType(candidateId, targetUserId, "MATCH");

        if (exists) {
            logger.info("Match notification already exists for candidate itemId {} and userId {}, skipping duplicate.",
                    candidateId, targetUserId);
            return null;
        }

        String message = "Possible match found! Your lost item has a 100% similarity match with a found item.";
        Notification notification = new Notification(
                "Possible Match Found!",
                message,
                "MATCH",
                candidateId,
                targetUserId,
                matchPercentage
        );

        Notification saved = notificationRepository.save(notification);
        if (saved.getNotificationId() == null) {
            saved.setNotificationId(saved.getId());
            saved = notificationRepository.save(saved);
        }

        logger.info("Created 100% match notification id {} for itemId {} and recipient userId {}.",
                saved.getId(), candidateId, targetUserId);
        return saved;
    }

    public List<Notification> getNotifications(String userId) {
        if (userId != null && !userId.isBlank()) {
            return notificationRepository.findByUserIdOrGlobalOrderByCreatedAtDesc(userId.trim());
        }
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public Notification markAsRead(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isEmpty()) {
            return null;
        }
        Notification notification = opt.get();
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead() {
        List<Notification> list = notificationRepository.findAll();
        for (Notification n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
    }
}
