package com.iams.audit.api;

import com.iams.audit.api.dto.AuditAssignmentRequest;
import com.iams.audit.api.dto.AuditAssignmentResponse;
import com.iams.audit.api.dto.AuditBatchScanRequest;
import com.iams.audit.api.dto.AuditBatchScanResponse;
import com.iams.audit.api.dto.AuditCertificateResponse;
import com.iams.audit.api.dto.AuditCorrectionRequest;
import com.iams.audit.api.dto.AuditCreateRequest;
import com.iams.audit.api.dto.AuditDashboardItemResponse;
import com.iams.audit.api.dto.AuditExceptionReportResponse;
import com.iams.audit.api.dto.AuditFindingReconciliationResponse;
import com.iams.audit.api.dto.AuditFindingResponse;
import com.iams.audit.api.dto.AuditProgressResponse;
import com.iams.audit.api.dto.AuditReconciliationRequest;
import com.iams.audit.api.dto.AuditRejectRequest;
import com.iams.audit.api.dto.AuditResponse;
import com.iams.audit.api.dto.AuditScanRequest;
import com.iams.audit.api.dto.AuditSubmitRequest;
import com.iams.audit.api.dto.AuditSummaryResponse;
import com.iams.audit.api.mapper.AuditMapper;
import com.iams.audit.application.AuditCreateCommand;
import com.iams.audit.application.AuditFindingCorrectionService;
import com.iams.audit.application.AuditReconciliationService;
import com.iams.audit.application.AuditReportService;
import com.iams.audit.application.AuditScanCommand;
import com.iams.audit.application.AuditScanService;
import com.iams.audit.application.AuditService;
import com.iams.audit.application.AuditWorkflowService;
import com.iams.audit.domain.Audit;
import com.iams.audit.domain.AuditAssignment;
import com.iams.audit.domain.AuditFinding;
import com.iams.audit.domain.AuditStatus;
import com.iams.storage.api.AttachmentResponse;
import com.iams.storage.application.AttachmentService;
import com.iams.storage.domain.Attachment;
import com.iams.storage.domain.AttachmentOwnerType;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * EPIC-AUD: physical audit management
 * (US-AUD-01/02/03/04/05/07/08/09/10/12/13/14/15/16/17/21/22/23/24).
 * See DEVELOPMENT_LOG.md for what remains Partial or Not-started (photo
 * evidence, offline sync, statistical sampling, cross-cycle analytics,
 * continuous-scan mode as a dedicated UX affordance).
 */
