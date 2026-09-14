package com.lost.link.lost.link_backend.controller;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import org.bson.types.ObjectId;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.lost.link.lost.link_backend.model.Item;
import com.lost.link.lost.link_backend.service.ImageFeatureService;
import com.lost.link.lost.link_backend.service.ImageStorageService;
import com.lost.link.lost.link_backend.service.ItemService;
import com.lost.link.lost.link_backend.service.GeminiImageMatchingService;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174",
    "http://127.0.0.1:5173",
    "http://127.0.0.1:5174"
}, allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS })
public class ItemController {

    private final ItemService itemService;
    private final ImageStorageService imageStorageService;
    private final ImageFeatureService imageFeatureService;
    private final GridFsTemplate gridFsTemplate;
    private final GeminiImageMatchingService geminiImageMatchingService;

    public ItemController(ItemService itemService, ImageStorageService imageStorageService,
            ImageFeatureService imageFeatureService, GridFsTemplate gridFsTemplate,
            GeminiImageMatchingService geminiImageMatchingService) {
        this.itemService = itemService;
        this.imageStorageService = imageStorageService;
        this.imageFeatureService = imageFeatureService;
        this.gridFsTemplate = gridFsTemplate;
        this.geminiImageMatchingService = geminiImageMatchingService;
    }

    @PostMapping("/lost")
    public ResponseEntity<Map<String, Object>> addLostItem(@RequestBody Map<String, Object> item) {
        Item savedItem = itemService.saveLostItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Lost item submitted successfully!",
                "item", savedItem.toResponseMap()));
    }

    @PostMapping(value = "/lost", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addLostItemWithImage(
            @RequestParam Map<String, String> itemData, @RequestPart("image") MultipartFile image) {
        return saveItemWithImage(itemData, image, "lost");
    }

    @PostMapping("/found")
    public ResponseEntity<Map<String, Object>> addFoundItem(@RequestBody Map<String, Object> item) {
        Item savedItem = itemService.saveFoundItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Found item submitted successfully!",
                "item", savedItem.toResponseMap()));
    }

    @PostMapping(value = "/found", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addFoundItemWithImage(
            @RequestParam Map<String, String> itemData, @RequestPart("image") MultipartFile image) {
        return saveItemWithImage(itemData, image, "found");
    }

    @PostMapping(value = "/match", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> matchLostItem(
            @RequestPart("image") MultipartFile image,
            @RequestParam(defaultValue = "lost") String itemType,
            @RequestParam(defaultValue = "0.65") double minSimilarity) {
        if (!"lost".equalsIgnoreCase(itemType) && !"found".equalsIgnoreCase(itemType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemType must be lost or found");
        }
        List<Item> candidates = "found".equalsIgnoreCase(itemType)
            ? itemService.getLostItems()
            : itemService.getFoundItems();
        List<Map<String, Object>> matches = geminiImageMatchingService.findMatches(image, candidates, minSimilarity);
        return ResponseEntity.ok(Map.of("matches", matches));
    }

    @GetMapping("/match/status")
    public ResponseEntity<Map<String, Object>> getMatchingStatus() {
        return ResponseEntity.ok(Map.of("configured", geminiImageMatchingService.isConfigured()));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getItemImage(@PathVariable String id) {
        Item item = itemService.getItem(id);
        if (item == null || !ObjectId.isValid(item.getImageFileId())) {
            return ResponseEntity.notFound().build();
        }
        var file = gridFsTemplate.findOne(new Query(
            Criteria.where("_id").is(new ObjectId(item.getImageFileId()))));
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        GridFsResource resource = gridFsTemplate.getResource(file);
        MediaType contentType = resource.getContentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(resource.getContentType());
        return ResponseEntity.ok()
            .contentType(contentType)
                .body(resource);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getItem(@PathVariable String id) {
        Item item = itemService.getItem(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item.toResponseMap());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getItems() {
        return ResponseEntity.ok(itemService.getAllItems().stream()
                .map(Item::toResponseMap)
                .toList());
    }

    private ResponseEntity<Map<String, Object>> saveItemWithImage(
            Map<String, String> itemData, MultipartFile image, String itemType) {
        String imageFileId = imageStorageService.store(image);
        Item savedItem = itemService.saveItem(new LinkedHashMap<>(itemData), itemType, imageFileId,
                image.getOriginalFilename(), image.getContentType(), imageFeatureService.extract(image));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", itemType.substring(0, 1).toUpperCase() + itemType.substring(1)
                        + " item submitted successfully!",
                "item", savedItem.toResponseMap()));
    }

}