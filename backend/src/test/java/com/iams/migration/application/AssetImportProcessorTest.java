package com.iams.migration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iams.asset.application.AssetRegisterCommand;
import com.iams.asset.application.AssetRegistrationService;
import com.iams.asset.domain.AssetCategory;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.common.exception.ValidationFailedException;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetImportProcessorTest {

    @Mock private AssetCategoryRepository categoryRepository;
    @Mock private OrgNodeRepository orgNodeRepository;
    @Mock private AssetRegistrationService assetRegistrationService;

    private AssetImportProcessor processor;
    private final UUID categoryId = UUID.randomUUID();
    private final UUID orgNodeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        processor = new AssetImportProcessor(categoryRepository, orgNodeRepository, assetRegistrationService);
    }

    private Map<String, String> baseRow() {
        Map<String, String> row = new HashMap<>();
        row.put("name", "Dell Latitude");
        row.put("categoryCode", "IT-LAPTOP");
        return row;
    }

    private void stubCategory() {
        AssetCategory category = mock(AssetCategory.class);
        when(category.getId()).thenReturn(categoryId);
        when(categoryRepository.findByCode("IT-LAPTOP")).thenReturn(Optional.of(category));
    }

    @Test
    void buildCommand_resolvesCodesAndParsesTypes() {
        stubCategory();
        OrgNode node = mock(OrgNode.class);
        when(node.getId()).thenReturn(orgNodeId);
        when(orgNodeRepository.findByCode("ROOM-101")).thenReturn(Optional.of(node));

        Map<String, String> row = baseRow();
        row.put("orgNodeCode", "ROOM-101");
        row.put("purchaseDate", "2026-01-15");
        row.put("purchaseCost", "1299.50");

        AssetRegisterCommand command = processor.buildCommand(row);

        assertThat(command.categoryId()).isEqualTo(categoryId);
        assertThat(command.orgNodeId()).isEqualTo(orgNodeId);
        assertThat(command.name()).isEqualTo("Dell Latitude");
        assertThat(command.purchaseDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(command.purchaseCost()).isEqualByComparingTo(new BigDecimal("1299.50"));
    }

    @Test
    void buildCommand_blankCategoryLeavesIdNull_soRequiredCheckOwnsIt() {
        Map<String, String> row = baseRow();
        row.put("categoryCode", "");
        assertThat(processor.buildCommand(row).categoryId()).isNull();
    }

    @Test
    void buildCommand_rejectsUnknownCategoryCode() {
        when(categoryRepository.findByCode("NOPE")).thenReturn(Optional.empty());
        Map<String, String> row = baseRow();
        row.put("categoryCode", "NOPE");
        assertThatThrownBy(() -> processor.buildCommand(row))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("categoryCode");
    }

    @Test
    void buildCommand_rejectsUnknownOrgNodeCode() {
        stubCategory();
        when(orgNodeRepository.findByCode("GHOST")).thenReturn(Optional.empty());
        Map<String, String> row = baseRow();
        row.put("orgNodeCode", "GHOST");
        assertThatThrownBy(() -> processor.buildCommand(row))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("orgNodeCode");
    }

    @Test
    void buildCommand_rejectsMalformedDate() {
        stubCategory();
        Map<String, String> row = baseRow();
        row.put("purchaseDate", "15/01/2026");
        assertThatThrownBy(() -> processor.buildCommand(row))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("purchaseDate");
    }

    @Test
    void buildCommand_rejectsMalformedCost() {
        stubCategory();
        Map<String, String> row = baseRow();
        row.put("purchaseCost", "not-a-number");
        assertThatThrownBy(() -> processor.buildCommand(row))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("purchaseCost");
    }
}
