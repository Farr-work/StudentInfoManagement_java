package com.studentinfomanagement.views.admin;

import com.studentinfomanagement.database.DatabaseHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class DashboardViewController {

    @FXML private Text txtTotalStudent;
    @FXML private Text txtStudying;
    @FXML private Text txtGraduated;
    @FXML private Text txtDropout;

    @FXML private VBox pnlNotifications;
    @FXML private VBox pnlActivities;

    @FXML private GridPane overlayInput;
    @FXML private TextField txtNotiTitle;
    @FXML private TextArea txtNotiContent;

    @FXML private GridPane overlayDetail;
    @FXML private Text lblDetailTitle;
    @FXML private Text lblDetailDate;
    @FXML private Text lblDetailContent;

    @FXML
    public void initialize() {
        loadAllData();
    }

    private void loadAllData() {
        loadStatistics();
        loadNotifications();
        loadActivities();
    }

    private void loadStatistics() {
        try (Connection conn = DatabaseHelper.getConnection()) {
            txtTotalStudent.setText(getCount(conn, "SELECT COUNT(*) FROM Student"));
            txtStudying.setText(getCount(conn, "SELECT COUNT(*) FROM Student WHERE trangthai = N'Đang học'"));
            txtGraduated.setText(getCount(conn, "SELECT COUNT(*) FROM Student WHERE trangthai = N'Tốt nghiệp'"));
            txtDropout.setText(getCount(conn, "SELECT COUNT(*) FROM Student WHERE trangthai = N'Thôi học'"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement cmd = conn.prepareStatement(sql);
             ResultSet rs = cmd.executeQuery()) {
            if (rs.next()) {
                return String.valueOf(rs.getInt(1));
            }
        }
        return "0";
    }

    private void loadNotifications() {
        pnlNotifications.getChildren().clear();
        String sql = "SELECT TOP 5 Id, Title, Content, CreatedAt FROM Notifications ORDER BY CreatedAt DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql);
             ResultSet rs = cmd.executeQuery()) {

            while (rs.next()) {
                String title = rs.getString("Title");
                String content = rs.getString("Content");
                LocalDateTime createdAt = rs.getTimestamp("CreatedAt").toLocalDateTime();
                String dateStr = createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                BorderPane border = new BorderPane();
                border.getStyleClass().add("list-item");
                border.setPadding(new Insets(15));
                VBox.setMargin(border, new Insets(0, 0, 10, 0));

                VBox vbox = new VBox(2);
                Label lblTitle = new Label(title);
                lblTitle.setFont(Font.font("System", FontWeight.MEDIUM, 14));
                
                Label lblDate = new Label(dateStr);
                lblDate.setFont(Font.font("System", 12));
                lblDate.setTextFill(Color.GRAY);

                vbox.getChildren().addAll(lblTitle, lblDate);
                border.setCenter(vbox);

                border.setOnMouseClicked(event -> showNotificationDetail(title, content, dateStr));

                pnlNotifications.getChildren().add(border);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadActivities() {
        pnlActivities.getChildren().clear();
        String sql = "SELECT TOP 5 ActionName, CreatedAt FROM ActivityLog ORDER BY CreatedAt DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql);
             ResultSet rs = cmd.executeQuery()) {

            while (rs.next()) {
                String action = rs.getString("ActionName");
                LocalDateTime createdAt = rs.getTimestamp("CreatedAt").toLocalDateTime();
                String timeAgo = getTimeAgo(createdAt);

                BorderPane border = new BorderPane();
                border.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");
                border.setPadding(new Insets(15));
                VBox.setMargin(border, new Insets(0, 0, 10, 0));

                GridPane grid = new GridPane();
                grid.setHgap(15);

                Circle dot = new Circle(5, Color.web("#22C55E"));
                GridPane.setMargin(dot, new Insets(5, 0, 0, 0));

                VBox vbox = new VBox(2);
                Label lblAction = new Label(action);
                lblAction.setWrapText(true);
                lblAction.setFont(Font.font("System", FontWeight.MEDIUM, 14));
                
                Label lblTime = new Label(timeAgo);
                lblTime.setFont(Font.font("System", 12));
                lblTime.setTextFill(Color.GRAY);

                vbox.getChildren().addAll(lblAction, lblTime);

                grid.add(dot, 0, 0);
                grid.add(vbox, 1, 0);

                border.setCenter(grid);
                pnlActivities.getChildren().add(border);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleAddNoti() {
        txtNotiTitle.setText("");
        txtNotiContent.setText("");
        overlayInput.setVisible(true);
    }

    @FXML
    private void handleCancelNoti() {
        overlayInput.setVisible(false);
    }

    @FXML
    private void handleSubmitNoti() {
        String title = txtNotiTitle.getText().trim();
        String content = txtNotiContent.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ tiêu đề và nội dung.");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String insertSql = "INSERT INTO Notifications (Title, Content, CreatedAt) VALUES (?, ?, GETDATE())";
            try (PreparedStatement cmd = conn.prepareStatement(insertSql)) {
                cmd.setString(1, title);
                cmd.setString(2, content);
                cmd.executeUpdate();
            }

            String cleanSql = "DELETE FROM Notifications WHERE Id NOT IN (SELECT TOP 5 Id FROM Notifications ORDER BY CreatedAt DESC)";
            try (PreparedStatement cmd = conn.prepareStatement(cleanSql)) {
                cmd.executeUpdate();
            }

            String logSql = "INSERT INTO ActivityLog (ActionName, CreatedAt) VALUES (?, GETDATE())";
            try (PreparedStatement cmd = conn.prepareStatement(logSql)) {
                cmd.setString(1, "Admin đã thêm thông báo: " + title);
                cmd.executeUpdate();
            }

            loadNotifications();
            loadActivities();
            overlayInput.setVisible(false);
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Lỗi Database: " + e.getMessage());
        }
    }

    @FXML
    private void handleCloseDetail() {
        overlayDetail.setVisible(false);
    }

    private void showNotificationDetail(String title, String content, String date) {
        lblDetailTitle.setText(title);
        lblDetailContent.setText(content);
        lblDetailDate.setText(date);
        overlayDetail.setVisible(true);
    }


    private String getTimeAgo(LocalDateTime date) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(date, now);
        long minutes = duration.toMinutes();

        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        long hours = duration.toHours();
        if (hours < 24) return hours + " giờ trước";
        long days = duration.toDays();
        if (days < 7) return days + " ngày trước";
        
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void showAlert(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(content);
        alert.show();
    }
}