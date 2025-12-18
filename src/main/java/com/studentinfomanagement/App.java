package com.studentinfomanagement;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private static Stage stage; 

    @Override
    public void start(Stage stage) throws IOException {
        App.stage = stage; 
        
        scene = new Scene(loadFXML("views/LoginWindow"));
        stage.setScene(scene);
        stage.setTitle("Student Info Management System");
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        
        if (stage != null) {
            stage.sizeToScene(); 
            stage.centerOnScreen();
        }
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}