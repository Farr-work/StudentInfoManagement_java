package com.studentinfomanagement.views;

import com.studentinfomanagement.App;
import com.studentinfomanagement.database.DatabaseHelper;
import com.studentinfomanagement.database.GlobalConfig;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginWindowController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private final DatabaseHelper dbHelper = new DatabaseHelper();

    @FXML
    private void handleLogin() throws IOException {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }

        String role = dbHelper.authenticateUser(username, password);

        if (role != null && !role.isEmpty()) {
            System.out.println("Login Success! User: " + GlobalConfig.CurrentUserID + ", Role: " + GlobalConfig.CurrentRole);

            if (role.equalsIgnoreCase("Admin")) {
                App.setRoot("views/MainWindow");
            } else {
                App.setRoot("views/StudentWindow"); 
            }
        } else {
            showAlert("Sai tên đăng nhập hoặc mật khẩu!");
        }
    }

    @FXML
    private void handleRegisterLink() throws IOException {
        App.setRoot("views/RegisterWindow");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}