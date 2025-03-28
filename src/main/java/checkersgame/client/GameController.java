package checkersgame.client;

import checkersgame.common.*;
import static checkersgame.common.Utils.*;

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
/**
 * <b>GameController is a class responsible for managing the UI logic and communication between the client and the server
 * during a game of checkers.</b>
 *
 * <p>
 * The class manages aspects such as initializing the game board and ui, updating the board after a move was made,
 * and displaying the possible moves delivered by the server when applicable.
 * </p>
 */
public class GameController{
    private Turn currentTurn = null;
    private CheckersGame gameStub;
    private CallBackImpl callback;
    private StackPane currentPiece;
    private StackPane [][] tileBoard; //stores the board's panes
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

    /**
     * Initializes GameController
     */
    @FXML
    private void initialize() throws RemoteException, InterruptedException {
        createGameStub();
        initializeBoard();
        initializeUI();
    }

    /**
     * Initializes the game UI including player names, board, sets unnecessary labels to be invisible
     */
    private void initializeUI() throws RemoteException {
        gameStub.playerReady(callback);

        UIBoard.setVisible(true);

        gameOverLabel.setVisible(false);
        joinGameButton.setVisible(false);
        homeScreenButton.setVisible(false);
        statusLabel.setVisible(false);

        player1Label.setText("BLACK: " + gameStub.getPlayer1Name());
        player2Label.setText("RED: " + gameStub.getPlayer2Name());
    }

    /**
     * Initializes the StackPane which represents the board and initializes the board's UI and its tiles, pieces
     */
    private void initializeBoard(){
        tileBoard = new StackPane[NUM_OF_ROWS][NUM_OF_COLUMNS];
        Piece [][] board;

        try {
            board = gameStub.getBoard();
            pieceColor = gameStub.getPlayerColor(callback); //get player's color
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        Platform.runLater(()-> {
            //calculate the radius of each piece on the board based on the tile width and height
            this.tile_width = UIBoard.getWidth() / NUM_OF_COLUMNS;
            this.tile_height = UIBoard.getHeight() / NUM_OF_ROWS;
            double piece_radius = Math.min(tile_width, tile_height);

            for (int row = 0; row < NUM_OF_ROWS; row++) {
                for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                    StackPane tempPane = new StackPane();
                    Rectangle tile = new Rectangle(tile_width, tile_height);

                    PieceUI newUI = null;

                    if((row + col) % 2 == 1){ //only add pieces to the black tiles
                        tile.setFill(Color.web("#704731")); //set the fill of the black tiles
                        if(row < 3){ //if it's the top half of the board -> it's a black piece
                            newUI = new PieceUI(board[row][col], piece_radius, piece_radius);
                        }else if(row > 4){ // if it's at the bottom of the board -> it's a white piece
                            newUI = new PieceUI(board[row][col], piece_radius, piece_radius);
                        }
                        tempPane.getChildren().add(TILE_INDEX, tile); //add the tile to the pane

                        if(newUI != null) {
                            newUI.setOnMouseClicked(this::handlePieceClick); //set mouse on click to call handlePieceClick
                            newUI.setCursor(Cursor.HAND); //change the cursor when hovering over a piece
                            tempPane.getChildren().add(PIECE_INDEX, newUI); //add the piece to its StackPane
                            StackPane.setAlignment(newUI, Pos.CENTER); // set the alignment of the piece in the StackPane(same as default)
                        }

                        tempPane.setUserData(new int[]{row, col}); //embed the position of the pane inside the pane

                        //add the pane containing the tile and the piece to its respective place in the UI StackPane board array
                        tileBoard[row][col] = tempPane;
                        UIBoard.add(tempPane, col, row); //add the pane to its respective position on the board
                    }else{
                        tile.setFill(Color.web("#efc59d")); //white tile
                        UIBoard.add(tile, col, row); //add the tile to its respective position on the board
                        tileBoard[row][col] = null; //set the position on the board to be null since no piece will be there
                    }
                }
            }
        });
    }

    /**
     * Attempts to look up and connect to gameStub
     * @throws InterruptedException if failed to connect RETRY_ATTEMPTS times
     */
    private void createGameStub() throws InterruptedException {
        Client player = Client.getInstance();
        Registry registry = player.getRegistry();
        callback = player.getCallback();
        player.getCallback().setController(this);
        String game_name = player.getGameID();

        int retries = 0;

        while(gameStub == null) {
            try {
                gameStub = (CheckersGame) registry.lookup(game_name);
            }catch (RemoteException | NotBoundException e) {
                if (retries == RETRY_ATTEMPTS)
                    throw new RuntimeException();
                Thread.sleep(10000);
                retries++;
            }
        }

        System.out.println("Connected to game successfully");
    }

    /**
     * Returns the coordinates of the given tile
     * @param pane a tile on the board
     * @return it's coordinates
     */
    private int[] getTilePosition(StackPane pane) throws IndexOutOfBoundsException{
        return (int[]) pane.getUserData();
    }

