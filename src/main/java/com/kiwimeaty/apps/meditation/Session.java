package com.kiwimeaty.apps.meditation;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;

public final record Session(String part, int day, Media track, ObjectProperty<Session.State> state) {

    public static Session create(String part, int day, Media track, State state) {
        final var stateProp = new SimpleObjectProperty<Session.State>(state);
        return new Session(part, day, track, stateProp);
    }

    public enum State {
        CLOSED, OPEN_CURRENT, OPEN_NEXT
    }
}