package com.kiwimeaty.apps.meditation;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
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
    @FXML
    private Accordion basics;

    private MediaPlayer mediaPlayer;

    @FXML
    private void start() {
        System.out.println("start");
        final var tracks = Path.of("src", "main", "resources", "com", "kiwimeaty", "apps", "meditation", "test-tracks");
        final var path = tracks.resolve("Take 15/01");

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

        // ############# basics (rest) ############################
        final var basicsPath = Path.of("src", "main", "resources", "com", "kiwimeaty", "apps", "meditation",
                "test-tracks");
        try {
            createAndFillTitledPanes(basics, basicsPath);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void createAndFillTitledPanes(Accordion sectionContainer, Path pathToSection) throws IOException {

        final List<Path> sectionDirectories;
        ObservableList<Media> tracks;

        try (var dirs = Files.walk(pathToSection).filter(Files::isDirectory).filter(not(pathToSection::equals))) {
            sectionDirectories = dirs.collect(Collectors.toUnmodifiableList());
        }

        for (Path directory : sectionDirectories) {
            final var subSectionName = directory.getFileName().toString();

            final var listView1 = new ListView<Media>();
            listView1.setCellFactory(listView -> new TrackListItem());
            tracks = createTracks(directory);
            listView1.setItems(tracks);
            listView1.setId(String.format("[%s:%s]", pathToSection.getFileName(), subSectionName));

            final var anchorPane1 = new AnchorPane(listView1);

            final var titledPane1 = new TitledPane(subSectionName, anchorPane1);
            // extend listViews' size to anchorPane (which means basically titledPane...)
            AnchorPane.setTopAnchor(listView1, 0.0);
            AnchorPane.setRightAnchor(listView1, 0.0);
            AnchorPane.setBottomAnchor(listView1, 0.0);
            AnchorPane.setLeftAnchor(listView1, 0.0);

            sectionContainer.getPanes().add(titledPane1);
        }

    }

    private static ObservableList<Media> createTracks(final Path tracksPath) throws IOException {
        try (var files = Files.walk(tracksPath).filter(Files::isRegularFile)) {
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
                if (meta.isEmpty())
                    meta.addListener((MapChangeListener.Change<? extends String, ? extends Object> chg) -> {
                        trackBtn.setText(String.format("%02d %s", meta.get("track number"), meta.get("title")));
                    });
                else // no change will occur anymore >> write it now
                    trackBtn.setText(String.format("%02d %s", meta.get("track number"), meta.get("title")));
                setGraphic(hbox);
            }
        }
    }
}
