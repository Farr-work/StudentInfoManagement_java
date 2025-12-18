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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class StudentsViewController {

    @FXML private TextField txtSearch;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, String> colMaSV;
    @FXML private TableColumn<Student, String> colHoTen;
    @FXML private TableColumn<Student, String> colTenLop;
    @FXML private TableColumn<Student, String> colGioiTinh;
    @FXML private TableColumn<Student, String> colDiaChi;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colSDT;
    @FXML private TableColumn<Student, String> colTrangThai;
    @FXML private TableColumn<Student, Void> colAction;

    @FXML private GridPane overlayInput;
    @FXML private TextField txtMaSV;
    @FXML private TextField txtHoTen;
    @FXML private TextField txtTenLop;
    @FXML private TextField txtGioiTinh;
    @FXML private TextField txtDiaChi;
    @FXML private TextField txtEmail;
    @FXML private TextField txtSDT;
    @FXML private TextField txtTrangThai;

    private ObservableList<Student> studentList = FXCollections.observableArrayList();
    private String currentEditingMasv = null; 

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
        setupSearch();
    }

    private void setupColumns() {
        colMaSV.setCellValueFactory(new PropertyValueFactory<>("masv"));
        colHoTen.setCellValueFactory(new PropertyValueFactory<>("hoten"));
        colTenLop.setCellValueFactory(new PropertyValueFactory<>("tenlop"));
        colGioiTinh.setCellValueFactory(new PropertyValueFactory<>("gioitinh"));
        colDiaChi.setCellValueFactory(new PropertyValueFactory<>("diachi"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colSDT.setCellValueFactory(new PropertyValueFactory<>("sdt"));
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("trangthai"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
private final Button btnDelete = new Button();

{
    ImageView editIcon = new ImageView(
        new Image(getClass().getResourceAsStream("/icons/edit.png"))
    );
    editIcon.setFitWidth(16);
    editIcon.setFitHeight(16);

    ImageView deleteIcon = new ImageView(
        new Image(getClass().getResourceAsStream("/icons/delete.png"))
    );
    deleteIcon.setFitWidth(16);
    deleteIcon.setFitHeight(16);

    btnEdit.setGraphic(editIcon);
    btnDelete.setGraphic(deleteIcon);

    btnEdit.getStyleClass().add("action-button");
    btnDelete.getStyleClass().add("action-button");

    btnEdit.setOnAction(event ->
        handleEdit(getTableView().getItems().get(getIndex()))
    );

    btnDelete.setOnAction(event ->
        handleDelete(getTableView().getItems().get(getIndex()))
    );
}

            private final HBox pane = new HBox(20, btnEdit, btnDelete);
            

            {
                btnEdit.getStyleClass().add("action-button");
                btnDelete.getStyleClass().add("action-button");
                btnDelete.setStyle("-fx-text-fill: red;");
                pane.setStyle("-fx-alignment: CENTER;");
                btnEdit.setOnAction(event -> handleEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadData() {
        studentList.clear();
        String sql = "SELECT * FROM Student";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement cmd = conn.prepareStatement(sql);
             ResultSet rs = cmd.executeQuery()) {

            while (rs.next()) {
                studentList.add(new Student(
                        rs.getString("masv"), rs.getString("hoten"), rs.getString("tenlop"),
                        rs.getString("gioitinh"), rs.getString("diachi"), rs.getString("email"),
                        rs.getString("sdt"), rs.getString("trangthai")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Lỗi tải dữ liệu: " + e.getMessage());
        }
        studentsTable.setItems(studentList);
    }

    private void setupSearch() {
        FilteredList<Student> filteredData = new FilteredList<>(studentList, p -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(student -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return student.getHoten().toLowerCase().contains(lowerCaseFilter) ||
                       student.getMasv().toLowerCase().contains(lowerCaseFilter);
            });
        });
        SortedList<Student> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(studentsTable.comparatorProperty());
        studentsTable.setItems(sortedData);
    }


    @FXML
    private void handleAdd() {
        currentEditingMasv = null;
        clearFields();
        txtMaSV.setDisable(false);
        overlayInput.setVisible(true);
    }

    private void handleEdit(Student student) {
        currentEditingMasv = student.getMasv();
        txtMaSV.setText(student.getMasv());
        txtHoTen.setText(student.getHoten());
        txtTenLop.setText(student.getTenlop());
        txtGioiTinh.setText(student.getGioitinh());
        txtDiaChi.setText(student.getDiachi());
        txtEmail.setText(student.getEmail());
        txtSDT.setText(student.getSdt());
        txtTrangThai.setText(student.getTrangthai());

        txtMaSV.setDisable(true);
        overlayInput.setVisible(true);
    }

    @FXML
    private void handleSave() {
        String masv = txtMaSV.getText().trim();
        String hoten = txtHoTen.getText().trim();

        if (masv.isEmpty() || hoten.isEmpty()) {
            showAlert("Vui lòng nhập Mã SV và Họ tên.");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            if (currentEditingMasv == null) {
                String sql = "INSERT INTO Student (masv, hoten, tenlop, gioitinh, diachi, email, sdt, trangthai) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement cmd = conn.prepareStatement(sql)) {
                    setParams(cmd, masv, hoten);
                    cmd.executeUpdate();
                }
                createUserForStudent(conn, masv, hoten);
                logActivity(conn, "Thêm SV mới: " + masv);
            } else {
                String sql = "UPDATE Student SET hoten=?, tenlop=?, gioitinh=?, diachi=?, email=?, sdt=?, trangthai=? WHERE masv=?";
                try (PreparedStatement cmd = conn.prepareStatement(sql)) {
                    cmd.setString(1, hoten);
                    cmd.setString(2, txtTenLop.getText());
                    cmd.setString(3, txtGioiTinh.getText());
                    cmd.setString(4, txtDiaChi.getText());
                    cmd.setString(5, txtEmail.getText());
                    cmd.setString(6, txtSDT.getText());
                    cmd.setString(7, txtTrangThai.getText().isEmpty() ? "Đang học" : txtTrangThai.getText());
                    cmd.setString(8, masv);
                    cmd.executeUpdate();
                }
                logActivity(conn, "Cập nhật SV: " + masv);
            }

            loadData();
            setupSearch(); 
            overlayInput.setVisible(false);
            showAlert("Lưu thành công!");

        } catch (SQLException e) {
            showAlert("Lỗi Database: " + e.getMessage());
        }
    }

    private void setParams(PreparedStatement cmd, String masv, String hoten) throws SQLException {
        cmd.setString(1, masv);
        cmd.setString(2, hoten);
        cmd.setString(3, txtTenLop.getText());
        cmd.setString(4, txtGioiTinh.getText());
        cmd.setString(5, txtDiaChi.getText());
        cmd.setString(6, txtEmail.getText());
        cmd.setString(7, txtSDT.getText());
        cmd.setString(8, txtTrangThai.getText().isEmpty() ? "Đang học" : txtTrangThai.getText());
    }

    private void createUserForStudent(Connection conn, String username, String fullname) {
        String sql = "INSERT INTO Users (Username, Password, FullName, RoleID, CreatedAt) VALUES (?, '123', ?, 2, GETDATE())";
        try (PreparedStatement cmd = conn.prepareStatement(sql)) {
            cmd.setString(1, username);
            cmd.setString(2, fullname);
            cmd.executeUpdate();
        } catch (SQLException e) {
            System.out.println("User có thể đã tồn tại.");
        }
    }

    private void handleDelete(Student student) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa sinh viên " + student.getHoten() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String sql = "DELETE FROM Student WHERE masv = ?";
                try (PreparedStatement cmd = conn.prepareStatement(sql)) {
                    cmd.setString(1, student.getMasv());
                    cmd.executeUpdate();
                }
                logActivity(conn, "Đã xóa SV: " + student.getMasv());
                loadData();
            } catch (SQLException e) {
                showAlert("Lỗi xóa: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        overlayInput.setVisible(false);
        clearFields();
    }

    @FXML
    private void handleExport() {
        try {
            File tempFile = File.createTempFile("StudentExport", ".txt");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Mã SV, Họ Tên, Lớp, Giới Tính, Địa Chỉ, Email, SĐT, Trạng Thái\n");
                for (Student s : studentList) {
                    writer.write(String.format("%s, %s, %s, %s, %s, %s, %s, %s\n",
                            s.getMasv(), s.getHoten(), s.getTenlop(), s.getGioitinh(),
                            s.getDiachi(), s.getEmail(), s.getSdt(), s.getTrangthai()));
                }
            }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(tempFile);
        } catch (IOException e) {
            showAlert("Lỗi xuất file: " + e.getMessage());
        }
    }

    private void logActivity(Connection conn, String action) throws SQLException {
        String sql = "INSERT INTO ActivityLog (ActionName, CreatedAt) VALUES (?, GETDATE())";
        try (PreparedStatement cmd = conn.prepareStatement(sql)) {
            cmd.setString(1, action);
            cmd.executeUpdate();
        }
    }

    private void clearFields() {
        txtMaSV.clear(); txtHoTen.clear(); txtTenLop.clear();
        txtGioiTinh.clear(); txtDiaChi.clear(); txtEmail.clear();
        txtSDT.clear(); txtTrangThai.clear();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.show();
    }

    public static class Student {
        private String masv;
        private String hoten;
        private String tenlop;
        private String gioitinh;
        private String diachi;
        private String email;
        private String sdt;
        private String trangthai;

        public Student(String masv, String hoten, String tenlop, String gioitinh, String diachi, String email, String sdt, String trangthai) {
            this.masv = masv;
            this.hoten = hoten;
            this.tenlop = tenlop;
            this.gioitinh = gioitinh;
            this.diachi = diachi;
            this.email = email;
            this.sdt = sdt;
            this.trangthai = trangthai;
        }

        public String getMasv() { return masv; }
        public void setMasv(String masv) { this.masv = masv; }

        public String getHoten() { return hoten; }
        public void setHoten(String hoten) { this.hoten = hoten; }

        public String getTenlop() { return tenlop; }
        public void setTenlop(String tenlop) { this.tenlop = tenlop; }

        public String getGioitinh() { return gioitinh; }
        public void setGioitinh(String gioitinh) { this.gioitinh = gioitinh; }

        public String getDiachi() { return diachi; }
        public void setDiachi(String diachi) { this.diachi = diachi; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getSdt() { return sdt; }
        public void setSdt(String sdt) { this.sdt = sdt; }

        public String getTrangthai() { return trangthai; }
        public void setTrangthai(String trangthai) { this.trangthai = trangthai; }
    }
}
