package checkersgame.client;

import checkersgame.common.Utils;
import checkersgame.common.ClientCallBack;
import checkersgame.common.MoveInfo;
import javafx.application.Platform;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Map;

/**
 * <p>This class represents a callback to the client</p>
 *
 * <p>This class implements the ClientCallBack interface</p>
 * <ul>
 *     <li>The callback is used by the server to contact the client</li>
 *     <li>The callback is used by the client to identify itself when contacting the server</li>
 * </ul>
 *
 * @author Ilay Zvi
 */
public class CallBackImpl extends UnicastRemoteObject implements ClientCallBack {
    private GameController controller;

    protected CallBackImpl() throws RemoteException {
        super();
    }

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
    @Override
    public void notifyTurnAndUpdate(Utils.Turn turn, Map<LinkedList<MoveInfo>, int[]> possibleMoves) throws RemoteException {
        controller.setCurrentTurn(turn);
        controller.setPossibleMoves(possibleMoves);
    }

    /**
     * Notifies the client that the board has been updated
     *
     * @param move a list of moves to update the board with
     * @param promotion a boolean indicating if the piece that was moved has been promoted
     */
    @Override
    public void sendBoardUpdate(LinkedList<MoveInfo> move, boolean promotion) throws RemoteException {
        Platform.runLater(() -> controller.updateBoard(move, promotion));
    }

    /**
     * Notifies the client that the game is over and if they won or not
     *
     * @param win boolean indicating if the client won the game, false -> loss true -> win
     */
    @Override
    public void notifyGameOver(boolean win) throws RemoteException {
        Platform.runLater(() -> controller.gameOver(win));
    }

    /**
     * Notifies the client about the registry id of the game they need to connect to
     *
     * @param gameID the ID of the game
     */
    @Override
    public void sendGameID(String gameID){
        Client.getInstance().setGameID(gameID); //sets the client's current game id

        Platform.runLater(()-> {
            assert SceneManager.getInstance() != null;
            try {
                SceneManager.getInstance().switchScene("GamePage.fxml"); //change to game page
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Checks if the client is currently connected
     *
     * @return true if the client is connected otherwise will throw a RemoteException
     */
    @Override
    public boolean sendHeartbeat() throws RemoteException {
        return true;
    }

    /**
     * Sets the value of the property controller.
     */
    public void setController(GameController controller) {
        this.controller = controller;
    }
}
