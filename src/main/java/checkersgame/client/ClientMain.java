package checkersgame.client;

import javafx.application.Application;
import javafx.stage.Stage;
import java.util.Locale;

/**
 * <b>This is the main class of the CheckersGame Client</b>
 *
 * <p>The class is responsible for initializing the client side of the CheckersGame</p>
 *
 * @author Ilay Zvi
 */
public class ClientMain extends Application {
    @Override
    public void start(Stage stage){
        stage.setOnCloseRequest(event -> System.exit(0)); //disconnect on window close
        Locale.setDefault(Locale.ENGLISH);

        SceneManager.initializeInstance(stage); //initialize scene manager
        try{
            Client.initializeInstance(); //initialize client
            System.out.println("initialized client");

            SceneManager sceneManager = SceneManager.getInstance();

            assert sceneManager != null;
            sceneManager.initializeScene("HomePage.fxml"); //initialize UI
        }catch (Exception e){
            System.out.println("An error has occurred while establishing connection with the server");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
