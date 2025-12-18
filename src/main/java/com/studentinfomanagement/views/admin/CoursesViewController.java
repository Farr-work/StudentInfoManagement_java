package com.studentinfomanagement.views.admin;

import com.studentinfomanagement.database.DatabaseHelper;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

public class CoursesViewController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilterSemester;
    @FXML private ComboBox<Department> cbFilterDepartment;
    
    @FXML private TableView<Course> dgCourses;
    @FXML private TableColumn<Course, String> colCode;
    @FXML private TableColumn<Course, String> colName;
    @FXML private TableColumn<Course, Integer> colCredits;
    @FXML private TableColumn<Course, String> colDept;
    @FXML private TableColumn<Course, String> colSemester;
    @FXML private TableColumn<Course, Void> colAction;

    @FXML private GridPane modalAddCourse;
    @FXML private TextField txtCourseCode;
    @FXML private TextField txtCourseName;
    @FXML private TextField txtCredits;
    @FXML private ComboBox<String> cbSemester;
    @FXML private ComboBox<Department> cbDepartment;
    @FXML private Label lblModalTitle;

    private ObservableList<Course> courseList = FXCollections.observableArrayList();
    private ObservableList<Department> deptList = FXCollections.observableArrayList();
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        setupColumns();
        loadDepartments();
        loadData();
        setupFilters();
        
        ObservableList<String> semesters = FXCollections.observableArrayList("HK1", "HK2", "HK3");
        cbSemester.setItems(semesters);
        
        ObservableList<String> filterSemesters = FXCollections.observableArrayList("Tất cả", "HK1", "HK2", "HK3");
        cbFilterSemester.setItems(filterSemesters);
        cbFilterSemester.getSelectionModel().selectFirst();
    }

    private void setupColumns() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCredits.setCellValueFactory(new PropertyValueFactory<>("credits"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("🗑");
            private final HBox pane = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("icon-btn-edit");
                btnDelete.getStyleClass().add("icon-btn-delete");
                pane.setStyle("-fx-alignment: CENTER;");
                btnEdit.setOnAction(event -> openEditModal(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadDepartments() {
        deptList.clear();
        String sql = "SELECT DepartmentID, DepartmentName FROM DEPARTMENTS";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                deptList.add(new Department(rs.getString("DepartmentID"), rs.getString("DepartmentName")));
            }
            cbDepartment.setItems(deptList);
            
            ObservableList<Department> filterDepts = FXCollections.observableArrayList(deptList);
            filterDepts.add(0, new Department("ALL", "--- Tất cả các khoa ---"));
            cbFilterDepartment.setItems(filterDepts);
            cbFilterDepartment.getSelectionModel().selectFirst();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadData() {
        courseList.clear();
        String sql = "SELECT s.SubjectID, s.SubjectName, s.Credits, s.Semester, s.DepartmentID, d.DepartmentName " +
                     "FROM SUBJECTS s LEFT JOIN DEPARTMENTS d ON s.DepartmentID = d.DepartmentID";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                courseList.add(new Course(
                    rs.getString("SubjectID"), rs.getString("SubjectName"),
                    rs.getInt("Credits"), rs.getString("Semester"),
                    rs.getString("DepartmentID"), rs.getString("DepartmentName")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        dgCourses.setItems(courseList);
    }

    private void setupFilters() {
        FilteredList<Course> filteredData = new FilteredList<>(courseList, p -> true);

        Runnable filterLogic = () -> {
            String search = txtSearch.getText().toLowerCase();
            String sem = cbFilterSemester.getValue();
            Department dept = cbFilterDepartment.getValue();

            filteredData.setPredicate(course -> {
                boolean matchSearch = search.isEmpty() || 
                                      course.getCode().toLowerCase().contains(search) || 
                                      course.getName().toLowerCase().contains(search);
                
                boolean matchSem = sem == null || sem.equals("Tất cả") || course.getSemester().equals(sem);
                
                boolean matchDept = dept == null || dept.getId().equals("ALL") || course.getDepartmentId().equals(dept.getId());

                return matchSearch && matchSem && matchDept;
            });
        };

        txtSearch.textProperty().addListener((o, ov, nv) -> filterLogic.run());
        cbFilterSemester.valueProperty().addListener((o, ov, nv) -> filterLogic.run());
        cbFilterDepartment.valueProperty().addListener((o, ov, nv) -> filterLogic.run());

        SortedList<Course> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(dgCourses.comparatorProperty());
        dgCourses.setItems(sortedData);
    }


    @FXML
    private void handleOpenAdd() {
        isEditMode = false;
        lblModalTitle.setText("Thêm môn học mới");
        clearForm();
        txtCourseCode.setDisable(false);
        modalAddCourse.setVisible(true);
    }

    private void openEditModal(Course course) {
        isEditMode = true;
        lblModalTitle.setText("Cập nhật môn học");
        
        txtCourseCode.setText(course.getCode());
        txtCourseCode.setDisable(true);
        txtCourseName.setText(course.getName());
        txtCredits.setText(String.valueOf(course.getCredits()));
        cbSemester.setValue(course.getSemester());
        
        for(Department d : cbDepartment.getItems()) {
            if(d.getId().equals(course.getDepartmentId())) {
                cbDepartment.setValue(d);
                break;
            }
        }
        
        modalAddCourse.setVisible(true);
    }

    @FXML
    private void handleCloseModal() {
        modalAddCourse.setVisible(false);
    }

    @FXML
    private void handleSave() {
        String id = txtCourseCode.getText().trim();
        String name = txtCourseName.getText().trim();
        String creditsStr = txtCredits.getText().trim();
        Department dept = cbDepartment.getValue();
        String semester = cbSemester.getValue();

        if (id.isEmpty() || name.isEmpty() || creditsStr.isEmpty() || dept == null || semester == null) {
            showAlert("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        try {
            int credits = Integer.parseInt(creditsStr);
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql;
                if (isEditMode) {
                    sql = "UPDATE SUBJECTS SET SubjectName=?, Credits=?, Semester=?, DepartmentID=? WHERE SubjectID=?";
                } else {
                    sql = "INSERT INTO SUBJECTS (SubjectName, Credits, Semester, DepartmentID, SubjectID) VALUES (?, ?, ?, ?, ?)";
                }

                try (PreparedStatement cmd = conn.prepareStatement(sql)) {
                    cmd.setString(1, name);
                    cmd.setInt(2, credits);
                    cmd.setString(3, semester);
                    cmd.setString(4, dept.getId());
                    cmd.setString(5, id);
                    cmd.executeUpdate();
                }
                
                showAlert("Lưu thành công!");
                handleCloseModal();
                loadData();
                setupFilters(); 
            }
        } catch (NumberFormatException e) {
            showAlert("Tín chỉ phải là số.");
        } catch (SQLException e) {
            showAlert("Lỗi Database: " + e.getMessage());
        }
    }

    private void handleDelete(Course course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Xóa môn học " + course.getCode() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement cmd = conn.prepareStatement("DELETE FROM SUBJECTS WHERE SubjectID=?")) {
                cmd.setString(1, course.getCode());
                cmd.executeUpdate();
                loadData();
                setupFilters();
            } catch (SQLException e) {
                showAlert("Không thể xóa (có thể đang có lớp học mở): " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExport() {
        try {
            File tempFile = File.createTempFile("CoursesExport", ".csv");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Ma Mon,Ten Mon,Tin Chi,Khoa,Hoc Ky\n");
                for (Course c : dgCourses.getItems()) {
                    writer.write(String.format("%s,%s,%d,%s,%s\n", c.getCode(), c.getName(), c.getCredits(), c.getDepartmentName(), c.getSemester()));
                }
            }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(tempFile);
        } catch (IOException e) {
            showAlert("Lỗi xuất file: " + e.getMessage());
        }
    }

    private void clearForm() {
        txtCourseCode.clear();
        txtCourseName.clear();
        txtCredits.setText("3");
        cbDepartment.getSelectionModel().clearSelection();
        cbSemester.getSelectionModel().clearSelection();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.show();
    }

    
    public static class Course {
        private String code, name, semester, departmentId, departmentName;
        private int credits;

        public Course(String code, String name, int credits, String semester, String departmentId, String departmentName) {
            this.code = code; this.name = name; this.credits = credits;
            this.semester = semester; this.departmentId = departmentId; this.departmentName = departmentName;
        }
        public String getCode() { return code; }
        public String getName() { return name; }
        public int getCredits() { return credits; }
        public String getSemester() { return semester; }
        public String getDepartmentId() { return departmentId; }
        public String getDepartmentName() { return departmentName; }
    }

    public static class Department {
        private String id, name;
        public Department(String id, String name) { this.id = id; this.name = name; }
        public String getId() { return id; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }
}