package com.plasmit.subscription.repository;

import com.plasmit.subscription.dto.request.*;
import com.plasmit.subscription.dto.response.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SubscriptionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SubscriptionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createPlan(CreatePlanRequest request, String featuresJson, Long userId) {
        String sql = """
                INSERT INTO subscription_plans
                (name, billing_cycle, price, currency, user_limit, branch_limit, storage_limit_gb,
                 included_features, status, created_by)
                VALUES
                (:name, :billingCycle, :price, :currency, :userLimit, :branchLimit, :storageLimitGb,
                 CAST(:features AS JSON), 'ACTIVE', :createdBy)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("name", request.getName())
                .addValue("billingCycle", request.getBillingCycle())
                .addValue("price", request.getPrice())
                .addValue("currency", request.getCurrency())
                .addValue("userLimit", request.getUserLimit())
                .addValue("branchLimit", request.getBranchLimit())
                .addValue("storageLimitGb", request.getStorageLimitGb())
                .addValue("features", featuresJson)
                .addValue("createdBy", userId), keyHolder);

        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    public List<SubscriptionPlanResponse> findPlans(String search, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, billing_cycle, price, currency, user_limit, branch_limit,
                       storage_limit_gb, CAST(included_features AS CHAR) AS included_features,
                       status, created_at
                FROM subscription_plans
                WHERE is_deleted = 0
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (search != null && !search.isBlank()) {
            sql.append(" AND name LIKE :search ");
            params.addValue("search", "%" + search.trim() + "%");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY created_at DESC ");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new SubscriptionPlanResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("billing_cycle"),
                        rs.getBigDecimal("price"),
                        rs.getString("currency"),
                        rs.getInt("user_limit"),
                        rs.getInt("branch_limit"),
                        rs.getInt("storage_limit_gb"),
                        rs.getString("included_features"),
                        rs.getString("status"),
                        rs.getString("created_at")
                )
        );
    }

    public int updatePlan(Long planId, UpdatePlanRequest request, String featuresJson, Long userId) {
        String sql = """
                UPDATE subscription_plans
                SET name = :name,
                    billing_cycle = :billingCycle,
                    price = :price,
                    currency = :currency,
                    user_limit = :userLimit,
                    branch_limit = :branchLimit,
                    storage_limit_gb = :storageLimitGb,
                    included_features = CAST(:features AS JSON),
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :planId
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("planId", planId)
                .addValue("name", request.getName())
                .addValue("billingCycle", request.getBillingCycle())
                .addValue("price", request.getPrice())
                .addValue("currency", request.getCurrency())
                .addValue("userLimit", request.getUserLimit())
                .addValue("branchLimit", request.getBranchLimit())
                .addValue("storageLimitGb", request.getStorageLimitGb())
                .addValue("features", featuresJson)
                .addValue("updatedBy", userId));
    }

    public int updatePlanStatus(Long planId, String status, Long userId) {
        String sql = """
                UPDATE subscription_plans
                SET status = :status,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :planId
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("planId", planId)
                .addValue("status", status)
                .addValue("updatedBy", userId));
    }

    public Long createHospitalMapping(CreateHospitalSubscriptionRequest request, Long userId) {
        String sql = """
                INSERT INTO hospital_subscriptions
                (hospital_id, plan_id, start_date, end_date, status, payment_status,
                 renewal_date, invoice_number, created_by)
                VALUES
                (:hospitalId, :planId, :startDate, :endDate, :status, :paymentStatus,
                 :endDate, :invoiceNumber, :createdBy)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("hospitalId", request.getHospitalId())
                .addValue("planId", request.getPlanId())
                .addValue("startDate", request.getStartDate())
                .addValue("endDate", request.getEndDate())
                .addValue("status", request.getStatus())
                .addValue("paymentStatus", request.getPaymentStatus())
                .addValue("invoiceNumber", request.getInvoiceNumber())
                .addValue("createdBy", userId), keyHolder);

        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    public List<HospitalSubscriptionMappingResponse> findHospitalMappings(Long hospitalId, Long planId, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT hs.id,
                       hs.hospital_id,
                       h.hospital_name AS hospital_name,
                       h.hospital_code AS hospital_code,
                       hs.plan_id,
                       sp.name AS plan_name,
                       hs.status AS subscription_status,
                       hs.payment_status,
                       hs.renewal_date,
                       hs.invoice_number
                FROM hospital_subscriptions hs
                JOIN hospitals h ON h.id = hs.hospital_id
                JOIN subscription_plans sp ON sp.id = hs.plan_id
                WHERE hs.is_deleted = 0
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (hospitalId != null) {
            sql.append(" AND hs.hospital_id = :hospitalId ");
            params.addValue("hospitalId", hospitalId);
        }

        if (planId != null) {
            sql.append(" AND hs.plan_id = :planId ");
            params.addValue("planId", planId);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND hs.status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY hs.created_at DESC ");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                new HospitalSubscriptionMappingResponse(
                        rs.getLong("id"),
                        rs.getLong("hospital_id"),
                        rs.getString("hospital_name"),
                        rs.getString("hospital_code"),
                        rs.getLong("plan_id"),
                        rs.getString("plan_name"),
                        rs.getString("subscription_status"),
                        rs.getString("payment_status"),
                        rs.getString("renewal_date"),
                        rs.getString("invoice_number")
                )
        );
    }

    public int updateHospitalMapping(Long mappingId, UpdateHospitalSubscriptionRequest request, Long userId) {
        String sql = """
                UPDATE hospital_subscriptions
                SET plan_id = :planId,
                    status = :status,
                    payment_status = :paymentStatus,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :mappingId
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("mappingId", mappingId)
                .addValue("planId", request.getPlanId())
                .addValue("status", request.getStatus())
                .addValue("paymentStatus", request.getPaymentStatus())
                .addValue("updatedBy", userId));
    }
}