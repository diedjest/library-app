package library_app.ui;

import library_app.database.DatabaseHelper;
import library_app.models.User;
import library_app.utils.CryptoUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class AdminDashboardForm extends JFrame {
    private DefaultTableModel tableModel;
    private JTable usersTable;

    public AdminDashboardForm() {
        setTitle("ООО «Книгомир» — Рабочий стол Администратора: Безопасность");
        setMinimumSize(new Dimension(750, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(new String[]{"ID", "Логин", "Роль", "Статус", "Ошибки"}, 0);
        usersTable = new JTable(tableModel);

        loadTableData();

        JButton unlockUserButton = new JButton("Снять блокировку");
        JButton addUserButton = new JButton("Добавить пользователя");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addUserButton);
        buttonPanel.add(unlockUserButton);

        mainPanel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        unlockUserButton.addActionListener(e -> unlockSelectedUser());
        addUserButton.addActionListener(e -> createNewUser());

        add(mainPanel);
    }

    private void loadTableData() {
        tableModel.setRowCount(0);
        try {
            List<User> users = DatabaseHelper.getAllUsers();
            for (User user : users) {
                tableModel.addRow(new Object[]{
                        user.getId(),
                        user.getUsername(),
                        user.getRole(),
                        user.getStatus(),
                        user.getFailedAttempts()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Не удалось загрузить список пользователей.\nДетали: " + ex.getMessage(), "Ошибка БД", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void unlockSelectedUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Выберите пользователя в таблице для разблокировки.", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int userId = (int) usersTable.getValueAt(selectedRow, 0);

        try {
            DatabaseHelper.unlockUserAndResetAttempts(userId);
            loadTableData();
            JOptionPane.showMessageDialog(this, "Пользователь успешно разблокирован.", "Успех", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при разблокировке пользователя.\nДетали: " + ex.getMessage(), "Ошибка сервера", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewUser() {
        JTextField usernameInputField = new JTextField();
        JComboBox<String> roleComboBox = new JComboBox<>(new String[]{"user", "administrator"});
        Object[] message = {"Логин:", usernameInputField, "Роль:", roleComboBox};

        int option = JOptionPane.showConfirmDialog(this, message, "Новый пользователь", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String newUsername = usernameInputField.getText().trim();
            String newRole = (String) roleComboBox.getSelectedItem();

            if (newUsername.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Имя пользователя не может быть пустым.", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (newUsername.length() > 50) {
                JOptionPane.showMessageDialog(this, "Длина логина не должна превышать 50 символов.", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (DatabaseHelper.checkUserExists(newUsername)) {
                    JOptionPane.showMessageDialog(this, "Пользователь с таким логином уже существует!", "Конфликт", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Запись пользователя с зашифрованным паролем "password" по умолчанию
                DatabaseHelper.addUser(newUsername, CryptoUtils.hashPassword("password"), newRole);
                loadTableData();
                JOptionPane.showMessageDialog(this, "Пользователь успешно добавлен.", "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при добавлении пользователя.\nДетали: " + ex.getMessage(), "Ошибка сервера", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}