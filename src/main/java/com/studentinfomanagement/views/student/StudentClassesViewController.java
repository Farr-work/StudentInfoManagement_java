package com.studentinfomanagement.views.student;

import com.studentinfomanagement.database.DatabaseHelper;
import com.studentinfomanagement.database.GlobalConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class StudentClassesViewController {

    @FXML private TextField txtSearch;
    
    @FXML private TableView<LopHocDisplay> ListLop;
    @FXML private TableColumn<LopHocDisplay, String> colMaLop;
    @FXML private TableColumn<LopHocDisplay, String> colTenLop;
    @FXML private TableColumn<LopHocDisplay, String> colThoiGian;
    @FXML private TableColumn<LopHocDisplay, String> colGiangVien;
    @FXML private TableColumn<LopHocDisplay, String> colSiSo;
    @FXML private TableColumn<LopHocDisplay, Void> colAction;

    private ObservableList<LopHocDisplay> listLopHoc = FXCollections.observableArrayList();
    private String currentStudentID;

    @FXML
    public void initialize() {
        currentStudentID = GlobalConfig.CurrentUserID;
        if (currentStudentID == null || currentStudentID.isEmpty()) {
            currentStudentID = "SV001";
        }

        setupColumns();
        loadRegisteredClasses();
        setupSearch();
    }

    private void setupColumns() {
        colMaLop.setCellValueFactory(new PropertyValueFactory<>("maLop"));
        colTenLop.setCellValueFactory(new PropertyValueFactory<>("tenLop"));
        colThoiGian.setCellValueFactory(new PropertyValueFactory<>("thoiGian"));
        colGiangVien.setCellValueFactory(new PropertyValueFactory<>("giangVien"));
        colSiSo.setCellValueFactory(new PropertyValueFactory<>("siSoHienTai"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Hủy môn");

            {
                btn.getStyleClass().add("btn-outline-danger");
                btn.setPrefWidth(90);
                btn.setOnAction(event -> handleCancel(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void loadRegisteredClasses() {
        listLopHoc.clear();
        
        String sql = "SELECT S.SectionID, Sub.SubjectName, S.Semester, " +
                     "ISNULL(L.LecturerName, 'Chưa phân công') AS LecturerName, " +
                     "ISNULL(S.MaxCapacity, 65) AS MaxCapacity, " +
                     "(SELECT COUNT(*) FROM REGISTRATIONS R2 WHERE R2.SectionID = S.SectionID) AS CurrentSiSo " +
                     "FROM REGISTRATIONS R " +
                     "JOIN SECTIONS S ON R.SectionID = S.SectionID " +
                     "JOIN SUBJECTS Sub ON S.SubjectID = Sub.SubjectID " +
                     "LEFT JOIN LECTURERS L ON S.LecturerID = L.LecturerID " +
                     "WHERE R.masv = ? " +
                     "ORDER BY S.Semester DESC, Sub.SubjectName ASC";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql)) {
            
            cmd.setString(1, currentStudentID);
            
            try (ResultSet rs = cmd.executeQuery()) {
                while (rs.next()) {
                    String sectionIdRaw = rs.getString("SectionID");
                    String sectionIdDisplay = sectionIdRaw.replace("_AUTO", "");
                    
                    listLopHoc.add(new LopHocDisplay(
                        sectionIdDisplay,
                        sectionIdRaw,
                        rs.getString("SubjectName"),
                        rs.getString("Semester"),
                        rs.getString("LecturerName"),
                        rs.getInt("CurrentSiSo"),
                        rs.getInt("MaxCapacity")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu: " + e.getMessage());
        }
        ListLop.setItems(listLopHoc);
    }

    private void setupSearch() {
        FilteredList<LopHocDisplay> filteredData = new FilteredList<>(listLopHoc, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(lop -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                if (lop.getTenLop().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (lop.getMaLop().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        SortedList<LopHocDisplay> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(ListLop.comparatorProperty());
        ListLop.setItems(sortedData);
    }

    private void handleCancel(LopHocDisplay lop) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận hủy");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn hủy môn học này không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            
            String sql = "DELETE FROM REGISTRATIONS WHERE masv = ? AND SectionID = ?";
            
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement cmd = conn.prepareStatement(sql)) {
                
                cmd.setString(1, currentStudentID);
                cmd.setString(2, lop.getMaLopGoc());
                
                int rows = cmd.executeUpdate();
                if (rows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Đã hủy môn học thành công!");
                    loadRegisteredClasses();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Không thể hủy môn học. Có thể bạn chưa đăng ký môn này.");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    public static class LopHocDisplay {
        private String maLop;     
        private String maLopGoc;  
        private String tenLop;
        private String thoiGian;
        private String giangVien;
        private int siSo;
        private int maxCapacity;

        public LopHocDisplay(String maLop, String maLopGoc, String tenLop, String thoiGian, String giangVien, int siSo, int maxCapacity) {
            this.maLop = maLop;
            this.maLopGoc = maLopGoc;
            this.tenLop = tenLop;
            this.thoiGian = thoiGian;
            this.giangVien = giangVien;
            this.siSo = siSo;
            this.maxCapacity = maxCapacity;
        }

        public String getMaLop() { return maLop; }
        public String getMaLopGoc() { return maLopGoc; }
        public String getTenLop() { return tenLop; }
        public String getThoiGian() { return thoiGian; }
        public String getGiangVien() { return giangVien; }
        
        public String getSiSoHienTai() { 
            return siSo + "/" + maxCapacity; 
        }
    }
}