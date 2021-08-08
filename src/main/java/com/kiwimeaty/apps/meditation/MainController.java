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
import com.kiwimeaty.apps.meditation.util.Session.State;

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

    private Map<String, List<Session>> sessionsByPart = new HashMap<>();

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

    private Button createNextSessionButton(List<Session> sessions) {
        final var nextSessionButton = new Button("Next Session");
        nextSessionButton.setOnAction(event -> {
            sessions.stream().map(s -> s.state().get()).forEach(System.out::println);
            System.out.println();
            final var latestUnlockedSession = sessions.stream()
                    .filter(s -> s.state().get() == Session.State.LATEST_UNLOCKED).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("no " + Session.State.LATEST_UNLOCKED + " exists"));
            playSession(latestUnlockedSession);
        });

        // bind button
        final var buttonDisabledBinding = Bindings.createBooleanBinding(() -> {
            return sessions.stream().allMatch(session -> session.state().get() == State.UNLOCKED);
        }, sessions.stream().map(Session::state).toArray(ObjectProperty[]::new)); // Session.States will be observed
        nextSessionButton.disableProperty().bind(buttonDisabledBinding);
        return nextSessionButton;
    }

    private ListView<Session> createListView(final Path part, final String partName, final Path pathToSeries)
            throws IOException {
        final var listView = new ListView<Session>();
        listView.setCellFactory(listView1 -> new SessionListItem());
        final var sessions = createSessions(part, partName);
        this.sessionsByPart.put(partName, sessions);

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
            sessions.add(Session.create(partName, 1, tracks.get(0), Session.State.LATEST_UNLOCKED));
            for (int i = 1; i < tracks.size(); i++)
                sessions.add(Session.create(partName, i + 1, tracks.get(i), Session.State.LOCKED));
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
                this.trackBtn.setText(String.format("%02d", Integer.valueOf(this.session.day())));
                // bind button
                final var buttonDisabledBinding = Bindings.createBooleanBinding(() -> {
                    return switch (this.session.state().get()) {
                        case LATEST_UNLOCKED, UNLOCKED -> false;
                        case LOCKED -> true;
                        default -> throw new IllegalArgumentException(
                                "no such state implemented yet: " + this.session.state().get());
                    };
                }, this.session.state()); // Session.State will be observed
                this.trackBtn.disableProperty().bind(buttonDisabledBinding);

                setGraphic(this.hbox);
            }
        }
    }

    private void playSession(Session session) {
        final var player = new PlayerController(session);
        player.showStage();
        // unlock next session
        player.getStage().setOnCloseRequest(event -> {
            if (session.state().get() == Session.State.UNLOCKED) {
                final var sessions = this.sessionsByPart.get(session.part());
                final var sessionIndex = sessions.indexOf(session);
                if (sessionIndex < sessions.size() - 1) {
                    final var nextSession = sessions.get(sessionIndex + 1);
                    if (nextSession.state().get() == State.LOCKED)
                        nextSession.state().set(Session.State.LATEST_UNLOCKED);
                }
            }
        });
    }
}
