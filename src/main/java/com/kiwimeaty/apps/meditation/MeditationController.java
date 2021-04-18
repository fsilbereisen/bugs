package com.kiwimeaty.apps.meditation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MeditationController implements Initializable {

    @FXML
    private Button basics01Stop;
    @FXML
    private Button basics01Start;
    @FXML
    private Button basics01Pause;
    @FXML
    private ListView<Media> basics02list;

    private MediaPlayer mediaPlayer;
    // private ListView<Media> tracksListView;
    // private static MediaPlayer mediaPlayer;

    @FXML
    private void start() {
        System.out.println("start");
        final var tracks = Path.of("src", "main", "resources", "com", "kiwimeaty", "apps", "meditation", "test-tracks");
        final var path = tracks.resolve("test.mp3");

        final var media = new Media(path.toUri().toString());
        mediaPlayer = new MediaPlayer(media);
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
        mediaPlayer.dispose();
        this.basics01Pause.setDisable(true);
        this.basics01Stop.setDisable(true);
        this.basics01Start.setDisable(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        basics01Stop.setDisable(true);
        basics01Pause.setDisable(true);
        final var tracksPath = Path.of("src", "main", "resources", "com", "kiwimeaty", "apps", "meditation",
                "test-tracks");
        ObservableList<Media> tracks;
        try {
            tracks = createTracks(tracksPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        basics02list.setCellFactory(listView -> new TrackListItem());
        basics02list.setItems(tracks);
    }

    private ObservableList<Media> createTracks(final Path tracksPath) throws IOException {
        try (var files = Files.walk(tracksPath).filter(Files::isRegularFile);) {
            ObservableList<Media> tracks = FXCollections.observableArrayList();
            files.map(Path::toUri).map(URI::toString).map(Media::new).forEach(tracks::add);
            return tracks;
        }
    }

    private class TrackListItem extends ListCell<Media> {
        HBox hbox = new HBox();
        Media track;
        Button trackBtn = new Button("No metadata found... yet?");

        public TrackListItem() {
            hbox.getChildren().addAll(trackBtn); // TODO replace hbox?
            trackBtn.setOnAction(clickEvent -> startTrack());
        }

        private void startTrack() {
            if (mediaPlayer != null)
                mediaPlayer.dispose(); // stop other track
            mediaPlayer = new MediaPlayer(track);
            mediaPlayer.setAutoPlay(true);
        }

        @Override
        protected void updateItem(Media item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty) {
                track = null;
                setGraphic(null);
            } else {
                track = item;
                final var meta = track.getMetadata();
                trackBtn.setText(String.format("%02d %s", meta.get("track number"), meta.get("title")));
                setGraphic(hbox);
            }
        }
    }
}
