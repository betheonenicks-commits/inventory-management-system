package com.iams.migration.application;

import com.iams.common.exception.ValidationFailedException;
import com.iams.migration.domain.ImportEntityType;
import com.iams.report.application.CsvExporter;
import com.iams.report.application.TabularReport;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * US-MIG-01: downloadable import templates whose columns match exactly what the
 * dry-run validator checks - guaranteed because the columns come from the same
 * {@link EntityImportProcessor} the validator uses, never a second declaration.
 * The rendered file carries one illustrative data row, and the version is served
 * in the download filename ("...v1.0.csv") so a superseded template is
 * recognisable (AC-MIG-01 second clause).
 */
@Service
public class ImportTemplateService {

    private final CsvExporter csvExporter;
    private final Map<ImportEntityType, EntityImportProcessor> processors;

    public ImportTemplateService(CsvExporter csvExporter, List<EntityImportProcessor> processorBeans) {
        this.csvExporter = csvExporter;
        this.processors = new EnumMap<>(ImportEntityType.class);
        for (EntityImportProcessor processor : processorBeans) {
            this.processors.put(processor.entityType(), processor);
        }
    }

    public record Template(String filename, String version, byte[] content) {
    }

    public Template generate(ImportEntityType entityType) {
        EntityImportProcessor processor = processors.get(entityType);
        if (processor == null) {
            // The template surface names four entity types; refuse the ones with no
            // importer yet, clearly, rather than hand back an empty file.
            throw ValidationFailedException.singleField("entityType",
                    entityType + " import is not available yet - no importer is registered for it in this release");
        }
        TabularReport report = new TabularReport(
                entityType.name().toLowerCase() + "-import-template",
                entityType.name() + " Import Template",
                Instant.now(),
                processor.columns(),
                List.of(processor.sampleRow()));
        byte[] content = csvExporter.export(report);
        String version = processor.templateVersion();
        return new Template(entityType.name().toLowerCase() + "-import-template-v" + version + ".csv", version, content);
    }
}
