package com.iams.report.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetCategoryRepository;
import com.iams.asset.domain.AssetCustomFieldDefinition;
import com.iams.asset.domain.AssetCustomFieldDefinitionRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.org.domain.Person;
import com.iams.org.domain.PersonRepository;
import com.iams.report.domain.AdHocReport;
import com.iams.report.domain.AdHocReportRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-RPT-15: build-your-own report over the asset register. The field
 * catalog is server-defined (built-in asset fields plus every CURRENT custom
 * field definition), so "available fields" is always what the schema
 * actually offers; a saved definition validates against the catalog at save
 * time but degrades at run time - a field whose definition was removed later
 * is omitted with a note in the title, and a filter whose referenced entity
 * died is dropped with a note (the SavedSearch resolve() discipline), never
 * a hard failure. Definitions are own-rows-only like saved searches and
 * export jobs: another user's report id is a 404, existence never leaks.
 * Rows come from the same org-scoped search the asset register uses, so an
 * ad hoc report can never show more than its owner's foreground view.
 */
@Service
public class AdHocReportService {

    static final int MAX_FIELDS = 30;
    private static final int PAGE_SIZE = 500;
    private static final String CUSTOM_PREFIX = "custom:";

    /** One catalog field: how it's labeled and how a cell is extracted (person names pre-resolved in bulk). */
    private record FieldDef(String label, BiFunction<Asset, Map<UUID, String>, String> extractor) {
    }

    private static final Map<String, FieldDef> BUILT_INS = new LinkedHashMap<>();
    static {
        BUILT_INS.put("assetNumber", new FieldDef("Asset Number", (a, p) -> a.getAssetNumber()));
        BUILT_INS.put("name", new FieldDef("Name", (a, p) -> a.getName()));
        BUILT_INS.put("category", new FieldDef("Category", (a, p) -> a.getCategory().getName()));
        BUILT_INS.put("status", new FieldDef("Status", (a, p) -> a.getStatus().getLabel()));
        BUILT_INS.put("location", new FieldDef("Location", (a, p) -> a.getOrgNode().getName()));
        BUILT_INS.put("assignedTo", new FieldDef("Assigned To",
                (a, p) -> a.getAssignedToPersonId() == null ? "" : p.getOrDefault(a.getAssignedToPersonId(), "(unknown person)")));
        BUILT_INS.put("serialNumber", new FieldDef("Serial Number", (a, p) -> nullable(a.getSerialNumber())));
        BUILT_INS.put("manufacturer", new FieldDef("Manufacturer", (a, p) -> nullable(a.getManufacturer())));
        BUILT_INS.put("model", new FieldDef("Model", (a, p) -> nullable(a.getModel())));
        BUILT_INS.put("description", new FieldDef("Description", (a, p) -> nullable(a.getDescription())));
        BUILT_INS.put("vendorName", new FieldDef("Vendor", (a, p) -> nullable(a.getVendorName())));
        BUILT_INS.put("purchaseOrderReference", new FieldDef("PO Reference", (a, p) -> nullable(a.getPurchaseOrderReference())));
        BUILT_INS.put("purchaseDate", new FieldDef("Purchase Date", (a, p) -> nullable(a.getPurchaseDate())));
        BUILT_INS.put("purchaseCost", new FieldDef("Purchase Cost", (a, p) -> nullable(a.getPurchaseCost())));
        BUILT_INS.put("warrantyStartDate", new FieldDef("Warranty Start", (a, p) -> nullable(a.getWarrantyStartDate())));
        BUILT_INS.put("warrantyEndDate", new FieldDef("Warranty End", (a, p) -> nullable(a.getWarrantyEndDate())));
        BUILT_INS.put("rfidTagId", new FieldDef("RFID Tag", (a, p) -> nullable(a.getRfidTagId())));
        BUILT_INS.put("createdAt", new FieldDef("Registered At", (a, p) -> a.getCreatedAt().toString()));
    }

    private final AdHocReportRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final AssetRepository assetRepository;
    private final AssetCustomFieldDefinitionRepository customFieldRepository;
    private final AssetCategoryRepository categoryRepository;
    private final AssetStatusDefRepository statusRepository;
    private final OrgNodeRepository orgNodeRepository;
    private final PersonRepository personRepository;
    private final OrgScopeGuard scopeGuard;

