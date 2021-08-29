package com.kiwimeaty.apps.meditation;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.kiwimeaty.apps.meditation.util.Session;
import com.kiwimeaty.apps.meditation.util.UnlockList;
import com.kiwimeaty.apps.meditation.util.UnlockList.ElementState;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;

public final class MainController implements Initializable {
    @FXML
    private Accordion basics;
    @FXML
    private Accordion discovery;

    private Map<String, UnlockList<Session>> sessionsByPart = new HashMap<>();

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
            final var listView = createListView(part, partName, pathToSeries);

            // this.sessionsByPart are available here, not before createListView()!
            final var sessions = this.sessionsByPart.get(partName);
            final Button nextSessionButton = createNextSessionButton(sessions);

            // build titledPane
            final var grid = new GridPane();
            grid.setVgap(4);
            grid.setPadding(new Insets(5, 5, 5, 5));

            grid.add(listView, 0, 0, 1, 3);

            grid.add(nextSessionButton, 1, 0);
            grid.add(new Button("Reset"), 1, 1);
            seriesContainer.getPanes().add(new TitledPane(partName, grid));
        }
    }

    private Button createNextSessionButton(UnlockList<Session> sessions) {
        final var nextSessionButton = new Button("Next Session");
        nextSessionButton.setOnAction(event -> {
            final var latestUnlockedSession = sessions.getLatestUnlockedElement();
            playSession(latestUnlockedSession);
        });

        // bind button
        final var buttonDisabledBinding = Bindings.createBooleanBinding(() -> {
            return sessions.getStates().stream().allMatch(state -> state.get() == ElementState.UNLOCKED);
        }, sessions.getStates().toArray(ObjectProperty[]::new)); // ElementStates will be observed
        nextSessionButton.disableProperty().bind(buttonDisabledBinding);
        return nextSessionButton;
    }

    private ListView<Session> createListView(final Path part, final String partName, final Path pathToSeries)
            throws IOException {
        final var sessions = createSessions(part, partName);
        this.sessionsByPart.put(partName, new UnlockList<>(sessions));
        final var listView = new ListView<Session>();
        listView.setCellFactory(listView1 -> new SessionListItem(this.sessionsByPart.get(partName)));

        listView.setItems(sessions);
        // eg: [Basics:Take10]
        listView.setId(String.format("[%s:%s]", pathToSeries.getFileName(), partName));
        return listView;
    }

    private static ObservableList<Session> createSessions(final Path tracksPath, final String partName)
            throws IOException {
        try (var files = Files.walk(tracksPath).filter(Files::isRegularFile)) {
            final ObservableList<Media> tracks = FXCollections.observableArrayList();
            files.map(Path::toUri).map(URI::toString).map(Media::new).forEach(tracks::add);

            ObservableList<Session> sessions = FXCollections.observableArrayList();
            sessions.add(new Session(partName, 1, tracks.get(0)));
            for (int i = 1; i < tracks.size(); i++)
                sessions.add(new Session(partName, i + 1, tracks.get(i)));
            return sessions;
        }
    }

    private final class SessionListItem extends ListCell<Session> {
        private HBox hbox = new HBox();
        private Session session;
        // for getting UnlockList.ElementState when updating session
        private UnlockList<Session> listIncludingSession;
        private Button trackBtn = new Button();

        SessionListItem(final UnlockList<Session> listIncludingSession) {
            this.hbox.getChildren().addAll(this.trackBtn);
            this.trackBtn.setOnAction(clickEvent -> showPlayer());
            this.listIncludingSession = listIncludingSession;
        }

        private void showPlayer() {
            playSession(this.session);
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
                final var sessionState = listIncludingSession.getState(this.session);

                this.trackBtn.setText(String.format("%02d", Integer.valueOf(this.session.day())));
                // bind button
                final var buttonDisabledBinding = Bindings.createBooleanBinding(() -> {
                    return switch (sessionState.get()) {
                        case LATEST_UNLOCKED, UNLOCKED -> false;
                        case LOCKED -> true;
                        default -> throw new IllegalArgumentException(
                                "no such state implemented yet: " + sessionState.get());
                    };
                }, sessionState); // UnlockList.ElementState will be observed
                this.trackBtn.disableProperty().bind(buttonDisabledBinding);

                setGraphic(this.hbox);
            }
        }
    }

    private void playSession(Session session) {
        final var sessions = this.sessionsByPart.get(session.part());
        final var player = new PlayerController(session, sessions.getState(session));
        player.showStage();
        // unlock next session
        player.getStage().setOnCloseRequest(event -> sessions.unlockNextElement(session));
    }
}
