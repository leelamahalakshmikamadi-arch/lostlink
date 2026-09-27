package com.lost.link.lost.link_backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.lost.link.lost.link_backend.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query(value = "{ '$or': [ { 'userId': ?0 }, { 'userId': null }, { 'userId': '' } ] }", sort = "{ 'createdAt': -1 }")
    List<Notification> findByUserIdOrGlobalOrderByCreatedAtDesc(String userId);

    @Query(value = "{ '$or': [ { 'userId': null }, { 'userId': '' } ] }", sort = "{ 'createdAt': -1 }")
    List<Notification> findGlobalByOrderByCreatedAtDesc();

    boolean existsByItemIdAndTypeAndMatchPercentage(String itemId, String type, Double matchPercentage);

    boolean existsByItemIdAndUserIdAndType(String itemId, String userId, String type);
}

