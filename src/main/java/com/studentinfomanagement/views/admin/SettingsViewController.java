package com.studentinfomanagement.views.admin;

import com.studentinfomanagement.database.DatabaseHelper;
import com.studentinfomanagement.database.GlobalConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SettingsViewController {

    @FXML private TextField txtID;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;
    
    @FXML private Label lblDisplayName;
    @FXML private Label lblDisplayRole;
    
    @FXML private PasswordField pbCurrentPass;
    @FXML private PasswordField pbNewPass;
    @FXML private PasswordField pbConfirmPass;

    @FXML
    public void initialize() {
        loadAdminProfile();
    }

    private void loadAdminProfile() {
        lblDisplayName.setText("Administrator");
        lblDisplayRole.setText("Quản Trị Hệ Thống");
        
        txtID.setText(GlobalConfig.CurrentUserID);
        txtFullName.setText("Admin User");
        txtEmail.setText("admin@system.com");
        
        setEditable(false);
    }
    
    private void setEditable(boolean editable) {
        txtFullName.setEditable(editable);
        txtEmail.setEditable(editable);
        txtPhone.setEditable(editable);
        txtAddress.setEditable(editable);
    }

    @FXML
    private void handleSaveInfo() {
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Chức năng cập nhật thông tin đang được hoàn thiện.");
    }

    @FXML
    private void handleChangePass() {
        String currentPass = pbCurrentPass.getText();
        String newPass = pbNewPass.getText();
        String confirmPass = pbConfirmPass.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            return;
        }

        String currentUserId = GlobalConfig.CurrentUserID;
        if (currentUserId == null || currentUserId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi phiên đăng nhập! Vui lòng đăng nhập lại.");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM Users WHERE Username = ? AND Password = ?";
            try (PreparedStatement checkCmd = conn.prepareStatement(checkSql)) {
                checkCmd.setString(1, currentUserId);
                checkCmd.setString(2, currentPass);
                
                ResultSet rs = checkCmd.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu hiện tại không đúng!");
                    return;
                }
            }

            String updateSql = "UPDATE Users SET Password = ? WHERE Username = ?";
            try (PreparedStatement updateCmd = conn.prepareStatement(updateSql)) {
                updateCmd.setString(1, newPass);
                updateCmd.setString(2, currentUserId);
                updateCmd.executeUpdate();
                
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi mật khẩu thành công!");
                clearPassFields();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi kết nối CSDL: " + e.getMessage());
        }
    }

    private void clearPassFields() {
        pbCurrentPass.clear();
        pbNewPass.clear();
        pbConfirmPass.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}