module checkersgame {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.xml.dom;
    requires java.rmi;
    requires java.sql;
    requires jdk.jshell;
    requires com.zaxxer.hikari;
    requires org.slf4j;


    exports checkersgame.common;
    opens checkersgame.common to javafx.fxml;
    exports checkersgame.server;
    opens checkersgame.server to javafx.fxml;
    exports checkersgame.client;
    opens checkersgame.client to javafx.fxml;
}