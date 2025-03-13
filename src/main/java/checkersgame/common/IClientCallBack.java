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
public interface IClientCallBack extends Remote {

    void notifyTurnAndUpdate(Utils.Turn turn, Map<LinkedList<MoveInfo>, int[]> possibleMoves) throws RemoteException;

    void sendBoardUpdate(LinkedList<MoveInfo> move, boolean promotion) throws RemoteException;

    void notifyGameOver(boolean win) throws RemoteException;

    void sendGameID(String gameID) throws RemoteException;

    boolean sendHeartbeat() throws RemoteException;
}
