package com.studentinfomanagement.views;

import com.studentinfomanagement.App;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;

public class StudentWindowController {

    @FXML
    private StackPane mainContent;

    @FXML
    public void initialize() {
        loadView("student/StudentDashboardView");
    }

    @FXML
    private void handleMenuClick(ActionEvent event) {
        Button btn = (Button) event.getSource();
        String viewName = (String) btn.getUserData();

        if (viewName != null) {
            switch (viewName) {
                case "StudentDashboard":
                    loadView("student/StudentDashboardView");
                    break;
                    
                case "StudentCourses":
                    loadView("student/StudentCoursesView"); 
                    break;
                    
                case "StudentClasses":
                    loadView("student/StudentClassesView");
                    break;
                    
                case "StudentSettings":
                     loadView("student/StudentSettingView");
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
            String path = "/com/studentinfomanagement/views/" + fxmlPath + ".fxml";
            
            URL url = App.class.getResource(path);
            if (url == null) {
                System.err.println("❌ KHÔNG TÌM THẤY FILE FXML TẠI: " + path);
                showAlert("Không tìm thấy file giao diện: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Node node = loader.load();
            
            mainContent.getChildren().clear();
            mainContent.getChildren().add(node);
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi khi tải giao diện: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setContentText(message);
        alert.show();
    }
}