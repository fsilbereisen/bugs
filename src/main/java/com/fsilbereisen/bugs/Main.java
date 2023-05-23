package com.fsilbereisen.bugs;

import java.util.function.Consumer;

import com.fsilbereisen.bugs.util.CustomListener;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;

public class Main {
    // ############ javafx listener >> some refs are not shown! ###########
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

    // ############## own listener >> all refs are shown ####################
    public static void main2(String[] args) {
        // ref is shown
        addListener2(
                normal2(bool1 -> notAllReferencesAreShown2(bool1)));

        // ref is also shown!
        addListener2(
                generic2(bool1 -> notAllReferencesAreShown2(bool1)));

    }

    private static void notAllReferencesAreShown2(final Boolean isSelected) {
    }

    private static void addListener2(CustomListener<Boolean> l) {
    }

    private static <T> CustomListener<T> generic2(final Consumer<T> consumer) {
        return (newValue) -> consumer.accept(newValue);
    }

    private static CustomListener<Boolean> normal2(final Consumer<Boolean> consumer) {
        return (newValue) -> consumer.accept(newValue);
    }
}
