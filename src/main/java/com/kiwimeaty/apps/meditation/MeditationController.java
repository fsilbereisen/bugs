package com.kiwimeaty.apps.meditation;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MeditationController implements Initializable {

    @FXML
    private Button basics01Stop;
    @FXML
    private Button basics01Start;
    @FXML
    private Button basics01Pause;

    private MediaPlayer mediaPlayer;

    @FXML
    private void start() {
        System.out.println("start");
        mediaPlayer.setAutoPlay(true);
        this.basics01Pause.setDisable(false);
        this.basics01Stop.setDisable(false);
        this.basics01Start.setDisable(true);
    }

    @FXML
    private void pause() {
        // TODO
    }

    @FXML
    private void stop() {
        // TODO
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        basics01Stop.setDisable(true);
        basics01Pause.setDisable(true);

        final var tracks = Path.of("src", "main", "resources", "com", "kiwimeaty", "apps", "meditation", "test-tracks");
        final var path = tracks.resolve("test.mp3");

        final var media = new Media(path.toUri().toString());
        mediaPlayer = new MediaPlayer(media);
    }
}
