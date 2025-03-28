package checkersgame.server;

import checkersgame.common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.UUID;
/**
 * <b>CheckersGameImpl is a class responsible for managing the game logic and communication between the server and the clients
 * during a game of checkers.</b>
 *
 * <p>
 * The class handles player information retrieval, player interactions such as performing moves and captures,
 * and notifies clients during game status changes.
 * </p>
 */
public class CheckersGameImpl extends UnicastRemoteObject implements CheckersGame {

    private final Board board; // the instance of the game's board
    private final ClientCallBack client1, client2; //each player's callbacks
    private final Player player1,player2; //information tied to each player
    //booleans representing if the players are ready
    private boolean player1Ready = false;
    private boolean player2Ready = false;

    private final UUID ID; //the game's UUID
    private Board.GameState gameState = Board.GameState.ONGOING; //the state of the game ongoing/player1win/player2win
    private int moveNumber = 0; //represents the number of moves made - 0 indexed

    /**
     * The main and only constructor of the CheckersGameImpl class
     * @param player1 an instance of a Player object representing player1
     * @param player2 an instance of a Player object representing player2
     * @param ID the game ID
     */
    protected CheckersGameImpl(Player player1, Player player2, UUID ID) throws RemoteException {
        super();
        this.player1 = player1;
        this.player2 = player2;
        this.client1 = player1.getCallBack();
        this.client2 = player2.getCallBack();
        this.ID = ID;
        board = new Board();
    }

    /**
     * Receives a regular move to perform from a player
     * @param move the move to perform
     * @throws RemoteException if the move doesn't exist in the list of possible moves or a connection error occurs
     */
    @Override
    public synchronized void attemptMove(LinkedList<MoveInfo> move) throws RemoteException{
        Utils.Turn curr = this.board.getCurrentTurn();
        boolean promotion = this.board.attemptMove(move);
        try {
            moveNumber = HomePageImpl.addMove(ID,
                    curr.equals(Utils.Turn.BLACK) ? player1.getPlayerUUID() : player2.getPlayerUUID(),
                    moveNumber, move, promotion);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        notifyClients(move, promotion);
    }

    /**
     * Receives a capture move to perform from a player
     * @param move the move to perform
     * @throws RemoteException if the move doesn't exist in the list of possible moves or a connection error occurs
     */
    @Override
    public synchronized void attemptCapture(LinkedList<MoveInfo> move) throws RemoteException {
        Utils.Turn curr = this.board.getCurrentTurn();
        boolean promotion = this.board.attemptCapture(move);

        try {
            moveNumber = HomePageImpl.addMove(ID,
                    curr.equals(Utils.Turn.BLACK) ? player1.getPlayerUUID() : player2.getPlayerUUID(),
                    moveNumber, move, promotion);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        gameState = this.board.checkWin();
        if (gameState != Board.GameState.ONGOING) {
            notifyClients(gameState == Board.GameState.PLAYER1WIN);
        } else
            notifyClients(move, promotion);
    }

    /**
     * Gets the value of the board property
     * @return a 2D Piece matrix representing the checkers board
     */
    @Override
    public Piece[][] getBoard() throws RemoteException {
        return this.board.getBoard();
    }

    /**
     * RMI method to signal to the server that the player is ready to play the game - after initialization of the board is done on the player side
     * @param player the player signaling to the server that they are ready
     */
    @Override
    public synchronized void playerReady(ClientCallBack player) throws RemoteException {
        if(player.equals(client1)){
            player1Ready = true;
        }else if(player.equals(client2)){
            player2Ready = true;
        }

        if(player1Ready && player2Ready){
            notifyClients(null, false);
        }
    }

    /**
     * Gets the color of the pieces of the player represented by the callback
     * @param player the callback representing the player
     * @return the color of their pieces
     */
    @Override
    public Utils.PieceColor getPlayerColor(ClientCallBack player) throws RemoteException {
        if(player.equals(client1)){
            return Utils.PieceColor.BLACK;
        }else if(player.equals(client2)){
            return Utils.PieceColor.WHITE;
        }else{
            return null;
        }
    }

    /**
     * RMI method called by the player to forfeit the game
     * @param player the callback of the player requesting to forfeit
     */
    @Override
    public synchronized void forfeit(ClientCallBack player) throws RemoteException {
        forfeitGame(player);
    }

    /**
     * Forfeits the game under the name of the given callback
     * @param player a callback representing the player(either player1 or player2)
     */
    public synchronized void forfeitGame(ClientCallBack player){
        if(gameState.equals(Board.GameState.ONGOING)) {
            gameState = player.equals(client2) ? Board.GameState.PLAYER1WIN : Board.GameState.PLAYER2WIN;
            notifyClients(gameState == Board.GameState.PLAYER1WIN);
        }
    }

    /**
     * Gets the username of player1
     */
    @Override
    public String getPlayer1Name() throws RemoteException {
        return player1.getUsername();
    }

    /**
     * Gets the username of player2
     */
    @Override
    public String getPlayer2Name() throws RemoteException {
        return player2.getUsername();
    }

    /**
     * Notify players that the game has ended
     * @param player1Win boolean indicating if player1 has won or not
     */
    private synchronized void notifyClients(boolean player1Win){
        try {
            client1.notifyGameOver(player1Win);
        }catch (RemoteException ignored){}

        try {
            client2.notifyGameOver(!player1Win);
        }catch (RemoteException ignored){}

        try {
            if (player1Win) {
                HomePageImpl.cleanUp(this.ID, player1.getPlayerUUID(), player2.getPlayerUUID());
            } else {
                HomePageImpl.cleanUp(this.ID, player2.getPlayerUUID(), player1.getPlayerUUID());
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

        player1.setGameUUID(null);
        player2.setGameUUID(null);
    }

    /**
     * <p>After a move was performed successfully, this method notifies clients about the next possible moves,
     * the next turn and board updates</p>
     * @param move the move to notify the players about -> if the move is null no notification will happen
     * @param promotion if the piece that the moves were performed with has been promoted
     */
    private synchronized void notifyClients(LinkedList<MoveInfo> move, boolean promotion) throws RemoteException {
        if(player1Ready && player2Ready) {

            board.updatePossibleMoves();

            if (move != null) { //if the move parameter is not null notify the clients about the move that was performed
                client1.sendBoardUpdate(move, promotion);
                client2.sendBoardUpdate(move, promotion);
            }

            Utils.Turn turn = this.board.getCurrentTurn(); //get the current player turn

            //notify the players according to the current player turn
            if (turn == Utils.Turn.WHITE) {
                client2.notifyTurnAndUpdate(turn, this.board.getPossibleMoves());
                client1.notifyTurnAndUpdate(null, null);
            } else {
                client1.notifyTurnAndUpdate(turn, this.board.getPossibleMoves());
                client2.notifyTurnAndUpdate(null, null);
            }
        }
    }
}