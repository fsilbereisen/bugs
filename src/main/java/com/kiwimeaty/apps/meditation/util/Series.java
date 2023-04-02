package com.kiwimeaty.apps.meditation.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A series is a whole meditation pack.
 */
public final class Series {
    private final String name;
    private final List<Part> parts;

    public Series(final Path path) throws IOException {
        this.name = path.getFileName().toString();
        this.parts = getParts(path);
    }

    private List<Part> getParts(final Path seriesPath)
            throws IOException {
        try (var partsStrm = Files.walk(seriesPath).filter(Files::isDirectory)
                .filter(Predicate.not(seriesPath::equals))) {

            final List<Part> parts1 = new ArrayList<>();
            for (final Path partPath : partsStrm.toList()) {
                final var part = new Part(this, partPath);
                parts1.add(part);
            }
            return parts1;
        }

    }

    public String name() {
        return this.name;
    }

    public List<Part> parts() {
        return List.copyOf(this.parts);
    }

    @Override
    public String toString() {
        return this.name;
    }
}