    /**
     * Highlights a given tile and relates a move to it using the highlight
     * @param pane tile to highlight
     * @param move move to relate to the tile
     */
    private void addHighlight(StackPane pane, LinkedList<MoveInfo> move){
        Rectangle highlight = new Rectangle(tile_width, tile_height);
        highlight.setOnMouseClicked(e -> handleTileClick(move)); //sets on click function relating it to the highlight
        highlight.setFill(Color.YELLOW);
        highlight.setOpacity(0.5);
        highlight.setCursor(Cursor.HAND);
        pane.getChildren().add(highlight);
        highlights.put(pane, highlight);
    }

    /**
     * Highlights a tile
     * @param pane a tile to highlight
     */
    private void addPathHighlight(StackPane pane){
        Rectangle highlight = new Rectangle(tile_width, tile_height);
        highlight.setFill(Color.BLUE);
        highlight.setOpacity(0.5);
        pane.getChildren().add(highlight);
        highlights.put(pane, highlight);
    }

    /**
     * Highlights a piece tile
     * @param piecePane the piece to highlight
     */
    private void addPieceHighlight(StackPane piecePane){
        Rectangle highlight = new Rectangle(tile_width, tile_height);
        highlight.setFill(Color.GREEN);
        highlight.setOpacity(0.3);
        piecePane.getChildren().add(PIECE_INDEX, highlight);
        highlights.put(piecePane, highlight);
    }

    /**
     * Removes all the highlights from the board
     */
    private void removeHighlights(){
        for(StackPane stack : highlights.keySet()){
            stack.getChildren().remove(highlights.get(stack));
        }

        highlights.clear();
    }

    /**
     * Piece on click function, if it's currently the player's turn and the piece belongs to the player - displays the possible moves
     */
    private void handlePieceClick(MouseEvent e){
        PieceUI temp = (PieceUI) e.getSource();

        if(currentTurn != null && currentTurn.getColor().equals(temp.getPiece().getColor())) {
            removeHighlights();
            currentPiece = (StackPane) temp.getParent();
            attemptMove(temp);
        }
    }

    /**
     * On tile click, send the server a request to execute the move that is related to that tile
     * @param move the move to be made
     */
    private void handleTileClick(LinkedList<MoveInfo> move){ //send MoveInfo instead
        try {
            removeHighlights();

            if(move.size() == 1 && Math.abs(move.getFirst().currentCol() - move.getFirst().newCol()) == 1) {
                gameStub.attemptMove(move);
            }else{
                gameStub.attemptCapture(move);
            }
            currentTurn = null; //set the current turn to null and await server update
            possibleMoves = null; //set the possibleMoves to null and await server update
        }catch (RemoteException e) {
            System.out.println("RemoteException");
        } catch (InterruptedException | NotBoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the board when notified by the server
     * @param move the move that was executed
     * @param promotion if the piece that is moving has been promoted
     */
    public synchronized void updateBoard(LinkedList<MoveInfo> move, boolean promotion){
        removeHighlights();

        if(move.size() == 1 && Math.abs(move.getFirst().currentCol() - move.getFirst().newCol()) == 1){
            //if it's a non-capturing move
            MoveInfo temp = move.getFirst();
            move(temp);
            if (promotion)
                ((PieceUI)(tileBoard[temp.newRow()][temp.newCol()].getChildren().get(PIECE_INDEX))).promote();
        }else{
            //if it's a capture move
            for(MoveInfo m : move){
                move(m);
                removePiece((m.currentRow() + m.newRow()) / 2, (m.currentCol() + m.newCol()) / 2);
            }
            MoveInfo temp = move.getLast();
            if (promotion)
                ((PieceUI)(tileBoard[temp.newRow()][temp.newCol()].getChildren().get(PIECE_INDEX))).promote();
        }
    }

    /**
     * Displays the possible moves of a piece that has been clicked
     * @param piece the piece that was clicked
     */
    private void attemptMove(PieceUI piece){
        int[] position;

        try {
            position = getTilePosition(currentPiece);
        }catch (IndexOutOfBoundsException e){
            System.out.println("attemptMove " + e.getMessage());
            return;
        }

        int row = position[0], col = position[1];

        addPieceHighlight((StackPane)piece.getParent()); // add highlight on the piece that was clicked

        //loop onto all the possible moves that are related to the piece that was clicked
        for (Map.Entry<LinkedList<MoveInfo>, int[]> entry : possibleMoves.entrySet()){
            int[] tempPosition = entry.getValue();

            if(tempPosition[0] == row && tempPosition[1] == col){
                LinkedList<MoveInfo> moveInfo = entry.getKey();

                //start from the end to the beginning of the list of moves
                for (int i = moveInfo.size() - 1; i >= 0; i--) {
                    MoveInfo move = moveInfo.get(i);
                    int newRow = move.newRow(), newCol = move.newCol();


                    if (i == moveInfo.size() - 1 && !highlights.containsKey(tileBoard[newRow][newCol])) {
                        //add the final move in the list if it hasn't been highlighted yet
                        addHighlight(tileBoard[newRow][newCol], moveInfo);
                    } else {
                        //add the path highlights, skip tiles that have already been highlighted
                        if (!highlights.containsKey(tileBoard[newRow][newCol])) {
                            addPathHighlight(tileBoard[newRow][newCol]);
                        }
                    }
                }
            }
        }

    }

    /**
     * Moves the piece from move's currentRow,currentCol to newRow, newCol
     * @param move a MoveInfo record
     */
    private void move(MoveInfo move){
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
}