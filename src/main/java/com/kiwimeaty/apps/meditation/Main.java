package com.kiwimeaty.apps.meditation;

import java.util.Locale;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    static { // run before any
        Locale.setDefault(Locale.CHINA);
    }
    private static ResourceBundle bundle = ResourceBundle.getBundle("com.kiwimeaty.apps.meditation.messages",
            Locale.ENGLISH);

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final var url = getClass().getResource("main.fxml");
        final var loader = new FXMLLoader(url, bundle);
        final var root = loader.<Parent>load();
        final var scene = new Scene(root);

        primaryStage.setTitle("My meditation app");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
