package com.medislot.service;

import com.medislot.dto.AuditDtos.AuditView;
import com.medislot.dto.HospitalDtos.PageResponse;
import com.medislot.entity.AuditLog;
import com.medislot.exception.ApiException;
import com.medislot.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Read-only audit log access for the platform administrator. */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository repo;

    public AuditQueryService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public PageResponse<AuditView> list(String action, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE",
                    "page must be 0 or more and size between 1 and 100.");
        }
        PageRequest p = PageRequest.of(page, size);
        Page<AuditLog> r = (action == null || action.isBlank())
                ? repo.findAllByOrderByCreatedAtDesc(p)
                : repo.findByActionOrderByCreatedAtDesc(action.trim().toUpperCase(Locale.ROOT), p);
        return new PageResponse<>(r.getContent().stream().map(a -> new AuditView(a.getId(), a.getActorUserId(),
                a.getActorEmail(), a.getAction(), a.getResourceType(), a.getResourceId(), a.getMetadata(),
                a.getIpAddress(), a.getCreatedAt())).toList(), r.getNumber(), r.getSize(), r.getTotalElements(),
                r.getTotalPages());
    }
}