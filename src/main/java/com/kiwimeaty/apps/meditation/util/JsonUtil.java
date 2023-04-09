package com.kiwimeaty.apps.meditation.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

    private static final String SERIES = "series";
    private static final String PARTS = "parts";
    private static final String SERIES_NAME = "seriesName";
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

    // ########################### helper #################################
    private static JsonObject updateDataJson(final JsonObject currentJsonObject, final Session session) {
        final var seriesArray = currentJsonObject.getJsonArray(SERIES);
        final var newSeriesArray = updateSeriesArray(seriesArray, session);
        return Json.createObjectBuilder().add(SERIES, newSeriesArray).build();
    }

    private static JsonArray updateSeriesArray(final JsonArray array, final Session session) {
        // JsonArray is immutable >> transfer to map
        final var map = array.stream().map(JsonValue::asJsonObject)
                .collect(Collectors.toMap(obj -> obj.getString(SERIES_NAME),
                        obj -> obj.getJsonArray(PARTS)));

        final var seriesName = session.part().series().name();
        final var parts = map.get(seriesName);
        // modify parts + add in map
        final var newParts = updatePartsArray(parts, session);
        map.merge(seriesName, newParts, (oldParts, newParts1) -> newParts1);

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

    private static JsonObject array2jsonObj(final String seriesName, final JsonArray parts) {
        final var builder = Json.createObjectBuilder()//
                .add(SERIES_NAME, seriesName)//
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
