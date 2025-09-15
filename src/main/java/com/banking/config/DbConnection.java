package com.banking.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
    private static String url = "jdbc:h2:~/atmDb;AUTO_SERVER=TRUE";
    private static String username = "sa";
    private static String password = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
