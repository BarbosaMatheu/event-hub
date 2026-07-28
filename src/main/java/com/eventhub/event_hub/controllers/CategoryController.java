package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.CategoryRecordDto;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<Category> saveCategory(@RequestBody @Valid CategoryRecordDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.save(dto));
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getOneCategory(@PathVariable(value = "id") UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.findById(id));
    }
}