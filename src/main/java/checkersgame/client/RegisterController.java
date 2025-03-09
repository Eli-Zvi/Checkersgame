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
 * <p>This class is responsible for the following actions:<br></p>
 * <ul>
 *     <li>Registering or Logging in the user</li>
 *     <li>Switching to home screen</li>
 * </ul>
 *
 * @author Ilay Zvi
 */
public class RegisterController {
//TODO ADD DOCUMENTATION
    @FXML
    private Button homeSceneButton, loginButton, registerButton;

    @FXML
    private TextField nameLoginField, passLoginField, userRegisterField, passRegisterField, passConRegisterField;

    @FXML
    private void initialize(){
        homeSceneButton.setCursor(Cursor.HAND);
        if (!Client.getInstance().isLoggedIn()) {
            loginButton.setCursor(Cursor.HAND);
            registerButton.setCursor(Cursor.HAND);
        }else{
            disableAll();
        }
    }

    @FXML
    private void homeSceneSwitch(ActionEvent event) throws IOException {
        assert SceneManager.getInstance() != null;
        SceneManager.getInstance().switchScene("HomePage.fxml");
    }

    @FXML
    void loginOnAction(ActionEvent event) {
        if(!nameLoginField.getText().isEmpty() && !passLoginField.getText().isEmpty() && !Client.getInstance().isLoggedIn()){
            String username = nameLoginField.getText().toLowerCase();
            String password = passLoginField.getText();
            clearLoginFields();
            if (username.length() > 7 && password.length() > 7 && password.length() < 17 && username.length() < 17){
                Client client = Client.getInstance();
                CallBackImpl callBack = client.getCallback();
                try {
                    UUID uuid = client.getHomeStub().login(callBack, username, new Password(password, client.getKey()));
                    client.setUUID(uuid);
                    client.setLoggedIn(true);
                    client.setUsername(username);
                    disableAll();
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
                } catch (GeneralSecurityException | SQLException | RemoteException e) {
                    if(e instanceof SQLException){
                        alert(Alert.AlertType.ERROR,"Login Error", "Invalid Username or Password").show();
                    }else{
                        alert(Alert.AlertType.ERROR,"Authentication Error", "An unknown error has occurred").show();
                    }
                }
            }
        }else{
            alert(Alert.AlertType.ERROR, "Login Error", "One or more fields are empty").show();
        }
    }

    @FXML
    void registerOnAction(ActionEvent event) { //add compare while ignoring caps on server side and that way create usernames of all different caps
        if(!userRegisterField.getText().isEmpty() && !passRegisterField.getText().isEmpty() && !passConRegisterField.getText().isEmpty()
            && !Client.getInstance().isLoggedIn()){
            String password = passRegisterField.getText();
            String passwordCon = passConRegisterField.getText();
            String username = userRegisterField.getText().toLowerCase();
            clearRegisterFields();
            if(!password.equals(passwordCon)){
                alert(Alert.AlertType.ERROR,"Registration Error", "Passwords do not match").show();
            }else if(password.length() < 8 || password.length() > 16){
                alert(Alert.AlertType.ERROR,"Registration Error", "Password length must be between 8 and 16").show();
            }else if(username.length() < 4 || username.length() > 16){
                alert(Alert.AlertType.ERROR,"Registration Error", "Username length must be between 4 and 16").show();
            }else {
                Client client = Client.getInstance();
                CallBackImpl callBack = client.getCallback();
                try {
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

    private Alert alert(Alert.AlertType type, String title, String header){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setWidth(400);
        alert.setHeight(200);
        return alert;
    }

    private void disableAll(){
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        passLoginField.setDisable(true);
        passRegisterField.setDisable(true);
        passConRegisterField.setDisable(true);
        nameLoginField.setDisable(true);
        userRegisterField.setDisable(true);
    }

    private void clearRegisterFields(){
        userRegisterField.clear();
        passRegisterField.clear();
        passConRegisterField.clear();
    }

    private void clearLoginFields(){
        nameLoginField.clear();
        passLoginField.clear();
    }
}
