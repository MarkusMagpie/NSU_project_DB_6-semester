package view;

import utils.DatabaseConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class LoginDialog extends JDialog {
    private final JComboBox<String> roleCombo;

    public LoginDialog(JFrame parent) {
        super(parent, "Выбор роли", true);

        setSize(350, 150);
        setResizable(false);
        setLocationRelativeTo(parent);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel welcomeLabel = new JLabel("Добро пожаловать в ИС аптеки!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(welcomeLabel, gbc);

        gbc.gridy = 2; gbc.gridwidth = 1;
        add(new JLabel("Роль:"), gbc);
        gbc.gridx = 1;
        String[] items = {"Регистратор", "Кладовщик", "Фармацевт", "Администратор"};
        roleCombo = new JComboBox<>(items);
        add(roleCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton loginButton = new JButton("Войти");
        add(loginButton, gbc);
        getRootPane().setDefaultButton(loginButton); // enter нажимает кнопку

        loginButton.addActionListener(e -> {
            String role = (String) roleCombo.getSelectedItem();
            String user = null;
            String pass = null;
            switch (role) {
                case "Регистратор":
                    user = "reg_user"; pass = "reg_pass";
                    break;
                case "Кладовщик":
                    user = "store_user"; pass = "store_pass";
                    break;
                case "Фармацевт":
                    user = "pharm_user"; pass = "pharm_pass";
                    break;
                case "Администратор":
                    user = "admin_user"; pass = "admin_pass";
                    break;
            }

            try {
                Connection conn = DatabaseConnection.getConnection(user, pass);
                // успешное подключение - открыть главное окно и закрыть текущее
                JOptionPane.showMessageDialog(this, "Подключение успешно. Выбрана роль: " + user);
                openMainWindow(role, conn);
                dispose();
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                JOptionPane.showMessageDialog(this, "Ошибка подключения к бд", "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void openMainWindow(String role,  Connection conn) {
        if ("Регистратор".equals(role)) {
            new RegistratorFrame(conn).setVisible(true);
        }  else if ("Кладовщик".equals(role)) {
             new StorekeeperFrame(conn).setVisible(true);
        } else if ("Фармацевт".equals(role)) {
            // new PharmacistFrame(conn).setVisible(true);
            return;
        } else if ("Администратор".equals(role)) {
             new AdminFrame(conn).setVisible(true);
        }
    }
}