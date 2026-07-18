package com.iams.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetCustomFieldDefinition;
import com.iams.asset.domain.AssetCustomFieldDefinitionRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.report.domain.AdHocReport;
import com.iams.report.domain.AdHocReportRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class AdHocReportServiceTest {

    @Mock private AdHocReportRepository repository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AssetRepository assetRepository;
    @Mock private AssetCustomFieldDefinitionRepository customFieldRepository;
    @Mock private AssetCategoryRepository categoryRepository;
    @Mock private AssetStatusDefRepository statusRepository;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private PersonRepository personRepository;
    @Mock private OrgScopeGuard scopeGuard;

    private AdHocReportService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new AdHocReportService(repository, currentUserProvider, assetRepository, customFieldRepository,
                categoryRepository, statusRepository, orgNodeRepository, personRepository, scopeGuard);
        userId = UUID.randomUUID();
        lenient().when(currentUserProvider.current())
                .thenReturn(new CurrentUser(userId, "im", Set.of("INVENTORY_MANAGER"), Set.of()));
        lenient().when(customFieldRepository.findAll()).thenReturn(List.of());
        lenient().when(scopeGuard.currentScopePathPrefix()).thenReturn(null);
        lenient().when(repository.save(any(AdHocReport.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AssetCustomFieldDefinition customField(String key, String label) {
        AssetCustomFieldDefinition def = new AssetCustomFieldDefinition();
        def.setFieldKey(key);
        def.setLabel(label);
        return def;
    }

    private Asset asset(String number, String name) {
        Asset asset = new Asset();
        asset.setAssetNumber(number);
        asset.setName(name);
        AssetCategory category = new AssetCategory();
        category.setName("Laptops");
        asset.setCategory(category);
        AssetStatusDef status = new AssetStatusDef();
        status.setLabel("In Use");
        asset.setStatus(status);
        OrgNode node = new OrgNode();
        node.setName("Library");
        asset.setOrgNode(node);
        return asset;
    }

    private AdHocReport ownDefinition(List<String> fields) {
        AdHocReport definition = new AdHocReport();
        definition.setId(UUID.randomUUID());
        definition.setUserId(userId);
        definition.setName("My Report");
        definition.setFields(new ArrayList<>(fields));
        lenient().when(repository.findByIdAndUserId(definition.getId(), userId)).thenReturn(Optional.of(definition));
        return definition;
    }

    @Test
    void create_rejectsFieldTheCatalogDoesNotOffer() {
        assertThatThrownBy(() -> service.create("r", List.of("assetNumber", "nosuchfield"), null, null, null, null,
                null, null))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("nosuchfield");
    }

    @Test
    void create_rejectsEmptyFieldSelection() {
        assertThatThrownBy(() -> service.create("r", List.of(), null, null, null, null, null, null))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void create_duplicateNamePerUserIsAConflict() {
        when(repository.existsByUserIdAndNameIgnoreCase(eq(userId), anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.create("Mine", List.of("assetNumber"), null, null, null, null, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_acceptsACurrentCustomFieldAndTrimsName() {
        when(customFieldRepository.findAll()).thenReturn(List.of(customField("warrantyProvider", "Warranty Provider")));
        when(repository.existsByUserIdAndNameIgnoreCase(eq(userId), anyString())).thenReturn(false);

        AdHocReport saved = service.create("  Laptops by provider  ",
                List.of("assetNumber", "custom:warrantyProvider"), null, null, null, null, null, null);

        assertThat(saved.getName()).isEqualTo("Laptops by provider");
        assertThat(saved.getFields()).containsExactly("assetNumber", "custom:warrantyProvider");
        assertThat(saved.getUserId()).isEqualTo(userId);
    }

    @Test
    void run_rendersColumnsInSavedOrder_includingCustomValuesAndAssignee() {
        AdHocReport definition = ownDefinition(List.of("assetNumber", "assignedTo", "custom:warrantyProvider"));
        when(customFieldRepository.findAll()).thenReturn(List.of(customField("warrantyProvider", "Warranty Provider")));

        Asset asset = asset("A-1", "Laptop");
        asset.getCustomAttributes().put("warrantyProvider", "Acme Cover");
        UUID personId = UUID.randomUUID();
        asset.setAssignedToPersonId(personId);
        Person person = new Person();
        person.setId(personId);
        person.setFullName("Jo Smith");
        when(assetRepository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(asset)));
        when(personRepository.findAllById(any())).thenReturn(List.of(person));

        TabularReport report = service.run(definition.getId());

        assertThat(report.columns()).containsExactly("Asset Number", "Assigned To", "Warranty Provider (custom)");
        assertThat(report.rows()).containsExactly(List.of("A-1", "Jo Smith", "Acme Cover"));
        assertThat(report.title()).isEqualTo("My Report");
    }

    @Test
    void run_removedCustomField_isOmittedWithANote_neverAHardFailure() {
        AdHocReport definition = ownDefinition(List.of("assetNumber", "custom:goneField"));
        // No definitions exist anymore - the field was removed after saving.
        when(assetRepository.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(asset("A-1", "Laptop"))));

        TabularReport report = service.run(definition.getId());

        assertThat(report.columns()).containsExactly("Asset Number");
        assertThat(report.rows()).containsExactly(List.of("A-1"));
        assertThat(report.title()).contains("goneField").contains("omitted");
    }

    @Test
    void run_deadCategoryFilter_isDroppedWithANoteAndTheReportStillRuns() {
        AdHocReport definition = ownDefinition(List.of("assetNumber"));
        UUID deadCategory = UUID.randomUUID();
        definition.setCategoryId(deadCategory);
        when(categoryRepository.existsById(deadCategory)).thenReturn(false);
        when(assetRepository.search(isNull(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(asset("A-1", "Laptop"))));

        TabularReport report = service.run(definition.getId());

        // The dead filter must reach the search as null, not the dead id.
        verify(assetRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
        assertThat(report.title()).contains("Category filter dropped");
        assertThat(report.rows()).hasSize(1);
    }

    @Test
    void run_anotherUsersReportId_is404() {
        UUID foreignId = UUID.randomUUID();
        when(repository.findByIdAndUserId(foreignId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.run(foreignId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void availableFields_offersBuiltInsPlusDistinctCustomFields() {
        when(customFieldRepository.findAll()).thenReturn(List.of(
                customField("warrantyProvider", "Warranty Provider"),
                customField("warrantyProvider", "Duplicate Across Categories"),
                customField("engineHours", "Engine Hours")));

        List<AdHocReportService.FieldOption> fields = service.availableFields();

        assertThat(fields).extracting(AdHocReportService.FieldOption::key)
                .contains("assetNumber", "name", "assignedTo", "custom:warrantyProvider", "custom:engineHours");
        assertThat(fields).extracting(AdHocReportService.FieldOption::key)
                .filteredOn(k -> k.equals("custom:warrantyProvider")).hasSize(1);
    }
}
