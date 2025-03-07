package checkersgame.common;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

/**
 * <b>Remote interface for client move and data requests</b><br>
 *
 * <p>The client will use the following methods to notify the server that it has finished initializing the board,
 * to fetch info about the board, and to attempt steps/captures</p>
 *
 * @author Ilay Zvi
 */
public interface ICheckersGame extends Remote {
    //use lock
    void attemptMove(LinkedList<MoveInfo> move) throws RemoteException, InterruptedException;
    //use lock
    void attemptCapture(LinkedList<MoveInfo> move) throws RemoteException, InterruptedException, NotBoundException;
    //use lock
    Piece[][] getBoard() throws RemoteException;

    void playerReady(IClientCallBack player) throws RemoteException;

    Utils.PieceColor getPlayerColor(IClientCallBack player) throws RemoteException;

    void forfeit(IClientCallBack player) throws RemoteException, NotBoundException;

    String getPlayer1Name() throws RemoteException;

    String getPlayer2Name() throws RemoteException;
}
