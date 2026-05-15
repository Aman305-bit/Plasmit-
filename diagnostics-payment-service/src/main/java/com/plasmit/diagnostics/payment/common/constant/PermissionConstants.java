package com.plasmit.diagnostics.payment.common.constant;

public final class PermissionConstants {

    private PermissionConstants() {
    }

    public static final String PAYMENT_REPORTS_VIEW = "payment_reports.view";
    public static final String PAYMENT_REPORTS_EXPORT = "payment_reports.export";
    public static final String PAYMENT_REPORTS_RECONCILE = "payment_reports.reconcile";
    public static final String PAYMENT_REPORTS_SETTLEMENT_MANAGE = "payment_reports.settlement.manage";
    public static final String PAYMENT_REPORTS_AUDIT_VIEW = "payment_reports.audit.view";

    public static final String BILLING_DIAGNOSTICS_VIEW = "billing.diagnostics.view";
    public static final String BILLING_DIAGNOSTICS_CREATE = "billing.diagnostics.create";
    public static final String BILLING_DIAGNOSTICS_OVERRIDE = "billing.diagnostics.override";
    public static final String BILLING_DIAGNOSTICS_HANDOFF = "billing.diagnostics.handoff";

    public static final String DIAGNOSTICS_VIEW = "diagnostics.view";
    public static final String DIAGNOSTICS_ORDER_CREATE = "diagnostics.order.create";
    public static final String DIAGNOSTICS_ORDER_UPDATE = "diagnostics.order.update";
    public static final String DIAGNOSTICS_ORDER_CANCEL = "diagnostics.order.cancel";
    public static final String DIAGNOSTICS_BILLING_AUTHORIZE = "diagnostics.billing_authorize";

    public static final String RADIOLOGY_VIEW = "radiology.view";
    public static final String RADIOLOGY_SCHEDULE_MANAGE = "radiology.schedule.manage";
    public static final String RADIOLOGY_SAFETY_MANAGE = "radiology.safety.manage";
    public static final String RADIOLOGY_PACS_MANAGE = "radiology.pacs.manage";
    public static final String RADIOLOGY_REPORT_CREATE = "radiology.report.create";
    public static final String RADIOLOGY_REPORT_SIGN = "radiology.report.sign";
    public static final String RADIOLOGY_IMAGE_SHARE_CREATE = "radiology.image_share.create";
    public static final String RADIOLOGY_IMAGE_SHARE_REVOKE = "radiology.image_share.revoke";
    public static final String RADIOLOGY_IMAGE_SHARE_AUDIT = "radiology.image_share.audit";

    public static final String PATHOLOGY_VIEW = "pathology.view";
    public static final String PATHOLOGY_SAMPLE_COLLECT = "pathology.sample.collect";
    public static final String PATHOLOGY_SAMPLE_ACCESSION = "pathology.sample.accession";
    public static final String PATHOLOGY_SAMPLE_REJECT = "pathology.sample.reject";
    public static final String PATHOLOGY_RESULT_ENTER = "pathology.result.enter";
    public static final String PATHOLOGY_RESULT_VERIFY_TECHNICAL = "pathology.result.verify_technical";
    public static final String PATHOLOGY_RESULT_VERIFY_CLINICAL = "pathology.result.verify_clinical";
    public static final String PATHOLOGY_MACHINE_MANAGE = "pathology.machine.manage";
    public static final String PATHOLOGY_MACHINE_MESSAGE_MANAGE = "pathology.machine.message.manage";
    public static final String PATHOLOGY_QC_MANAGE = "pathology.qc.manage";

    public static final String DIAGNOSTIC_REPORTS_VIEW = "diagnostic_reports.view";
    public static final String DIAGNOSTIC_REPORTS_EDIT = "diagnostic_reports.edit";
    public static final String DIAGNOSTIC_REPORTS_VALIDATE = "diagnostic_reports.validate";
    public static final String DIAGNOSTIC_REPORTS_SIGN = "diagnostic_reports.sign";
    public static final String DIAGNOSTIC_REPORTS_RELEASE = "diagnostic_reports.release";
    public static final String DIAGNOSTIC_REPORTS_AMEND = "diagnostic_reports.amend";

    public static final String CRITICAL_ALERTS_ACKNOWLEDGE = "critical_alerts.acknowledge";

    public static final String AUDIT_LOGS_VIEW = "audit_logs.view";
}