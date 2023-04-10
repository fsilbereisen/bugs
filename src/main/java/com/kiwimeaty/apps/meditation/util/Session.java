package com.kiwimeaty.apps.meditation.util;

import com.kiwimeaty.apps.meditation.util.UnlockList.ElementState;

import javafx.beans.property.ObjectProperty;
import javafx.scene.media.Media;

/**
 * A session is one certain track.
 * 
 * @param part  the part for this session
 * @param day   the day of the part
 * @param track the file
 */
public record Session(Part part, int day, Media track) {//
    public ObjectProperty<ElementState> getState() {
        return this.part.sessions().getState(this);
    }
}