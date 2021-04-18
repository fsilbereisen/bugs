package com.kiwimeaty.apps.meditation;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

public class ListViewSampleTracks extends Application {
	private static MediaPlayer mediaPlayer;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		primaryStage.setTitle("List View Sample");

		ObservableList<Media> tracks = createTracks();
		ListView<Media> listView = createTracksListView(tracks);

		Scene listViewScene = new Scene(listView, 350, 100);
		primaryStage.setScene(listViewScene);
		primaryStage.show();
	}

	public ListView<Media> createTracksListView(ObservableList<Media> tracks) {
		final ListView<Media> tracksListView = new ListView<>();
		tracksListView.setCellFactory(listView -> new TrackListItem());
		tracksListView.setItems(tracks);
		return tracksListView;
	}

	private ObservableList<Media> createTracks() throws IOException {
		ObservableList<Media> tracks = FXCollections.observableArrayList();
		final var tracksPath = Path.of("src", "main", "resources", "com", "kiwimeaty", "apps", "meditation",
				"test-tracks");

		try (var files = Files.walk(tracksPath).filter(Files::isRegularFile);) {
			files.map(Path::toUri).map(URI::toString).map(Media::new).forEach(tracks::add);
			return tracks;
		}
	}

	private class TrackListItem extends ListCell<Media> {
		HBox hbox = new HBox();
		Media track;
		Button detailsButton = new Button("No metadata found... yet?");

		public TrackListItem() {
			hbox.getChildren().addAll(detailsButton); // TODO replace hbox?
			detailsButton.setOnAction(clickEvent -> startTrack());

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
				System.out.println(track.getMetadata());
				updateButtonName(track);
			}
		}

		// https://gist.github.com/ThomasBassa/f7c20ba4b6341a05cd2375f24f63e6c5
		private void updateButtonName(Media track) {
			// Get the juicy metadata... when it arrives. It's not immediate :\
			ObservableMap<String, Object> meta = track.getMetadata();

			// So we have to register a listener for that map
			/*
			 * The awful MapChangeListener.Change bit is to make chg non-ambiguous because
			 * it could be an InvalidationListener obj instead.
			 */
			meta.addListener((MapChangeListener.Change<? extends String, ? extends Object> chg) -> {//
				detailsButton.setText(String.format("%02d %s", meta.get("track number"), meta.get("title")));
			});
			setGraphic(hbox);
		}
	}
}
