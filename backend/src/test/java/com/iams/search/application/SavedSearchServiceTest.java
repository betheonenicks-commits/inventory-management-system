package com.iams.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.search.domain.SavedSearch;
import com.iams.search.domain.SavedSearchRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavedSearchServiceTest {

    @Mock private SavedSearchRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AssetCategoryRepository categoryRepository;
    @Mock private AssetStatusDefRepository statusRepository;
    @Mock private OrgNodeRepository orgNodeRepository;

    private SavedSearchService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new SavedSearchService(repository, currentUserProvider, categoryRepository, statusRepository,
                orgNodeRepository);
        userId = UUID.randomUUID();
        when(currentUserProvider.current()).thenReturn(new CurrentUser(userId, "im", Set.of("INVENTORY_MANAGER")));
    }

    @Test
    void create_requiresNameAndAtLeastOneFilter() {
        assertThatThrownBy(() -> service.create(" ", "laptops", null, null, null, null, null))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.create("Empty", null, null, null, null, null, null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_duplicateNamePerUserIsConflict() {
        when(repository.existsByUserIdAndNameIgnoreCase(userId, "IT in B")).thenReturn(true);

        assertThatThrownBy(() -> service.create("IT in B", "x", null, null, null, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_savesOwnRowWithTrimmedFields() {
        when(repository.existsByUserIdAndNameIgnoreCase(userId, "IT in B")).thenReturn(false);
        when(repository.save(any(SavedSearch.class))).thenAnswer(inv -> inv.getArgument(0));

        SavedSearch saved = service.create(" IT in B ", "  latitude  ", UUID.randomUUID(), null, null, null, null);

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getName()).isEqualTo("IT in B");
        assertThat(saved.getQuery()).isEqualTo("latitude");
        assertThat(saved.getCreatedBy()).isEqualTo(userId);
    }

    @Test
    void delete_someoneElsesIdIs404NotLeaked() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void resolve_dropsDeadReferencesWithNotesAndKeepsLiveClauses() {
        UUID id = UUID.randomUUID();
        UUID deadCategory = UUID.randomUUID();
        UUID liveStatus = UUID.randomUUID();
        SavedSearch search = new SavedSearch();
        search.setId(id);
        search.setUserId(userId);
        search.setName("Quarterly IT");
        search.setQuery("laptop");
        search.setCategoryId(deadCategory);
        search.setStatusId(liveStatus);
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(search));
        when(categoryRepository.existsById(deadCategory)).thenReturn(false);
        when(statusRepository.existsById(liveStatus)).thenReturn(true);

        SavedSearchService.ResolvedSavedSearch resolved = service.resolve(id);

        assertThat(resolved.categoryId()).isNull();
        assertThat(resolved.statusId()).isEqualTo(liveStatus);
        assertThat(resolved.query()).isEqualTo("laptop");
        assertThat(resolved.droppedFilterNotes()).hasSize(1);
        assertThat(resolved.droppedFilterNotes().get(0)).contains("Category filter dropped");
    }

    @Test
    void resolve_allReferencesAliveMeansNoNotes() {
        UUID id = UUID.randomUUID();
        SavedSearch search = new SavedSearch();
        search.setId(id);
        search.setUserId(userId);
        search.setName("Just text");
        search.setQuery("projector");
        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(search));

        SavedSearchService.ResolvedSavedSearch resolved = service.resolve(id);

        assertThat(resolved.droppedFilterNotes()).isEmpty();
        verify(repository).findByIdAndUserId(id, userId);
    }
}
