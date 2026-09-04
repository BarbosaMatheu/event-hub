package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.CategoryRecordDto;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void findByIdThrowsWhenCategoryDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(id));
    }

    @Test
    void saveCreatesCategoryFromDto() {
        CategoryRecordDto dto = new CategoryRecordDto("Workshops");
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category saved = categoryService.save(dto);

        assertEquals("Workshops", saved.getName());
    }

    @Test
    void updateChangesNameOfExistingCategory() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Old name");

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = categoryService.update(id, new CategoryRecordDto("New name"));

        assertEquals("New name", updated.getName());
    }

    @Test
    void deleteRemovesExistingCategory() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));

        categoryService.delete(id);

        verify(categoryRepository).delete(existing);
    }
}
