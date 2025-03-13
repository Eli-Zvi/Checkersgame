package checkersgame.server;

import checkersgame.common.IClientCallBack;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * <p>The Player class stores information relating to a client</p>
 */
public class Player {

    private String username;
    private UUID playerUUID, gameUUID;
    private final IClientCallBack callBack;
    private boolean isLoggedIn = false;
    private final SecretKey key;

    /**
     * Initializes an instance of the Player class with isLoggedIn set to false, username and playerUUID set to null
     * @param callBack the callback that belongs to the player
     * @param key the key that belongs to the player
     */
    public Player(IClientCallBack callBack, SecretKey key) {
        this.username = null;
        this.playerUUID = null;
        this.callBack = callBack;
        this.key = key;
    }

    /**
     * Gets the value of the property name.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the value of the property playerUUID.
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * Gets the value of the property callBack.
     */
    public IClientCallBack getCallBack() {
        return callBack;
    }

    /**
     * Gets the value of the property isLoggedIn.
     */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    /**
     * Gets the value of the property key.
     */
    public SecretKey getKey() {
        return key;
    }

    /**
     * Gets the value of the property gameUUID.
     */
    public UUID getGameUUID(){
        return gameUUID;
    }

    /**
     * Registers the player, sets isLoggedIn to true, sets username to the given username, sets the uuid to the given uuid
     * @param username the username the client has registered with
     * @param playerUUID uuid related to the player
     */
    public void registerPlayer(String username, UUID playerUUID){
        if (this.username == null && this.playerUUID == null){
            this.username = username;
            this.playerUUID = playerUUID;
            this.isLoggedIn = true;
        }
    }

    /**
     * Sets the value of the property gameUUID.
     */
    public void setGameUUID(UUID gameUUID){
        this.gameUUID = gameUUID;
    }
}
