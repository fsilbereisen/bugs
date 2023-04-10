package com.kiwimeaty.apps.meditation.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.media.Media;

/**
 * A part is a list of all sessions.
 */
public final class Part {
    private final String name;
    private final Series series;

    private final UnlockList<Session> sessions;

    public Part(final Series series, final Path path) throws IOException {
        this.series = series;
        this.name = path.getFileName().toString();
        this.sessions = createSessions(path);
    }

    private UnlockList<Session> createSessions(final Path tracksPath)
            throws IOException {
        try (var files = Files.walk(tracksPath).filter(Files::isRegularFile)) {
            final List<Media> tracks = new ArrayList<>();
            files.map(Path::toUri).map(URI::toString).map(Media::new).forEach(tracks::add);

            final List<Session> sessions1 = new ArrayList<>();
            for (var i = 0; i < tracks.size(); i++)
                sessions1.add(new Session(this, i + 1, tracks.get(i)));
            return new UnlockList<>(sessions1);
        }
    }

    public void setIndexOfLatestUnlockedElement(final int value) {
        this.sessions.setIndexOfLatestUnlockedElement(value);
    }

    public String name() {
        return this.name;
    }

    public Series series() {
        return this.series;
    }

    public UnlockList<Session> sessions() {
        return this.sessions;
    }

    @Override
    public String toString() {
        return this.name;
    }

}