package com.example.functionality;

import java.sql.*;

public class H2DatabaseManager {
    // JDBC URL, username, and password
    private static final String JDBC_URL = "jdbc:h2:~/test";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";

    // SQL statements
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS data_table (" +
            "deviceid VARCHAR(255), " +
            "recordid VARCHAR(255), " +
            "data CLOB, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    private static final String INSERT_SQL = "INSERT INTO data_table (deviceid, recordid, data) VALUES (?, ?, ?)";
   // private static final String SELECT_LAST_SQL = "SELECT * FROM data_table WHERE deviceid = ? ORDER BY recordid DESC LIMIT 1";
    private static final String SELECT_LAST_SQL = "SELECT * FROM data_table ORDER BY recordid DESC LIMIT 1";

    private static final String DELETE_SQL = "DELETE FROM data_table WHERE deviceid = ? AND recordid = ?";

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            // Create table
         //  dropTable();
            stmt.executeUpdate(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    private static final String DROP_TABLE_SQL = "DROP TABLE IF EXISTS data_table";
    public static void dropTable() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            // Drop table if exists
            stmt.executeUpdate(DROP_TABLE_SQL);
            System.out.println("Table 'data_table' dropped successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addData(String deviceId, String recordId, String data) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            pstmt.setString(1, deviceId);
            pstmt.setString(2, recordId);
            pstmt.setString(3, data);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            
        }
    }

    public static ResultSet getLastData(String deviceId) {
        ResultSet rs = null;
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_LAST_SQL)) {
           // pstmt.setString(1, deviceId);
            rs = pstmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }

    public static void deleteData(String deviceId, String recordId) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setString(1, deviceId);
            pstmt.setString(2, recordId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
