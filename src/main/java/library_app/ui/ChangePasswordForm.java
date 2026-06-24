package library_app.ui;

import library_app.database.DatabaseHelper;
import library_app.models.User;
import library_app.utils.CryptoUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class ChangePasswordForm extends JFrame {
    private JPasswordField currentPasswordInputField;
    private JPasswordField newPasswordInputField;
    private JPasswordField confirmPasswordInputField;
    private User currentUser;

    public ChangePasswordForm(User user) {
        this.currentUser = user;

        setTitle("ООО «Книгомир» — Смена временного пароля");
        setMinimumSize(new Dimension(450, 350));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        currentPasswordInputField = new JPasswordField();
        newPasswordInputField = new JPasswordField();
        confirmPasswordInputField = new JPasswordField();
        JButton changePasswordButton = new JButton("Изменить пароль");

        mainPanel.add(new JLabel("Текущий пароль:"));
        mainPanel.add(currentPasswordInputField);

        mainPanel.add(new JLabel("Новый пароль:"));
        mainPanel.add(newPasswordInputField);

        mainPanel.add(new JLabel("Подтверждение:"));
        mainPanel.add(confirmPasswordInputField);

        mainPanel.add(new JLabel("")); // Пустая метка для выравнивания сетки
        mainPanel.add(changePasswordButton);

        changePasswordButton.addActionListener(e -> attemptPasswordChange());

        add(mainPanel);
    }

    private void attemptPasswordChange() {
        String currentPassword = new String(currentPasswordInputField.getPassword());
        String newPassword = new String(newPasswordInputField.getPassword());
        String confirmPassword = new String(confirmPasswordInputField.getPassword());

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Все поля обязательны для заполнения!", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Новый пароль и подтверждение не совпадают!", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Проверка текущего пароля на уровне объекта сущности
            if (currentUser.getPasswordHash().equals(CryptoUtils.hashPassword(currentPassword))) {
                DatabaseHelper.updatePassword(currentUser.getId(), CryptoUtils.hashPassword(newPassword));
                JOptionPane.showMessageDialog(this, "Вы успешно сменили пароль.", "Успех", JOptionPane.INFORMATION_MESSAGE);

                this.dispose();
                if ("administrator".equals(currentUser.getRole())) {
                    new AdminDashboardForm().setVisible(true);
                } else {
                    new UserDashboardForm(currentUser.getUsername()).setVisible(true);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Текущий пароль введен неверно!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Произошла ошибка при работе с базой данных.\n" + ex.getMessage(), "Ошибка сервера", JOptionPane.ERROR_MESSAGE);
        }
    }
}