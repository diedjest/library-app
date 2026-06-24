package library_app.ui;

import library_app.database.DatabaseHelper;
import library_app.models.User;
import library_app.utils.CryptoUtils;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LoginForm extends JFrame {
    private JTextField usernameInputField;
    private JPasswordField passwordInputField;
    private JButton[] captchaButtons;
    private JButton selectedCaptchaButton;

    public LoginForm() {
        setTitle("ООО «Книгомир» — ИС Библиотека: Вход в систему");
        setMinimumSize(new Dimension(450, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel welcomeLabel = new JLabel("Авторизация", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        fieldsPanel.setMaximumSize(new Dimension(400, 60));

        usernameInputField = new JTextField();
        passwordInputField = new JPasswordField();

        fieldsPanel.add(new JLabel("Логин:"));
        fieldsPanel.add(usernameInputField);
        fieldsPanel.add(new JLabel("Пароль:"));
        fieldsPanel.add(passwordInputField);

        JPanel captchaWrapperPanel = new JPanel(new BorderLayout(0, 10));
        captchaWrapperPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        JLabel captchaLabel = new JLabel("Соберите пазл (кликайте для обмена фрагментов):", SwingConstants.CENTER);
        captchaWrapperPanel.add(captchaLabel, BorderLayout.NORTH);

        JPanel captchaGridPanel = new JPanel(new GridLayout(2, 2, 2, 2));
        captchaGridPanel.setMaximumSize(new Dimension(240, 240));

        // Инициализация фрагментов капчи и их перемешивание
        List<Integer> pieces = Arrays.asList(0, 1, 2, 3);
        Collections.shuffle(pieces);

        captchaButtons = new JButton[4];
        selectedCaptchaButton = null;

        for (int i = 0; i < 4; i++) {
            int imageId = pieces.get(i);
            ImageIcon icon = new ImageIcon(new ImageIcon("images/" + (imageId + 1) + ".png")
                    .getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH));

            captchaButtons[i] = new JButton(icon);
            captchaButtons[i].setPreferredSize(new Dimension(120, 120));
            captchaButtons[i].putClientProperty("id", imageId);
            captchaButtons[i].setFocusPainted(false);
            captchaButtons[i].setContentAreaFilled(false);

            // Обработчик события клика для сборки пазла
            captchaButtons[i].addActionListener(e -> {
                JButton clicked = (JButton) e.getSource();
                if (selectedCaptchaButton == null) {
                    selectedCaptchaButton = clicked;
                    clicked.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
                } else {
                    Icon tempIcon = selectedCaptchaButton.getIcon();
                    int tempId = (int) selectedCaptchaButton.getClientProperty("id");

                    selectedCaptchaButton.setIcon(clicked.getIcon());
                    selectedCaptchaButton.putClientProperty("id", clicked.getClientProperty("id"));
                    selectedCaptchaButton.setBorder(UIManager.getBorder("Button.border"));

                    clicked.setIcon(tempIcon);
                    clicked.putClientProperty("id", tempId);

                    selectedCaptchaButton = null;
                }
            });
            captchaGridPanel.add(captchaButtons[i]);
        }

        JPanel gridCenteringPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        gridCenteringPanel.add(captchaGridPanel);
        captchaWrapperPanel.add(gridCenteringPanel, BorderLayout.CENTER);

        JButton loginButton = new JButton("Войти");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(welcomeLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(fieldsPanel);
        mainPanel.add(captchaWrapperPanel);
        mainPanel.add(loginButton);

        loginButton.addActionListener(e -> attemptLogin());

        add(mainPanel);
    }

    private void attemptLogin() {
        String username = usernameInputField.getText().trim();
        String password = new String(passwordInputField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Все поля обязательны для заполнения!", "Ошибка валидации", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Валидация ограничения базы данных на длину логина (предотвращение SQL ошибок)
        if (username.length() > 50) {
            JOptionPane.showMessageDialog(this, "Длина логина не должна превышать 50 символов.", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Проверка правильности собранной интерактивной капчи.
        // Массив targetOrder задает эталонный порядок:
        // 0 - верх лево, 1 - верх право, 2 - низ лево, 3 - низ право.
        int[] targetOrder = {0, 1, 2, 3};
        boolean isCaptchaSolved = true;
        for (int i = 0; i < 4; i++) {
            if ((int) captchaButtons[i].getClientProperty("id") != targetOrder[i]) {
                isCaptchaSolved = false;
                break;
            }
        }

        processAuthentication(username, password, isCaptchaSolved);
    }

    private void processAuthentication(String username, String password, boolean isCaptchaSolved) {
        try {
            User user = DatabaseHelper.getUserByUsername(username);

            if (user == null) {
                JOptionPane.showMessageDialog(this, "Вы ввели неверный логин или пароль.", "Ошибка авторизации", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ("blocked".equals(user.getStatus())) {
                JOptionPane.showMessageDialog(this, "Вы заблокированы. Обратитесь к администратору.", "Аккаунт заблокирован", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Проверка неактивности учетной записи более 30 дней
            if (user.getLastLogin() != null) {
                LocalDateTime lastLoginTime = user.getLastLogin().toLocalDateTime();
                if (ChronoUnit.DAYS.between(lastLoginTime, LocalDateTime.now()) > 30) {
                    DatabaseHelper.blockUser(user.getId());
                    JOptionPane.showMessageDialog(this, "Вы заблокированы. Обратитесь к администратору.", "Аккаунт заблокирован", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String inputHash = CryptoUtils.hashPassword(password);
            boolean isPasswordCorrect = user.getPasswordHash().equals(inputHash);

            if (isPasswordCorrect && isCaptchaSolved) {
                DatabaseHelper.recordSuccessfulLogin(user.getId());
                JOptionPane.showMessageDialog(this, "Вы успешно авторизовались.", "Успех", JOptionPane.INFORMATION_MESSAGE);
                openNextInterface(user);
            } else {
                handleFailedAttempt(user, isCaptchaSolved);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Произошла ошибка при работе с базой данных. Проверьте соединение с сервером.\nДетали: " + ex.getMessage(), "Ошибка сервера", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleFailedAttempt(User user, boolean isCaptchaSolved) throws SQLException {
        int newAttemptsCount = user.getFailedAttempts() + 1;
        if (newAttemptsCount >= 3) {
            DatabaseHelper.updateFailedAttempts(user.getId(), newAttemptsCount);
            DatabaseHelper.blockUser(user.getId());
            JOptionPane.showMessageDialog(this, "Вы заблокированы. Обратитесь к администратору.", "Аккаунт заблокирован", JOptionPane.ERROR_MESSAGE);
        } else {
            DatabaseHelper.updateFailedAttempts(user.getId(), newAttemptsCount);
            if (!isCaptchaSolved) {
                JOptionPane.showMessageDialog(this, "Пазл собран неверно! Попытка: " + newAttemptsCount + " из 3", "Ошибка капчи", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Вы ввели неверный логин или пароль. Попытка: " + newAttemptsCount + " из 3", "Ошибка авторизации", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openNextInterface(User user) {
        this.dispose();
        if (user.isFirstLogin()) {
            new ChangePasswordForm(user).setVisible(true);
        } else if ("administrator".equals(user.getRole())) {
            new AdminDashboardForm().setVisible(true);
        } else {
            new UserDashboardForm(user.getUsername()).setVisible(true);
        }
    }
}