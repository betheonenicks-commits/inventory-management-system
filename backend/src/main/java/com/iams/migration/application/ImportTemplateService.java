package com.iams.migration.application;

import com.iams.common.exception.ValidationFailedException;
import com.iams.migration.domain.ImportEntityType;
import com.iams.report.application.CsvExporter;
import com.iams.report.application.TabularReport;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * US-MIG-01: downloadable import templates whose columns match exactly what the
 * dry-run validator checks. The column list is not re-declared here - it is read
 * from the same {@link AssetImportProcessor#COLUMNS} the validator uses, so the
 * two cannot diverge. The rendered file carries one illustrative data row and the
 * template version is served in the download filename ("...v1.0.csv") so a
 * superseded template is recognisable (AC-MIG-01 second clause).
 */
@Service
public class ImportTemplateService {

    private final CsvExporter csvExporter;

    public ImportTemplateService(CsvExporter csvExporter) {
        this.csvExporter = csvExporter;
    }

    public record Template(String filename, String version, byte[] content) {
    }

    public Template generate(ImportEntityType entityType) {
        if (entityType != ImportEntityType.ASSET) {
            // The template surface names four entity types; only ASSET is executable in
            // this first slice. Refuse clearly rather than hand back an empty file.
            throw ValidationFailedException.singleField("entityType",
                    entityType + " import is not available yet - only ASSET is supported in this release");
        }
        TabularReport report = new TabularReport(
                "asset-import-template",
                "Asset Import Template",
                Instant.now(),
                AssetImportProcessor.COLUMNS,
                List.of(AssetImportProcessor.SAMPLE_ROW));
        byte[] content = csvExporter.export(report);
        String version = AssetImportProcessor.TEMPLATE_VERSION;
        return new Template("asset-import-template-v" + version + ".csv", version, content);
    }
}
