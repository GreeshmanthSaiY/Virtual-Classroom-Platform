package com.virtualclassroom.dto;


public class AuthResponse {

    private String token;
    private String tokenType;
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;

    public AuthResponse() {}

    public AuthResponse(String token, String tokenType, Long id, String email, String fullName, String role, String status) {
        this.token = token;
        this.tokenType = tokenType;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType;
        private Long id;
        private String email;
        private String fullName;
        private String role;
        private String status;

        public Builder token(String token) {
            this.token = token;
            return this;
        }
        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }
        public Builder role(String role) {
            this.role = role;
            return this;
        }
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, tokenType, id, email, fullName, role, status);
        }
    }
}
