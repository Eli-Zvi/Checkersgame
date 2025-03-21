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
public interface HomePage extends Remote {

    /**
     * Attempts to register the user in and returns his new UUID if successful
     * @param client client attempting to register
     * @param username the username the client is attempting to register with
     * @param password the password the client is attempting to register with
     * @return the user's UUID if login is successful, null otherwise
     */
    UUID register(ClientCallBack client, String username, Password password) throws RemoteException, SQLException;

    /**
     * Attempts to log the user in and returns his UUID if successful
     * @param client client attempting to login
     * @param username the username the client is attempting to log in with
     * @param password the password the client is attempting to log in with
     * @return the user's UUID if login is successful, null otherwise
     */
    UUID login(ClientCallBack client, String username, Password password) throws RemoteException, SQLException, GeneralSecurityException;

    /**
     * <p>Initial key exchange between the client and the server</p>
     * @param client the client the server is exchanging the keys with
     * @param encryptedAESKey the client's encrypted AES key
     */
    void registerCallBack(ClientCallBack client, String encryptedAESKey) throws RemoteException;

    /**
     * Adds the client to the game queue if they are not in a game, notifies gameInitializer
     * @param client the client requesting to join the game queue
     */
    void joinGame(ClientCallBack client) throws RemoteException, SQLException;

    /**
     * Returns the server's public key
     * @return the server's public key in string form
     */
    String getServerPublicKey() throws RemoteException;

    /**
     * Fetches and returns the given player's WINS and LOSSES in the form of a string if they exist
     * @param client the client requesting their win rate loss
     * @return a string containing the wins and losses of the player in the format of WINS-LOSSES
     */
    String getWinRate(ClientCallBack client) throws RemoteException;

    /**
     * Fetches a list of FinishedGames from the database and returns it
     * @return a list containing FinishedGames or an empty list
     */
    ArrayList<FinishedGame> getReplayableGames() throws RemoteException;

    /**
     * Fetches a list of moves from the database and returns it
     * @param gameID the UUID of the game to fetch the moves of from the database
     * @return a list containing the moves obtained from the database, null if an error occurs or the game does not have moves
     */
    ArrayList<LinkedList<MoveInfo>> getReplayMoves(UUID gameID) throws RemoteException;
}
