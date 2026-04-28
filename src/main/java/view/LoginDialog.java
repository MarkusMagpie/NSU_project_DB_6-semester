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

        setSize(300, 150);
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Роль:"), gbc);
        gbc.gridx = 1;
        roleCombo = new JComboBox<>(new String[]{"Регистратор", "Кладовщик", "Фармацевт", "Администратор"});
        add(roleCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JButton loginButton = new JButton("Войти");
        add(loginButton, gbc);

        loginButton.addActionListener(e -> {
            String role = (String) roleCombo.getSelectedItem();
            String user = null;
            String pass = null;
            switch (role) {
                case "Регистратор":
                    user = "reg_user";
                    pass = "reg_pass";
                    break;
                case "Кладовщик":
                    user = "store_user";
                    pass = "store_pass";
                    break;
                case "Фармацевт":
                    user = "pharm_user";
                    pass = "pharm_pass";
                    break;
                case "Администратор":
                    user = "admin_user";
                    pass = "admin_pass";
                    break;
            }

            try {
                Connection conn = DatabaseConnection.getConnection(user, pass);
                // успешное подключение - открыть главное окно и закрыть текущее
                System.out.println("подключился к бд с ролью " + user);
                JOptionPane.showMessageDialog(this, "Подключение успешно!");
                openMainWindow(role, conn);
                dispose();
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
                JOptionPane.showMessageDialog(this, "Ошибка подключения: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void openMainWindow(String role,  Connection conn) {
        JFrame mainFrame = new JFrame("Аптека - " + role);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);
        mainFrame.setLocationRelativeTo(null);

        mainFrame.setVisible(true);

        if ("Регистратор".equals(role)) {
            new RegistratorFrame(conn).setVisible(true);
        }
    }
}