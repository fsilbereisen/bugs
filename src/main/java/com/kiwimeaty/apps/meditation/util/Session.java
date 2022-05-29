package com.kiwimeaty.apps.meditation.util;

import javafx.scene.media.Media;

/**
 * A session is one certain track.
 * 
 * @param part  the part
 * @param day   the day of the part
 * @param track the file
 */
public final record Session(String part, int day, Media track) {//
}