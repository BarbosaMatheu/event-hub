package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.CategoryRequest;
import com.eventhub.event_hub.dtos.CategoryResponse;
import com.eventhub.event_hub.exceptions.ConflictException;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public CategoryService(CategoryRepository categoryRepository, EventRepository eventRepository) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return CategoryResponse.from(getOrThrow(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        return CategoryResponse.from(categoryRepository.save(new Category(name)));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = getOrThrow(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A category named '" + name + "' already exists");
        }
        category.setName(name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = getOrThrow(id);
        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cannot delete a category that still has events");
        }
        categoryRepository.delete(category);
    }

    private Category getOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }
}
