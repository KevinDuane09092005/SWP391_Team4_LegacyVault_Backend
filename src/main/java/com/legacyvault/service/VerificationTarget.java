package com.legacyvault.service;

import com.legacyvault.entity.AssetAssignment;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.HandoverCase;

public record VerificationTarget(
        HandoverCase handoverCase,
        AssetAssignment assignment,
        Contact beneficiaryContact
) {
}
