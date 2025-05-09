package com.siemens.internship.service;

import com.siemens.internship.exception.NotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemService, covering basic CRUD delegation,
 * transactional processing (loadAndProcess), asynchronous batch processing,
 * interruption handling in simulateWorkload, and email validation utility.
 */
class ItemServiceTest {

    @Mock
    private ItemRepository repo;  // Mocked repository for DB operations

    private ExecutorService executor;  // Real executor for async methods
    private ItemService service;      // Service under test

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        executor = Executors.newSingleThreadExecutor();
        service = new ItemService(repo, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    /**
     * findAll() should delegate directly to repo.findAll() and return the same list.
     */
    @Test
    void findAll_shouldReturnAllItems() {
        List<Item> items = List.of(
                new Item(1L, "A", "a", "NEW", "a@a.com"),
                new Item(2L, "B", "b", "NEW", "b@b.com")
        );
        when(repo.findAll()).thenReturn(items);

        List<Item> result = service.findAll();

        assertThat(result).isSameAs(items);
        verify(repo).findAll();
    }

    /**
     * findById() when item exists should return Optional.of(item).
     */
    @Test
    void findById_whenExists_shouldReturnItem() {
        Item item = new Item(5L, "X", "x", "NEW", "x@x.com");
        when(repo.findById(5L)).thenReturn(Optional.of(item));

        Optional<Item> result = service.findById(5L);

        assertThat(result).contains(item);
        verify(repo).findById(5L);
    }

    /**
     * findById() when no item found should return Optional.empty().
     */
    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        Optional<Item> result = service.findById(99L);
        assertThat(result).isEmpty();
        verify(repo).findById(99L);
    }

    /**
     * save() should pass the entity to repo.save() and return the saved instance.
     */
    @Test
    void save_shouldDelegateToRepository() {
        Item in = new Item(null, "N", "n", "NEW", "n@n.com");
        Item out = new Item(10L, "N", "n", "NEW", "n@n.com");
        when(repo.save(in)).thenReturn(out);

        Item result = service.save(in);

        assertThat(result).isSameAs(out);
        verify(repo).save(in);
    }

    /**
     * deleteById() should call repo.deleteById() with the given ID.
     */
    @Test
    void deleteById_shouldDelegateToRepository() {
        service.deleteById(42L);
        verify(repo).deleteById(42L);
    }

    /**
     * loadAndProcess() when item exists should mark status PROCESSED and save.
     */
    @Test
    void loadAndProcess_whenExists_marksProcessedAndSaves() {
        Item raw = new Item(7L, "name", "desc", "NEW", "e@e.com");
        when(repo.findById(7L)).thenReturn(Optional.of(raw));
        when(repo.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        Item processed = service.loadAndProcess(7L);

        assertThat(processed.getStatus()).isEqualTo("PROCESSED");
        assertThat(processed.getId()).isEqualTo(7L);
        verify(repo).findById(7L);
        verify(repo).save(processed);
    }

    /**
     * loadAndProcess() when item not found should throw NotFoundException.
     */
    @Test
    void loadAndProcess_whenNotExists_throwsNotFound() {
        when(repo.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadAndProcess(100L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Item 100 not found");
        verify(repo).findById(100L);
        verify(repo, never()).save(any());
    }

    /**
     * simulateWorkload() when thread is interrupted should restore the interrupted status.
     */
    @Test
    void simulateWorkload_whenInterrupted_restoresInterruptStatus() throws Exception {
        // Interrupt the current thread before invoking
        Thread current = Thread.currentThread();
        current.interrupt();

        // Use reflection to access the private simulateWorkload method
        Method method = ItemService.class.getDeclaredMethod("simulateWorkload");
        method.setAccessible(true);
        method.invoke(service);

        // After invocation, the interrupted flag should still be set
        assertTrue(current.isInterrupted(), "Thread's interrupted status should be restored");
    }

    /**
     * processItemsAsync() should load & process each ID,
     * mark status, and save the updated items.
     */
    @Test
    void processItemsAsync_shouldProcessAllIds() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(repo.findAllIds()).thenReturn(ids);
        when(repo.findById(anyLong())).thenAnswer(inv -> Optional.of(
                new Item(inv.getArgument(0), "n", "d", null, "e@test.com")
        ));
        when(repo.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Item> processed = service.processItemsAsync();

        assertThat(processed).hasSize(3);
        processed.forEach(i -> assertThat(i.getStatus()).isEqualTo("PROCESSED"));
        ids.forEach(id -> {
            verify(repo).findById(id);
            verify(repo).save(argThat(item -> item.getId().equals(id)));
        });
    }

    /**
     * processItemsAsync() should return empty list when no IDs found,
     * and never call save().
     */
    @Test
    void processItemsAsync_whenEmpty_shouldReturnEmpty() {
        when(repo.findAllIds()).thenReturn(Collections.emptyList());

        List<Item> processed = service.processItemsAsync();

        assertThat(processed).isEmpty();
        verify(repo, never()).save(any());
    }

    /**
     * processItemsAsync() should swallow errors for missing items
     * and continue processing others.
     */
    @Test
    void processItemsAsync_whenOneIdNotFound_swallowErrorAndProcessOthers() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(repo.findAllIds()).thenReturn(ids);
        when(repo.findById(1L)).thenReturn(Optional.of(new Item(1L, "A", "a", null, "a@test.com")));
        when(repo.findById(2L)).thenReturn(Optional.empty());
        when(repo.findById(3L)).thenReturn(Optional.of(new Item(3L, "C", "c", null, "c@test.com")));
        when(repo.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Item> processed = service.processItemsAsync();

        assertThat(processed)
                .extracting(Item::getId)
                .containsExactlyInAnyOrder(1L, 3L);
        processed.forEach(i -> assertThat(i.getStatus()).isEqualTo("PROCESSED"));
        verify(repo).findById(2L);
        verify(repo, times(2)).save(any());
    }

    /**
     * isValidEmail() should return true for valid email patterns.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "john.doe@sub.domain.org",
            "name.surname+tag@company.co.uk",
            "u_n-i@domain.io"
    })
    void isValidEmail_shouldReturnTrue_forValidEmails(String email) {
        assertTrue(ItemService.isValidEmail(email),
                () -> "Expected valid for email = " + email);
    }

    /**
     * isValidEmail() should return false for null or invalid email formats.
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "plainaddress",
            "@no-local-part.com",
            "no-at-symbol.com",
            "user@",
            "user@.com",
            "user@com",
            "user@domain.c",
            "user name@d.com",
            "user@domain..com"
    })
    void isValidEmail_shouldReturnFalse_forInvalidEmails(String email) {
        assertFalse(ItemService.isValidEmail(email),
                () -> "Expected invalid for email = " + email);
    }
}
