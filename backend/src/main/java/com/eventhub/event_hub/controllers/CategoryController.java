package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.CategoryRequest;
import com.eventhub.event_hub.dtos.CategoryResponse;
import com.eventhub.event_hub.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Public to read, ADMIN to change")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List all categories (alphabetical)")
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable UUID id) {
        return categoryService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable UUID id, @RequestBody @Valid CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category", description = "409 if events still use it.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