    public AdHocReportService(AdHocReportRepository repository, CurrentUserProvider currentUserProvider,
                              AssetRepository assetRepository,
                              AssetCustomFieldDefinitionRepository customFieldRepository,
                              AssetCategoryRepository categoryRepository, AssetStatusDefRepository statusRepository,
                              OrgNodeRepository orgNodeRepository, PersonRepository personRepository,
                              OrgScopeGuard scopeGuard) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.assetRepository = assetRepository;
        this.customFieldRepository = customFieldRepository;
        this.categoryRepository = categoryRepository;
        this.statusRepository = statusRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.personRepository = personRepository;
        this.scopeGuard = scopeGuard;
    }

    public record FieldOption(String key, String label) {
    }

    /** The builder's palette: built-ins plus every current custom field definition (distinct by fieldKey). */
    @Transactional(readOnly = true)
    public List<FieldOption> availableFields() {
        List<FieldOption> options = new ArrayList<>();
        BUILT_INS.forEach((key, def) -> options.add(new FieldOption(key, def.label())));
        Set<String> seen = new HashSet<>();
        for (AssetCustomFieldDefinition def : customFieldRepository.findAll()) {
            if (seen.add(def.getFieldKey())) {
                options.add(new FieldOption(CUSTOM_PREFIX + def.getFieldKey(), def.getLabel() + " (custom)"));
            }
        }
        return options;
    }

    @Transactional
    public AdHocReport create(String name, List<String> fieldKeys, String query, UUID categoryId, UUID statusId,
                              UUID orgNodeId, LocalDate purchasedFrom, LocalDate purchasedTo) {
        UUID userId = currentUserProvider.current().id();
        if (name == null || name.isBlank()) {
            throw ValidationFailedException.singleField("name", "A name is required");
        }
        if (fieldKeys == null || fieldKeys.isEmpty()) {
            throw ValidationFailedException.singleField("fields", "Choose at least one field");
        }
        if (fieldKeys.size() > MAX_FIELDS) {
            throw ValidationFailedException.singleField("fields", "At most " + MAX_FIELDS + " fields");
        }
        // Save-time strictness, run-time leniency: you can't SAVE a field the
        // catalog doesn't currently offer - degradation is only for fields
        // that existed then and were removed later (the AC's exact case).
        Set<String> known = new HashSet<>();
        availableFields().forEach(o -> known.add(o.key()));
        List<String> unknown = fieldKeys.stream().filter(k -> !known.contains(k)).toList();
        if (!unknown.isEmpty()) {
            throw ValidationFailedException.singleField("fields", "Unknown field(s): " + String.join(", ", unknown));
        }
        if (purchasedFrom != null && purchasedTo != null && purchasedTo.isBefore(purchasedFrom)) {
            throw ValidationFailedException.singleField("purchasedTo", "Must not be before purchasedFrom");
        }
        if (repository.existsByUserIdAndNameIgnoreCase(userId, name.trim())) {
            throw new ConflictException("ADHOC_REPORT_NAME_TAKEN", "You already have a custom report with this name");
        }
        AdHocReport report = new AdHocReport();
        report.setUserId(userId);
        report.setName(name.trim());
        report.setFields(new ArrayList<>(fieldKeys.stream().distinct().toList()));
        report.setQuery(query != null && !query.isBlank() ? query.trim() : null);
        report.setCategoryId(categoryId);
        report.setStatusId(statusId);
        report.setOrgNodeId(orgNodeId);
        report.setPurchasedFrom(purchasedFrom);
        report.setPurchasedTo(purchasedTo);
        report.setCreatedBy(userId);
        return repository.save(report);
    }

    @Transactional(readOnly = true)
    public List<AdHocReport> listOwn() {
        return repository.findByUserIdOrderByNameAsc(currentUserProvider.current().id());
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(requireOwn(id));
    }

    /** Reruns against current data, degrading per the AC instead of failing. */
    @Transactional(readOnly = true)
    public TabularReport run(UUID id) {
        AdHocReport definition = requireOwn(id);
        List<String> notes = new ArrayList<>();

        // Resolve each saved field key against the CURRENT catalog; a dead one
        // (custom definition removed since save) is omitted with a note.
        Map<String, FieldDef> currentCustom = new LinkedHashMap<>();
        for (AssetCustomFieldDefinition def : customFieldRepository.findAll()) {
            currentCustom.putIfAbsent(CUSTOM_PREFIX + def.getFieldKey(),
                    new FieldDef(def.getLabel() + " (custom)",
                            (a, p) -> Objects.toString(a.getCustomAttributes().get(def.getFieldKey()), "")));
        }
        List<String> columns = new ArrayList<>();
        List<FieldDef> extractors = new ArrayList<>();
        for (String key : definition.getFields()) {
            FieldDef def = BUILT_INS.containsKey(key) ? BUILT_INS.get(key) : currentCustom.get(key);
            if (def == null) {
                notes.add("Column '" + key.replace(CUSTOM_PREFIX, "") + "' omitted - the field no longer exists");
                continue;
            }
            columns.add(def.label());
            extractors.add(def);
        }
        if (columns.isEmpty()) {
            // Every saved field is gone. Still not a hard failure: an empty,
            // clearly-annotated report states the situation better than a 500.
            columns.add("(no columns)");
        }

        // Filters degrade exactly like SavedSearchService.resolve().
        UUID categoryId = definition.getCategoryId();
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            notes.add("Category filter dropped - the saved category no longer exists");
            categoryId = null;
        }
        UUID statusId = definition.getStatusId();
        if (statusId != null && !statusRepository.existsById(statusId)) {
            notes.add("Status filter dropped - the saved status no longer exists");
            statusId = null;
        }
        UUID orgNodeId = definition.getOrgNodeId();
        String locationPrefix = null;
        if (orgNodeId != null) {
            var node = orgNodeRepository.findById(orgNodeId).orElse(null);
            if (node == null) {
                notes.add("Location filter dropped - the saved location no longer exists");
            } else {
                locationPrefix = node.getPath();
            }
        }

        List<Asset> assets = new ArrayList<>();
        Pageable page = PageRequest.of(0, PAGE_SIZE, Sort.by("assetNumber"));
        while (true) {
            var slice = assetRepository.search(categoryId, statusId, definition.getQuery(), locationPrefix,
                    scopeGuard.currentScopePathPrefix(), definition.getPurchasedFrom(), definition.getPurchasedTo(),
                    null, null, page);
            assets.addAll(slice.getContent());
            if (!slice.hasNext()) {
                break;
            }
            page = slice.nextPageable();
        }

        Map<UUID, String> personNames = resolvePersonNames(assets, extractors);
        List<List<String>> rows = assets.stream()
                .map(asset -> extractors.isEmpty() ? List.of("")
                        : extractors.stream().map(e -> e.extractor().apply(asset, personNames)).toList())
                .toList();

        String title = definition.getName()
                + (notes.isEmpty() ? "" : " — " + String.join("; ", notes));
        return new TabularReport("adhoc-" + definition.getId(), title, Instant.now(), columns, rows);
    }

    /** Bulk person lookup, only when the assignedTo column is actually selected - no per-row queries. */
    private Map<UUID, String> resolvePersonNames(List<Asset> assets, List<FieldDef> extractors) {
        boolean wantsAssignee = extractors.stream()
                .anyMatch(e -> e == BUILT_INS.get("assignedTo"));
        if (!wantsAssignee) {
            return Map.of();
        }
        Set<UUID> personIds = new HashSet<>();
        for (Asset asset : assets) {
            if (asset.getAssignedToPersonId() != null) {
                personIds.add(asset.getAssignedToPersonId());
            }
        }
        Map<UUID, String> names = new HashMap<>();
        for (Person person : personRepository.findAllById(personIds)) {
            names.put(person.getId(), person.getFullName());
        }
        return names;
    }

    private AdHocReport requireOwn(UUID id) {
        return repository.findByIdAndUserId(id, currentUserProvider.current().id())
                .orElseThrow(() -> NotFoundException.of("AdHocReport", id));
    }

    private static String nullable(Object value) {
        return Objects.toString(value, "");
    }
}
