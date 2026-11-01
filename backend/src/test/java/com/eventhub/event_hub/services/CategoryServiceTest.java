package com.eventhub.event_hub.services;

import com.eventhub.event_hub.dtos.CategoryRequest;
import com.eventhub.event_hub.dtos.CategoryResponse;
import com.eventhub.event_hub.exceptions.ConflictException;
import com.eventhub.event_hub.exceptions.ResourceNotFoundException;
import com.eventhub.event_hub.models.Category;
import com.eventhub.event_hub.repositories.CategoryRepository;
import com.eventhub.event_hub.repositories.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private EventRepository eventRepository;
    @InjectMocks private CategoryService service;

    @Test
    void createTrimsTheNameAndSaves() {
        when(categoryRepository.existsByNameIgnoreCase("Workshops")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse created = service.create(new CategoryRequest("  Workshops "));

        assertEquals("Workshops", created.name());
    }

    @Test
    void createRejectsDuplicateNamesIgnoringCase() {
        when(categoryRepository.existsByNameIgnoreCase("music")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(new CategoryRequest("music")));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateRenamesTheCategory() {
        UUID id = UUID.randomUUID();
        Category existing = new Category("Old");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("New", id)).thenReturn(false);

        CategoryResponse updated = service.update(id, new CategoryRequest("New"));

        assertEquals("New", updated.name());
    }

    @Test
    void updateCannotTakeTheNameOfAnotherCategory() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(new Category("Old")));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Taken", id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(id, new CategoryRequest("Taken")));
    }

    @Test
    void deleteIsRefusedWhileEventsUseTheCategory() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(new Category("Used")));
        when(eventRepository.existsByCategoryId(id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(id));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteRemovesAnUnusedCategory() {
        UUID id = UUID.randomUUID();
        Category category = new Category("Unused");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(id)).thenReturn(false);

        service.delete(id);

        verify(categoryRepository).delete(category);
    }

    @Test
    void findByIdOfUnknownCategoryIsNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void findAllMapsEveryCategory() {
        when(categoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(new Category("A"), new Category("B")));

        assertEquals(2, service.findAll().size());
    }
}
