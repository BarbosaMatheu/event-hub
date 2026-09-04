package com.eventhub.event_hub.controllers;

import com.eventhub.event_hub.dtos.CategoryRecordDto;
import com.eventhub.event_hub.dtos.CategoryResponseDto;
import com.eventhub.event_hub.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> saveCategory(@RequestBody @Valid CategoryRecordDto dto) {
        var category = categoryService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponseDto.fromEntity(category));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        List<CategoryResponseDto> categories = categoryService.findAll().stream()
                .map(CategoryResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getOneCategory(@PathVariable(value = "id") UUID id) {
        var category = categoryService.findById(id);
        return ResponseEntity.status(HttpStatus.OK).body(CategoryResponseDto.fromEntity(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> updateCategory(@PathVariable(value = "id") UUID id, @RequestBody @Valid CategoryRecordDto dto) {
        var category = categoryService.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(CategoryResponseDto.fromEntity(category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable(value = "id") UUID id) {
        categoryService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
