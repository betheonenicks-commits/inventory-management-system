package com.iams.search.application;

import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.search.domain.SavedSearch;
import com.iams.search.domain.SavedSearchRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-SRC-04. Always operates on the calling user's own rows - the same
 * "no id parameter to abuse" shape as DashboardPreferenceService; another
 * user's saved-search id is a 404, not a 403, so existence never leaks.
 * <p>
 * resolve() is the AC's graceful-degradation half: each stored reference is
 * existence-checked, a dead one drops out of the effective filter set with a
 * human-readable note, and the remaining clauses still apply - never a hard
 * failure because a category died after the search was saved.
 */
@Service
public class SavedSearchService {

    private final SavedSearchRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final AssetCategoryRepository categoryRepository;
    private final AssetStatusDefRepository statusRepository;
    private final OrgNodeRepository orgNodeRepository;

    public SavedSearchService(SavedSearchRepository repository, CurrentUserProvider currentUserProvider,
                               AssetCategoryRepository categoryRepository, AssetStatusDefRepository statusRepository,
                               OrgNodeRepository orgNodeRepository) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
        this.orgNodeRepository = orgNodeRepository;
    }

    @Transactional
    public SavedSearch create(String name, String query, UUID categoryId, UUID statusId, UUID orgNodeId,
                               LocalDate purchasedFrom, LocalDate purchasedTo) {
        UUID userId = currentUserProvider.current().id();
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "A name is required");
        }
        boolean hasAnyFilter = (query != null && !query.isBlank()) || categoryId != null || statusId != null
                || orgNodeId != null || purchasedFrom != null || purchasedTo != null;
        if (!hasAnyFilter) {
            throw ValidationFailedException.singleField("filters", "At least one filter criterion is required");
        }
        if (purchasedFrom != null && purchasedTo != null && purchasedTo.isBefore(purchasedFrom)) {
            throw ValidationFailedException.singleField("purchasedTo", "Must not be before purchasedFrom");
        }
        if (repository.existsByUserIdAndNameIgnoreCase(userId, name.trim())) {
            throw new ConflictException("SAVED_SEARCH_NAME_TAKEN", "You already have a saved search with this name");
        }
        SavedSearch search = new SavedSearch();
        search.setUserId(userId);
        search.setName(name.trim());
        search.setQuery(query != null && !query.isBlank() ? query.trim() : null);
        search.setCategoryId(categoryId);
        search.setStatusId(statusId);
        search.setOrgNodeId(orgNodeId);
        search.setPurchasedFrom(purchasedFrom);
        search.setPurchasedTo(purchasedTo);
        search.setCreatedBy(userId);
        return repository.save(search);
    }

    @Transactional(readOnly = true)
    public List<SavedSearch> list() {
        return repository.findByUserIdOrderByNameAsc(currentUserProvider.current().id());
    }

    @Transactional
    public void delete(UUID id) {
        SavedSearch search = requireOwn(id);
        repository.delete(search);
    }

    /** The one-click re-apply: effective (still-valid) filters plus notes for anything dropped. */
    @Transactional(readOnly = true)
    public ResolvedSavedSearch resolve(UUID id) {
        SavedSearch search = requireOwn(id);
        List<String> notes = new ArrayList<>();
        UUID categoryId = search.getCategoryId();
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            notes.add("Category filter dropped - the saved category no longer exists");
            categoryId = null;
        }
        UUID statusId = search.getStatusId();
        if (statusId != null && !statusRepository.existsById(statusId)) {
            notes.add("Status filter dropped - the saved status no longer exists");
            statusId = null;
        }
        UUID orgNodeId = search.getOrgNodeId();
        if (orgNodeId != null && !orgNodeRepository.existsById(orgNodeId)) {
            notes.add("Location filter dropped - the saved location no longer exists");
            orgNodeId = null;
        }
        return new ResolvedSavedSearch(search.getId(), search.getName(), search.getQuery(), categoryId, statusId,
                orgNodeId, search.getPurchasedFrom(), search.getPurchasedTo(), notes);
    }

    private SavedSearch requireOwn(UUID id) {
        return repository.findByIdAndUserId(id, currentUserProvider.current().id())
                .orElseThrow(() -> NotFoundException.of("SavedSearch", id));
    }

    public record ResolvedSavedSearch(UUID id, String name, String query, UUID categoryId, UUID statusId,
                                       UUID orgNodeId, LocalDate purchasedFrom, LocalDate purchasedTo,
                                       List<String> droppedFilterNotes) {
    }
}
