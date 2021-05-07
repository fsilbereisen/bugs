package com.kiwimeaty.apps.meditation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class PlayerController implements Initializable {
    private MediaPlayer mediaPlayer;
    private Stage stage;

    @FXML
    private Button stop;
    @FXML
    private Button play;
    @FXML
    private Button pause;
    @FXML
    private Slider progressBar;
    @FXML
    private Slider volumeSlider;

    public PlayerController(final Media track) {
        this.mediaPlayer = new MediaPlayer(track);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("player.fxml"));
        loader.setController(this);

        this.stage = new Stage();
        this.stage.setTitle(track.getMetadata().get("album").toString());
        try {
            this.stage.setScene(new Scene(loader.load()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        this.stage.setOnCloseRequest(event -> this.mediaPlayer.dispose());

        // configure progressBar
        this.mediaPlayer.setOnReady(() -> this.progressBar.setMax(track.getDuration().toSeconds()));
        this.mediaPlayer.currentTimeProperty()
                .addListener((observable, oldVal, newVal) -> this.progressBar.setValue(newVal.toSeconds()));
        this.progressBar
                .setOnMousePressed(event -> this.mediaPlayer.seek(Duration.seconds(this.progressBar.getValue())));
        this.progressBar
                .setOnMouseDragged(event -> this.mediaPlayer.seek(Duration.seconds(this.progressBar.getValue())));

        // configure volumeSlider
        this.mediaPlayer.setVolume(0.3); // start with 30%
        this.volumeSlider.setValue(this.mediaPlayer.getVolume() * 100); // initValue: 0...1 >> 0...100
        this.volumeSlider.valueProperty()
                .addListener((observable) -> this.mediaPlayer.setVolume(this.volumeSlider.getValue() / 100));
    }

    public void showStage() {
        this.stage.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.stop.setDisable(true);
        this.pause.setDisable(true);
    }

    @FXML
    private void play() {
        this.mediaPlayer.play();

        this.pause.setDisable(false);
        this.stop.setDisable(false);
        this.play.setDisable(true);
    }

    @FXML
    private void pause() {
        this.mediaPlayer.pause();

        this.pause.setDisable(true);
        this.play.setDisable(false);
        this.stop.setDisable(false);
    }

    @FXML
    private void stop() {
        this.mediaPlayer.stop();

        this.pause.setDisable(true);
        this.stop.setDisable(true);
        this.play.setDisable(false);
    }

    @FXML
    private void minus10() {
        this.mediaPlayer.seek(this.mediaPlayer.getCurrentTime().subtract(Duration.seconds(10)));
    }

    @FXML
    private void plus10() {
        this.mediaPlayer.seek(this.mediaPlayer.getCurrentTime().add(Duration.seconds(10)));
    }
}
