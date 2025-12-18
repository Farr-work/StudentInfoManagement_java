package com.studentinfomanagement.views.student;

import com.studentinfomanagement.database.DatabaseHelper;
import com.studentinfomanagement.database.GlobalConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javax.sql.rowset.CachedRowSet;

public class StudentSettingViewController {

    @FXML private Label txtDisplayName;
    @FXML private Label txtDisplayRole;
    
    @FXML private TextField txtID;
    @FXML private TextField txtClassDept;
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAddress;

    @FXML private PasswordField pbCurrentPass;
    @FXML private PasswordField pbNewPass;
    @FXML private PasswordField pbConfirmPass;

    private final DatabaseHelper dbHelper = new DatabaseHelper();
    private String currentUsername;

    @FXML
    public void initialize() {
        currentUsername = GlobalConfig.CurrentUserID;
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = "SV001"; 
        }
        
        loadStudentData();
    }

    private void loadStudentData() {
        if (currentUsername == null) return;

        try {
            CachedRowSet crs = dbHelper.getStudentInfo(currentUsername);
            if (crs != null && crs.next()) {
                txtID.setText(currentUsername);
                String fullName = crs.getString("hoten");
                String className = crs.getString("tenlop");
                
                txtFullName.setText(fullName);
                txtClassDept.setText(className);
                txtEmail.setText(crs.getString("email"));
                txtPhone.setText(crs.getString("sdt"));
                txtAddress.setText(crs.getString("diachi"));

                txtDisplayName.setText(fullName);
                String cohort = className; 
                if (className != null && className.contains("-")) {
                    cohort = className.split("-")[0].trim(); 
                }
                txtDisplayRole.setText("Sinh viên - " + cohort);
            } else {
                txtFullName.setText("Lỗi: Không tìm thấy thông tin");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải thông tin: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveInfo() {
        showAlert(Alert.AlertType.INFORMATION, "Chức năng cập nhật thông tin đang được hoàn thiện.");
    }

    @FXML
    private void handleChangePass() {
        String currentPass = pbCurrentPass.getText();
        String newPass = pbNewPass.getText();
        String confirmPass = pbConfirmPass.getText();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Vui lòng nhập đầy đủ thông tin mật khẩu!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Mật khẩu xác nhận không khớp!");
            return;
        }

        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Mật khẩu mới phải tối thiểu 6 ký tự!");
            return;
        }

        if (currentPass.equals(newPass)) {
            showAlert(Alert.AlertType.ERROR, "Mật khẩu mới phải khác mật khẩu hiện tại!");
            return;
        }

        Object[] result = dbHelper.changePassword(currentUsername, currentPass, newPass);
        boolean success = (boolean) result[0];
        String message = (String) result[1];

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, message);
            pbCurrentPass.clear();
            pbNewPass.clear();
            pbConfirmPass.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, message);
        }
    }

    private void showAlert(Alert.AlertType type, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Lỗi" : "Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}