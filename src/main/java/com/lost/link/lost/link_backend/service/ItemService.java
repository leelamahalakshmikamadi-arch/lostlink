package com.lost.link.lost.link_backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Item saveLostItem(Map<String, Object> itemData) {
        return itemRepository.save(new Item(itemData));
    }

    public Item saveFoundItem(Map<String, Object> itemData) {
        Item item = new Item(itemData);
        item.setItemType("found");
        return itemRepository.save(item);
    }

    public Item saveItem(Map<String, Object> itemData, String itemType, String imageFileId,
            String imageFilename, String imageContentType, List<Double> imageFeatures) {
        Item item = new Item(itemData);
        item.setItemType(itemType);
        item.setImageFileId(imageFileId);
        item.setImageFilename(imageFilename);
        item.setImageContentType(imageContentType);
        item.setImageFeatures(imageFeatures);
        return itemRepository.save(item);
    }

    public List<Item> getLostItems() {
        return itemRepository.findAll().stream()
                .filter(item -> !"found".equals(item.getItemType()))
                .toList();
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public List<Item> getFoundItems() {
        return itemRepository.findAll().stream()
                .filter(item -> "found".equals(item.getItemType()))
                .collect(Collectors.toList());
    }

    public Item getItem(String id) {
        return itemRepository.findById(id).orElse(null);
    }

    public Item updateItem(Item item) {
        return itemRepository.save(item);
    }
}