package checkersgame.server;

import checkersgame.common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.UUID;
//TODO ADD DOCUMENTATION AND LOCKS AND STATE CONTROL MAYBE ON BOARD SIDE
public class CheckersGame extends UnicastRemoteObject implements ICheckersGame {
//add game state validation before a move, including turns
    private final Board board;
    private final IClientCallBack client1, client2;
    private final Player player1,player2;
    private boolean player1Ready = false;
    private boolean player2Ready = false;
    private final UUID ID;
    private Board.GameState gameState = Board.GameState.ONGOING;
    private int moveNumber = 0;

    public CheckersGame(Player player1, Player player2, UUID ID) throws RemoteException { // add client 2 later
        super();
        this.player1 = player1;
        this.player2 = player2;
        this.client1 = player1.getCallBack();
        this.client2 = player2.getCallBack();
        this.ID = ID;
        board = new Board();
    }

    @Override //lock
    public synchronized void attemptMove(LinkedList<MoveInfo> move) throws RemoteException{
        Utils.Turn curr = this.board.getCurrentTurn();
        boolean promotion = this.board.attemptMove(move); //add a check that the move exists in possible moves
        try {
            moveNumber = HomePage.addMove(ID,
                    curr.equals(Utils.Turn.BLACK) ? player1.getPlayerUUID() : player2.getPlayerUUID(),
                    moveNumber, move, promotion);
        }catch (SQLException e){
            e.printStackTrace();
        }
        notifyClients(move, promotion);
    }

    @Override //lock - needs to be fixed and also add sending callback/uuid to identify the person attempting to play
    public synchronized void attemptCapture(LinkedList<MoveInfo> move) throws RemoteException {
        Utils.Turn curr = this.board.getCurrentTurn();
        boolean promotion = this.board.attemptCapture(move); //add a check that the move exists in possible moves

        try {
            moveNumber = HomePage.addMove(ID,
                    curr.equals(Utils.Turn.BLACK) ? player1.getPlayerUUID() : player2.getPlayerUUID(),
                    moveNumber, move, promotion);
        }catch (SQLException e){
            e.printStackTrace();
        }

        gameState = this.board.checkWin();
        if (gameState != Board.GameState.ONGOING) {
            //System.out.println(gameState);
            notifyClients(gameState == Board.GameState.PLAYER1WIN);
        }
        else
            notifyClients(move, promotion);
    }

    @Override
    public synchronized Piece[][] getBoard() throws RemoteException {
        return this.board.getBoard();
    }

    @Override
    public synchronized void playerReady(IClientCallBack player) throws RemoteException {
        if(player.equals(client1)){
            player1Ready = true;
        }else if(player.equals(client2)){
            player2Ready = true;
        }

        if(player1Ready && player2Ready){
            notifyClients(null, false);
        }
    }

    @Override
    public Utils.PieceColor getPlayerColor(IClientCallBack player) throws RemoteException {
        if(player.equals(client1)){
            return Utils.PieceColor.BLACK;
        }else if(player.equals(client2)){
            return Utils.PieceColor.WHITE;
        }else{
            return null;
        }
    }

    @Override
    public void forfeit(IClientCallBack player) throws RemoteException {
        gameState = player.equals(client2) ? Board.GameState.PLAYER1WIN : Board.GameState.PLAYER2WIN;
        notifyClients(gameState == Board.GameState.PLAYER1WIN);
    }

    @Override
    public String getPlayer1Name() throws RemoteException {
        return player1.getName();
    }

    @Override
    public String getPlayer2Name() throws RemoteException {
        return player2.getName();
    }

    public synchronized void notifyClients(boolean player1Win) throws RemoteException {
        client1.notifyGameOver(player1Win);
        client2.notifyGameOver(!player1Win);

        try {
            if (player1Win) {
                HomePage.cleanUp(this.ID, player1.getPlayerUUID(), player2.getPlayerUUID());
            } else {
                HomePage.cleanUp(this.ID, player2.getPlayerUUID(), player1.getPlayerUUID());
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public synchronized void notifyClients(LinkedList<MoveInfo> move, boolean promotion) throws RemoteException {
        if(player1Ready && player2Ready) {
            board.checkPossibleCaptures();
            if (board.getPossibleMoves().isEmpty()) {
                board.checkPossibleMoves();
            }
            /*
            for (Map.Entry<LinkedList<MoveInfo>, int[]> m : board.getPossibleMoves().entrySet()) {
                for (MoveInfo moveInfo : m.getKey()) {
                    System.out.println("Piece" + Arrays.toString(m.getValue()) + "Move Info: " + moveInfo);
                }
            } */

            if (move != null) {
                client1.sendBoardUpdate(move, promotion);
                client2.sendBoardUpdate(move, promotion);
            }

            Utils.Turn turn = this.board.getCurrentTurn();
            //System.out.println("notify:" + this.board.getCurrentTurn());
            if (turn == Utils.Turn.WHITE) {
                client2.notifyTurnAndUpdate(turn, this.board.getPossibleMoves());
                client1.notifyTurnAndUpdate(null, null);
            } else {
                client1.notifyTurnAndUpdate(turn, this.board.getPossibleMoves());
                client2.notifyTurnAndUpdate(null, null);
            }
        }
    }

    public UUID getID() {
        return ID;
    }
}