package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.entity.ActivityLog;
import com.aihiringplatform.backend.repository.ActivityLogRepository;
import com.aihiringplatform.backend.service.AdminService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminActivityLogController {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private AdminService adminService;

    /**
     * Paginated, filterable activity log endpoint.
     * GET /api/admin/logs?page=0&size=25&search=user@email.com&role=CANDIDATE&actionType=LOGIN&status=FAILURE&from=2024-01-01&to=2024-12-31
     */
    @GetMapping
    public Page<ActivityLog> getLogs(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        adminService.verifyAdmin(auth.getName());

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<ActivityLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("userEmail")), likePattern),
                        cb.like(cb.lower(root.get("ipAddress")), likePattern),
                        cb.like(cb.lower(root.get("actionDescription")), likePattern)
                ));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("userRole")), role.trim().toUpperCase()));
            }
            if (actionType != null && !actionType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("actionType")), actionType.trim().toUpperCase()));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return activityLogRepository.findAll(spec, pageable);
    }

    /**
     * Fetches the most recent security events (SECURITY_EVENT status).
     * GET /api/admin/logs/security-events?limit=50
     */
    @GetMapping("/security-events")
    public List<ActivityLog> getSecurityEvents(
            Authentication auth,
            @RequestParam(defaultValue = "50") int limit
    ) {
        adminService.verifyAdmin(auth.getName());

        Pageable pageable = PageRequest.of(0, Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<ActivityLog> spec = (root, query, cb) ->
                cb.equal(cb.upper(root.get("status")), "SECURITY_EVENT");

        return activityLogRepository.findAll(spec, pageable).getContent();
    }

    /**
     * Aggregated summary of activity data.
     * GET /api/admin/logs/summary
     */
    @GetMapping("/summary")
    public Map<String, Object> getSummary(Authentication auth) {
        adminService.verifyAdmin(auth.getName());

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        LocalDateTime last7d  = LocalDateTime.now().minusDays(7);

        Specification<ActivityLog> loginSuccessLast24h = (root, query, cb) -> cb.and(
                cb.equal(cb.upper(root.get("actionType")), "LOGIN"),
                cb.equal(cb.upper(root.get("status")), "SUCCESS"),
                cb.greaterThanOrEqualTo(root.get("createdAt"), last24h)
        );
        Specification<ActivityLog> loginFailureLast24h = (root, query, cb) -> cb.and(
                cb.equal(cb.upper(root.get("actionType")), "LOGIN"),
                cb.equal(cb.upper(root.get("status")), "FAILURE"),
                cb.greaterThanOrEqualTo(root.get("createdAt"), last24h)
        );
        Specification<ActivityLog> securityEventsLast7d = (root, query, cb) -> cb.and(
                cb.equal(cb.upper(root.get("status")), "SECURITY_EVENT"),
                cb.greaterThanOrEqualTo(root.get("createdAt"), last7d)
        );
        Specification<ActivityLog> totalLogs = (root, query, cb) -> cb.conjunction();

        return Map.of(
                "totalLogs", activityLogRepository.count(),
                "loginSuccessLast24h", activityLogRepository.count(loginSuccessLast24h),
                "loginFailureLast24h", activityLogRepository.count(loginFailureLast24h),
                "securityEventsLast7d", activityLogRepository.count(securityEventsLast7d)
        );
    }
}
