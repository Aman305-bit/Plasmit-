package com.plasmit.pathology.hospital.machine.dto.response;

import java.time.LocalDateTime;

public record MachineImportResponse(
        Long messageId,
        Long specimenId,
        Long resultEntryId,
        String importStatus,
        Long importedBy,
        LocalDateTime importedAt
) {
}