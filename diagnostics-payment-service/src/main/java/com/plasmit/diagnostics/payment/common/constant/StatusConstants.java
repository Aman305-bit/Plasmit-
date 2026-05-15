package com.plasmit.diagnostics.payment.common.constant;

public final class StatusConstants {

    private StatusConstants() {
    }

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    public static final String PAID = "Paid";
    public static final String PARTIAL = "Partial";
    public static final String DUE = "Due";
    public static final String REFUNDED = "Refunded";
    public static final String CANCELLED = "Cancelled";

    public static final String SETTLEMENT_PENDING = "Pending";
    public static final String SETTLEMENT_SETTLED = "Settled";
    public static final String SETTLEMENT_UNSETTLED = "Unsettled";
    public static final String SETTLEMENT_MISMATCH = "Mismatch";
}