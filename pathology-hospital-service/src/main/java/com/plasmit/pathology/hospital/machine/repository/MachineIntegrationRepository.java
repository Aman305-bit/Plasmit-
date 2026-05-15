package com.plasmit.pathology.hospital.machine.repository;

import com.plasmit.pathology.hospital.common.exception.ApiException;
import com.plasmit.pathology.hospital.machine.dto.request.MachineMessageCreateRequest;
import com.plasmit.pathology.hospital.machine.dto.request.MachineMessageParameterRequest;
import com.plasmit.pathology.hospital.machine.dto.response.*;
import com.plasmit.pathology.hospital.pathology.dto.request.ResultEntryRequest;
import com.plasmit.pathology.hospital.pathology.dto.request.ResultParameterRequest;
import com.plasmit.pathology.hospital.pathology.dto.response.PathologySpecimenResponse;
import com.plasmit.pathology.hospital.pathology.repository.PathologySpecimenRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class MachineIntegrationRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final PathologySpecimenRepository specimenRepository;

    public MachineIntegrationRepository(NamedParameterJdbcTemplate jdbc,
                                        PathologySpecimenRepository specimenRepository) {
        this.jdbc = jdbc;
        this.specimenRepository = specimenRepository;
    }

    public List<PathologyMachineResponse> findMachines(Long tenantId,
                                                       Long hospitalId,
                                                       String branchId,
                                                       String modality,
                                                       String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    machine_code,
                    machine_name,
                    vendor_name,
                    model_name,
                    department,
                    modality,
                    connection_type,
                    interface_status,
                    location,
                    is_active,
                    created_at
                FROM pathology_machines
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (modality != null && !modality.isBlank()) {
            sql.append(" AND modality = :modality ");
            params.addValue("modality", modality);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND interface_status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY machine_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new PathologyMachineResponse(
                rs.getLong("id"),
                rs.getString("machine_code"),
                rs.getString("machine_name"),
                rs.getString("vendor_name"),
                rs.getString("model_name"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("connection_type"),
                rs.getString("interface_status"),
                rs.getString("location"),
                rs.getInt("is_active") == 1,
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public void validateMachineExists(Long tenantId,
                                      Long hospitalId,
                                      String branchId,
                                      Long machineId) {

        String sql = """
                SELECT COUNT(*)
                FROM pathology_machines
                WHERE id = :machineId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                  AND is_active = 1
                  AND interface_status = 'Active'
                """;

        Long count = jdbc.queryForObject(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("machineId", machineId),
                Long.class
        );

        if (count == null || count == 0) {
            throw ApiException.notFound("Active pathology machine not found.");
        }
    }

    public Long createMachineMessage(Long tenantId,
                                     Long hospitalId,
                                     String branchId,
                                     Long userId,
                                     MachineMessageCreateRequest request,
                                     Long matchedSpecimenId,
                                     String matchStatus,
                                     String processingStatus) {

        String sql = """
                INSERT INTO pathology_machine_result_messages (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    machine_id,
                    message_control_id,
                    machine_sample_id,
                    barcode_no,
                    patient_uhid,
                    patient_name,
                    raw_message,
                    matched_specimen_id,
                    match_status,
                    processing_status,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :machineId,
                    :messageControlId,
                    :machineSampleId,
                    :barcodeNo,
                    :patientUhid,
                    :patientName,
                    :rawMessage,
                    :matchedSpecimenId,
                    :matchStatus,
                    :processingStatus,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("machineId", request.machineId())
                .addValue("messageControlId", request.messageControlId())
                .addValue("machineSampleId", request.machineSampleId())
                .addValue("barcodeNo", request.barcodeNo())
                .addValue("patientUhid", request.patientUhid())
                .addValue("patientName", request.patientName())
                .addValue("rawMessage", request.rawMessage())
                .addValue("matchedSpecimenId", matchedSpecimenId)
                .addValue("matchStatus", matchStatus)
                .addValue("processingStatus", processingStatus)
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void createMessageParameter(Long tenantId,
                                       Long hospitalId,
                                       String branchId,
                                       Long messageId,
                                       Long machineId,
                                       MachineMessageParameterRequest request) {

        MachineMapping mapping = findMapping(
                tenantId,
                hospitalId,
                branchId,
                machineId,
                request.machineTestCode()
        );

        String sql = """
                INSERT INTO pathology_machine_result_parameters (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    message_id,
                    machine_test_code,
                    machine_test_name,
                    parameter_code,
                    parameter_name,
                    result_value,
                    unit,
                    reference_range,
                    abnormal_flag,
                    critical_flag,
                    display_order
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :messageId,
                    :machineTestCode,
                    :machineTestName,
                    :parameterCode,
                    :parameterName,
                    :resultValue,
                    :unit,
                    :referenceRange,
                    :abnormalFlag,
                    :criticalFlag,
                    :displayOrder
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("messageId", messageId)
                .addValue("machineTestCode", request.machineTestCode())
                .addValue("machineTestName", request.machineTestName())
                .addValue("parameterCode", mapping == null ? request.machineTestCode() : mapping.parameterCode())
                .addValue("parameterName", mapping == null ? request.machineTestCode() : mapping.parameterName())
                .addValue("resultValue", request.resultValue())
                .addValue("unit", request.unit() == null && mapping != null ? mapping.unit() : request.unit())
                .addValue("referenceRange", mapping == null ? null : mapping.referenceRange())
                .addValue("abnormalFlag", Boolean.TRUE.equals(request.abnormalFlag()) ? 1 : 0)
                .addValue("criticalFlag", Boolean.TRUE.equals(request.criticalFlag()) ? 1 : 0)
                .addValue("displayOrder", request.displayOrder() == null ? 0 : request.displayOrder());

        jdbc.update(sql, params);
    }

    public Long autoMatchSpecimenByBarcode(Long tenantId,
                                           Long hospitalId,
                                           String branchId,
                                           String barcodeNo) {

        if (barcodeNo == null || barcodeNo.isBlank()) {
            return null;
        }

        String sql = """
                SELECT id
                FROM pathology_specimens
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND barcode_no = :barcodeNo
                  AND is_deleted = 0
                ORDER BY id DESC
                LIMIT 1
                """;

        List<Long> rows = jdbc.queryForList(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("barcodeNo", barcodeNo),
                Long.class
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    public Long countMessages(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              LocalDate fromDate,
                              LocalDate toDate,
                              String processingStatus,
                              String matchStatus,
                              Long machineId,
                              String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM pathology_machine_result_messages
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(received_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = messageListParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                processingStatus, matchStatus, machineId, search
        );

        appendMessageFilters(sql, processingStatus, matchStatus, machineId, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<MachineMessageResponse> findMessages(Long tenantId,
                                                     Long hospitalId,
                                                     String branchId,
                                                     LocalDate fromDate,
                                                     LocalDate toDate,
                                                     String processingStatus,
                                                     String matchStatus,
                                                     Long machineId,
                                                     String search,
                                                     Integer page,
                                                     Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    machine_id,
                    message_control_id,
                    machine_sample_id,
                    barcode_no,
                    patient_uhid,
                    patient_name,
                    matched_specimen_id,
                    match_status,
                    processing_status,
                    rejection_reason,
                    received_at,
                    processed_at
                FROM pathology_machine_result_messages
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(received_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = messageListParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                processingStatus, matchStatus, machineId, search
        );

        appendMessageFilters(sql, processingStatus, matchStatus, machineId, search);

        sql.append("""
                ORDER BY received_at DESC
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
            Long messageId = rs.getLong("id");
            return new MachineMessageResponse(
                    messageId,
                    rs.getLong("machine_id"),
                    rs.getString("message_control_id"),
                    rs.getString("machine_sample_id"),
                    rs.getString("barcode_no"),
                    rs.getString("patient_uhid"),
                    rs.getString("patient_name"),
                    rs.getObject("matched_specimen_id") == null ? null : rs.getLong("matched_specimen_id"),
                    rs.getString("match_status"),
                    rs.getString("processing_status"),
                    rs.getString("rejection_reason"),
                    rs.getTimestamp("received_at") == null ? null : rs.getTimestamp("received_at").toLocalDateTime(),
                    rs.getTimestamp("processed_at") == null ? null : rs.getTimestamp("processed_at").toLocalDateTime(),
                    findMessageParameters(tenantId, hospitalId, branchId, messageId)
            );
        });
    }

    public MachineMessageResponse findMessageById(Long tenantId,
                                                  Long hospitalId,
                                                  String branchId,
                                                  Long messageId) {

        String sql = """
                SELECT
                    id,
                    machine_id,
                    message_control_id,
                    machine_sample_id,
                    barcode_no,
                    patient_uhid,
                    patient_name,
                    matched_specimen_id,
                    match_status,
                    processing_status,
                    rejection_reason,
                    received_at,
                    processed_at
                FROM pathology_machine_result_messages
                WHERE id = :messageId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<MachineMessageResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("messageId", messageId),
                (rs, rowNum) -> new MachineMessageResponse(
                        rs.getLong("id"),
                        rs.getLong("machine_id"),
                        rs.getString("message_control_id"),
                        rs.getString("machine_sample_id"),
                        rs.getString("barcode_no"),
                        rs.getString("patient_uhid"),
                        rs.getString("patient_name"),
                        rs.getObject("matched_specimen_id") == null ? null : rs.getLong("matched_specimen_id"),
                        rs.getString("match_status"),
                        rs.getString("processing_status"),
                        rs.getString("rejection_reason"),
                        rs.getTimestamp("received_at") == null ? null : rs.getTimestamp("received_at").toLocalDateTime(),
                        rs.getTimestamp("processed_at") == null ? null : rs.getTimestamp("processed_at").toLocalDateTime(),
                        findMessageParameters(tenantId, hospitalId, branchId, rs.getLong("id"))
                )
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Machine result message not found.");
        }

        return rows.get(0);
    }

    public List<MachineResultParameterResponse> findMessageParameters(Long tenantId,
                                                                      Long hospitalId,
                                                                      String branchId,
                                                                      Long messageId) {

        String sql = """
                SELECT
                    id,
                    machine_test_code,
                    machine_test_name,
                    parameter_code,
                    parameter_name,
                    result_value,
                    unit,
                    reference_range,
                    abnormal_flag,
                    critical_flag,
                    display_order
                FROM pathology_machine_result_parameters
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND message_id = :messageId
                  AND is_deleted = 0
                ORDER BY display_order ASC, id ASC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("messageId", messageId),
                (rs, rowNum) -> new MachineResultParameterResponse(
                        rs.getLong("id"),
                        rs.getString("machine_test_code"),
                        rs.getString("machine_test_name"),
                        rs.getString("parameter_code"),
                        rs.getString("parameter_name"),
                        rs.getString("result_value"),
                        rs.getString("unit"),
                        rs.getString("reference_range"),
                        rs.getInt("abnormal_flag") == 1,
                        rs.getInt("critical_flag") == 1,
                        rs.getInt("display_order")
                )
        );
    }

    public void matchMessage(Long tenantId,
                             Long hospitalId,
                             String branchId,
                             Long messageId,
                             Long specimenId) {

        specimenRepository.findSpecimenById(tenantId, hospitalId, branchId, specimenId);

        String sql = """
                UPDATE pathology_machine_result_messages
                SET matched_specimen_id = :specimenId,
                    match_status = 'Matched',
                    processing_status = 'Matched',
                    updated_at = NOW()
                WHERE id = :messageId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        int updated = jdbc.update(
                sql,
                baseParams(tenantId, hospitalId, branchId)
                        .addValue("messageId", messageId)
                        .addValue("specimenId", specimenId)
        );

        if (updated == 0) {
            throw ApiException.notFound("Machine result message not found.");
        }
    }

    public MachineImportResponse importMessage(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long messageId,
                                               Long userId) {

        MachineMessageResponse message = findMessageById(tenantId, hospitalId, branchId, messageId);

        if (message.matchedSpecimenId() == null) {
            throw ApiException.workflow("Message must be matched with specimen before import.");
        }

        if ("Imported".equals(message.processingStatus())) {
            throw ApiException.workflow("Machine result message is already imported.");
        }

        if ("Rejected".equals(message.processingStatus())) {
            throw ApiException.workflow("Rejected message cannot be imported.");
        }

        PathologySpecimenResponse specimen = specimenRepository.findSpecimenById(
                tenantId,
                hospitalId,
                branchId,
                message.matchedSpecimenId()
        );

        if (!"Received".equals(specimen.status()) && !"InProcess".equals(specimen.status())) {
            throw ApiException.workflow("Only Received or InProcess specimen can import machine result.");
        }

        List<ResultParameterRequest> resultParameters = message.parameters()
                .stream()
                .map(parameter -> new ResultParameterRequest(
                        parameter.parameterCode(),
                        parameter.parameterName(),
                        parameter.resultValue(),
                        parameter.unit(),
                        parameter.referenceRange(),
                        parameter.abnormalFlag(),
                        parameter.criticalFlag(),
                        parameter.displayOrder()
                ))
                .toList();

        ResultEntryRequest resultRequest = new ResultEntryRequest(
                "Result imported from machine message " + message.messageId(),
                message.parameters().stream().anyMatch(MachineResultParameterResponse::abnormalFlag),
                message.parameters().stream().anyMatch(MachineResultParameterResponse::criticalFlag),
                resultParameters
        );

        Long resultEntryId = specimenRepository.createResultEntry(
                tenantId,
                hospitalId,
                branchId,
                message.matchedSpecimenId(),
                userId,
                resultRequest
        );

        for (ResultParameterRequest parameter : resultParameters) {
            specimenRepository.createResultParameter(
                    tenantId,
                    hospitalId,
                    branchId,
                    resultEntryId,
                    message.matchedSpecimenId(),
                    parameter
            );
        }

        specimenRepository.markResultEntered(
                tenantId,
                hospitalId,
                branchId,
                message.matchedSpecimenId(),
                userId
        );

        String importSql = """
                INSERT INTO pathology_machine_result_imports (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    message_id,
                    specimen_id,
                    result_entry_id,
                    import_status,
                    imported_by,
                    remarks
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :messageId,
                    :specimenId,
                    :resultEntryId,
                    'Imported',
                    :importedBy,
                    :remarks
                )
                """;

        MapSqlParameterSource importParams = baseParams(tenantId, hospitalId, branchId)
                .addValue("messageId", messageId)
                .addValue("specimenId", message.matchedSpecimenId())
                .addValue("resultEntryId", resultEntryId)
                .addValue("importedBy", userId)
                .addValue("remarks", "Machine result imported into pathology result entry.");

        jdbc.update(importSql, importParams);

        String updateSql = """
                UPDATE pathology_machine_result_messages
                SET processing_status = 'Imported',
                    processed_at = NOW(),
                    updated_at = NOW()
                WHERE id = :messageId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, baseParams(tenantId, hospitalId, branchId).addValue("messageId", messageId));

        return new MachineImportResponse(
                messageId,
                message.matchedSpecimenId(),
                resultEntryId,
                "Imported",
                userId,
                LocalDateTime.now()
        );
    }

    public void rejectMessage(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              Long messageId,
                              String reason) {

        MachineMessageResponse message = findMessageById(tenantId, hospitalId, branchId, messageId);

        if ("Imported".equals(message.processingStatus())) {
            throw ApiException.workflow("Imported message cannot be rejected.");
        }

        String sql = """
                UPDATE pathology_machine_result_messages
                SET processing_status = 'Rejected',
                    rejection_reason = :reason,
                    processed_at = NOW(),
                    updated_at = NOW()
                WHERE id = :messageId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(
                sql,
                baseParams(tenantId, hospitalId, branchId)
                        .addValue("messageId", messageId)
                        .addValue("reason", reason)
        );
    }

    private MachineMapping findMapping(Long tenantId,
                                       Long hospitalId,
                                       String branchId,
                                       Long machineId,
                                       String machineTestCode) {

        String sql = """
                SELECT
                    parameter_code,
                    parameter_name,
                    unit,
                    reference_range
                FROM pathology_machine_test_mappings
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND machine_id = :machineId
                  AND machine_test_code = :machineTestCode
                  AND status = 'Active'
                  AND is_deleted = 0
                LIMIT 1
                """;

        List<MachineMapping> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId)
                        .addValue("machineId", machineId)
                        .addValue("machineTestCode", machineTestCode),
                (rs, rowNum) -> new MachineMapping(
                        rs.getString("parameter_code"),
                        rs.getString("parameter_name"),
                        rs.getString("unit"),
                        rs.getString("reference_range")
                )
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    private void appendMessageFilters(StringBuilder sql,
                                      String processingStatus,
                                      String matchStatus,
                                      Long machineId,
                                      String search) {

        if (processingStatus != null && !processingStatus.isBlank()) {
            sql.append(" AND processing_status = :processingStatus ");
        }

        if (matchStatus != null && !matchStatus.isBlank()) {
            sql.append(" AND match_status = :matchStatus ");
        }

        if (machineId != null) {
            sql.append(" AND machine_id = :machineId ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        barcode_no LIKE :search
                        OR machine_sample_id LIKE :search
                        OR patient_uhid LIKE :search
                        OR patient_name LIKE :search
                        OR message_control_id LIKE :search
                    )
                    """);
        }
    }

    private MapSqlParameterSource messageListParams(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String processingStatus,
                                                    String matchStatus,
                                                    Long machineId,
                                                    String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (processingStatus != null && !processingStatus.isBlank()) {
            params.addValue("processingStatus", processingStatus);
        }

        if (matchStatus != null && !matchStatus.isBlank()) {
            params.addValue("matchStatus", matchStatus);
        }

        if (machineId != null) {
            params.addValue("machineId", machineId);
        }

        if (search != null && !search.isBlank()) {
            params.addValue("search", "%" + search.trim() + "%");
        }

        return params;
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }

    private record MachineMapping(
            String parameterCode,
            String parameterName,
            String unit,
            String referenceRange
    ) {
    }
}