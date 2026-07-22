package com.iams.migration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.iams.inventory.application.VendorService;
import com.iams.migration.domain.ImportEntityType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorImportProcessorTest {

    @Mock private VendorService vendorService;

    @Test
    void metadataIsVendorScoped() {
        VendorImportProcessor processor = new VendorImportProcessor(vendorService);
        assertThat(processor.entityType()).isEqualTo(ImportEntityType.VENDOR);
        assertThat(processor.columns()).containsExactly("name", "contactEmail", "contactPhone");
        assertThat(processor.requiredColumns()).containsExactly("name");
        assertThat(processor.sampleRow()).hasSize(3);
    }

    @Test
    void validate_delegatesTrimmedNameToVendorService() {
        VendorImportProcessor processor = new VendorImportProcessor(vendorService);
        processor.validate(Map.of("name", "  Dell Direct  "));
        verify(vendorService).validate("Dell Direct");
    }

    @Test
    void create_delegatesTrimmedFieldsToVendorService() {
        VendorImportProcessor processor = new VendorImportProcessor(vendorService);
        processor.create(Map.of("name", " Acme ", "contactEmail", " orders@acme.example ", "contactPhone", " +1-555 "));
        verify(vendorService).create("Acme", "orders@acme.example", "+1-555");
    }

    @Test
    void create_blankOptionalFieldsBecomeNull() {
        VendorImportProcessor processor = new VendorImportProcessor(vendorService);
        processor.create(Map.of("name", "Acme", "contactEmail", "", "contactPhone", "   "));
        verify(vendorService).create("Acme", null, null);
    }
}
