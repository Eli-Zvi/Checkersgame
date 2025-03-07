package checkersgame.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
//TODO ADD DOCUMENTATION
public class SceneManager {
    private static SceneManager instance;
    private final Stage stage;

    private SceneManager(Stage stage) {
        this.stage = stage;
    }

    public static void initializeInstance(Stage stage){
        if(instance == null){
            instance = new SceneManager(stage);
        }
    }

    public static SceneManager getInstance() {
        if (instance == null) {
            return null;
        }else return instance;
    }

    public void initializeScene(String fxmlFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
        Scene _scene = new Scene(fxmlLoader.load());
        stage.setTitle("CheckersGame");
        stage.setScene(_scene);
        stage.setResizable(false);
        stage.show();
    }

    public <T> T switchScene(String scene) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(scene));
        Scene _scene = new Scene(fxmlLoader.load());
        stage.setScene(_scene);
        stage.show();
        return fxmlLoader.getController();
    }
}
