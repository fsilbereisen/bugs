package com.kiwimeaty.apps.meditation.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;

public final record Session(String part, int day, Media track, ObjectProperty<Session.State> state) {

    public static Session create(String part, int day, Media track, State state) {
        final var stateProp = new SimpleObjectProperty<Session.State>(state);
        return new Session(part, day, track, stateProp);
    }

    public enum State {
        /** This session is still locked. */
        LOCKED,
        /** This session is unlocked. */
        UNLOCKED,
        /** This is the latest unlocked session. */
        LATEST_UNLOCKED
    }
}