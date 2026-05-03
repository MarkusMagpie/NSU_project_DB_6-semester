package view;

import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.List;



public class AdminFrame extends JFrame {
    private DefaultTableModel ordersTableModel;
    private Connection connection;

    public AdminFrame(Connection connection) {
        this.connection = connection;

        setTitle("Аптека - Администратор");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu systemMenu = new JMenu("Система");
        JMenuItem reconnectItem = new JMenuItem("Сменить пользователя");
        reconnectItem.addActionListener(e -> reLogin());
        systemMenu.add(reconnectItem);
        JMenuItem checkConnectionItem = new JMenuItem("Проверить соединение");
        checkConnectionItem.addActionListener(e -> checkConnection());
        systemMenu.add(checkConnectionItem);

        menuBar.add(systemMenu);
        setJMenuBar(menuBar);

        // вкладки
        JTabbedPane tabbedPane = new JTabbedPane();

        add(tabbedPane);

        // закытие окна -> закрытие соединения
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void reLogin() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        new LoginDialog(null).setVisible(true);

        dispose();
    }

    private void checkConnection() {
        boolean alive = DatabaseConnection.isConnectionAlive(connection);
        String msg = alive ? "Соединение с БД активно" : "Соединение с БД потеряно";
        System.out.println(msg);
        JOptionPane.showMessageDialog(this, msg, "Статус соединения",
                alive ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        if (!alive) {
            int option = JOptionPane.showConfirmDialog(this,
                    "Соединение потеряно. Переподключиться?",
                    "Ошибка", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) reLogin();
        }
    }
}