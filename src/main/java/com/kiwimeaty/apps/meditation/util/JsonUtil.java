package com.kiwimeaty.apps.meditation.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParsingException;

public final class JsonUtil {
    private static final JsonWriterFactory writerFactory;

    private static final String COURSE = "courses";
    private static final String PARTS = "parts";
    private static final String COURSE_NAME = "course";
    private static final String PART_NAME = "part";
    private static final String LATEST_UNLOCKED_INDEX = "index";

    static {
        final var config = new HashMap<String, Object>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        writerFactory = Json.createWriterFactory(config);
    }

    private static JsonObject readFile(final Path path) throws IOException {
        final var json = Files.readString(path);
        try (var in = new StringReader(json); var reader = Json.createReader(in)) {
            return reader.readObject();
        } catch (final JsonParsingException ex) {
            throw new IOException(ex);
        }
    }

    public static void storeToJson(final Path pathToJson, final Session session)
            throws IOException {
        final var file = pathToJson.resolve("data.json");
        final var currentDataJsonObject = readFile(file);
        System.out.println("before: " + currentDataJsonObject);
        final var newDataJsonObject = updateDataJson(currentDataJsonObject, session);
        System.out.println("after: " + newDataJsonObject);
        System.out.println();
        Files.writeString(file, jsonPrettyPrint(newDataJsonObject),
                StandardCharsets.UTF_8);
    }

    public static void updateCoursesFromJsonFile(final Path pathToJson, final List<Course> courses)
            throws IOException {
        final var file = pathToJson.resolve("data.json");
        final var currentDataJsonObject = readFile(file);
        updateIndexes(currentDataJsonObject, courses);
    }

    // ########################### helper #################################
    private static void updateIndexes(final JsonObject currentDataJsonObject, final List<Course> courses) {
        final var coursesArray = currentDataJsonObject.getJsonArray(COURSE);
        coursesArray.stream().map(JsonValue::asJsonObject)
                .forEach(courseObject -> {
                    final var courseName = courseObject.getString(COURSE_NAME);
                    final var jsonCourse = Course.findCourse(courseName, courses);
                    // TODO ifPresentOrElse() >> or else: delete jsonCourse
                    jsonCourse.ifPresent(existingCourse -> {
                        final var partsOfCourseArray = courseObject.getJsonArray(PARTS);
                        partsOfCourseArray.stream().map(JsonValue::asJsonObject).forEach(partObject -> {
                            final var partName = partObject.getString(PART_NAME);
                            final var jsonPart = existingCourse.findPart(partName);
                            // TODO ifPresentOrElse() >> or else: delete jsonPart
                            jsonPart.ifPresent(existingPart -> existingPart
                                    .setIndexOfLatestUnlockedElement(partObject.getInt(LATEST_UNLOCKED_INDEX)));
                        });
                    });
                });
    }

    private static JsonObject updateDataJson(final JsonObject currentJsonObject, final Session session) {
        final var coursesArray = currentJsonObject.getJsonArray(COURSE);
        final var newCoursesArray = updateCoursesArray(coursesArray, session);
        return Json.createObjectBuilder().add(COURSE, newCoursesArray).build();
    }

    private static JsonArray updateCoursesArray(final JsonArray array, final Session session) {
        // JsonArray is immutable >> transfer to map
        final var map = array.stream().map(JsonValue::asJsonObject)
                .collect(Collectors.toMap(obj -> obj.getString(COURSE_NAME),
                        obj -> obj.getJsonArray(PARTS)));

        final var courseName = session.part().course().name();
        final var parts = map.get(courseName);
        // modify parts + add in map
        final var newParts = updatePartsArray(parts, session);
        map.merge(courseName, newParts, (oldParts, newParts1) -> newParts1);

        // retransfer
        final var listOfObjects = map.entrySet().stream().map(entry -> array2jsonObj(entry.getKey(), entry.getValue()))
                .toList();
        final var newArrayBuilder = Json.createArrayBuilder();
        listOfObjects.forEach(obj -> newArrayBuilder.add(obj));
        return newArrayBuilder.build();
    }

    private static JsonArray updatePartsArray(final JsonArray partsArray, final Session session) {
        // JsonArray is immutable >> transfer to map
        final var map = partsArray != null ? //
                partsArray.stream().map(JsonValue::asJsonObject)
                        .collect(Collectors.toMap(obj -> obj.getString(PART_NAME),
                                obj -> obj.getInt(LATEST_UNLOCKED_INDEX)))
                // if no parts array exists yet in json, create it
                : new HashMap<String, Integer>();
        // modify/add entry
        map.merge(session.part().name(), session.part().sessions().getIndexOfLatestUnlockedElement(),
                (oldInt, newInt) -> newInt);
        // retransfer
        final var listOfObjects = map.entrySet().stream().map(entry -> int2jsonObj(entry.getKey(), entry.getValue()))
                .toList();
        final var newArrayBuilder = Json.createArrayBuilder();
        listOfObjects.forEach(obj -> newArrayBuilder.add(obj));
        return newArrayBuilder.build();
    }

    private static JsonObject array2jsonObj(final String courseName, final JsonArray parts) {
        final var builder = Json.createObjectBuilder()//
                .add(COURSE_NAME, courseName)//
                .add(PARTS, parts);
        return builder.build();
    }

    private static JsonObject int2jsonObj(final String partName, final int indexOfLatestUnlockedElement) {
        final var builder = Json.createObjectBuilder()//
                .add(PART_NAME, partName)//
                .add(LATEST_UNLOCKED_INDEX, indexOfLatestUnlockedElement);
        return builder.build();
    }

    private static String jsonPrettyPrint(final JsonObject obj) throws IOException {
        try (var out = new StringWriter(); var writer = writerFactory.createWriter(out)) {
            writer.writeObject(obj);
            return out.toString();
        }
    }

    private JsonUtil() {// hidden
    }
}
