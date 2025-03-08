package checkersgame.server;

import checkersgame.common.IClientCallBack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final Map<IClientCallBack, Player> clients;

    public UserManager(){
        this.clients = new ConcurrentHashMap<>();
    }

    public void addClient(IClientCallBack callback, Player player){
        clients.put(callback, player);
    }

    public boolean existsByCallback(IClientCallBack callBack){
        return clients.containsKey(callBack);
    }

    public Player findByCallback(IClientCallBack callBack){
        return clients.get(callBack);
    }

    public void removePlayer(IClientCallBack callBack){
        clients.remove(callBack);
    }

}
