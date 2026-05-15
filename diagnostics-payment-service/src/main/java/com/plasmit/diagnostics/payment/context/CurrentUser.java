package com.plasmit.diagnostics.payment.context;

public class CurrentUser {

    private Long userId;
    private Long tenantId;
    private Long hospitalId;
    private String email;
    private String role;
    private String userType;

    public CurrentUser() {
    }

    public CurrentUser(Long userId,
                       Long tenantId,
                       Long hospitalId,
                       String email,
                       String role,
                       String userType) {
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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}