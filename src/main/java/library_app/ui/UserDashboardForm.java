package library_app.ui;

import javax.swing.*;
import java.awt.*;

public class UserDashboardForm extends JFrame {

    public UserDashboardForm(String username) {
        setTitle("ООО «Книгомир» — Рабочий стол Библиотекаря: " + username);
        setMinimumSize(new Dimension(500, 350));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel welcomeLabel = new JLabel("Добро пожаловать в ИС Библиотека, " + username + "!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        add(welcomeLabel, BorderLayout.CENTER);
    }
}