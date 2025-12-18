package com.studentinfomanagement.database;

import java.sql.*;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.util.function.Consumer;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlserver://SQL8011.site4now.net;databaseName=db_ac1c01_qlsv;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "db_ac1c01_qlsv_admin";
    private static final String PASS = "qlsv123@";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public String authenticateUser(String username, String password) {
        String role = null;
        String sql = "SELECT RoleID FROM Users WHERE Username = ? AND Password = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int roleId = rs.getInt("RoleID");
                    role = (roleId == 1) ? "Admin" : "Student";

                    GlobalConfig.CurrentUserID = username; 
                    GlobalConfig.CurrentRole = role;
                    // ---------------------------------------------------
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return role;
    }

    public Object[] registerAdmin(String username, String password) {
        try (Connection conn = getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM Users WHERE Username = ?";
            try (PreparedStatement checkCmd = conn.prepareStatement(checkSql)) {
                checkCmd.setString(1, username);
                try (ResultSet rs = checkCmd.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return new Object[]{false, "Tên đăng nhập đã tồn tại."};
                    }
                }
            }

            String insertSql = "INSERT INTO Users (Username, Password, FullName, RoleID, CreatedAt) " +
                               "VALUES (?, ?, 'Administrator', 1, GETDATE())";

            try (PreparedStatement cmd = conn.prepareStatement(insertSql)) {
                cmd.setString(1, username);
                cmd.setString(2, password);

                int rows = cmd.executeUpdate();
                if (rows > 0) {
                    return new Object[]{true, "Đăng ký Admin thành công."};
                }
            }
        } catch (SQLException e) {
            return new Object[]{false, "Lỗi: " + e.getMessage()};
        }
        return new Object[]{false, "Lỗi không xác định"};
    }

    public CachedRowSet getDataTable(String sql) {
        CachedRowSet crs = null;
        try (Connection conn = getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql);
             ResultSet rs = cmd.executeQuery()) {

            crs = RowSetProvider.newFactory().createCachedRowSet();
            crs.populate(rs);

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
        return crs;
    }

    public CachedRowSet getStudentInfo(String studentID) {
        String sql = "SELECT hoten, tenlop, diachi, email, sdt FROM Student WHERE masv = ?";
        CachedRowSet crs = null;
        try (Connection conn = getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)) {
            
            cmd.setString(1, studentID);
            try (ResultSet rs = cmd.executeQuery()) {
                crs = RowSetProvider.newFactory().createCachedRowSet();
                crs.populate(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return crs;
    }

    public Object[] changePassword(String username, String currentPassword, String newPassword) {
        try (Connection conn = getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM Users WHERE Username = ? AND Password = ?";
            try (PreparedStatement checkCmd = conn.prepareStatement(checkSql)) {
                checkCmd.setString(1, username);
                checkCmd.setString(2, currentPassword);
                
                try (ResultSet rs = checkCmd.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        return new Object[]{false, "Mật khẩu hiện tại không đúng!"};
                    }
                }
            }

            String updateSql = "UPDATE Users SET Password = ? WHERE Username = ?";
            try (PreparedStatement updateCmd = conn.prepareStatement(updateSql)) {
                updateCmd.setString(1, newPassword);
                updateCmd.setString(2, username);
                
                int rows = updateCmd.executeUpdate();
                if (rows > 0) {
                    return new Object[]{true, "Đổi mật khẩu thành công!"};
                } else {
                    return new Object[]{false, "Lỗi không xác định khi cập nhật DB."};
                }
            }
        } catch (SQLException e) {
            return new Object[]{false, "Lỗi Database: " + e.getMessage()};
        }
    }
}