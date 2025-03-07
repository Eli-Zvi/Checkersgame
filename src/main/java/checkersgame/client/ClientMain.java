package checkersgame.client;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.Locale;

/**
 * <b>This main class of the Client of CheckersGame</b>
 *
 * <p>The Client class is responsible for initializing the client side of the CheckersGame</p>
 *
 * @author Ilay Zvi
 */
public class ClientMain extends Application {
    @Override
    public void start(Stage stage){
        stage.setOnCloseRequest(event -> System.exit(0)); //add call to server that the connection is being closed and then have it call a forfeit if needed
        Locale.setDefault(Locale.ENGLISH);
        SceneManager.initializeInstance(stage); //initialize scene manager
        Client.initializeInstance(); //initialize client
        SceneManager sceneManager = SceneManager.getInstance();
        try{
            assert sceneManager != null;
            sceneManager.initializeScene("HomePage.fxml"); //initialize UI
        }catch (Exception e){
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
