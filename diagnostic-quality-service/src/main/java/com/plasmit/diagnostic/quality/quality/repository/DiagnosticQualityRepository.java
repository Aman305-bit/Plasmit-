package com.plasmit.diagnostic.quality.quality.repository;

import com.plasmit.diagnostic.quality.common.exception.ApiException;
import com.plasmit.diagnostic.quality.quality.dto.request.*;
import com.plasmit.diagnostic.quality.quality.dto.response.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Repository
public class DiagnosticQualityRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DiagnosticQualityRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EquipmentResponse> findEquipments(Long tenantId, Long hospitalId, String branchId,
                                                  String status, String department) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_equipments
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (status != null && !status.isBlank()) {
            sql.append(" AND operational_status = :status ");
            params.addValue("status", status);
        }

        if (department != null && !department.isBlank()) {
            sql.append(" AND department = :department ");
            params.addValue("department", department);
        }

        sql.append(" ORDER BY equipment_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapEquipment(rs));
    }

    public EquipmentResponse findEquipmentById(Long tenantId, Long hospitalId, String branchId, Long equipmentId) {
        String sql = """
                SELECT *
                FROM diagnostic_equipments
                WHERE id = :equipmentId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<EquipmentResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("equipmentId", equipmentId),
                (rs, rowNum) -> mapEquipment(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Equipment not found.");
        }

        return rows.get(0);
    }

    public MaintenanceResponse createMaintenance(Long tenantId, Long hospitalId, String branchId,
                                                 Long userId, CreateMaintenanceRequest request) {
        findEquipmentById(tenantId, hospitalId, branchId, request.equipmentId());

        String sql = """
                INSERT INTO diagnostic_equipment_maintenance (
                    tenant_id, hospital_id, branch_id,
                    equipment_id, maintenance_type, maintenance_status,
                    scheduled_at, performed_by, vendor_ticket_no, issue_description,
                    created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :equipmentId, :maintenanceType, 'Scheduled',
                    :scheduledAt, :performedBy, :vendorTicketNo, :issueDescription,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("equipmentId", request.equipmentId())
                .addValue("maintenanceType", request.maintenanceType())
                .addValue("scheduledAt", request.scheduledAt() == null ? null : Timestamp.valueOf(request.scheduledAt()))
                .addValue("performedBy", request.performedBy())
                .addValue("vendorTicketNo", request.vendorTicketNo())
                .addValue("issueDescription", request.issueDescription())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();

        updateEquipmentStatus(tenantId, hospitalId, branchId, request.equipmentId(), "Maintenance", userId);

        return findMaintenanceById(tenantId, hospitalId, branchId, id);
    }

    public MaintenanceResponse completeMaintenance(Long tenantId, Long hospitalId, String branchId,
                                                   Long maintenanceId, Long userId,
                                                   CompleteMaintenanceRequest request) {
        MaintenanceResponse existing = findMaintenanceById(tenantId, hospitalId, branchId, maintenanceId);

        String sql = """
                UPDATE diagnostic_equipment_maintenance
                SET maintenance_status = 'Completed',
                    completed_at = NOW(),
                    performed_by = :performedBy,
                    resolution_notes = :resolutionNotes,
                    updated_at = NOW()
                WHERE id = :maintenanceId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("maintenanceId", maintenanceId)
                .addValue("performedBy", request.performedBy())
                .addValue("resolutionNotes", request.resolutionNotes()));

        updateEquipmentStatus(tenantId, hospitalId, branchId, existing.equipmentId(), "Operational", userId);

        return findMaintenanceById(tenantId, hospitalId, branchId, maintenanceId);
    }

    public MaintenanceResponse findMaintenanceById(Long tenantId, Long hospitalId, String branchId, Long maintenanceId) {
        String sql = """
                SELECT *
                FROM diagnostic_equipment_maintenance
                WHERE id = :maintenanceId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<MaintenanceResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("maintenanceId", maintenanceId),
                (rs, rowNum) -> new MaintenanceResponse(
                        rs.getLong("id"),
                        rs.getLong("equipment_id"),
                        rs.getString("maintenance_type"),
                        rs.getString("maintenance_status"),
                        rs.getTimestamp("scheduled_at") == null ? null : rs.getTimestamp("scheduled_at").toLocalDateTime(),
                        rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toLocalDateTime(),
                        rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toLocalDateTime(),
                        rs.getString("performed_by"),
                        rs.getString("vendor_ticket_no"),
                        rs.getString("issue_description"),
                        rs.getString("resolution_notes"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                )
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Maintenance record not found.");
        }

        return rows.get(0);
    }

    public CalibrationResponse createCalibration(Long tenantId, Long hospitalId, String branchId,
                                                 Long userId, CreateCalibrationRequest request) {
        findEquipmentById(tenantId, hospitalId, branchId, request.equipmentId());

        String sql = """
                INSERT INTO diagnostic_equipment_calibrations (
                    tenant_id, hospital_id, branch_id,
                    equipment_id, calibration_status, calibrated_at, next_due_at,
                    calibrated_by, certificate_no, remarks, created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :equipmentId, :calibrationStatus, :calibratedAt, :nextDueAt,
                    :calibratedBy, :certificateNo, :remarks, :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("equipmentId", request.equipmentId())
                .addValue("calibrationStatus", request.calibrationStatus())
                .addValue("calibratedAt", Timestamp.valueOf(request.calibratedAt()))
                .addValue("nextDueAt", request.nextDueAt() == null ? null : Timestamp.valueOf(request.nextDueAt()))
                .addValue("calibratedBy", request.calibratedBy())
                .addValue("certificateNo", request.certificateNo())
                .addValue("remarks", request.remarks())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        String eqStatus = "Passed".equals(request.calibrationStatus()) ? "Operational" : "CalibrationDue";

        String updateSql = """
                UPDATE diagnostic_equipments
                SET operational_status = :status,
                    last_calibrated_at = :calibratedAt,
                    next_calibration_due_at = :nextDueAt,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :equipmentId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                """;

        jdbc.update(updateSql, params.addValue("status", eqStatus).addValue("updatedBy", userId));

        return new CalibrationResponse(
                Objects.requireNonNull(keyHolder.getKey()).longValue(),
                request.equipmentId(),
                request.calibrationStatus(),
                request.calibratedAt(),
                request.nextDueAt(),
                request.calibratedBy(),
                request.certificateNo(),
                request.remarks()
        );
    }

    public QcEventResponse createQcEvent(Long tenantId, Long hospitalId, String branchId,
                                         Long userId, CreateQcEventRequest request, String qcNo) {
        if (request.equipmentId() != null) {
            findEquipmentById(tenantId, hospitalId, branchId, request.equipmentId());
        }

        boolean failed = request.results().stream().anyMatch(r -> "Failed".equals(r.resultStatus()));
        String qcStatus = failed ? "Failed" : "Passed";

        String sql = """
                INSERT INTO diagnostic_quality_control_events (
                    tenant_id, hospital_id, branch_id,
                    equipment_id, qc_no, qc_type, department, modality,
                    qc_status, performed_at, performed_by, remarks, created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :equipmentId, :qcNo, :qcType, :department, :modality,
                    :qcStatus, NOW(), :performedBy, :remarks, :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("equipmentId", request.equipmentId())
                .addValue("qcNo", qcNo)
                .addValue("qcType", request.qcType())
                .addValue("department", request.department())
                .addValue("modality", request.modality())
                .addValue("qcStatus", qcStatus)
                .addValue("performedBy", request.performedBy())
                .addValue("remarks", request.remarks())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long qcEventId = Objects.requireNonNull(keyHolder.getKey()).longValue();

        for (QcResultRequest result : request.results()) {
            insertQcResult(tenantId, hospitalId, branchId, qcEventId, result);
        }

        return findQcEventById(tenantId, hospitalId, branchId, qcEventId);
    }

    public QcEventResponse findQcEventById(Long tenantId, Long hospitalId, String branchId, Long qcEventId) {
        String sql = """
                SELECT *
                FROM diagnostic_quality_control_events
                WHERE id = :qcEventId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<QcEventResponse> rows = jdbc.query(sql,
                baseParams(tenantId, hospitalId, branchId).addValue("qcEventId", qcEventId),
                (rs, rowNum) -> new QcEventResponse(
                        rs.getLong("id"),
                        rs.getObject("equipment_id") == null ? null : rs.getLong("equipment_id"),
                        rs.getString("qc_no"),
                        rs.getString("qc_type"),
                        rs.getString("department"),
                        rs.getString("modality"),
                        rs.getString("qc_status"),
                        rs.getTimestamp("performed_at") == null ? null : rs.getTimestamp("performed_at").toLocalDateTime(),
                        rs.getString("performed_by"),
                        rs.getString("remarks"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                        findQcResults(tenantId, hospitalId, branchId, qcEventId)
                ));

        if (rows.isEmpty()) {
            throw ApiException.notFound("QC event not found.");
        }

        return rows.get(0);
    }

    public List<QcEventResponse> findQcEvents(Long tenantId, Long hospitalId, String branchId,
                                              LocalDate fromDate, LocalDate toDate, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_quality_control_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (status != null && !status.isBlank()) {
            sql.append(" AND qc_status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY created_at DESC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new QcEventResponse(
                rs.getLong("id"),
                rs.getObject("equipment_id") == null ? null : rs.getLong("equipment_id"),
                rs.getString("qc_no"),
                rs.getString("qc_type"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("qc_status"),
                rs.getTimestamp("performed_at") == null ? null : rs.getTimestamp("performed_at").toLocalDateTime(),
                rs.getString("performed_by"),
                rs.getString("remarks"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                findQcResults(tenantId, hospitalId, branchId, rs.getLong("id"))
        ));
    }

    public List<QcResultResponse> findQcResults(Long tenantId, Long hospitalId, String branchId, Long qcEventId) {
        String sql = """
                SELECT *
                FROM diagnostic_quality_control_results
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND qc_event_id = :qcEventId
                  AND is_deleted = 0
                ORDER BY id ASC
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, branchId).addValue("qcEventId", qcEventId),
                (rs, rowNum) -> new QcResultResponse(
                        rs.getLong("id"),
                        rs.getLong("qc_event_id"),
                        rs.getString("parameter_name"),
                        rs.getString("expected_value"),
                        rs.getString("observed_value"),
                        rs.getString("unit"),
                        rs.getString("result_status"),
                        rs.getString("remarks")
                ));
    }

    public List<InventoryItemResponse> findInventoryItems(Long tenantId, Long hospitalId, String branchId,
                                                          Boolean lowStockOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_inventory_items
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        if (Boolean.TRUE.equals(lowStockOnly)) {
            sql.append(" AND current_stock <= reorder_level ");
        }

        sql.append(" ORDER BY item_name ASC ");

        return jdbc.query(sql.toString(), baseParams(tenantId, hospitalId, branchId),
                (rs, rowNum) -> mapInventoryItem(rs));
    }

    public InventoryItemResponse findInventoryItemById(Long tenantId, Long hospitalId, String branchId, Long itemId) {
        String sql = """
                SELECT *
                FROM diagnostic_inventory_items
                WHERE id = :itemId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<InventoryItemResponse> rows = jdbc.query(sql,
                baseParams(tenantId, hospitalId, branchId).addValue("itemId", itemId),
                (rs, rowNum) -> mapInventoryItem(rs));

        if (rows.isEmpty()) {
            throw ApiException.notFound("Inventory item not found.");
        }

        return rows.get(0);
    }

    public StockMovementResponse stockMovement(Long tenantId, Long hospitalId, String branchId,
                                               Long userId, StockMovementRequest request) {
        InventoryItemResponse item = findInventoryItemById(tenantId, hospitalId, branchId, request.itemId());

        BigDecimal before = item.currentStock();
        BigDecimal after;

        if ("IN".equals(request.movementType())) {
            after = before.add(request.quantity());
        } else if ("OUT".equals(request.movementType())) {
            if (before.compareTo(request.quantity()) < 0) {
                throw ApiException.business("Insufficient stock.");
            }
            after = before.subtract(request.quantity());
        } else {
            after = request.quantity();
        }

        updateItemStock(tenantId, hospitalId, branchId, request.itemId(), after, userId);

        return insertStockMovement(
                tenantId, hospitalId, branchId, userId,
                request.itemId(), request.movementType(), request.quantity(),
                before, after, request.referenceType(), request.referenceId(), request.remarks()
        );
    }

    public InventoryConsumptionResponse consumeInventory(Long tenantId, Long hospitalId, String branchId,
                                                         Long userId, InventoryConsumptionRequest request) {
        InventoryItemResponse item = findInventoryItemById(tenantId, hospitalId, branchId, request.itemId());

        if (item.currentStock().compareTo(request.quantityConsumed()) < 0) {
            throw ApiException.business("Insufficient stock for consumption.");
        }

        BigDecimal before = item.currentStock();
        BigDecimal after = before.subtract(request.quantityConsumed());

        updateItemStock(tenantId, hospitalId, branchId, request.itemId(), after, userId);

        insertStockMovement(
                tenantId, hospitalId, branchId, userId,
                request.itemId(), "OUT", request.quantityConsumed(),
                before, after, "DIAGNOSTIC_CONSUMPTION", request.diagnosticOrderId(), request.remarks()
        );

        String sql = """
                INSERT INTO diagnostic_inventory_consumptions (
                    tenant_id, hospital_id, branch_id,
                    item_id, diagnostic_order_id, diagnostic_order_line_id, service_id,
                    quantity_consumed, consumed_for, consumed_by, remarks
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :itemId, :diagnosticOrderId, :diagnosticOrderLineId, :serviceId,
                    :quantityConsumed, :consumedFor, :consumedBy, :remarks
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("itemId", request.itemId())
                .addValue("diagnosticOrderId", request.diagnosticOrderId())
                .addValue("diagnosticOrderLineId", request.diagnosticOrderLineId())
                .addValue("serviceId", request.serviceId())
                .addValue("quantityConsumed", request.quantityConsumed())
                .addValue("consumedFor", request.consumedFor())
                .addValue("consumedBy", userId)
                .addValue("remarks", request.remarks());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return new InventoryConsumptionResponse(
                Objects.requireNonNull(keyHolder.getKey()).longValue(),
                request.itemId(),
                request.diagnosticOrderId(),
                request.diagnosticOrderLineId(),
                request.serviceId(),
                request.quantityConsumed(),
                request.consumedFor(),
                userId,
                request.remarks(),
                java.time.LocalDateTime.now()
        );
    }

    public void insertAudit(Long tenantId, Long hospitalId, String branchId,
                            String entityType, Long entityId, String eventType,
                            String fromStatus, String toStatus, String notes,
                            Long userId, String role, String requestId) {
        String sql = """
                INSERT INTO diagnostic_quality_audit_events (
                    tenant_id, hospital_id, branch_id,
                    entity_type, entity_id, event_type,
                    from_status, to_status, notes,
                    actor_user_id, actor_role, request_id
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :entityType, :entityId, :eventType,
                    :fromStatus, :toStatus, :notes,
                    :actorUserId, :actorRole, :requestId
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("entityType", entityType)
                .addValue("entityId", entityId)
                .addValue("eventType", eventType)
                .addValue("fromStatus", fromStatus)
                .addValue("toStatus", toStatus)
                .addValue("notes", notes)
                .addValue("actorUserId", userId)
                .addValue("actorRole", role)
                .addValue("requestId", requestId));
    }

    private void insertQcResult(Long tenantId, Long hospitalId, String branchId,
                                Long qcEventId, QcResultRequest result) {
        String sql = """
                INSERT INTO diagnostic_quality_control_results (
                    tenant_id, hospital_id, branch_id,
                    qc_event_id, parameter_name, expected_value, observed_value,
                    unit, result_status, remarks
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :qcEventId, :parameterName, :expectedValue, :observedValue,
                    :unit, :resultStatus, :remarks
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("qcEventId", qcEventId)
                .addValue("parameterName", result.parameterName())
                .addValue("expectedValue", result.expectedValue())
                .addValue("observedValue", result.observedValue())
                .addValue("unit", result.unit())
                .addValue("resultStatus", result.resultStatus())
                .addValue("remarks", result.remarks()));
    }

    private void updateEquipmentStatus(Long tenantId, Long hospitalId, String branchId,
                                       Long equipmentId, String status, Long userId) {
        String sql = """
                UPDATE diagnostic_equipments
                SET operational_status = :status,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :equipmentId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("equipmentId", equipmentId)
                .addValue("status", status)
                .addValue("updatedBy", userId));
    }

    private void updateItemStock(Long tenantId, Long hospitalId, String branchId,
                                 Long itemId, BigDecimal stock, Long userId) {
        String sql = """
                UPDATE diagnostic_inventory_items
                SET current_stock = :stock,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :itemId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("itemId", itemId)
                .addValue("stock", stock)
                .addValue("updatedBy", userId));
    }

    private StockMovementResponse insertStockMovement(Long tenantId, Long hospitalId, String branchId,
                                                      Long userId, Long itemId, String movementType,
                                                      BigDecimal quantity, BigDecimal before, BigDecimal after,
                                                      String referenceType, Long referenceId, String remarks) {
        String sql = """
                INSERT INTO diagnostic_inventory_stock_movements (
                    tenant_id, hospital_id, branch_id,
                    item_id, movement_type, quantity, stock_before, stock_after,
                    reference_type, reference_id, remarks, created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :itemId, :movementType, :quantity, :stockBefore, :stockAfter,
                    :referenceType, :referenceId, :remarks, :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("itemId", itemId)
                .addValue("movementType", movementType)
                .addValue("quantity", quantity)
                .addValue("stockBefore", before)
                .addValue("stockAfter", after)
                .addValue("referenceType", referenceType)
                .addValue("referenceId", referenceId)
                .addValue("remarks", remarks)
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return new StockMovementResponse(
                Objects.requireNonNull(keyHolder.getKey()).longValue(),
                itemId,
                movementType,
                quantity,
                before,
                after,
                referenceType,
                referenceId,
                remarks,
                java.time.LocalDateTime.now()
        );
    }

    private EquipmentResponse mapEquipment(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new EquipmentResponse(
                rs.getLong("id"),
                rs.getString("equipment_code"),
                rs.getString("equipment_name"),
                rs.getString("equipment_type"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("vendor_name"),
                rs.getString("model_name"),
                rs.getString("serial_no"),
                rs.getString("location"),
                rs.getString("operational_status"),
                rs.getTimestamp("last_calibrated_at") == null ? null : rs.getTimestamp("last_calibrated_at").toLocalDateTime(),
                rs.getTimestamp("next_calibration_due_at") == null ? null : rs.getTimestamp("next_calibration_due_at").toLocalDateTime(),
                rs.getInt("is_active") == 1,
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private InventoryItemResponse mapInventoryItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        BigDecimal currentStock = rs.getBigDecimal("current_stock");
        BigDecimal reorderLevel = rs.getBigDecimal("reorder_level");

        return new InventoryItemResponse(
                rs.getLong("id"),
                rs.getString("item_code"),
                rs.getString("item_name"),
                rs.getString("item_type"),
                rs.getString("department"),
                rs.getString("unit"),
                currentStock,
                rs.getBigDecimal("minimum_stock"),
                reorderLevel,
                rs.getString("batch_no"),
                rs.getDate("expiry_date") == null ? null : rs.getDate("expiry_date").toLocalDate(),
                rs.getString("status"),
                currentStock.compareTo(reorderLevel) <= 0,
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }
}