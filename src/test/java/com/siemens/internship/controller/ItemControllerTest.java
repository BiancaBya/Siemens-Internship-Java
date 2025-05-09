package com.siemens.internship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ItemController, verifying REST endpoints behavior
 */
@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mvc; // Entry point for server-side Spring MVC test support

    @MockBean
    private ItemService service; // Mocked service to control behavior in tests

    @Autowired
    private ObjectMapper mapper; // JSON (de)serializer for request/response bodies

    /**
     * GET /api/items should return HTTP 200 and a list of items in JSON.
     */
    @Test
    void getAllItems_returns200AndList() throws Exception {
        // Prepare a sample list of items to be returned by the service
        List<Item> items = List.of(
                new Item(1L, "n1", "d1", "NEW", "a@b.com"),
                new Item(2L, "n2", "d2", "NEW", "c@d.com")
        );
        Mockito.when(service.findAll()).thenReturn(items);

        // Perform GET and assert status and JSON payload
        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Two items
                .andExpect(jsonPath("$[0].id").value(1))    // First item ID
                .andExpect(jsonPath("$[1].email").value("c@d.com")); // Second email
    }

    /**
     * POST /api/items with invalid email should return HTTP 400 and error message.
     */
    @Test
    void createItem_whenInvalidEmail_returns400() throws Exception {
        Item bad = new Item(null, "n", "d", "NEW", "invalid-email");

        // No need to stub service.save: controller rejects before calling service
        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    /**
     * POST /api/items with null email should also yield HTTP 400.
     */
    @Test
    void createItem_whenEmailNull_returns400() throws Exception {
        Item noEmail = new Item(null, "n", "d", "NEW", null);

        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(noEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    /**
     * POST /api/items with valid email should return HTTP 201 and the created item.
     */
    @Test
    void createItem_whenValid_returns201() throws Exception {
        Item in = new Item(null, "n", "d", "NEW", "a@b.com");
        Item out = new Item(1L, "n", "d", "NEW", "a@b.com");
        Mockito.when(service.save(any())).thenReturn(out);

        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(in)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    /**
     * PUT /api/items/{id} for existing item should return HTTP 200 with updated data.
     */
    @Test
    void updateItem_whenExists_returns200AndUpdated() throws Exception {
        Item in = new Item(null, "n", "d", "NEW", "x@x.com");
        Item updated = new Item(5L, "n", "d", "NEW", "x@x.com");
        Mockito.when(service.findById(5L)).thenReturn(Optional.of(in));
        Mockito.when(service.save(any())).thenReturn(updated);

        mvc.perform(put("/api/items/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("x@x.com"));
    }

    /**
     * PUT /api/items/{id} with invalid email should return HTTP 400.
     */
    @Test
    void updateItem_whenInvalidEmail_returns400() throws Exception {
        Item in = new Item(null, "n", "d", "NEW", "not-an-email");
        // Stub findById to prevent NPE, though controller rejects early
        Mockito.when(service.findById(1L))
                .thenReturn(Optional.of(new Item(1L, "n", "d", "NEW", "a@b.com")));

        mvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(in)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    /**
     * PUT /api/items/{id} with null email should also return HTTP 400.
     */
    @Test
    void updateItem_whenEmailNull_returns400() throws Exception {
        Item in = new Item(null, "n", "d", "NEW", null);
        Mockito.when(service.findById(2L))
                .thenReturn(Optional.of(new Item(2L, "n", "d", "NEW", "a@b.com")));

        mvc.perform(put("/api/items/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(in)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    /**
     * PUT /api/items/{id} when item does not exist should return HTTP 404.
     */
    @Test
    void updateItem_whenNotExists_returns404() throws Exception {
        Item in = new Item(null, "n", "d", "NEW", "x@x.com");
        Mockito.when(service.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(put("/api/items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(in)))
                .andExpect(status().isNotFound());
    }

    /**
     * DELETE /api/items/{id} when item exists should return HTTP 204 No Content.
     */
    @Test
    void deleteItem_whenExists_returns204() throws Exception {
        Mockito.when(service.findById(7L))
                .thenReturn(Optional.of(new Item(7L, "n", "d", "NEW", "e@e.com")));

        mvc.perform(delete("/api/items/7"))
                .andExpect(status().isNoContent());
    }

    /**
     * DELETE /api/items/{id} when item does not exist should return HTTP 404.
     */
    @Test
    void deleteItem_whenNotExists_returns404() throws Exception {
        Mockito.when(service.findById(8L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/items/8"))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /api/items/process should return HTTP 200 and a list of processed items.
     */
    @Test
    void processItems_returns200AndProcessedList() throws Exception {
        List<Item> processed = List.of(
                new Item(1L, "n", "d", "PROCESSED", "a@b.com"),
                new Item(2L, "n2", "d2", "PROCESSED", "b@b.com")
        );
        Mockito.when(service.processItemsAsync()).thenReturn(processed);

        mvc.perform(get("/api/items/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }

    /**
     * GET /api/items/{id} when item exists should return HTTP 200 and the item JSON.
     */
    @Test
    void getItemById_whenFound_returns200() throws Exception {
        Item item = new Item(1L, "n", "d", "NEW", "a@b.com");
        Mockito.when(service.findById(1L)).thenReturn(Optional.of(item));

        mvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("a@b.com"));
    }

    /**
     * GET /api/items/{id} when item does not exist should return HTTP 404.
     */
    @Test
    void getItemById_whenNotFound_returns404() throws Exception {
        Mockito.when(service.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound());
    }
}



