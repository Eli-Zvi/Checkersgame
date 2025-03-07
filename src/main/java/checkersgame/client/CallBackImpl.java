package checkersgame.client;

import checkersgame.common.Utils;
import checkersgame.common.IClientCallBack;
import checkersgame.common.MoveInfo;
import javafx.application.Platform;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 *
 * @author Ilay Zvi
 */
public class CallBackImpl extends UnicastRemoteObject implements IClientCallBack {
    private GameController controller;

    protected CallBackImpl() throws RemoteException {
        super();
    }

    @Override //add boolean that returns true if successfully received and (false) technically will throw an exception
    public void notifyTurnAndUpdate(Utils.Turn turn, Map<LinkedList<MoveInfo>, int[]> possibleMoves) throws RemoteException {
        controller.setCurrentTurn(turn);
        controller.setPossibleMoves(possibleMoves);
    }

    @Override
    public void sendBoardUpdate(LinkedList<MoveInfo> move, boolean promotion) throws RemoteException {
        Platform.runLater(() -> controller.updateBoard(move, promotion));
    }

    @Override
    public void notifyGameOver(boolean win) throws RemoteException {
        Platform.runLater(() -> controller.gameOver(win));
    }

    @Override
    public void sendGameID(String gameID){
        Client.getInstance().setGameID(gameID);
        Platform.runLater(()-> {
            assert SceneManager.getInstance() != null;
            try {
                SceneManager.getInstance().switchScene("GamePage.fxml");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Checks if the client is currently connected
     */
    @Override
    public boolean isConnected() throws RemoteException {
        return true;
    }

    /**
     * Sets the value of the property controller.
     */
    public void setController(GameController controller) {
        this.controller = controller;
    }
}
