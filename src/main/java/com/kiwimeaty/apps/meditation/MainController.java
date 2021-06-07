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

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
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

public final class MainController implements Initializable {
    @FXML
    private ListView<Media> basics02list;
    @FXML
    private Accordion basics;
    @FXML
    private Accordion discovery;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        // ############# basics (rest) ############################
        final var testPath = Path.of("src", "main", "resources", "com", "kiwimeaty", "apps", "meditation",
                "test-tracks");
        try {
            createAndFillTitledPanes(this.basics, testPath.resolve("Basics"));
            createAndFillTitledPanes(this.discovery, testPath.resolve("Discovery"));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void createAndFillTitledPanes(final Accordion seriesContainer, final Path pathToSeries) throws IOException {

        final List<Path> parts;

        try (var partsStrm = Files.walk(pathToSeries).filter(Files::isDirectory).filter(not(pathToSeries::equals))) {
            parts = partsStrm.collect(Collectors.toUnmodifiableList());
        }

        for (Path part : parts) {
            final var partName = part.getFileName().toString();

            final var listView = new ListView<Session>();
            listView.setCellFactory(listView1 -> new SessionListItem());
            final var sessions = createSessions(part, partName);

            listView.setItems(sessions);
            // eg: [Basics:Take10]
            listView.setId(String.format("[%s:%s]", pathToSeries.getFileName(), partName));

            final var anchorPane = new AnchorPane(listView);

            final var titledPane = new TitledPane(partName, anchorPane);
            // extend listViews' size to anchorPane (which means basically titledPane...)
            AnchorPane.setTopAnchor(listView, 0.0);
            AnchorPane.setRightAnchor(listView, 0.0);
            AnchorPane.setBottomAnchor(listView, 0.0);
            AnchorPane.setLeftAnchor(listView, 0.0);

            seriesContainer.getPanes().add(titledPane);
        }
    }

    private static ObservableList<Session> createSessions(final Path tracksPath, final String partName)
            throws IOException {
        try (var files = Files.walk(tracksPath).filter(Files::isRegularFile)) {
            final ObservableList<Media> tracks = FXCollections.observableArrayList();
            files.map(Path::toUri).map(URI::toString).map(Media::new).forEach(tracks::add);

            ObservableList<Session> sessions = FXCollections.observableArrayList();
            sessions.add(Session.create(partName, 1, tracks.get(0), Session.State.OPEN_CURRENT));
            for (int i = 1; i < tracks.size(); i++)
                sessions.add(Session.create(partName, i + 1, tracks.get(i), Session.State.CLOSED));
            return sessions;
        }
    }

    private final class SessionListItem extends ListCell<Session> {
        private HBox hbox = new HBox();
        private Session session;
        private Button trackBtn = new Button();

        SessionListItem() {
            this.hbox.getChildren().addAll(this.trackBtn);
            this.trackBtn.setOnAction(clickEvent -> showPlayer());
        }

        private void showPlayer() {
            final var player = new PlayerController(this.session);
            player.showStage();
        }

        @Override
        protected void updateItem(final Session session, final boolean empty) {
            super.updateItem(session, empty);
            setText(null);
            if (empty) {
                this.session = null;
                setGraphic(null);
            } else {
                this.session = session;
                this.trackBtn.setText(String.format("%02d", Integer.valueOf(this.session.day())));
                // bind button
                final var buttonDisabledBinding = Bindings.createBooleanBinding(() -> {
                    return switch (this.session.status().get()) {
                        case OPEN_CURRENT, OPEN_NEXT -> false;
                        default -> true;
                    };
                }, this.session.status()); // Session.Status will be observed
                this.trackBtn.disableProperty().bind(buttonDisabledBinding);

                setGraphic(this.hbox);
            }
        }
    }
}
