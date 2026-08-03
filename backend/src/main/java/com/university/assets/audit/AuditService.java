package com.university.assets.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.assets.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

/**
 * Central, reusable audit trail writer. Records are append-only; there is no
 * update or delete path exposed anywhere in the application.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void log(String action, String module, String entityType, UUID entityId,
                    Map<String, Object> oldValues, Map<String, Object> newValues) {
        log(action, module, entityType, entityId, oldValues, newValues, true);
    }

    public void log(String action, String module, String entityType, UUID entityId,
                    Map<String, Object> oldValues, Map<String, Object> newValues, boolean success) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setModule(module);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setOldValues(toJson(oldValues));
            entry.setNewValues(toJson(newValues));
            entry.setSuccess(success);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
                entry.setUserId(user.getId());
                entry.setUserEmail(user.getUsername());
            }
            fillRequestDetails(entry);
            repository.save(entry);
        } catch (Exception e) {
            // Auditing must never break the business operation.
            log.error("Failed to write audit log for action {}", action, e);
        }
    }

    /** Used by the auth flow where no authenticated principal exists yet. */
    public void logAuth(String action, String email, UUID userId, boolean success) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setModule("AUTH");
            entry.setEntityType("User");
            entry.setEntityId(userId);
            entry.setUserId(userId);
            entry.setUserEmail(email);
            entry.setSuccess(success);
            fillRequestDetails(entry);
            repository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write auth audit log", e);
        }
    }

    private void fillRequestDetails(AuditLog entry) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            entry.setIpAddress(forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
            entry.setUserAgent(request.getHeader("User-Agent"));
        }
    }

    private String toJson(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return values.toString();
        }
    }
}
