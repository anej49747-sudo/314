package csit314.renovation.dispute.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.company.repository.DesignerCompanyRepository;
import csit314.renovation.dispute.dto.DesignerReportPageResponse;
import csit314.renovation.dispute.dto.DesignerReportResponse;
import csit314.renovation.dispute.dto.ResolveDesignerReportRequest;
import csit314.renovation.dispute.dto.SubmitDesignerReportRequest;
import csit314.renovation.dispute.entity.DesignerReport;
import csit314.renovation.dispute.entity.DesignerReportStatus;
import csit314.renovation.dispute.repository.DesignerReportRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DesignerDisputeService {
    private final DesignerReportRepository reports;
    private final UserAccountRepository accounts;
    private final AuthenticationService authenticationService;
    private final DesignerCompanyRepository companies;

    public DesignerDisputeService(DesignerReportRepository reports, UserAccountRepository accounts,
            AuthenticationService authenticationService, DesignerCompanyRepository companies) {
        this.reports = reports;
        this.accounts = accounts;
        this.authenticationService = authenticationService;
        this.companies = companies;
    }

    @Transactional
    public DesignerReportResponse submit(Long designerId, SubmitDesignerReportRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Customer sign-in required");
        }
        UserAccount customer = accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(a -> a.isActive() && a.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new AccessDeniedException("Customer access required"));
        UserAccount designer = accounts.findByIdAndRoleAndActiveTrue(designerId, UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));
        String description = request.description() == null ? "" : request.description().strip();
        if (request.reason() == null || description.length() < 20 || description.length() > 2000) {
            throw new IllegalArgumentException("Invalid report details");
        }
        if (reports.existsByCustomer_IdAndDesigner_IdAndStatus(
                customer.getId(), designerId, DesignerReportStatus.OPEN)) {
            throw new DuplicateOpenReportException();
        }
        try {
            // The partial unique index also protects simultaneous submissions.
            return response(reports.saveAndFlush(
                    DesignerReport.submit(customer, designer, request.reason(), description)));
        } catch (DataIntegrityViolationException exception) {
            String detail = exception.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("designer_reports_one_open_per_pair")) {
                throw new DuplicateOpenReportException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DesignerReportPageResponse search(String status, String query, int page,
            Authentication authentication) {
        authenticationService.currentAdmin(authentication);
        if (page < 0) throw new IllegalArgumentException("Page must be nonnegative");
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100) throw new IllegalArgumentException("Search is too long");
        DesignerReportStatus filter = parseStatus(status);
        Long refId = null;
        try {
            String ref = normalized.startsWith("ref-") ? normalized.substring(4) : normalized;
            if (ref.matches("[0-9]{1,18}")) refId = Long.valueOf(ref);
        } catch (NumberFormatException ignored) {
            // Search as ordinary text if the reference is not a valid database ID.
        }
        var matches = reports.search(filter, normalized, refId, PageRequest.of(page, 20,
                Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"))))
                .map(this::response);
        return DesignerReportPageResponse.from(matches,
                reports.countByStatus(DesignerReportStatus.OPEN),
                reports.countByStatus(DesignerReportStatus.RESOLVED),
                reports.countByStatus(DesignerReportStatus.DISMISSED));
    }

    @Transactional(readOnly = true)
    public DesignerReportResponse find(Long reportId, Authentication authentication) {
        authenticationService.currentAdmin(authentication);
        return response(reports.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found")));
    }

    @Transactional
    public DesignerReportResponse decide(Long reportId, ResolveDesignerReportRequest request,
            Authentication authentication) {
        var adminSession = authenticationService.currentAdmin(authentication);
        UserAccount admin = accounts.findById(adminSession.id())
                .orElseThrow(() -> new AccessDeniedException("Admin access required"));
        DesignerReport report = reports.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        String note = request.note() == null ? "" : request.note().strip();
        if (request.outcome() == null || request.outcome() == DesignerReportStatus.OPEN
                || note.length() < 10 || note.length() > 1000) {
            throw new IllegalArgumentException("Invalid resolution details");
        }
        if (report.getStatus() != DesignerReportStatus.OPEN) {
            throw new ReportAlreadyClosedException();
        }
        // Conditional update prevents concurrent administrators from overwriting a decision.
        int updated = reports.closeOpenReport(reportId, request.outcome().name(), note, admin.getId());
        if (updated == 0) throw new ReportAlreadyClosedException();
        // Refresh to return the exact persisted final decision.
        reports.flush();
        return response(reports.findById(reportId).orElseThrow());
    }

    private DesignerReportResponse response(DesignerReport report) {
        String name = companies.findById(report.getDesigner().getId())
                .map(company -> company.getCompanyName())
                .filter(companyName -> !companyName.isBlank())
                .orElse(report.getDesigner().getDisplayName());
        return DesignerReportResponse.from(report, name);
    }

    private DesignerReportStatus parseStatus(String value) {
        if (value == null || value.isBlank() || value.equals("ALL")) return null;
        return switch (value) {
            case "OPEN" -> DesignerReportStatus.OPEN;
            case "RESOLVED" -> DesignerReportStatus.RESOLVED;
            case "DISMISSED" -> DesignerReportStatus.DISMISSED;
            default -> throw new IllegalArgumentException("Invalid report status");
        };
    }
}
