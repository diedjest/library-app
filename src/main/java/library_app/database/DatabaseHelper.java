package library_app.database;

import library_app.models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/library_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "1234";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, role, is_first_login, failed_attempts, last_login, status FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getBoolean("is_first_login"),
                        rs.getInt("failed_attempts"),
                        rs.getTimestamp("last_login"),
                        rs.getString("status")
                );
            }
            return null;
        }
    }

    public static void blockUser(int userId) throws SQLException {
        String sql = "UPDATE users SET status = 'blocked' WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public static void updateFailedAttempts(int userId, int attempts) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attempts);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public static void recordSuccessfulLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0, last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public static void updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, is_first_login = FALSE WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public static List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role, is_first_login, failed_attempts, last_login, status FROM users ORDER BY id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getBoolean("is_first_login"),
                        rs.getInt("failed_attempts"),
                        rs.getTimestamp("last_login"),
                        rs.getString("status")
                ));
            }
        }
        return users;
    }

    public static void unlockUserAndResetAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET status = 'active', failed_attempts = 0 WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public static boolean checkUserExists(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static void addUser(String username, String passwordHash, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role, is_first_login) VALUES (?, ?, ?, TRUE)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            stmt.executeUpdate();
        }
    }
}