@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    private final AuditService auditService;
    private final AuditScanService scanService;
    private final AuditWorkflowService workflowService;
    private final AuditFindingCorrectionService correctionService;
    private final AuditReportService reportService;
    private final AuditReconciliationService reconciliationService;
    private final AttachmentService attachmentService;
    private final AuditMapper mapper;

    public AuditController(AuditService auditService, AuditScanService scanService, AuditWorkflowService workflowService,
                            AuditFindingCorrectionService correctionService, AuditReportService reportService,
                            AuditReconciliationService reconciliationService, AttachmentService attachmentService,
                            AuditMapper mapper) {
        this.auditService = auditService;
        this.scanService = scanService;
        this.workflowService = workflowService;
        this.correctionService = correctionService;
        this.reportService = reportService;
        this.reconciliationService = reconciliationService;
        this.attachmentService = attachmentService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('audits:write')")
    public ResponseEntity<AuditResponse> create(@Valid @RequestBody AuditCreateRequest request) {
        Audit audit = auditService.create(new AuditCreateCommand(request.name(), request.auditType(),
                request.scopeOrgNodeId(), request.scopeCategoryId(), request.assetIds(), request.nominalApproverId(),
                request.scheduledDate()));
        return ResponseEntity.created(URI.create("/api/v1/audits/" + audit.getId())).body(mapper.toResponse(audit));
    }

    @GetMapping
    @PreAuthorize("@perm.has('audits:read')")
    public List<AuditResponse> list(@RequestParam(required = false) AuditStatus status) {
        return auditService.list(status).stream().map(mapper::toResponse).toList();
    }

    /**
     * Any authenticated user - not just audits:read holders - needs to name an audit
     * as a legal-hold scope target (US-CMP-06), mirroring UserController.pickable().
     * Returns only id/name, never the sensitive fields on AuditResponse.
     */
    @GetMapping("/pickable")
    public List<AuditSummaryResponse> pickable() {
        return auditService.list(null).stream().map(mapper::toSummary).toList();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@perm.has('audits:read')")
    public List<AuditDashboardItemResponse> dashboard() {
        return reportService.dashboard().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('audits:read')")
    public AuditResponse get(@PathVariable UUID id) {
        return mapper.toResponse(auditService.get(id));
    }

    @GetMapping("/{id}/progress")
    @PreAuthorize("@perm.has('audits:read')")
    public AuditProgressResponse progress(@PathVariable UUID id) {
        return mapper.toResponse(auditService.progress(id));
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("@perm.has('audits:write')")
    public ResponseEntity<AuditAssignmentResponse> assign(@PathVariable UUID id, @Valid @RequestBody AuditAssignmentRequest request) {
        AuditAssignment assignment = auditService.assignAuditor(id, request.auditorUserId(), request.subScope());
        return ResponseEntity.created(URI.create("/api/v1/audits/" + id + "/assignments/" + assignment.getId()))
                .body(mapper.toResponse(assignment));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("@perm.has('audits:read')")
    public List<AuditAssignmentResponse> assignments(@PathVariable UUID id) {
        return auditService.assignments(id).stream().map(mapper::toResponse).toList();
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @PreAuthorize("@perm.has('audits:write')")
    public AuditAssignmentResponse unassign(@PathVariable UUID id, @PathVariable UUID assignmentId) {
        return mapper.toResponse(auditService.unassignAuditor(id, assignmentId));
    }

    @PostMapping("/{id}/scans")
    @PreAuthorize("@perm.has('audits:write')")
    public ResponseEntity<AuditFindingResponse> scan(@PathVariable UUID id, @Valid @RequestBody AuditScanRequest request) {
        AuditFinding finding = scanService.recordScan(id, toCommand(request));
        AuditFindingResponse response = mapper.toResponse(finding, correctionService.corrections(finding.getId()));
        return ResponseEntity.created(URI.create("/api/v1/audits/" + id + "/findings/" + finding.getId())).body(response);
    }

    @PostMapping("/{id}/scans/batch")
    @PreAuthorize("@perm.has('audits:write')")
    public AuditBatchScanResponse scanBatch(@PathVariable UUID id, @Valid @RequestBody AuditBatchScanRequest request) {
        List<AuditScanCommand> commands = request.scans().stream().map(this::toCommand).toList();
        return mapper.toBatchResponse(scanService.recordBatchScan(id, commands), correctionService::corrections);
    }

    @GetMapping("/{id}/findings/{findingId}")
    @PreAuthorize("@perm.has('audits:read')")
    public AuditFindingResponse getFinding(@PathVariable UUID id, @PathVariable UUID findingId) {
        AuditFinding finding = correctionService.getFinding(id, findingId);
        return mapper.toResponse(finding, correctionService.corrections(findingId), reconciliationService.forFinding(findingId));
    }

    @PostMapping("/{id}/findings/{findingId}/corrections")
    @PreAuthorize("@perm.has('audits:write')")
    public AuditFindingResponse correctFinding(@PathVariable UUID id, @PathVariable UUID findingId,
                                                @Valid @RequestBody AuditCorrectionRequest request) {
        correctionService.correct(id, findingId, request.fieldName(), request.newValue());
        AuditFinding finding = correctionService.getFinding(id, findingId);
        return mapper.toResponse(finding, correctionService.corrections(findingId), reconciliationService.forFinding(findingId));
    }

    // US-AUD-11: photo evidence, brokered through the backend (US-PLAT-02).
    // The getFinding() call is the authorization anchor: 404 unless the
    // finding exists and belongs to this audit, same as corrections.
    @PostMapping("/{id}/findings/{findingId}/evidence")
    @PreAuthorize("@perm.has('audits:write')")
    public ResponseEntity<AttachmentResponse> uploadEvidence(@PathVariable UUID id, @PathVariable UUID findingId,
            @RequestParam("file") MultipartFile file) {
        AuditFinding finding = correctionService.getFinding(id, findingId);
        Attachment stored = attachmentService.storeImage(AttachmentOwnerType.AUDIT_FINDING, finding.getId(), file);
        return ResponseEntity
                .created(URI.create("/api/v1/audits/" + id + "/findings/" + findingId + "/evidence/" + stored.getId()))
                .body(AttachmentResponse.from(stored));
    }

    @GetMapping("/{id}/findings/{findingId}/evidence")
    @PreAuthorize("@perm.has('audits:read')")
    public List<AttachmentResponse> listEvidence(@PathVariable UUID id, @PathVariable UUID findingId) {
        AuditFinding finding = correctionService.getFinding(id, findingId);
        return attachmentService.listFor(AttachmentOwnerType.AUDIT_FINDING, finding.getId()).stream()
                .map(AttachmentResponse::from).toList();
    }

    @GetMapping("/{id}/findings/{findingId}/evidence/{attachmentId}")
    @PreAuthorize("@perm.has('audits:read')")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable UUID id, @PathVariable UUID findingId,
            @PathVariable UUID attachmentId) {
        AuditFinding finding = correctionService.getFinding(id, findingId);
        AttachmentService.StoredAttachment stored =
                attachmentService.load(AttachmentOwnerType.AUDIT_FINDING, finding.getId(), attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.metadata().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(stored.metadata().getFileName()).build().toString())
                .body(stored.content());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@perm.has('audits:write')")
    public AuditResponse submit(@PathVariable UUID id, @Valid @RequestBody AuditSubmitRequest request) {
        return mapper.toResponse(workflowService.submit(id, request.password(), request.signatureName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@perm.has('approvals:write')")
    public AuditResponse approve(@PathVariable UUID id) {
        return mapper.toResponse(workflowService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@perm.has('approvals:write')")
    public AuditResponse reject(@PathVariable UUID id, @Valid @RequestBody AuditRejectRequest request) {
        return mapper.toResponse(workflowService.reject(id, request.reason()));
    }

    /** US-AUD-14: escalate a pending approval that's sat untouched past the configured threshold. */
    @PostMapping("/{id}/escalate")
    @PreAuthorize("@perm.has('approvals:write')")
    public AuditResponse escalate(@PathVariable UUID id) {
        return mapper.toResponse(workflowService.escalate(id));
    }

    /** US-AUD-21: reconcile a Missing finding found later, outside any active audit - a new linked record, never an edit. */
    @PostMapping("/{id}/findings/{findingId}/reconcile")
    @PreAuthorize("@perm.has('audits:write')")
    public AuditFindingReconciliationResponse reconcile(@PathVariable UUID id, @PathVariable UUID findingId,
                                                          @Valid @RequestBody AuditReconciliationRequest request) {
        return mapper.toResponse(reconciliationService.reconcile(id, findingId, request.foundLocationNote()));
    }

    @GetMapping("/{id}/exceptions")
    @PreAuthorize("@perm.has('audits:read')")
    public AuditExceptionReportResponse exceptions(@PathVariable UUID id) {
        return mapper.toExceptionReport(id, reportService.exceptions(id), correctionService::corrections, reconciliationService::forFinding);
    }

    @GetMapping("/{id}/certificate")
    @PreAuthorize("@perm.has('audits:read')")
    public AuditCertificateResponse certificate(@PathVariable UUID id) {
        return mapper.toResponse(reportService.certificate(id));
    }

    private AuditScanCommand toCommand(AuditScanRequest request) {
        return new AuditScanCommand(request.assetId(), request.condition(), request.remarks(), request.deviceId());
    }
}
