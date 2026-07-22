package com.iams.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.inventory.domain.Vendor;
import com.iams.inventory.domain.VendorRepository;
import com.iams.org.application.PersonService;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.Person;
import com.iams.usr.application.OrgScopeGuard;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock private AssetRepository assetRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private PersonService personService;
    @Mock private OrgScopeGuard scopeGuard;
    @Mock private CurrentUserProvider currentUserProvider;

    private SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(assetRepository, vendorRepository, personService, scopeGuard, currentUserProvider);
    }

    @Test
    void global_blankTermIsRejectedBeforeAnyQueryRuns() {
        assertThatThrownBy(() -> service.global("  ")).isInstanceOf(ValidationFailedException.class);
        verifyNoInteractions(assetRepository, vendorRepository, personService);
    }

    @Test
    void global_vendorGroupOnlySearchedWhenCallerHoldsInventoryRead() {
        // A Viewer (dashboards:read + reports:read) can see people (reports:read gates the
        // employee-assets picker) but not assets or vendors.
        when(personService.list("acme")).thenReturn(List.of());
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "viewer",
                Set.of("VIEWER"), Set.of("dashboards:read", "reports:read")));

        SearchService.GlobalSearchResult result = service.global("acme");

        assertThat(result.vendorsSearched()).isFalse();
        assertThat(result.vendors()).isEmpty();
        assertThat(result.assets()).isEmpty();
        verifyNoInteractions(vendorRepository, assetRepository);
    }

    @Test
    void global_groupsPopulateForAPermittedCaller() {
        when(scopeGuard.currentScopePathPrefix()).thenReturn("/root/bldg-b");
        when(assetRepository.search(any(), any(), eq("lat"), any(), eq("/root/bldg-b"), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(asset("AST-1", "Latitude"))));
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Latitude Supplies");
        vendor.setActive(true);
        when(vendorRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc("lat")).thenReturn(List.of(vendor));
        Person person = new Person();
        person.setId(UUID.randomUUID());
        person.setFullName("Latha K");
        when(personService.list("lat")).thenReturn(List.of(person));
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "im",
                Set.of("INVENTORY_MANAGER"),
                Set.of("assets:read", "assets:write", "inventory:read", "reports:read")));

        SearchService.GlobalSearchResult result = service.global("lat");

        assertThat(result.assets()).extracting(SearchService.AssetHit::name).containsExactly("Latitude");
        assertThat(result.vendorsSearched()).isTrue();
        assertThat(result.vendors()).extracting(SearchService.VendorHit::name).containsExactly("Latitude Supplies");
        assertThat(result.people()).extracting(SearchService.PersonHit::fullName).containsExactly("Latha K");
    }

    @Test
    void global_systemOperatorGetsEmptyResultsAndTouchesNoBusinessData() {
        // US-USR-05 (AC-USR-05-X): SYSTEM_OPERATOR holds only system:read/write, so search
        // must not be a side door to asset valuations or person PII - every group is empty and
        // no asset/vendor/person repository is queried at all.
        when(currentUserProvider.current()).thenReturn(new CurrentUser(UUID.randomUUID(), "sysop",
                Set.of("SYSTEM_OPERATOR"), Set.of("system:read", "system:write")));

        SearchService.GlobalSearchResult result = service.global("anything");

        assertThat(result.assets()).isEmpty();
        assertThat(result.vendors()).isEmpty();
        assertThat(result.people()).isEmpty();
        verifyNoInteractions(assetRepository, vendorRepository, personService);
    }

    @Test
    void byCode_unknownCodeIs404AndBlankIs400() {
        when(assetRepository.findByAnyCodeWithAssociations("AST-9999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.byCode("AST-9999")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.byCode(" ")).isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void byCode_outOfScopeHitIsRefusedNotHidden() {
        Asset asset = asset("AST-5", "Projector");
        when(assetRepository.findByAnyCodeWithAssociations("AST-5")).thenReturn(Optional.of(asset));
        org.mockito.Mockito.doThrow(new AccessDeniedException("outside scope"))
                .when(scopeGuard).requireWithinScope(any(), eq("asset"), any());

        assertThatThrownBy(() -> service.byCode("AST-5")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void byCode_trimsScannerWhitespace() {
        Asset asset = asset("AST-5", "Projector");
        when(assetRepository.findByAnyCodeWithAssociations("AST-5")).thenReturn(Optional.of(asset));

        assertThat(service.byCode(" AST-5 ").assetNumber()).isEqualTo("AST-5");
        verify(assetRepository).findByAnyCodeWithAssociations("AST-5");
    }

    private static Asset asset(String number, String name) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetNumber(number);
        asset.setName(name);
        AssetCategory category = new AssetCategory();
        category.setName("IT");
        asset.setCategory(category);
        AssetStatusDef status = new AssetStatusDef();
        status.setLabel("In Use");
        asset.setStatus(status);
        OrgNode node = new OrgNode();
        node.setId(UUID.randomUUID());
        node.setName("HQ");
        asset.setOrgNode(node);
        return asset;
    }
}
