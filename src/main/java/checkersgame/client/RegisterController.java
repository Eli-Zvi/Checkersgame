package checkersgame.client;

import checkersgame.common.Password;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.UUID;

/**
 * <b>This class represents the register scene's controller</b><br>
 *
 * <p>This class is responsible for the following actions:</p>
 * <ol>
 *     <li>Registering or Logging in the user</li>
 *     <li>Switching to home screen</li>
 * </ol>
 *
 * @author Ilay Zvi
 */
public class RegisterController {
    @FXML
    private Button homeSceneButton, loginButton, registerButton;

    @FXML
    private TextField nameLoginField, passLoginField, userRegisterField, passRegisterField, passConRegisterField;

    /**
     * Initializes the UI according to the user's current Login status
     */
    @FXML
    private void initialize(){
        homeSceneButton.setCursor(Cursor.HAND);
        if (!Client.getInstance().isLoggedIn()) {
            loginButton.setCursor(Cursor.HAND);
            registerButton.setCursor(Cursor.HAND);
        }else{ //should not be able to get here
            disableAll();
        }
    }

    /**
     * Switches to home scene
     */
    @FXML
    private void homeSceneSwitch(ActionEvent event) throws IOException {
        assert SceneManager.getInstance() != null;
        SceneManager.getInstance().switchScene("HomePage.fxml");
    }

    /**
     * <p>Login button on action function, validates user input, and sends to server</p>
     * <p>If login is successful logs in and displays the success message otherwise displays error message</p>
     */
    @FXML
    private void loginOnAction(ActionEvent event) {
        if(!nameLoginField.getText().isEmpty() && !passLoginField.getText().isEmpty() && !Client.getInstance().isLoggedIn()){
            String username = nameLoginField.getText().toLowerCase();
            String password = passLoginField.getText();
            clearLoginFields();

            if (username.length() > 3 && password.length() > 7 && password.length() < 17 && username.length() < 17){
                Client client = Client.getInstance();
                CallBackImpl callBack = client.getCallback();

                try {
                    //gets homestub and sends callback, username, and an encrypted password
                    UUID uuid = client.getHomeStub().login(callBack, username, new Password(password, client.getKey()));

                    client.setUUID(uuid);
                    client.setLoggedIn(true);
                    client.setUsername(username);
                    disableAll();

                    //display success message and switch to home screen
                    alert(Alert.AlertType.INFORMATION,
                            "Login successful!",
                            "Successfully logged in with username: " + username)
                            .showAndWait().ifPresent(response ->{
                                try{
                                    homeSceneSwitch(null);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                    });

                } catch (GeneralSecurityException | SQLException | RemoteException e) { //display error when error occurs on server side
                    if(e instanceof SQLException){
                        alert(Alert.AlertType.ERROR,"Login Error", "Invalid Username or Password").show();
                    }else{
                        alert(Alert.AlertType.ERROR,"Authentication Error", "An unknown error has occurred").show();
                    }
                }
            }else{
                alert(Alert.AlertType.ERROR,"Login Error", "Invalid Username or Password").show();
            }
        }else{
            alert(Alert.AlertType.ERROR, "Login Error", "One or more fields are empty").show();
        }
    }

    /**
     * <p>Registration button on action function, validates user input, and sends to server</p>
     * <p>If registration is successful, logs in and displays the appropriate message, otherwise displays error message</p>
     */
    @FXML
    private void registerOnAction(ActionEvent event) { //add compare while ignoring caps on server side and that way create usernames of all different caps
        if(!userRegisterField.getText().isEmpty() && !passRegisterField.getText().isEmpty() && !passConRegisterField.getText().isEmpty()
            && !Client.getInstance().isLoggedIn()){
            String password = passRegisterField.getText();
            String passwordCon = passConRegisterField.getText();
            String username = userRegisterField.getText().toLowerCase();
            clearRegisterFields();

            if(!password.equals(passwordCon)){ //if the 2 password fields are not equal display error
                alert(Alert.AlertType.ERROR,"Registration Error", "Passwords do not match").show();
            }else if(password.length() < 8 || password.length() > 16){ //if the password's length is invalid display error
                alert(Alert.AlertType.ERROR,"Registration Error", "Password length must be between 8 and 16").show();
            }else if(username.length() < 4 || username.length() > 16){ //if username's length is invalid display error
                alert(Alert.AlertType.ERROR,"Registration Error", "Username length must be between 4 and 16").show();
            }else {
                Client client = Client.getInstance();
                CallBackImpl callBack = client.getCallback();

                try {
                    //gets homestub and sends callback, username, and an encrypted password
                    UUID uuid = client.getHomeStub().register(callBack, username, new Password(password, client.getKey()));
                    if (uuid == null) {
                        System.out.println("already logged in");
                        return;
                    }

                    client.setUUID(uuid);
                    client.setLoggedIn(true);
                    client.setUsername(username);
                    disableAll();

                    alert(Alert.AlertType.INFORMATION,
                            "Successful registration and login!",
                            "Logged in with username: " + username).showAndWait().ifPresent(response -> {
                        try {
                            homeSceneSwitch(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (GeneralSecurityException | SQLException | RemoteException e) {
                    if (e instanceof SQLException) {
                        alert(Alert.AlertType.ERROR,"Registration Error", "Username already exists").show();
                    } else {
                        alert(Alert.AlertType.ERROR,"Authentication Error", "An unknown error has occurred").show();
                    }
                }
            }
        }else{
            alert(Alert.AlertType.ERROR,"Registration Error", "One or more fields are empty").show();
        }
    }

    /**
     * Creates a new alert with the specified parameters
     * @param type the type of the alert
     * @param title the title of the alert
     * @param header the header text of the alert
     * @return a new alert with the specified parameters
     */
    private Alert alert(Alert.AlertType type, String title, String header){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setWidth(400);
        alert.setHeight(200);
        return alert;
    }

    /**
     * Disables all buttons and fields
     */
    private void disableAll(){
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        passLoginField.setDisable(true);
        passRegisterField.setDisable(true);
        passConRegisterField.setDisable(true);
        nameLoginField.setDisable(true);
        userRegisterField.setDisable(true);
    }

    /**
     * Clears the registration fields, username, password,
     */
    private void clearRegisterFields(){
        userRegisterField.clear();
        passRegisterField.clear();
        passConRegisterField.clear();
    }

    /**
     * Clears the login fields, username and password
     */
    private void clearLoginFields(){
        nameLoginField.clear();
        passLoginField.clear();
    }
}
