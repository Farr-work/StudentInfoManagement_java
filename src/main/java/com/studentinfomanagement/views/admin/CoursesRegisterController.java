package com.studentinfomanagement.views.admin;

import com.studentinfomanagement.database.DatabaseHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class CoursesRegisterController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<Department> cbFilterDepartment;
    @FXML private ComboBox<String> cbStatus;
    
    @FXML private TableView<SubjectViewModel> dgPortal;
    @FXML private TableColumn<SubjectViewModel, String> colID;
    @FXML private TableColumn<SubjectViewModel, String> colName;
    @FXML private TableColumn<SubjectViewModel, String> colDept;
    @FXML private TableColumn<SubjectViewModel, Integer> colCredits;
    @FXML private TableColumn<SubjectViewModel, Boolean> colStatus;
    @FXML private TableColumn<SubjectViewModel, Void> colAction;

    private ObservableList<SubjectViewModel> subjectList = FXCollections.observableArrayList();
    private ObservableList<Department> deptList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadDepartments();
        loadData();
        setupFilters();
        
        cbStatus.setItems(FXCollections.observableArrayList("Tất cả", "Đang mở", "Đang đóng"));
        cbStatus.getSelectionModel().selectFirst();
    }

    private void setupColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("subjectID"));
        colName.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setPrefWidth(90);
                btn.setPrefHeight(30);
                btn.setOnAction(event -> handleToggle(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SubjectViewModel subject = getTableView().getItems().get(getIndex());
                    btn.setText(subject.isActive() ? "Đóng lớp" : "Mở lớp");
                    btn.getStyleClass().clear();
                    btn.getStyleClass().add(subject.isActive() ? "btn-action-close" : "btn-action-open");
                    btn.setStyle("-fx-Alignment: CENTER;");
                    setGraphic(btn);
                }
            }
        });
    }

    private void loadDepartments() {
        deptList.clear();
        deptList.add(new Department("ALL", "--- Tất cả các khoa ---"));
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DepartmentID, DepartmentName FROM DEPARTMENTS")) {
            while (rs.next()) {
                deptList.add(new Department(rs.getString("DepartmentID"), rs.getString("DepartmentName")));
            }
            cbFilterDepartment.setItems(deptList);
            cbFilterDepartment.getSelectionModel().selectFirst();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadData() {
        subjectList.clear();
        String sql = "SELECT s.SubjectID, s.SubjectName, s.Credits, s.Semester, s.DepartmentID, " +
                     "d.DepartmentName, s.IsActive " +
                     "FROM SUBJECTS s LEFT JOIN DEPARTMENTS d ON s.DepartmentID = d.DepartmentID " +
                     "ORDER BY s.SubjectName";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean active = rs.getObject("IsActive") != null && rs.getBoolean("IsActive");
                String deptName = rs.getString("DepartmentName");
                if (deptName == null) deptName = "Chưa phân khoa";

                subjectList.add(new SubjectViewModel(
                        rs.getString("SubjectID"), rs.getString("SubjectName"),
                        rs.getInt("Credits"), rs.getString("DepartmentID"),
                        deptName, rs.getString("Semester"), active
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        dgPortal.setItems(subjectList);
    }

    private void setupFilters() {
        FilteredList<SubjectViewModel> filteredData = new FilteredList<>(subjectList, p -> true);
        Runnable filterLogic = () -> {
            String search = txtSearch.getText().toLowerCase();
            Department dept = cbFilterDepartment.getValue();
            String status = cbStatus.getValue();

            filteredData.setPredicate(s -> {
                boolean matchSearch = search.isEmpty() || s.getSubjectID().toLowerCase().contains(search) || s.getSubjectName().toLowerCase().contains(search);
                boolean matchDept = dept == null || dept.getId().equals("ALL") || s.getDepartmentID().equals(dept.getId());
                boolean matchStatus = status == null || status.equals("Tất cả") || (status.equals("Đang mở") && s.isActive()) || (status.equals("Đang đóng") && !s.isActive());
                return matchSearch && matchDept && matchStatus;
            });
        };
        txtSearch.textProperty().addListener((o, ov, nv) -> filterLogic.run());
        cbFilterDepartment.valueProperty().addListener((o, ov, nv) -> filterLogic.run());
        cbStatus.valueProperty().addListener((o, ov, nv) -> filterLogic.run());
        SortedList<SubjectViewModel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(dgPortal.comparatorProperty());
        dgPortal.setItems(sortedData);
    }

    private void handleToggle(SubjectViewModel subject) {
        boolean newStatus = !subject.isActive();
        String actionName = newStatus ? "Mở" : "Đóng";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(actionName + " đăng ký môn " + subject.getSubjectName() + "?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String updateSql = "UPDATE SUBJECTS SET IsActive = ? WHERE SubjectID = ?";
                try (PreparedStatement cmd = conn.prepareStatement(updateSql)) {
                    cmd.setBoolean(1, newStatus);
                    cmd.setString(2, subject.getSubjectID());
                    cmd.executeUpdate();
                }
                if (newStatus) {
                    String checkSectionSql = "SELECT COUNT(*) FROM SECTIONS WHERE SubjectID = ?";
                    try (PreparedStatement checkCmd = conn.prepareStatement(checkSectionSql)) {
                        checkCmd.setString(1, subject.getSubjectID());
                        ResultSet rs = checkCmd.executeQuery();
                        if (rs.next() && rs.getInt(1) == 0) {
                            String autoCreateSql = "INSERT INTO SECTIONS (SectionID, SubjectID, Semester, MaxCapacity) VALUES (?, ?, ?, 65)";
                            try (PreparedStatement createCmd = conn.prepareStatement(autoCreateSql)) {
                                createCmd.setString(1, subject.getSubjectID() + "_01");
                                createCmd.setString(2, subject.getSubjectID());
                                createCmd.setString(3, "HK1");
                                createCmd.executeUpdate();
                                showAlert("Hệ thống đã tự động tạo lớp '" + subject.getSubjectID() + "_01'.");
                            }
                        }
                    }
                }
                loadData();
                setupFilters(); 
            } catch (SQLException e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setContentText("Lỗi: " + e.getMessage());
                err.show();
            }
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.show();
    }

    public static class SubjectViewModel {
        private String subjectID, subjectName, departmentID, departmentName, semester;
        private int credits;
        private boolean active;

        public SubjectViewModel(String subjectID, String subjectName, int credits, String departmentID, String departmentName, String semester, boolean active) {
            this.subjectID = subjectID; this.subjectName = subjectName; this.credits = credits;
            this.departmentID = departmentID; this.departmentName = departmentName; this.semester = semester; this.active = active;
        }
        public String getSubjectID() { return subjectID; }
        public String getSubjectName() { return subjectName; }
        public String getDepartmentID() { return departmentID; }
        public String getDepartmentName() { return departmentName; }
        public int getCredits() { return credits; }
        public boolean isActive() { return active; }
        public Boolean getStatus() { return active; }
    }

    public static class Department {
        private String id, name;
        public Department(String id, String name) { this.id = id; this.name = name; }
        public String getId() { return id; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }
}