package checkersgame.server;

import checkersgame.common.IClientCallBack;

import javax.crypto.SecretKey;
import java.util.UUID;
//TODO ADD DOCUMENTATION

// stores user information if they are logged on or creates a temporary "guest" instance
public class Player {

    private String name;
    private UUID playerUUID;
    private final IClientCallBack callBack;
    private boolean isLoggedIn = false;
    private final SecretKey key;

    public Player(String name, UUID playerUUID, IClientCallBack callBack, SecretKey key) {
        this.name = name;
        this.playerUUID = playerUUID;
        this.callBack = callBack;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public IClientCallBack getCallBack() {
        return callBack;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public SecretKey getKey() {
        return key;
    }

    public void setName(String username){
        if(this.name == null)
            this.name = username;
    }

    public void setPlayerUUID(UUID playerUUID) {
        if(this.playerUUID == null)
            this.playerUUID = playerUUID;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.isLoggedIn = loggedIn;
    }
}
