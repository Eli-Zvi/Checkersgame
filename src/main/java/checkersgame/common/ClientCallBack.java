package checkersgame.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Map;

/**
 * <b>Remote interface for notifying the client about game updates.</b><br>
 *
 * <p>Notifies the client about the gameID after a request to join game, turn and board state updates, and the possible moves</p>
 *
 * @author Ilay Zvi
 */
public interface ClientCallBack extends Remote {

    /**
     * Notifies the client about the next turn and their possible moves during the turn
     * <ol>
     *   <li>if it's the client's turn -> possibleMoves will be the list of moves that the client can make</li>
     *   <li>it's not the client's turn -> possibleMoves will be null as it is not their turn</li>
     * </ol>
     *
     * @param turn the next turn in the game
     * @param possibleMoves - the client's possible moves in the next turn
     */
    void notifyTurnAndUpdate(Utils.Turn turn, Map<LinkedList<MoveInfo>, int[]> possibleMoves) throws RemoteException;

    /**
     * Notifies the client that the board has been updated
     *
     * @param move a list of moves to update the board with
     * @param promotion a boolean indicating if the piece that was moved has been promoted
     */
    void sendBoardUpdate(LinkedList<MoveInfo> move, boolean promotion) throws RemoteException;

    /**
     * Notifies the client that the game is over and if they won or not
     *
     * @param win boolean indicating if the client won the game, false -> loss true -> win
     */
    void notifyGameOver(boolean win) throws RemoteException;

    /**
     * Notifies the client about the registry id of the game they need to connect to
     *
     * @param gameID the ID of the game
     */
    void sendGameID(String gameID) throws RemoteException;

    /**
     * Checks if the client is currently connected
     *
     * @return true if the client is connected otherwise will throw a RemoteException
     */
    boolean sendHeartbeat() throws RemoteException;
}
