package com.studentinfomanagement.views;

import com.studentinfomanagement.App;
import com.studentinfomanagement.database.DatabaseHelper;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterWindowController {

    @FXML private TextField txtVeriCode;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    private final DatabaseHelper dbHelper = new DatabaseHelper();

    @FXML
    private void handleRegister() {
        String vericode = txtVeriCode.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (vericode.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        if (!"2005".equals(vericode)) {
            showAlert(Alert.AlertType.ERROR, "Truy cập bị từ chối", "Mã định danh không đúng! Bạn không có quyền tạo tài khoản Admin.");
            return;
        }

        Object[] result = dbHelper.registerAdmin(username, password);
        boolean isSuccess = (boolean) result[0];
        String message = (String) result[1];

        if (isSuccess) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký Admin thành công! Vui lòng đăng nhập.");
            try {
                App.setRoot("views/LoginWindow");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại", message);
        }
    }

    @FXML
    private void handleLoginLink() throws IOException {
        App.setRoot("views/LoginWindow");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}