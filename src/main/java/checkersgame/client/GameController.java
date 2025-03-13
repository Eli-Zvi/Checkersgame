package checkersgame.client;

import checkersgame.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.*;

import static checkersgame.common.Utils.*;

//TODO ADD DOCUMENTATION AND CHECK FOR LOCKS

public class GameController{
    private Turn currentTurn = null;
    private ICheckersGame gameStub;
    private CallBackImpl callback;
    private StackPane currentPiece;
    private StackPane [][] tileBoard;
    private double tile_width, tile_height;
    private final HashMap<StackPane, Rectangle> highlights = new HashMap<>();
    private Map<LinkedList<MoveInfo>, int[]> possibleMoves;
    private PieceColor pieceColor;

    @FXML
    private GridPane UIBoard;

    @FXML
    private Label player1Label, player2Label, playerTurnLabel, statusLabel, gameOverLabel;

    @FXML
    private Button joinGameButton, homeScreenButton, forfeitButton;

    @FXML
    private void initialize() throws NotBoundException, RemoteException, InterruptedException {
        initializeBoard();
        gameStub.playerReady(callback);
        gameOverLabel.setVisible(false);
        UIBoard.setVisible(true);
        joinGameButton.setVisible(false);
        homeScreenButton.setVisible(false);
        statusLabel.setVisible(false);
        player1Label.setText("BLACK: " + gameStub.getPlayer1Name());
        player2Label.setText("RED: " + gameStub.getPlayer2Name());
    }

