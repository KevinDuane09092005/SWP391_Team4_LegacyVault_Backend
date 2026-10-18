package com.legacyvault.repository;

import com.legacyvault.entity.HandoverReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoverReceiptRepository extends JpaRepository<HandoverReceipt, String> {

    List<HandoverReceipt> findByCaseId(String caseId);

    List<HandoverReceipt> findByBeneficiaryContactId(String beneficiaryContactId);

    Optional<HandoverReceipt> findByCaseIdAndAssetId(String caseId, String assetId);

    Optional<HandoverReceipt> findByDigitalReceiptHash(String digitalReceiptHash);
}
