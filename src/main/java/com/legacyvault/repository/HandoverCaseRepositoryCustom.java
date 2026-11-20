package com.legacyvault.repository;

import com.legacyvault.entity.HandoverCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HandoverCaseRepositoryCustom {
    Page<HandoverCase> searchVerificationQueue(String status, String search, Pageable pageable);
}