    private void initializeBoard() throws NotBoundException, InterruptedException {
        Client player = Client.getInstance();
        Registry registry = player.getRegistry();
        callback = player.getCallback();
        player.getCallback().setController(this);
        String game_name = player.getGameID();

        while(gameStub == null) { //TODO IMPLEMENT RETRY
            try {
                gameStub = (ICheckersGame) registry.lookup(game_name);
            }catch (RemoteException e) {
                wait(1000);
            }
        }
        try {
            System.out.println(Arrays.toString(registry.list())); //TODO MAKE THIS ILLEGAL
        }catch (RemoteException e){

        }
        System.out.println("Connected to game successfully");
        tileBoard = new StackPane[NUM_OF_ROWS][NUM_OF_COLUMNS];
        Piece [][] board;

        try {
            board = gameStub.getBoard();
            pieceColor = gameStub.getPlayerColor(callback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        Platform.runLater(()-> {
            this.tile_width = UIBoard.getWidth() / NUM_OF_COLUMNS;
            this.tile_height = UIBoard.getHeight() / NUM_OF_ROWS;
            double piece_radius = Math.min(tile_width, tile_height);

            for (int row = 0; row < NUM_OF_ROWS; row++) {
                for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                    StackPane tempPane = new StackPane();
                    Rectangle tile = new Rectangle(tile_width, tile_height);

                    PieceUI newUI = null;

                    if((row + col) % 2 == 1){
                        tile.setFill(Color.web("#704731"));
                        if(row < 3){
                            newUI = new PieceUI(board[row][col], piece_radius, piece_radius);
                        }else if(row > 4){
                            newUI = new PieceUI(board[row][col], piece_radius, piece_radius);
                        }
                        tempPane.getChildren().add(TILE_INDEX, tile);

                        if(newUI != null) {
                            newUI.setOnMouseClicked(this::handlePieceClick);
                            newUI.setCursor(Cursor.HAND);
                            tempPane.getChildren().add(PIECE_INDEX, newUI);
                            StackPane.setAlignment(newUI, Pos.CENTER);
                        }
                        tempPane.setUserData(new int[]{row, col});
                        tileBoard[row][col] = tempPane;
                        UIBoard.add(tempPane, col, row);
                    }else{
                        tile.setFill(Color.web("#efc59d"));
                        UIBoard.add(tile, col, row);
                        tileBoard[row][col] = null;
                    }
                }
            }
        });
    }

    private int[] getTilePosition(StackPane pane) throws IndexOutOfBoundsException{
        return (int[]) pane.getUserData();
    }

    private void addHighlight(StackPane pane, LinkedList<MoveInfo> move){
        Rectangle highlight = new Rectangle(tile_width, tile_height);
        highlight.setOnMouseClicked(e -> handleTileClick(move));
        highlight.setFill(Color.YELLOW);
        highlight.setOpacity(0.5);
        highlight.setCursor(Cursor.HAND);
        pane.getChildren().add(highlight);
        highlights.put(pane, highlight);
    }

    private void addPathHighlight(StackPane pane){
        Rectangle highlight = new Rectangle(tile_width, tile_height);
        highlight.setFill(Color.BLUE);
        highlight.setOpacity(0.5);
        pane.getChildren().add(highlight);
        highlights.put(pane, highlight);
    }

    private void addPieceHighlight(StackPane piecePane){
        Rectangle highlight = new Rectangle(tile_width, tile_height);
        highlight.setFill(Color.GREEN);
        highlight.setOpacity(0.3);
        piecePane.getChildren().add(PIECE_INDEX, highlight);
        highlights.put(piecePane, highlight);
    }

    private void removeHighlights(){
        for(StackPane stack : highlights.keySet()){
            stack.getChildren().remove(highlights.get(stack));
        }

        highlights.clear();
    }

    private void handlePieceClick(MouseEvent e){
        PieceUI temp = (PieceUI) e.getSource();

        if(currentTurn != null && currentTurn.getColor().equals(temp.getPiece().getColor())) {
            removeHighlights();
            currentPiece = (StackPane) temp.getParent();
            attemptMove(temp);
        }
    }

    private void handleTileClick(LinkedList<MoveInfo> move){ //send MoveInfo instead
        try {
            removeHighlights();
            //int[] pos = possibleMoves.get(move);

            if(move.size() == 1 && Math.abs(move.getFirst().currentCol() - move.getFirst().newCol()) == 1) {
                gameStub.attemptMove(move);
            }else{
                gameStub.attemptCapture(move);
            }
            currentTurn = null;
            possibleMoves = null;
        }catch (RemoteException e) {
            System.out.println("RemoteException");
        } catch (InterruptedException | NotBoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateBoard(LinkedList<MoveInfo> move, boolean promotion){
        removeHighlights();
        if(move.size() == 1 && Math.abs(move.getFirst().currentCol() - move.getFirst().newCol()) == 1){
            MoveInfo temp = move.getFirst();
            swap(temp);
            if (promotion)
                ((PieceUI)(tileBoard[temp.newRow()][temp.newCol()].getChildren().get(PIECE_INDEX))).promote();
        }else{
            for(MoveInfo m : move){
                swap(m);
                removePiece((m.currentRow() + m.newRow()) / 2, (m.currentCol() + m.newCol()) / 2);
            }
            MoveInfo temp = move.getLast();
            if (promotion)
                ((PieceUI)(tileBoard[temp.newRow()][temp.newCol()].getChildren().get(PIECE_INDEX))).promote();
        }
    }

    private void printBoard(){
        for (StackPane []pane : tileBoard){
            for(StackPane p : pane){
                if(p != null && p.getChildren().size() == 2){
                    PieceUI piece = (PieceUI) p.getChildren().get(PIECE_INDEX);
                    System.out.print(piece.getPiece().getColor() + " ");
                }else if(p == null){
                    System.out.print("x ");
                }else{
                    System.out.print("tile ");
                }
            }
            System.out.println();
        }
        System.out.println("--------------------------------");
    }

    private void attemptMove(PieceUI temp){
        int[] position;

        try {
            position = getTilePosition(currentPiece);
        }catch (IndexOutOfBoundsException e){
            System.out.println("attemptMove " + e.getMessage());
            return;
        }

        int row = position[0], col = position[1];

        addPieceHighlight((StackPane)temp.getParent());
        for (Map.Entry<LinkedList<MoveInfo>, int[]> entry : possibleMoves.entrySet()){
            int[] tempPosition = entry.getValue();
            if(tempPosition[0] == row && tempPosition[1] == col){
                LinkedList<MoveInfo> moveInfo = entry.getKey();

                for (int i = moveInfo.size() - 1; i >= 0; i--) {
                    MoveInfo move = moveInfo.get(i);
                    int newRow = move.newRow(), newCol = move.newCol();

                    if (i == moveInfo.size() - 1 && !highlights.containsKey(tileBoard[newRow][newCol])) {
                        addHighlight(tileBoard[newRow][newCol], moveInfo);
                    } else {
                        if (!highlights.containsKey(tileBoard[newRow][newCol])) {
                            addPathHighlight(tileBoard[newRow][newCol]);
                        }
                    }
                }
            }
        }

    }

    private void swap(MoveInfo move){
        try {
            PieceUI temp = (PieceUI) tileBoard[move.currentRow()][move.currentCol()].getChildren().remove(PIECE_INDEX);
            tileBoard[move.newRow()][move.newCol()].getChildren().add(temp);
        }catch (ClassCastException e){
            System.out.println(move);
        }
    }

    /**
     * Removes a piece from the board
     * @param row the piece's row
     * @param col the piece's column
     */
    private void removePiece(int row, int col){
        tileBoard[row][col].getChildren().remove(PIECE_INDEX);
    }

    /**
     * Sets the value of the currentTurn and sets the turn label accordingly
     * @param turn the next player turn in the game
     */
    public void setCurrentTurn(Utils.Turn turn){
        currentTurn = turn;
        Platform.runLater(() -> {
            if (currentTurn == null) {
                playerTurnLabel.setText(pieceColor.equals(PieceColor.BLACK) ? "TURN: RED(OPPONENT)" : "TURN: BLACK(OPPONENT)");
            } else {
                playerTurnLabel.setText(pieceColor.equals(PieceColor.BLACK) ? "TURN: BLACK(YOUR TURN)" : "TURN: RED(YOUR TURN)");
            }
        });
    }

    /**
     * Sets the value of possibleMoves property, used for turn update
     * @param possibleMoves new possible Moves either null or a map
     */
    public void setPossibleMoves(Map<LinkedList<MoveInfo>, int[]> possibleMoves){
        this.possibleMoves = possibleMoves;
    }

    /**
     * <p>Game over procedure</p>
     * <ol>
     *     <li>Sets the game stub to null</li>
     *     <li>Sets the callback's controller to null</li>
     *     <li>Displays You Win/Lose based on @param win</li>
     *     <li>Sets gameOverLabel, homeScreenButton, and joinGameButton to be visible</li>
     *     <li>Sets the UIBoard, playerTurnLabel, forfeitButton to not be visible</li>
     * </ol>
     * @param win boolean indicating if the player has won or not
     */
    public void gameOver(boolean win){
        gameStub = null;
        callback.setController(null);

        if(win){
            gameOverLabel.setText("YOU WIN!");
            gameOverLabel.setStyle("-fx-text-fill: green;");
        }else{
            gameOverLabel.setText("YOU LOSE!");
            gameOverLabel.setStyle("-fx-text-fill: red;");
        }

        joinGameButton.setCursor(Cursor.HAND);
        homeScreenButton.setCursor(Cursor.HAND);

        gameOverLabel.setVisible(true);
        homeScreenButton.setVisible(true);
        joinGameButton.setVisible(true);

        UIBoard.setVisible(false);
        playerTurnLabel.setVisible(false);
        forfeitButton.setVisible(false);
    }

    /**
     * Home button on action function, switches to home screen
     */
    @FXML
    void homeScreenOnAction(ActionEvent event) throws IOException {
        assert SceneManager.getInstance() != null;
        SceneManager.getInstance().switchScene("HomePage.fxml");
    }

    /**
     * Join button on action function, disables all buttons and displays In Queue message, requests server to join game
     */
    @FXML
    void joinButtonOnAction(ActionEvent event) throws RemoteException {
        try{
            joinGameButton.setDisable(true);
            homeScreenButton.setDisable(true);
            statusLabel.setVisible(true);
            statusLabel.setText("In Queue");
            Client.getInstance().getHomeStub().joinGame(Client.getInstance().getCallback());
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * Forfeit on action function, sends a request to server to forfeit the game
     */
    @FXML
    void forfeitOnAction(ActionEvent event) throws NotBoundException, RemoteException {
        gameStub.forfeit(callback);
    }
}