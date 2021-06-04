package com.kiwimeaty.apps.meditation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class PlayerController implements Initializable {
    private MediaPlayer mediaPlayer;
    private ReadOnlyObjectProperty<Status> status;
    private Stage stage;

    @FXML
    private Button stopBtn;
    @FXML
    private Button playBtn;
    @FXML
    private Button pauseBtn;
    @FXML
    private Button minus10Btn;
    @FXML
    private Button plus10Btn;
    @FXML
    private Slider progressBar;
    @FXML
    private Slider volumeSlider;

    public PlayerController(final Session session) {
        final var track = session.track();
        this.mediaPlayer = new MediaPlayer(track);
        this.status = this.mediaPlayer.statusProperty();

        final var loader = new FXMLLoader(getClass().getResource("player.fxml"));
        loader.setController(this);

        this.stage = new Stage();
        this.stage.setTitle(String.format("%s: Day %02d", session.part(), Integer.valueOf(session.day())));
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
                .addListener(observable -> this.mediaPlayer.setVolume(this.volumeSlider.getValue() / 100));
    }

    public void showStage() {
        this.stage.show();
    }

    /**
     * Set when buttons are disabled.
     */
    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final var playDisabledBinding = this.status.isEqualTo(Status.PLAYING);
        final var pauseDisabledBinding = Bindings.createBooleanBinding(() -> {
            final var status = this.status.getValue();
            return status != null ? switch (status) {
                case PAUSED, STOPPED, READY -> true;
                default -> false;
            } : true;// disable button
        }, this.status); // this.status will be observed

        final var stopDisabledBinding = this.status.isEqualTo(Status.STOPPED).or(this.status.isEqualTo(Status.READY));
        final var plusAndMinus10DisabledBinding = this.status.isEqualTo(Status.STOPPED)
                .or(this.status.isEqualTo(Status.READY));
        final var progressBarDisabledBinding = this.status.isEqualTo(Status.STOPPED);

        this.playBtn.disableProperty().bind(playDisabledBinding);
        this.pauseBtn.disableProperty().bind(pauseDisabledBinding);
        this.stopBtn.disableProperty().bind(stopDisabledBinding);
        this.minus10Btn.disableProperty().bind(plusAndMinus10DisabledBinding);
        this.plus10Btn.disableProperty().bind(plusAndMinus10DisabledBinding);
        this.progressBar.disableProperty().bind(progressBarDisabledBinding);
    }

    @FXML
    private void play() {
        this.mediaPlayer.play();
    }

    @FXML
    private void pause() {
        this.mediaPlayer.pause();
    }

    @FXML
    private void stop() {
        this.mediaPlayer.stop();
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
