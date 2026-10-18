package com.legacyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class UpdateAssignmentRequest {

    private String assetId;

    @NotBlank(message = "Executor contact ID is required")
    private String executorContactId;

    private String handoverInstructions;

    @NotEmpty(message = "At least one beneficiary contact ID is required")
    private List<String> beneficiaryContactIds;

    public UpdateAssignmentRequest() {}

    public UpdateAssignmentRequest(String assetId, String executorContactId, 
                                   String handoverInstructions, List<String> beneficiaryContactIds) {
        this.assetId = assetId;
        this.executorContactId = executorContactId;
        this.handoverInstructions = handoverInstructions;
        this.beneficiaryContactIds = beneficiaryContactIds;
    }

    public UpdateAssignmentRequest(String executorContactId, 
                                   String handoverInstructions, List<String> beneficiaryContactIds) {
        this.executorContactId = executorContactId;
        this.handoverInstructions = handoverInstructions;
        this.beneficiaryContactIds = beneficiaryContactIds;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getExecutorContactId() {
        return executorContactId;
    }

    public void setExecutorContactId(String executorContactId) {
        this.executorContactId = executorContactId;
    }

    public String getHandoverInstructions() {
        return handoverInstructions;
    }

    public void setHandoverInstructions(String handoverInstructions) {
        this.handoverInstructions = handoverInstructions;
    }

    public List<String> getBeneficiaryContactIds() {
        return beneficiaryContactIds != null ? beneficiaryContactIds : new ArrayList<>();
    }

    public void setBeneficiaryContactIds(List<String> beneficiaryContactIds) {
        this.beneficiaryContactIds = beneficiaryContactIds;
    }
}
