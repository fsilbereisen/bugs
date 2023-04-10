package com.kiwimeaty.apps.meditation;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.kiwimeaty.apps.meditation.util.JsonUtil;
import com.kiwimeaty.apps.meditation.util.Part;
import com.kiwimeaty.apps.meditation.util.Series;
import com.kiwimeaty.apps.meditation.util.Session;
import com.kiwimeaty.apps.meditation.util.UnlockList;
import com.kiwimeaty.apps.meditation.util.UnlockList.ElementState;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public final class MainController implements Initializable {
    @FXML
    private TabPane tabs;

    private final List<Series> series = new ArrayList<>();

    // TODO from test-tracks to real tracks
    private final Path tracksPath = Path.of("src", "main", "resources", "com", "kiwimeaty",
            "apps", "meditation", "test-tracks");
    // private final Path tracksPath = Path.of("tracks");

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try (var seriesStrm = Files.walk(this.tracksPath, 1).filter(Files::isDirectory)
                .filter(not(this.tracksPath::equals))) {
            for (final Path seriesPath : seriesStrm.toList()) {
                final var series1 = new Series(seriesPath);
                this.series.add(series1);
                createAndFillTitledPanes(series1);
            }

            JsonUtil.updateSeriesFromJsonFile(this.tracksPath, this.series);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void createAndFillTitledPanes(final Series series1) throws IOException {

        final var accordion = new Accordion();
        for (final Part part : series1.parts()) {
            final var listView = createListView(part);

            final var nextSessionButton = createNextSessionButton(part.sessions());
            final var resetButton = createResetPartButton(part);

            // build titledPane
            final var grid = new GridPane();
            grid.setVgap(4);
            grid.setPadding(new Insets(5, 5, 5, 5));

            grid.add(listView, 0, 0, 1, 3);

            grid.add(nextSessionButton, 1, 0);
            grid.add(resetButton, 1, 1);

            accordion.getPanes().add(new TitledPane(part.name(), grid));
        }
        this.tabs.getTabs().add(new Tab(series1.name(), accordion));
    }

    private Button createNextSessionButton(final UnlockList<Session> sessions) {
        final var nextSessionButton = new Button("Next Session");
        nextSessionButton.setOnAction(event -> {
            final var latestUnlockedSession = sessions.getLatestUnlockedElement();
            playSession(latestUnlockedSession);
        });

        // bind button
        final var buttonDisabledBinding = Bindings.createBooleanBinding(
                () -> sessions.getStates().stream().allMatch(state -> state.get() == ElementState.UNLOCKED),
                sessions.getStates().toArray(ObjectProperty[]::new)); // ElementStates will be observed
        nextSessionButton.disableProperty().bind(buttonDisabledBinding);
        return nextSessionButton;
    }

    private Button createResetPartButton(final Part part) {
        final var resetButton = new Button("Reset");
        resetButton.setOnAction(event -> showResetConfirmation(part));

        // bind button
        final var firstSessionState = part.sessions().get(0).getState();
        final var buttonDisabledBinding = Bindings.createBooleanBinding(() -> {
            return firstSessionState.get() == ElementState.LATEST_UNLOCKED;
        }, firstSessionState); // ElementState of first session will be observed
        resetButton.disableProperty().bind(buttonDisabledBinding);
        return resetButton;
    }

    private void showResetConfirmation(final Part part) throws IllegalArgumentException {
        final var alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Reset Part " + part.name());
        alert.setHeaderText("Are you sure you want to reset this part?");
        alert.setContentText("You'll have to redo each session to unlock them again.");

        final Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            part.sessions().resetList(); // TODO store reset
            // storeProgressIntoFile(sessions.getLatestUnlockedElement().part(),
            // sessions.getIndexOfLatestUnlockedElement());
        }
        alert.close();
    }

    private ListView<Session> createListView(final Part part)
            throws IOException {
        final var listView = new ListView<Session>();
        listView.setCellFactory(listView1 -> new SessionListItem());

        listView.setItems(FXCollections.observableArrayList(part.sessions()));
        // eg: .... [Basics:Take10]
        listView.setId("[%s:%s]".formatted(part.series().name(), part.name()));
        return listView;
    }

    private final class SessionListItem extends ListCell<Session> {
        private HBox hbox = new HBox();
        private Session session;
        private Button trackBtn = new Button();

        private SessionListItem() {
            this.hbox.getChildren().addAll(this.trackBtn);
            this.trackBtn.setOnAction(clickEvent -> showPlayer());
        }

        private void showPlayer() {
            playSession(this.session);
        }

        @Override
        protected void updateItem(final Session session1, final boolean empty) {
            super.updateItem(session1, empty);
            setText(null);
            if (empty) {
                this.session = null;
                setGraphic(null);
            } else {
                this.session = session1;
                final var sessionState = this.session.getState();

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

    private void playSession(final Session session) {
        final var player = new PlayerController(session, session.getState());
        player.showStage();
        // unlock next session
        player.getStage().setOnCloseRequest(event -> {
            final var sessions = session.part().sessions();
            final var hasUnlockedNextSession = sessions.unlockNextElement(session);
            if (hasUnlockedNextSession)
                try {
                    JsonUtil.storeToJson(this.tracksPath, session);
                } catch (final IOException ex) {
                    final var alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Store data.json");
                    alert.setHeaderText("There was an error storing data to data.json");
                    alert.setContentText(ex.toString());
                    alert.close();
                }
        });
    }
}
