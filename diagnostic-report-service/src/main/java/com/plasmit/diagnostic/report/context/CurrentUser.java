package com.plasmit.diagnostic.report.context;

public class CurrentUser {

    private Long userId;
    private Long tenantId;
    private Long hospitalId;
    private String email;
    private String role;
    private String userType;

    public CurrentUser() {
    }

    public CurrentUser(Long userId, Long tenantId, Long hospitalId, String email, String role, String userType) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.hospitalId = hospitalId;
        this.email = email;
        this.role = role;
        this.userType = userType;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getUserType() {
        return userType;
    }
}