package com.kiwimeaty.apps.meditation.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A course is a whole meditation pack. It consists of one or more parts.
 */
public final class Course {

    public static Optional<Course> findCourse(final String courseName, final List<Course> courses) {
        if (courseName == null)
            return Optional.empty();
        return courses.stream().filter(s -> courseName.equals(s.name())).findAny();
    }

    private final String name;
    private final List<Part> parts;

    public Course(final Path path) throws IOException {
        this.name = path.getFileName().toString();
        this.parts = getParts(path);
    }

    private List<Part> getParts(final Path coursePath)
            throws IOException {
        try (var partsStrm = Files.walk(coursePath).filter(Files::isDirectory)
                .filter(Predicate.not(coursePath::equals))) {

            final List<Part> parts1 = new ArrayList<>();
            for (final Path partPath : partsStrm.toList()) {
                final var part = new Part(this, partPath);
                parts1.add(part);
            }
            return parts1;
        }
    }

    public Optional<Part> findPart(final String partName) {
        if (partName == null)
            return Optional.empty();
        return this.parts.stream().filter(p -> partName.equals(p.name())).findAny();
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