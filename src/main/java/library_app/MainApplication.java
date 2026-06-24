package library_app;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MainApplication {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/library_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "pass";

    private static JFrame currentFrame;

    public static void main(String[] resignation) {
        SwingUtilities.invokeLater(MainApplication::showLoginWindow);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void showLoginWindow() {
        if (currentFrame != null) currentFrame.dispose();

        JFrame frame = new JFrame("Библиотека: Вход в систему");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("Авторизация", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JTextField loginField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Войти");

        panel.add(titleLabel);
        panel.add(new JLabel("Логин:"));
        panel.add(loginField);
        panel.add(new JLabel("Пароль:"));
        panel.add(passwordField);
        panel.add(loginButton);

        loginButton.addActionListener(e -> {
            String username = loginField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Все поля обязательны для заполнения!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            processLogin(frame, username, password);
        });

        currentFrame = frame;
        frame.add(panel);
        frame.setVisible(true);
    }

    private static void processLogin(JFrame parent, String username, String password) {
        String sql = "SELECT id, password_hash, role, is_first_login, failed_attempts, last_login, status FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(parent, "Вы ввели неверный логин или пароль. Пожалуйста проверьте ещё раз введенные данные.", "Ошибка авторизации", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int userId = rs.getInt("id");
            String dbHash = rs.getString("password_hash");
            String role = rs.getString("role");
            boolean isFirstLogin = rs.getBoolean("is_first_login");
            int failedAttempts = rs.getInt("failed_attempts");
            Timestamp lastLogin = rs.getTimestamp("last_login");
            String status = rs.getString("status");

            if ("blocked".equals(status)) {
                JOptionPane.showMessageDialog(parent, "Вы заблокированы. Обратитесь к администратору.", "Аккаунт заблокирован", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (lastLogin != null) {
                LocalDateTime lastLoginTime = lastLogin.toLocalDateTime();
                if (ChronoUnit.DAYS.between(lastLoginTime, LocalDateTime.now()) > 30) {
                    executeControlQuery("UPDATE users SET status = 'blocked' WHERE id = ?", userId);
                    JOptionPane.showMessageDialog(parent, "Вы заблокированы. Обратитесь к администратору.", "Аккаунт заблокирован", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String inputHash = hashPassword(password);
            if (dbHash.equals(inputHash)) {
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE users SET failed_attempts = 0, last_login = CURRENT_TIMESTAMP WHERE id = ?")) {
                    updateStmt.setInt(1, userId);
                    updateStmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(parent, "Вы успешно авторизовались.", "Успех", JOptionPane.INFORMATION_MESSAGE);

                if (isFirstLogin) {
                    showChangePasswordWindow(userId, role);
                } else {
                    loadMainInterface(role, username);
                }
            } else {
                failedAttempts++;
                if (failedAttempts >= 3) {
                    executeControlQuery("UPDATE users SET failed_attempts = ?, status = 'blocked' WHERE id = ?", failedAttempts, userId);
                    JOptionPane.showMessageDialog(parent, "Вы заблокированы. Обратитесь к администратору.", "Аккаунт заблокирован", JOptionPane.ERROR_MESSAGE);
                } else {
                    executeControlQuery("UPDATE users SET failed_attempts = ? WHERE id = ?", failedAttempts, userId);
                    JOptionPane.showMessageDialog(parent, "Вы ввели неверный логин или пароль. Пожалуйста проверьте ещё раз введенные данные.", "Ошибка авторизации", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(parent, "Ошибка работы с БД: " + ex.getMessage(), "Критическая ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showChangePasswordWindow(int userId, String role) {
        currentFrame.dispose();
        JFrame frame = new JFrame("Смена временного пароля");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPasswordField currentPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confirmPass = new JPasswordField();
        JButton changeButton = new JButton("Изменить пароль");

        panel.add(new JLabel("Текущий пароль:")); panel.add(currentPass);
        panel.add(new JLabel("Новый пароль:")); panel.add(newPass);
        panel.add(new JLabel("Подтверждение:")); panel.add(confirmPass);
        panel.add(new JLabel("")); panel.add(changeButton);

        changeButton.addActionListener(e -> {
            String curStr = new String(currentPass.getPassword());
            String newStr = new String(newPass.getPassword());
            String confStr = new String(confirmPass.getPassword());

            if (curStr.isEmpty() || newStr.isEmpty() || confStr.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Все поля обязательны для заполнения!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newStr.equals(confStr)) {
                JOptionPane.showMessageDialog(frame, "Новый пароль и подтверждение не совпадают!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement check = conn.prepareStatement("SELECT password_hash FROM users WHERE id = ?");
                check.setInt(1, userId);
                ResultSet rs = check.executeQuery();
                if (rs.next() && rs.getString("password_hash").equals(hashPassword(curStr))) {

                    PreparedStatement update = conn.prepareStatement(
                            "UPDATE users SET password_hash = ?, is_first_login = FALSE WHERE id = ?");
                    update.setString(1, hashPassword(newStr));
                    update.setInt(2, userId);
                    update.executeUpdate();

                    JOptionPane.showMessageDialog(frame, "Вы успешно сменили пароль.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                    loadMainInterface(role, "user_id_" + userId);
                } else {
                    JOptionPane.showMessageDialog(frame, "Текущий пароль введен неверно!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        currentFrame = frame;
        frame.add(panel);
        frame.setVisible(true);
    }

    private static void loadMainInterface(String role, String username) {
        currentFrame.dispose();
        if ("administrator".equals(role)) {
            showAdminBranch();
        } else {
            showUserBranch(username);
        }
    }

    public static void showAdminBranch() {
        JFrame frame = new JFrame("Рабочий стол Администратора: Безопасность");
        frame.setSize(700, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Логин", "Роль", "Статус", "Ошибки"}, 0);
        JTable table = new JTable(model);
        refreshAdminTable(model);

        JButton unlockButton = new JButton("Снять блокировку");
        JButton addButton = new JButton("Добавить пользователя");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(unlockButton);

        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        unlockButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) return;
            int id = (int) table.getValueAt(selectedRow, 0);

            executeControlQuery("UPDATE users SET status = 'active', failed_attempts = 0 WHERE id = ?", id);
            refreshAdminTable(model);
            JOptionPane.showMessageDialog(frame, "Пользователь успешно разблокирован.");
        });

        addButton.addActionListener(e -> {
            JTextField uField = new JTextField();
            JComboBox<String> rBox = new JComboBox<>(new String[]{"user", "administrator"});
            Object[] message = {"Логин:", uField, "Роль:", rBox};

            int option = JOptionPane.showConfirmDialog(frame, message, "Новый пользователь", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String newUser = uField.getText().trim();
                String newRole = (String) rBox.getSelectedItem();

                if (newUser.isEmpty()) return;

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    PreparedStatement check = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
                    check.setString(1, newUser);
                    if (check.executeQuery().next()) {
                        JOptionPane.showMessageDialog(frame, "Пользователь с таким логином уже существует!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, role, is_first_login) VALUES (?, ?, ?, TRUE)");
                    ins.setString(1, newUser);
                    ins.setString(2, hashPassword("password"));
                    ins.setString(3, newRole);
                    ins.executeUpdate();

                    refreshAdminTable(model);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        currentFrame = frame;
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    public static void showUserBranch(String username) {
        JFrame frame = new JFrame("Рабочий стол Библиотекаря: " + username);
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        JLabel lbl = new JLabel("Добро пожаловать в ИС Библиотека!", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        frame.add(lbl);

        currentFrame = frame;
        frame.setVisible(true);
    }

    private static void refreshAdminTable(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username, role, status, failed_attempts FROM users ORDER BY id")) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("username"),
                        rs.getString("role"), rs.getString("status"), rs.getInt("failed_attempts")});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private static void executeControlQuery(String sql, Object... params) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}