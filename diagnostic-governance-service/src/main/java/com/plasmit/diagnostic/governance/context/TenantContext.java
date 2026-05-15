package com.plasmit.diagnostic.governance.context;

public final class TenantContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> BRANCH_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentUser(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static CurrentUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static Long getUserId() {
        CurrentUser user = CURRENT_USER.get();
        return user == null ? null : user.getUserId();
    }

    public static Long getTenantId() {
        CurrentUser user = CURRENT_USER.get();
        return user == null ? null : user.getTenantId();
    }

    public static Long getHospitalId() {
        CurrentUser user = CURRENT_USER.get();
        return user == null ? null : user.getHospitalId();
    }

    public static String getRole() {
        CurrentUser user = CURRENT_USER.get();
        return user == null ? null : user.getRole();
    }

    public static String getEmail() {
        CurrentUser user = CURRENT_USER.get();
        return user == null ? null : user.getEmail();
    }

    public static void setBranchId(String branchId) {
        BRANCH_ID.set(branchId);
    }

    public static String getBranchId() {
        return BRANCH_ID.get();
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
        BRANCH_ID.remove();
        REQUEST_ID.remove();
    }
}