package com.siemens.internship.controller;

import com.siemens.internship.exception.NotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing endpoints for CRUD operations and
 * asynchronous processing of Item resources.
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    // Standardized error message key for invalid emails
    private static final String INVALID_EMAIL_MSG = "Invalid email format";

    /**
     * Constructor injection of the ItemService dependency.
     *
     * @param itemService service layer for business logic
     */
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * GET /api/items
     * Retrieve a list of all items.
     *
     * @return 200 OK with JSON array of items
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.findAll());
    }

    /**
     * POST /api/items
     * Create a new item. Validates email format before persisting.
     *
     * @param item JSON payload representing the new item
     * @return 201 Created with saved item, or 400 Bad Request
     *         with error map if email is invalid
     */
    @PostMapping
    public ResponseEntity<?> createItem(@RequestBody Item item) {
        // Validate email using service utility
        if (!ItemService.isValidEmail(item.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("email", INVALID_EMAIL_MSG));
        }
        Item saved = itemService.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/items/{id}
     * Fetch a single item by its ID.
     *
     * @param id path variable identifying the item
     * @return 200 OK with item JSON, or 404 Not Found if missing
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        Item found = itemService.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Item with id=" + id + " not found"));
        return ResponseEntity.ok(found);
    }

    /**
     * PUT /api/items/{id}
     * Update an existing item. Validates email and existence.
     *
     * @param id   path variable of item to update
     * @param item JSON payload with updated fields
     * @return 200 OK with updated item, 400 for invalid email,
     *         or 404 if item does not exist
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long id,
            @RequestBody Item item) {

        // Reject invalid email before any DB operation
        if (!ItemService.isValidEmail(item.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("email", INVALID_EMAIL_MSG));
        }

        // Ensure the item exists before updating
        if (itemService.findById(id).isEmpty()) {
            throw new NotFoundException("Item with id=" + id + " not found");
        }
        // Force the path ID onto the entity
        item.setId(id);
        Item updated = itemService.save(item);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/items/{id}
     * Remove an item by ID. Returns 204 No Content if successful,
     * or 404 if the item does not exist.
     *
     * @param id identifier of item to delete
     * @return 204 No Content or 404 Not Found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (itemService.findById(id).isEmpty()) {
            throw new NotFoundException("Item with id=" + id + " not found");
        }
        itemService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/items/process
     * Trigger asynchronous processing of all items.
     *
     * @return 200 OK with list of successfully processed items
     */
    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() {
        return ResponseEntity.ok(itemService.processItemsAsync());
    }
}


