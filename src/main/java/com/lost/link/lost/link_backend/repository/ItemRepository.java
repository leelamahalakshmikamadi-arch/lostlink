package com.lost.link.lost.link_backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lost.link.lost.link_backend.model.Item;

public interface ItemRepository extends MongoRepository<Item, String> {
}