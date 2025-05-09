package com.siemens.internship.service;

import com.siemens.internship.exception.NotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Service layer responsible for managing Item entities.
 * Encapsulates business logic, data access, validation, and asynchronous processing
 * of items retrieved from the repository.
 */
@Service
public class ItemService {

    /**
     * Repository for performing CRUD operations on Item entities.
     */
    private final ItemRepository itemRepository;

    /**
     * Thread pool used to execute asynchronous processing tasks for items.
     */
    private final ExecutorService processingExecutor;

    /**
     * Regular expression to validate email addresses.
     * Disallows consecutive dots
     * Requires a valid local-part and domain
     * Ensures top-level domain has at least two letters
     */
    private static final String EMAIL_REGEX = "^(?!.*\\.{2})"
            + "[A-Za-z0-9._%+-]+@"
            + "[A-Za-z0-9.-]+\\."
            + "[A-Za-z]{2,}$";

    /**
     * Constructor injection of repository and executor.
     *
     * @param itemRepository    repository for Item persistence
     * @param processingExecutor thread pool for async item processing
     */
    public ItemService(ItemRepository itemRepository,
                       ExecutorService processingExecutor) {
        this.itemRepository = itemRepository;
        this.processingExecutor = processingExecutor;
    }

    /**
     * Retrieve all items from the data store.
     *
     * @return list of all Item entities
     */
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    /**
     * Find a single item by its ID.
     *
     * @param id unique ID of the item to retrieve
     * @return Optional containing the found Item or empty if not present
     */
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    /**
     * Save or update an Item entity in the data store.
     *
     * @param item the Item to persist
     * @return the saved Item with any updates (e.g., generated ID)
     */
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
     * Delete an item by its ID.
     *
     * @param id the unique identifier of the item to remove
     */
    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Validate email format against a predefined regex pattern.
     *
     * @param email the email string to validate
     * @return true if the email is non-null and matches the pattern; false otherwise
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    /**
     * Load an item by ID, simulate processing, mark it as processed, and save it in a single transaction.
     * If the item is not found, a NotFoundException is thrown.
     *
     * @param id the unique ID of the item to process
     * @return the processed and saved Item
     * @throws NotFoundException if no Item with the given ID exists
     */
    @Transactional
    public Item loadAndProcess(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item " + id + " not found"));

        // Simulate a time-consuming processing step
        simulateWorkload();

        // Update entity state
        item.setStatus("PROCESSED");

        // Persist changes and return
        return itemRepository.save(item);
    }

    /**
     * Process all items asynchronously using the configured executor.
     * Logs and swallows any exceptions for individual items,
     * then returns a list of successfully processed items.
     *
     * @return list of Items that were processed without errors
     */
    public List<Item> processItemsAsync() {
        // Retrieve all item IDs
        List<Long> ids = itemRepository.findAllIds();

        // Processing tasks in parallel
        List<CompletableFuture<Item>> futures = ids.stream()
                .map(id -> CompletableFuture
                        .supplyAsync(() -> loadAndProcess(id), processingExecutor)
                        .handle((item, ex) -> {
                            // If an error occurred, swallow and return null
                            return (ex != null) ? null : item;
                        })
                )
                .collect(Collectors.toList());

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect only successful results
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Simulate a workload by sleeping the current thread briefly.
     * This represents placeholder logic for a real processing step.
     */
    private void simulateWorkload() {
        try {
            // Delay
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Restore interrupt status if thread was interrupted
            Thread.currentThread().interrupt();
        }
    }
}




