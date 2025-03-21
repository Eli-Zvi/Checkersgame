package checkersgame.server;

import checkersgame.common.ClientCallBack;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The class manages the connected users
 */
public class UserManager {
    private final Map<ClientCallBack, Player> clients;

    public UserManager(){
        this.clients = new ConcurrentHashMap<>();
    }

    /**
     * Adds the callback and the player to the client map with the callback as the key and player as the value
     * @param callback a given callback
     * @param player client related to the callback
     */
    public void addClient(ClientCallBack callback, Player player){
        clients.put(callback, player);
    }

    /**
     * Checks if the callback exists in the client map
     * @param callBack a given callback
     * @return true if the callback exists in the client map false otherwise
     */
    public boolean existsByCallback(ClientCallBack callBack){
        return clients.containsKey(callBack);
    }

    /**
     * A player value related to the callback
     * @param callBack a given callback
     * @return the player value related to it
     */
    public Player findByCallback(ClientCallBack callBack){
        return clients.get(callBack);
    }

    /**
     * Returns all the callbacks in the client map
     * @return a set containing all the callbacks
     */
    public Set<ClientCallBack> getCallbacks(){
        return clients.keySet();
    }
}