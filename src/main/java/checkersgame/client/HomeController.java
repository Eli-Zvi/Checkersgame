package checkersgame.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 * <b>This class represents the home scene's controller</b><br>
 *
 * <p>This class is responsible for the following actions:<br></p>
 * <ul>
 *     <li>Displaying the appropriate UI according to the client's information</li>
 *     <li>Switching to replay screen</li>
 *     <li>Switching into registration screen</li>
 *     <li>Calling onto homepage's stub to join a game</li>
 * </ul>
 *
 * @author Ilay Zvi
 */
public class HomeController {

    @FXML
    private Button joinGameButton, loginSceneButton, rgstrSceneButton, replayButton;

    @FXML
    private Label statusLabel, usernameLabel, numOfWinsLabel;

    /**
     * <b>Initializes the screen according to the client's login status</b>
     *
     * @throws RemoteException if an issue occurs while trying to retrieve win rate
     */
    @FXML
    private void initialize() throws RemoteException {
        if(Client.getInstance().isLoggedIn()) {
            joinGameButton.setDisable(false);
            usernameLabel.setVisible(true);

            rgstrSceneButton.setDisable(true);
            loginSceneButton.setDisable(true);
            statusLabel.setVisible(false);

            joinGameButton.setCursor(Cursor.HAND);

            numOfWinsLabel.setText("Win-Lose: " + Client.getInstance().getHomeStub().getWinRate(Client.getInstance().getCallback()));
            usernameLabel.setText("Logged in as: " + Client.getInstance().getUsername());
        }else{
            statusLabel.setText("Please log in to play");
            statusLabel.setVisible(true);

            joinGameButton.setDisable(true);
            usernameLabel.setVisible(false);
            numOfWinsLabel.setVisible(false);

            loginSceneButton.setCursor(Cursor.HAND);
            rgstrSceneButton.setCursor(Cursor.HAND);
        }
    }

    @FXML
    private void joinButtonOnAction(ActionEvent event) throws IOException{
        try{
            joinGameButton.setDisable(true);
            loginSceneButton.setDisable(true);
            rgstrSceneButton.setDisable(true);
            replayButton.setDisable(true);

            statusLabel.setVisible(true);
            statusLabel.setText("In Queue");

            Client.getInstance().getHomeStub().joinGame(Client.getInstance().getCallback());
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @FXML
    private void loginSceneOnAction(ActionEvent event) throws IOException {
        assert SceneManager.getInstance() != null;
        SceneManager.getInstance().switchScene("Register-LoginPage.fxml");
    }

    @FXML
    private void registerSceneOnAction(ActionEvent event) throws IOException{
        assert SceneManager.getInstance() != null;
        SceneManager.getInstance().switchScene("Register-LoginPage.fxml");
    }

    @FXML
    void replayOnAction(ActionEvent event) throws IOException {
        assert SceneManager.getInstance() != null;
        SceneManager.getInstance().switchScene("ReplayPage.fxml");
    }
}
