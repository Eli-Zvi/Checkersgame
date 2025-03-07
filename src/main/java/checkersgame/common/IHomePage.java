package checkersgame.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

/**
 * <b>Remote interface for establishing initial connection with client, handling user registration and authentication,<br>
 * match creation/joining, and the retrieval of game and user data.</b>
 *
 * @author Ilay Zvi
 */
public interface IHomePage extends Remote {

    UUID register(IClientCallBack client, String username, Password password) throws RemoteException, SQLException;

    UUID login(IClientCallBack client, String username, Password password) throws RemoteException, SQLException, GeneralSecurityException;

    void registerCallBack(IClientCallBack client, String encryptedAESKey) throws RemoteException;

    void joinGame(IClientCallBack client) throws RemoteException, SQLException;

    String getServerPublicKey() throws RemoteException;

    String getWinRate(IClientCallBack client) throws RemoteException;

    ArrayList<FinishedGame> getReplayableGames() throws RemoteException;

    ArrayList<LinkedList<MoveInfo>> getReplayMoves(UUID gameID) throws RemoteException;
}
