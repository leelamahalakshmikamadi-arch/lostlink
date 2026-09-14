package com.lost.link.lost.link_backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lost.link.lost.link_backend.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findAllByOrderByCreatedAtDesc();
}

