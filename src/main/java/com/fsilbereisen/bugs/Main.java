package com.fsilbereisen.bugs;

import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;

public class Main {
    private static ReadOnlyBooleanProperty bool;

    public static void main(String[] args) {
        // ref is shown
        bool.addListener(
                normal(bool1 -> notAllReferencesAreShown(bool1)));

        // ref is not shown
        bool.addListener(
                generic(bool1 -> notAllReferencesAreShown(bool1)));

    }

    private static void notAllReferencesAreShown(final Boolean isSelected) {
    }

    private static <T> ChangeListener<T> generic(final Consumer<T> consumer) {
        return (observable, oldValue, newValue) -> consumer.accept(newValue);
    }

    private static ChangeListener<Boolean> normal(final Consumer<Boolean> consumer) {
        return (observable, oldValue, newValue) -> consumer.accept(newValue);
    }
}
