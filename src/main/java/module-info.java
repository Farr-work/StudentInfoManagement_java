module com.studentinfomanagement {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.sql.rowset;
    requires com.microsoft.sqlserver.jdbc;
    requires java.desktop;

    opens com.studentinfomanagement to javafx.fxml;
    opens com.studentinfomanagement.views to javafx.fxml;
    
    // Admin
    opens com.studentinfomanagement.views.admin to javafx.fxml, javafx.base;
    
    // Student
    opens com.studentinfomanagement.views.student to javafx.fxml, javafx.base;

    // XÓA DÒNG NÀY ĐI VÌ BẠN KHÔNG CÓ FOLDER MODELS
    // opens com.studentinfomanagement.models to javafx.base;

    exports com.studentinfomanagement;
}