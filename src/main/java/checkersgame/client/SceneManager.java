package checkersgame.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
/**
 * <b>This Singleton class represents the client's UI</b><br>
 *
 * <p>The class is responsible for initializing the UI and switching scenes when called upon</p>
 *
 * @author Ilay Zvi
 */
public class SceneManager {
    private static SceneManager instance;
    private final Stage stage;

    /**
     * Private constructor for the singleton SceneManager class<br>
     *
     * Creates a new instance of SceneManager and initializes stage
     */
    private SceneManager(Stage stage) {
        this.stage = stage;
    }

    /**
     * This method initializes singleton instance of the class SceneManager
     */
    public synchronized static void initializeInstance(Stage stage){
        if(instance == null){
            instance = new SceneManager(stage);
        }
    }

    /**
     * Gets the value of the property instance.
     */
    public static SceneManager getInstance() {
        if (instance == null) {
            return null;
        }else return instance;
    }

    /**
     * Initializes the client UI with the specified @param fxmlFile as the scene
     *
     * @param fxmlFile a string representation of the scene's fxml file name
     */
    public void initializeScene(String fxmlFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
        Scene _scene = new Scene(fxmlLoader.load());
        stage.setTitle("Checkers Game");
        stage.setScene(_scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Changes the current scene to the one specified in the @param scene
     *
     * @param scene a string representation of the scene's fxml file name
     */
    public void switchScene(String scene) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(scene));
        Scene _scene = new Scene(fxmlLoader.load());
        stage.setScene(_scene);
        stage.show();
        fxmlLoader.getController();
    }

    public static void displayNetworkError(){
        new Alert(Alert.AlertType.ERROR, "A Network Error Has Occurred, the program will now exit").showAndWait();
        System.exit(-1);
    }
}
