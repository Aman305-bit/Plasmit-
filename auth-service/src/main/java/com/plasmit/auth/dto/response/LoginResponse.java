package com.plasmit.auth.dto.response;

public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private boolean requiresMfa;
    private AuthUserResponse user;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, boolean requiresMfa, AuthUserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.requiresMfa = requiresMfa;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isRequiresMfa() {
        return requiresMfa;
    }

    public AuthUserResponse getUser() {
        return user;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setRequiresMfa(boolean requiresMfa) {
        this.requiresMfa = requiresMfa;
    }

    public void setUser(AuthUserResponse user) {
        this.user = user;
    }

    public static class AuthUserResponse {

        private Long id;
        private Long tenantId;
        private String name;
        private String email;
        private String role;
        private String userType;

        public AuthUserResponse() {
        }

        public AuthUserResponse(Long id, Long tenantId, String name, String email, String role, String userType) {
            this.id = id;
            this.tenantId = tenantId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.userType = userType;
        }

        public Long getId() {
            return id;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public String getName() {
            return name;
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

        public void setId(Long id) {
            this.id = id;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public void setName(String name) {
            this.name = name;
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
}