module com.kiwimeaty.apps.meditation {
    requires javafx.base;
    requires javafx.fxml;

    requires javafx.controls;
    requires transitive javafx.media;

    requires transitive javafx.graphics;

    // third party
    requires transitive java.json;

    opens com.kiwimeaty.apps.meditation to javafx.fxml;

    exports com.kiwimeaty.apps.meditation;
    exports com.kiwimeaty.apps.meditation.util;
}
