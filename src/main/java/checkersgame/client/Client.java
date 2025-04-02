package checkersgame.client;

import checkersgame.common.HomePage;
import checkersgame.common.KeyUtils;
import static checkersgame.common.Utils.RETRY_ATTEMPTS;

import javax.crypto.SecretKey;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.GeneralSecurityException;
import java.util.UUID;

/**
 * <b>This Singleton class represents the client's information</b><br>
 * It establishes initial connection with the server and stores client information
 *
 * <p>The Client class is responsible for establishing initial connection with the RMI registry,<br>
 * retrieving the remote homePage stub, generating a new aes key, and registering the aes
 * key and callback with the server.</p>
 *
 * @author Ilay Zvi
 */
public class Client{
    private HomePage homeStub;
    private final CallBackImpl callback;
    private Registry registry;
    private static Client client;
    private String gameID, username;
    private boolean isLoggedIn = false;
    private UUID uuid;
    private final SecretKey key;

    /**
     * Private constructor for the singleton Client class<br>
     *
     * Creates a new instance of Client
     *
     * @throws RemoteException - if there is an issue connecting to the RMI registry.
     * @throws GeneralSecurityException - if failure occurs during key creation
     */
    private Client() throws RemoteException, GeneralSecurityException, InterruptedException {
        initializeRegistry();
        initializeHomeStub();

        this.key = KeyUtils.generateAESKey(); //create aes key
        callback = new CallBackImpl(); //create callback

        //register with server
        homeStub.registerCallBack(callback, KeyUtils.rsaEncrypt(key,KeyUtils.base64ToPublicKey(homeStub.getServerPublicKey())));
    }

    /**
     * Initializes client registry
     */
    private void initializeRegistry() throws InterruptedException {
        int retries = 0;

        while(registry == null) { //establish connection with server
            try {
                registry = LocateRegistry.getRegistry("localhost", 1099);
            }catch (RemoteException ignored){
                if (retries == RETRY_ATTEMPTS)
                    throw new RuntimeException();

                Thread.sleep(5000);
                retries++;
            }
        }
    }

    /**
     * Initializes client home stub
     */
    private void initializeHomeStub() throws InterruptedException{
        int retries = 0;

        while(homeStub == null) { //establish connection with landing page
            try{
                homeStub = (HomePage) registry.lookup("home");
            }catch (RemoteException | NotBoundException ignored){
                if (retries == RETRY_ATTEMPTS)
                    throw new RuntimeException();

                Thread.sleep(5000);
                retries++;
            }
        }
    }

    /**
     * This method initializes singleton instance of the class Client
     */
    public synchronized static void initializeInstance(){
        if(client == null){
            try {
                client = new Client();
            }catch (RemoteException | GeneralSecurityException | InterruptedException ignored){}
        }
    }

    /**
     * Gets the value of the property instance.
     */
    public static Client getInstance(){
        return client;
    }

    /**
     * Gets the value of the property callBack.
     */
    public CallBackImpl getCallback() {
        return callback;
    }

    /**
     * Gets the value of the property homeStub.
     */
    public HomePage getHomeStub() {
        return homeStub;
    }

    /**
     * Gets the value of the property registry.
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * Gets the value of the property gameID.
     */
    public String getGameID() {
        return gameID;
    }

    /**
     * Gets the value of the property isLoggedIn.
     * <p>
     * <b>Default value:</b><br>
     * false
     * </p>
     */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    /**
     * Gets the value of the property key.
     */
    public SecretKey getKey(){
        return key;
    }

    /**
     * Gets the value of the property UUID.
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Gets the value of the property username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the value of the property gameID.
     */
    public void setGameID(String gameID) {
        this.gameID = gameID;
    }

    /**
     * Sets the value of the property isLoggedIn.
     */
    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    /**
     * Sets the value of the property uuid.
     *
     * <p>If the client's UUID has not been set yet, sets it to the given value</p>
     */
    public void setUUID(UUID uuid) {
        if(this.uuid == null)
            this.uuid = uuid;
    }

    /**
     * Sets the value of the property username.
     *
     * <p>If the client's username has not been set yet, sets it to the given value</p>
     */
    public void setUsername(String username) {
        if(this.username == null)
            this.username = username;
    }
}
