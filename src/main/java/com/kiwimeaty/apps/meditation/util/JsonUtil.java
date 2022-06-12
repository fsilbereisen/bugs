package com.kiwimeaty.apps.meditation.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
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

    private static final String PARTS = "parts";
    private static final String NAME = "name";
    private static final String INDEX_OF_LATEST_UNLOCKED_ELEMENT = "indexOfLatestUnlocked";

    static {
        final var config = new HashMap<String, Object>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        writerFactory = Json.createWriterFactory(config);
    }

    public static JsonObject readFile(final Path path) throws IOException {
        // final JsonArray obj;
        final JsonObject obj;
        final var json = Files.readString(path);
        try (var in = new StringReader(json); var reader = Json.createReader(in)) {
            obj = reader.readObject();
        } catch (final JsonParsingException ex) {
            throw new IOException(ex);
        }
        return obj;
    }

    public static void storeToJson(final Path pathToJson, final String part, final int indexOfLatestUnlockedElement)
            throws IOException {
        final var file = pathToJson.resolve("data.json");
        final var currentRootJsonObject = readFile(file);
        System.out.println("before: " + currentRootJsonObject);
        final var newRootJsonObject = updateJsonObject(currentRootJsonObject, part, indexOfLatestUnlockedElement);
        System.out.println("after: " + newRootJsonObject);
        System.out.println();
        try {
            Files.writeString(file, jsonPrettyPrint(newRootJsonObject),
                    StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    // ###################################################

    private static JsonObject updateJsonObject(final JsonObject currentJsonObject, final String part,
            final int indexOfLatestUnlockedElement) {
        final var array = currentJsonObject.getJsonArray(PARTS);
        final var newArray = updateJsonArray(array, part, indexOfLatestUnlockedElement);
        return Json.createObjectBuilder().add(PARTS, newArray).build();
    }

    private static JsonArray updateJsonArray(final JsonArray array, final String part,
            final int indexOfLatestUnlockedElement) {
        // JsonArray is immutable >> transfer to map
        final var map = array.stream().map(JsonValue::asJsonObject)
                .collect(Collectors.toMap(obj -> obj.getString(NAME),
                        obj -> obj.getInt(INDEX_OF_LATEST_UNLOCKED_ELEMENT)));
        // modify/add entry
        map.merge(part, indexOfLatestUnlockedElement, (oldI, newI) -> newI);
        // retransfer
        final var listOfObjects = map.entrySet().stream().map(entry -> jsonObject(entry.getKey(), entry.getValue()))
                .toList();
        final var newArrayBuilder = Json.createArrayBuilder();
        listOfObjects.forEach(obj -> newArrayBuilder.add(obj));
        return newArrayBuilder.build();
    }

    private static JsonObject jsonObject(final String part, final int indexOfLatestUnlockedElement) {
        final var builder = Json.createObjectBuilder()//
                .add(NAME, part)//
                .add(INDEX_OF_LATEST_UNLOCKED_ELEMENT, indexOfLatestUnlockedElement);
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
