package com.studentinfomanagement.views.student;

import com.studentinfomanagement.database.DatabaseHelper;
import com.studentinfomanagement.database.GlobalConfig;
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

public class StudentCoursesViewController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cboDepartmentFilter;
    
    @FXML private TableView<CourseDisplayModel> ListHocPhan;
    @FXML private TableColumn<CourseDisplayModel, String> colID;
    @FXML private TableColumn<CourseDisplayModel, String> colName;
    @FXML private TableColumn<CourseDisplayModel, Integer> colCredits;
    @FXML private TableColumn<CourseDisplayModel, String> colCount;
    @FXML private TableColumn<CourseDisplayModel, String> colDept;
    @FXML private TableColumn<CourseDisplayModel, Void> colAction;

    private ObservableList<CourseDisplayModel> courseList = FXCollections.observableArrayList();
    private String currentStudentID;

    @FXML
    public void initialize() {
        currentStudentID = GlobalConfig.CurrentUserID;
        if (currentStudentID == null || currentStudentID.isEmpty()) currentStudentID = "UNKNOWN";

        setupColumns();
        loadDepartments();
        loadCourses();
        setupFilters();
    }

    private void setupColumns() {
        colID.setCellValueFactory(new PropertyValueFactory<>("maHocPhan"));
        colName.setCellValueFactory(new PropertyValueFactory<>("tenMonHoc"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("tinChi"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("khoa"));
        
        colCount.setCellValueFactory(new PropertyValueFactory<>("siSoHienThi"));
        colCount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("badge-count");
                    setGraphic(lbl);
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Đăng ký");
            {
                btn.setPrefWidth(90);
                btn.getStyleClass().add("btn-primary");
                btn.setOnAction(event -> handleRegister(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void loadDepartments() {
        ObservableList<String> depts = FXCollections.observableArrayList("Tất cả các khoa");
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DepartmentName FROM DEPARTMENTS")) {
            while (rs.next()) {
                depts.add(rs.getString("DepartmentName"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        cboDepartmentFilter.setItems(depts);
        cboDepartmentFilter.getSelectionModel().selectFirst();
    }

    private void loadCourses() {
        courseList.clear();
        String sql = "SELECT S.SubjectID, S.SubjectName, ISNULL(S.Credits, 0) AS Credits, " +
                     "ISNULL(D.DepartmentName, 'Chưa phân khoa') AS DepartmentName, " +
                     "(SELECT COUNT(*) FROM REGISTRATIONS R JOIN SECTIONS SEC ON R.SectionID = SEC.SectionID WHERE SEC.SubjectID = S.SubjectID) AS CurrentCount " +
                     "FROM SUBJECTS S LEFT JOIN DEPARTMENTS D ON S.DepartmentID = D.DepartmentID " +
                     "WHERE ISNULL(S.IsActive, 1) = 1";

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int count = rs.getInt("CurrentCount");
                courseList.add(new CourseDisplayModel(
                    rs.getString("SubjectID"),
                    rs.getString("SubjectName"),
                    rs.getInt("Credits"),
                    rs.getString("DepartmentName"),
                    count + "/65"
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        ListHocPhan.setItems(courseList);
    }

    private void setupFilters() {
        FilteredList<CourseDisplayModel> filteredData = new FilteredList<>(courseList, p -> true);

        Runnable filterLogic = () -> {
        String search = txtSearch.getText().toLowerCase().trim();
        String selectedDept = cboDepartmentFilter.getValue();

        filteredData.setPredicate(course -> {
            boolean matchSearch = search.isEmpty() || 
                                  course.getMaHocPhan().toLowerCase().contains(search) || 
                                  course.getTenMonHoc().toLowerCase().contains(search);
            
            boolean matchDept = (selectedDept == null || selectedDept.equals("Tất cả các khoa")) || 
                                 (course.getKhoa() != null && course.getKhoa().equals(selectedDept));

            return matchSearch && matchDept;
        });
    };

    txtSearch.textProperty().addListener((o, ov, nv) -> filterLogic.run());
    
    cboDepartmentFilter.valueProperty().addListener((o, ov, nv) -> filterLogic.run());

    SortedList<CourseDisplayModel> sortedData = new SortedList<>(filteredData);
    sortedData.comparatorProperty().bind(ListHocPhan.comparatorProperty());
    
    ListHocPhan.setItems(sortedData);
}

    private void handleRegister(CourseDisplayModel course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Đăng ký môn " + course.getMaHocPhan() + "?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sectionID = "";
                String findSql = "SELECT TOP 1 SectionID FROM SECTIONS WHERE SubjectID = ?";
                try (PreparedStatement cmd = conn.prepareStatement(findSql)) {
                    cmd.setString(1, course.getMaHocPhan());
                    ResultSet rs = cmd.executeQuery();
                    if (rs.next()) {
                        sectionID = rs.getString("SectionID");
                    } else {
                        sectionID = course.getMaHocPhan() + "_AUTO";
                        String createSql = "INSERT INTO SECTIONS (SectionID, SubjectID, Semester, MaxCapacity) VALUES (?, ?, 'HK1', 65)";
                        try (PreparedStatement createCmd = conn.prepareStatement(createSql)) {
                            createCmd.setString(1, sectionID);
                            createCmd.setString(2, course.getMaHocPhan());
                            createCmd.executeUpdate();
                        }
                    }
                }

                String checkSql = "SELECT COUNT(*) FROM REGISTRATIONS WHERE masv = ? AND SectionID = ?";
                try (PreparedStatement checkCmd = conn.prepareStatement(checkSql)) {
                    checkCmd.setString(1, currentStudentID);
                    checkCmd.setString(2, sectionID);
                    ResultSet rs = checkCmd.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        showAlert("Bạn đã đăng ký môn này rồi!");
                        return;
                    }
                }

                String insertSql = "INSERT INTO REGISTRATIONS (masv, SectionID, RegistrationDate) VALUES (?, ?, GETDATE())";
                try (PreparedStatement insertCmd = conn.prepareStatement(insertSql)) {
                    insertCmd.setString(1, currentStudentID);
                    insertCmd.setString(2, sectionID);
                    insertCmd.executeUpdate();
                    
                    showAlert("Đăng ký thành công!");
                    loadCourses(); 
                }

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Lỗi: " + e.getMessage());
            }
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.show();
    }

    public static class CourseDisplayModel {
        private String maHocPhan, tenMonHoc, khoa, siSoHienThi;
        private int tinChi;

        public CourseDisplayModel(String ma, String ten, int tc, String k, String ss) {
            this.maHocPhan = ma; this.tenMonHoc = ten; this.tinChi = tc; this.khoa = k; this.siSoHienThi = ss;
        }
        public String getMaHocPhan() { return maHocPhan; }
        public String getTenMonHoc() { return tenMonHoc; }
        public int getTinChi() { return tinChi; }
        public String getKhoa() { return khoa; }
        public String getSiSoHienThi() { return siSoHienThi; }
    }
}