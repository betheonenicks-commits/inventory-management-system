package com.iams.search.api;

import com.iams.search.application.SavedSearchService;
import com.iams.search.domain.SavedSearch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-SRC-04. No @PreAuthorize - personal config over the ungated asset
 * search, own-rows-only enforced in the service (same reasoning as
 * DashboardController's preferences endpoints).
 */
@RestController
@RequestMapping("/api/v1/saved-searches")
public class SavedSearchController {

    private final SavedSearchService service;

    public SavedSearchController(SavedSearchService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SavedSearchResponse> create(@Valid @RequestBody SavedSearchCreateRequest request) {
        SavedSearch search = service.create(request.name(), request.query(), request.categoryId(),
                request.statusId(), request.orgNodeId(), request.purchasedFrom(), request.purchasedTo());
        return ResponseEntity.created(URI.create("/api/v1/saved-searches/" + search.getId()))
                .body(toResponse(search));
    }

    @GetMapping
    public List<SavedSearchResponse> list() {
        return service.list().stream().map(SavedSearchController::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/resolved")
    public SavedSearchService.ResolvedSavedSearch resolve(@PathVariable UUID id) {
        return service.resolve(id);
    }

    private static SavedSearchResponse toResponse(SavedSearch s) {
        return new SavedSearchResponse(s.getId(), s.getName(), s.getQuery(), s.getCategoryId(), s.getStatusId(),
                s.getOrgNodeId(), s.getPurchasedFrom(), s.getPurchasedTo());
    }

    // Size caps mirror the V41 column widths - without them an oversized value
    // reaches the DB and surfaces as a 500 (found by this epic's gate, fixed here).
    public record SavedSearchCreateRequest(@NotBlank @Size(max = 120) String name,
                                            @Size(max = 255) String query, UUID categoryId, UUID statusId,
                                            UUID orgNodeId, LocalDate purchasedFrom, LocalDate purchasedTo) {
    }

    public record SavedSearchResponse(UUID id, String name, String query, UUID categoryId, UUID statusId,
                                       UUID orgNodeId, LocalDate purchasedFrom, LocalDate purchasedTo) {
    }
}
