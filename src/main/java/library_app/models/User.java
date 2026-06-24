package library_app.models;

import java.sql.Timestamp;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String role;
    private boolean isFirstLogin;
    private int failedAttempts;
    private Timestamp lastLogin;
    private String status;

    public User(int id, String username, String passwordHash, String role, boolean isFirstLogin, int failedAttempts, Timestamp lastLogin, String status) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isFirstLogin = isFirstLogin;
        this.failedAttempts = failedAttempts;
        this.lastLogin = lastLogin;
        this.status = status;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isFirstLogin() { return isFirstLogin; }
    public int getFailedAttempts() { return failedAttempts; }
    public Timestamp getLastLogin() { return lastLogin; }
    public String getStatus() { return status; }
}