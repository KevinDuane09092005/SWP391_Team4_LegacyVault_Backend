package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class UpdateAssignmentRequest {

    @JsonAlias({"asset_id"})
    private String assetId;

    @NotBlank(message = "Executor contact ID is required")
    @JsonAlias({"executor_contact_id"})
    private String executorContactId;

    @JsonAlias({"handover_protocol"})
    private String handoverProtocol;

    @JsonAlias({"handover_instructions"})
    private String handoverInstructions;

    @NotEmpty(message = "At least one beneficiary contact ID is required")
    @JsonAlias({"beneficiary_contact_ids"})
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

    public String getHandoverProtocol() {
        return handoverProtocol;
    }

    public void setHandoverProtocol(String handoverProtocol) {
        this.handoverProtocol = handoverProtocol;
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
