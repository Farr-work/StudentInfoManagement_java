package com.studentinfomanagement.views.student;

import com.studentinfomanagement.database.DatabaseHelper;
import com.studentinfomanagement.database.GlobalConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class StudentDashboardViewController {

    @FXML private Label txtStudentName;
    @FXML private Label txtStudentID;
    @FXML private Label txtClass;

    @FXML private VBox vboxNotifications;

    @FXML private GridPane overlayDetail;
    @FXML private Label lblDetailTitle;
    @FXML private Label lblDetailDate;
    @FXML private Text txtDetailContent;

    private String currentStudentID;

    @FXML
    public void initialize() {
        currentStudentID = GlobalConfig.CurrentUserID;
        if (currentStudentID == null || currentStudentID.isEmpty()) {
            currentStudentID = "SV001"; 
        }

        loadStudentProfile();
        loadNotifications();
    }

    private void loadStudentProfile() {
        String sql = "SELECT hoten, tenlop FROM Student WHERE masv = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)) {
            
            cmd.setString(1, currentStudentID);
            try (ResultSet rs = cmd.executeQuery()) {
                if (rs.next()) {
                    txtStudentName.setText(rs.getString("hoten"));
                    txtStudentID.setText("MSSV: " + currentStudentID);
                    txtClass.setText("Lớp: " + rs.getString("tenlop"));
                } else {
                    txtStudentName.setText("Không tìm thấy thông tin");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadNotifications() {
        vboxNotifications.getChildren().clear();
        String sql = "SELECT TOP 20 Title, Content, CreatedAt FROM Notifications ORDER BY CreatedAt DESC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql);
             ResultSet rs = cmd.executeQuery()) {

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            while (rs.next()) {
                String title = rs.getString("Title");
                String content = rs.getString("Content");
                Timestamp ts = rs.getTimestamp("CreatedAt");
                String date = (ts != null) ? sdf.format(ts) : "";

                VBox item = createNotificationItem(title, content, date);
                vboxNotifications.getChildren().add(item);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createNotificationItem(String title, String content, String date) {
        VBox item = new VBox(5);
        item.getStyleClass().add("notification-item");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("noti-title");
        lblTitle.setWrapText(true);

        Label lblContent = new Label(content);
        lblContent.getStyleClass().add("noti-content");
        lblContent.setWrapText(true);
        lblContent.setMaxHeight(45); 

        Label lblDate = new Label("📅 " + date + " • Xem chi tiết");
        lblDate.getStyleClass().add("noti-date");

        item.getChildren().addAll(lblTitle, lblContent, lblDate);

        item.setOnMouseClicked(event -> showDetail(title, content, date));

        return item;
    }

    private void showDetail(String title, String content, String date) {
        lblDetailTitle.setText(title);
        txtDetailContent.setText(content);
        lblDetailDate.setText(date);
        overlayDetail.setVisible(true);
    }

    @FXML
    private void handleCloseDetail() {
        overlayDetail.setVisible(false);
    }
}