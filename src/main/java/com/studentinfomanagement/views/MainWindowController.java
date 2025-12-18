package com.studentinfomanagement.views;

import com.studentinfomanagement.App;
import java.io.IOException;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;

public class MainWindowController {

    @FXML
    private StackPane mainContent; 

    @FXML
    public void initialize() {
        loadView("admin/DashboardView");
    }

    @FXML
    private void handleMenuClick(ActionEvent event) {
        Button btn = (Button) event.getSource();
        
        String viewName = (String) btn.getUserData();

        if (viewName != null) {
            switch (viewName) {
                case "Dashboard":
                    loadView("admin/DashboardView");
                    break;
                case "Students":
                     loadView("admin/StudentsView"); 
                    break;
                case "Courses":
                     loadView("admin/CoursesView");
                    break;
                case "Portal":
                     loadView("admin/CoursesRegister");
                    break;
                case "Settings":
                     loadView("admin/SettingsView");
                    break;
            }
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText("Bạn chắc chắn muốn đăng xuất?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            App.setRoot("views/LoginWindow");
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/" + fxmlPath + ".fxml"));
            Node node = loader.load();
            
            mainContent.getChildren().clear();
            mainContent.getChildren().add(node);
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Không tìm thấy file giao diện: " + fxmlPath);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}