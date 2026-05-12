package com.plasmit.lab.hospital.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.plasmit.lab.hospital.service.LabService.CreatePackageRequest;
import com.plasmit.lab.hospital.service.LabService.CreateTestRequest;
import com.plasmit.lab.hospital.service.LabService.UpdatePackageRequest;
import com.plasmit.lab.hospital.service.LabService.UpdateTestRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LabRepository {

    private static final Logger log = LoggerFactory.getLogger(LabRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public LabRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextTestCode(Long hospitalId) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lab_tests WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );

        int next = count == null ? 1 : count + 1;
        return String.format("LAB-%06d", next);
    }

    public String generateNextPackageCode(Long hospitalId) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lab_test_packages WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );

        int next = count == null ? 1 : count + 1;
        return String.format("PKG-%06d", next);
    }

    public Long createTest(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long createdBy,
            String testCode,
            CreateTestRequest request
    ) {

        log.debug("Creating lab test. tenantId={} hospitalId={} branchId={}", tenantId, hospitalId, branchId);

        String sql = """
                INSERT INTO lab_tests (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    test_code,
                    test_name,
                    test_category,
                    sample_type,
                    report_type,
                    unit_name,
                    normal_range,
                    interpretation,
                    price,
                    tax_rate,
                    turnaround_time_hours,
                    is_billable,
                    status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'ACTIVE', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                testCode,
                request.testName(),
                request.testCategory(),
                request.sampleType(),
                emptyDefault(request.reportType(), "MANUAL"),
                request.unitName(),
                request.normalRange(),
                request.interpretation(),
                request.price(),
                request.taxRate() == null ? BigDecimal.ZERO : request.taxRate(),
                request.turnaroundTimeHours() == null ? 24 : request.turnaroundTimeHours(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updateTest(
            Long testId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            UpdateTestRequest request
    ) {

        String sql = """
                UPDATE lab_tests
                SET test_name = ?,
                    test_category = ?,
                    sample_type = ?,
                    report_type = ?,
                    unit_name = ?,
                    normal_range = ?,
                    interpretation = ?,
                    price = ?,
                    tax_rate = ?,
                    turnaround_time_hours = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(
                sql,
                request.testName(),
                request.testCategory(),
                request.sampleType(),
                emptyDefault(request.reportType(), "MANUAL"),
                request.unitName(),
                request.normalRange(),
                request.interpretation(),
                request.price(),
                request.taxRate() == null ? BigDecimal.ZERO : request.taxRate(),
                request.turnaroundTimeHours() == null ? 24 : request.turnaroundTimeHours(),
                updatedBy,
                testId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public List<TestRecord> findTests(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String query,
            String category,
            String status,
            int limit,
            int offset
    ) {

        String searchValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        String categoryValue = category == null || category.isBlank() ? null : category.trim();
        String statusValue = status == null || status.isBlank() ? null : status.trim();

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    test_code,
                    test_name,
                    test_category,
                    sample_type,
                    report_type,
                    unit_name,
                    normal_range,
                    interpretation,
                    price,
                    tax_rate,
                    turnaround_time_hours,
                    is_billable,
                    status,
                    created_at,
                    updated_at
                FROM lab_tests
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                  AND (? IS NULL OR status = ?)
                  AND (? IS NULL OR test_category = ?)
                  AND (
                        ? IS NULL
                        OR test_name LIKE ?
                        OR test_code LIKE ?
                        OR test_category LIKE ?
                  )
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapTest,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                statusValue,
                statusValue,
                categoryValue,
                categoryValue,
                searchValue,
                searchValue,
                searchValue,
                searchValue,
                limit,
                offset
        );
    }

    public Optional<TestRecord> findTestById(Long testId, Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    test_code,
                    test_name,
                    test_category,
                    sample_type,
                    report_type,
                    unit_name,
                    normal_range,
                    interpretation,
                    price,
                    tax_rate,
                    turnaround_time_hours,
                    is_billable,
                    status,
                    created_at,
                    updated_at
                FROM lab_tests
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                LIMIT 1
                """;

        List<TestRecord> result = jdbcTemplate.query(
                sql,
                this::mapTest,
                testId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public int archiveTest(Long testId, Long tenantId, Long hospitalId, Long branchId, Long updatedBy) {

        String sql = """
                UPDATE lab_tests
                SET status = 'ARCHIVED',
                    is_deleted = 1,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, updatedBy, testId, tenantId, hospitalId, branchId, branchId);
    }

    public Long createPackage(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long createdBy,
            String packageCode,
            CreatePackageRequest request
    ) {

        String sql = """
                INSERT INTO lab_test_packages (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    package_code,
                    package_name,
                    package_category,
                    description,
                    package_price,
                    tax_rate,
                    is_billable,
                    status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'ACTIVE', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                packageCode,
                request.packageName(),
                request.packageCategory(),
                request.description(),
                request.packagePrice(),
                request.taxRate() == null ? BigDecimal.ZERO : request.taxRate(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updatePackage(
            Long packageId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            UpdatePackageRequest request
    ) {

        String sql = """
                UPDATE lab_test_packages
                SET package_name = ?,
                    package_category = ?,
                    description = ?,
                    package_price = ?,
                    tax_rate = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(
                sql,
                request.packageName(),
                request.packageCategory(),
                request.description(),
                request.packagePrice(),
                request.taxRate() == null ? BigDecimal.ZERO : request.taxRate(),
                updatedBy,
                packageId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public void replacePackageItems(
            Long packageId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long userId,
            List<Long> testIds
    ) {

        jdbcTemplate.update(
                """
                UPDATE lab_test_package_items
                SET is_deleted = 1
                WHERE package_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                """,
                packageId,
                tenantId,
                hospitalId
        );

        int sort = 1;

        for (Long testId : testIds) {
            jdbcTemplate.update(
                    """
                    INSERT INTO lab_test_package_items (
                        tenant_id,
                        hospital_id,
                        branch_id,
                        package_id,
                        test_id,
                        sort_order,
                        created_by,
                        created_at,
                        is_deleted
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 0)
                    ON DUPLICATE KEY UPDATE
                        sort_order = VALUES(sort_order),
                        is_deleted = 0
                    """,
                    tenantId,
                    hospitalId,
                    branchId,
                    packageId,
                    testId,
                    sort++,
                    userId
            );
        }
    }

    public List<PackageRecord> findPackages(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String query,
            String status,
            int limit,
            int offset
    ) {

        String searchValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        String statusValue = status == null || status.isBlank() ? null : status.trim();

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    package_code,
                    package_name,
                    package_category,
                    description,
                    package_price,
                    tax_rate,
                    is_billable,
                    status,
                    created_at,
                    updated_at
                FROM lab_test_packages
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                  AND (? IS NULL OR status = ?)
                  AND (
                        ? IS NULL
                        OR package_name LIKE ?
                        OR package_code LIKE ?
                        OR package_category LIKE ?
                  )
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapPackage,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                statusValue,
                statusValue,
                searchValue,
                searchValue,
                searchValue,
                searchValue,
                limit,
                offset
        );
    }

    public Optional<PackageRecord> findPackageById(Long packageId, Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    package_code,
                    package_name,
                    package_category,
                    description,
                    package_price,
                    tax_rate,
                    is_billable,
                    status,
                    created_at,
                    updated_at
                FROM lab_test_packages
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                LIMIT 1
                """;

        List<PackageRecord> result = jdbcTemplate.query(
                sql,
                this::mapPackage,
                packageId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public List<PackageItemRecord> findPackageItems(Long packageId, Long tenantId, Long hospitalId) {

        String sql = """
                SELECT
                    pi.id,
                    pi.package_id,
                    pi.test_id,
                    t.test_code,
                    t.test_name,
                    t.test_category,
                    t.price,
                    pi.sort_order
                FROM lab_test_package_items pi
                INNER JOIN lab_tests t
                    ON t.id = pi.test_id
                    AND t.tenant_id = pi.tenant_id
                    AND t.hospital_id = pi.hospital_id
                    AND t.is_deleted = 0
                WHERE pi.package_id = ?
                  AND pi.tenant_id = ?
                  AND pi.hospital_id = ?
                  AND pi.is_deleted = 0
                ORDER BY pi.sort_order ASC
                """;

        return jdbcTemplate.query(sql, this::mapPackageItem, packageId, tenantId, hospitalId);
    }

    public int archivePackage(Long packageId, Long tenantId, Long hospitalId, Long branchId, Long updatedBy) {

        String sql = """
                UPDATE lab_test_packages
                SET status = 'ARCHIVED',
                    is_deleted = 1,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, updatedBy, packageId, tenantId, hospitalId, branchId, branchId);
    }

    public boolean allTestsExist(Long tenantId, Long hospitalId, List<Long> testIds) {

        if (testIds == null || testIds.isEmpty()) {
            return true;
        }

        String placeholders = String.join(",", testIds.stream().map(id -> "?").toList());

        String sql = """
                SELECT COUNT(*)
                FROM lab_tests
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND id IN (%s)
                """.formatted(placeholders);

        Object[] params = new Object[testIds.size() + 2];
        params[0] = tenantId;
        params[1] = hospitalId;

        for (int i = 0; i < testIds.size(); i++) {
            params[i + 2] = testIds.get(i);
        }

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params);
        return count != null && count == testIds.size();
    }

    public List<BillingServiceRecord> findBillingServices(Long tenantId, Long hospitalId, Long branchId, String query) {

        String searchValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";

        String sql = """
                SELECT
                    CONCAT('LAB_TEST_', id) AS service_id,
                    'LAB_TEST' AS service_type,
                    id AS reference_id,
                    test_code AS service_code,
                    test_name AS service_name,
                    test_category AS category,
                    price AS amount,
                    tax_rate AS tax_rate
                FROM lab_tests
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_billable = 1
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND (? IS NULL OR test_name LIKE ? OR test_code LIKE ? OR test_category LIKE ?)

                UNION ALL

                SELECT
                    CONCAT('LAB_PACKAGE_', id) AS service_id,
                    'LAB_PACKAGE' AS service_type,
                    id AS reference_id,
                    package_code AS service_code,
                    package_name AS service_name,
                    package_category AS category,
                    package_price AS amount,
                    tax_rate AS tax_rate
                FROM lab_test_packages
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_billable = 1
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND (? IS NULL OR package_name LIKE ? OR package_code LIKE ? OR package_category LIKE ?)

                UNION ALL

                SELECT
                    CONCAT('DOCTOR_CONSULTATION_', id) AS service_id,
                    'DOCTOR_CONSULTATION' AS service_type,
                    id AS reference_id,
                    doctor_code AS service_code,
                    CONCAT(full_name, ' - Consultation') AS service_name,
                    specialization AS category,
                    consultation_fee AS amount,
                    0.00 AS tax_rate
                FROM doctors
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND (? IS NULL OR full_name LIKE ? OR doctor_code LIKE ? OR specialization LIKE ?)
                """;

        return jdbcTemplate.query(
                sql,
                this::mapBillingService,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                searchValue,
                searchValue,
                searchValue,
                searchValue,

                tenantId,
                hospitalId,
                branchId,
                branchId,
                searchValue,
                searchValue,
                searchValue,
                searchValue,

                tenantId,
                hospitalId,
                branchId,
                branchId,
                searchValue,
                searchValue,
                searchValue,
                searchValue
        );
    }

    private TestRecord mapTest(ResultSet rs, int rowNum) throws SQLException {
        return new TestRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getString("test_code"),
                rs.getString("test_name"),
                rs.getString("test_category"),
                rs.getString("sample_type"),
                rs.getString("report_type"),
                rs.getString("unit_name"),
                rs.getString("normal_range"),
                rs.getString("interpretation"),
                rs.getBigDecimal("price"),
                rs.getBigDecimal("tax_rate"),
                getNullableInteger(rs, "turnaround_time_hours"),
                rs.getInt("is_billable") == 1,
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private PackageRecord mapPackage(ResultSet rs, int rowNum) throws SQLException {
        return new PackageRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getString("package_code"),
                rs.getString("package_name"),
                rs.getString("package_category"),
                rs.getString("description"),
                rs.getBigDecimal("package_price"),
                rs.getBigDecimal("tax_rate"),
                rs.getInt("is_billable") == 1,
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private PackageItemRecord mapPackageItem(ResultSet rs, int rowNum) throws SQLException {
        return new PackageItemRecord(
                rs.getLong("id"),
                rs.getLong("package_id"),
                rs.getLong("test_id"),
                rs.getString("test_code"),
                rs.getString("test_name"),
                rs.getString("test_category"),
                rs.getBigDecimal("price"),
                rs.getInt("sort_order")
        );
    }

    private BillingServiceRecord mapBillingService(ResultSet rs, int rowNum) throws SQLException {
        return new BillingServiceRecord(
                rs.getString("service_id"),
                rs.getString("service_type"),
                rs.getLong("reference_id"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("category"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("tax_rate")
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private String emptyDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record TestRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String testCode,
            String testName,
            String testCategory,
            String sampleType,
            String reportType,
            String unitName,
            String normalRange,
            String interpretation,
            BigDecimal price,
            BigDecimal taxRate,
            Integer turnaroundTimeHours,
            boolean billable,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record PackageRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String packageCode,
            String packageName,
            String packageCategory,
            String description,
            BigDecimal packagePrice,
            BigDecimal taxRate,
            boolean billable,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record PackageItemRecord(
            Long id,
            Long packageId,
            Long testId,
            String testCode,
            String testName,
            String testCategory,
            BigDecimal price,
            Integer sortOrder
    ) {
    }

    public record BillingServiceRecord(
            String serviceId,
            String serviceType,
            Long referenceId,
            String serviceCode,
            String serviceName,
            String category,
            BigDecimal amount,
            BigDecimal taxRate
    ) {
    }
}