package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The main entry point of the client application, which initializes and launches the graphical user interface.
 */
public class ClientApplication extends Application {

    /**
     * Initializes and displays the main application window.
     *
     * @param stage The primary stage for this application.
     * @throws IOException If loading the FXML file fails.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("client-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);

        stage.setTitle("MY_ONLINE_Paint");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Launches the JavaFX application.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        launch();
    }
}