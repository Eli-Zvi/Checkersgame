package checkersgame.client;

import static checkersgame.common.Utils.*;
import checkersgame.common.FinishedGame;
import checkersgame.common.MoveInfo;
import checkersgame.common.Piece;
import checkersgame.common.Utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

/**
 * <b>This class represents the replay scene's controller</b>
 *
 * <p>This class is responsible for the following actions:</p>
 * <ul>
 *     <li>Displaying the list of replayable games</li>
 *     <li>Requesting the game info(such as moves, player, winner, etc.)</li>
 *     <li>Displaying the replay board, and cycling through the moves according to the user's request</li>
 * </ul>
 *
 * @author Ilay Zvi
 */
public class ReplayController {

    private StackPane [][] tileBoard;
    private ArrayList<LinkedList<MoveInfo>> possibleMoves;
    private Turn currentTurn = Turn.BLACK;

    @FXML
    private Label gameOverLabel,player1Label,player2Label,playerTurnLabel;

    @FXML
    private GridPane UIBoard;

    @FXML
    ImageView arrow;

    @FXML
    private ListView<FinishedGame> gameList;

    /**
     * Initializes the list of games that can be replayed
     *
     * <p>Requests the list of replayable games from the server and displays them</p>
     */
    @FXML
    private void initialize() throws RemoteException {
        ArrayList<FinishedGame> gameStrings = Client.getInstance().getHomeStub().getReplayableGames();

        if(gameStrings != null && !gameStrings.isEmpty()) {
            ObservableList<FinishedGame> list = FXCollections.observableArrayList(gameStrings);
            gameList.setItems(list);

            gameList.setCellFactory(new Callback<>() {
                @Override
                public ListCell<FinishedGame> call(ListView<FinishedGame> param) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(FinishedGame item, boolean empty) {
                            super.updateItem(item, empty);

                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                // Create a new button for each row
                                HBox hbox = new HBox(10.0);

                                Button button = getButton(item);
                                Label label = new Label("GAME " + (getIndex() + 1) + " - " + item); //show game index + the game details
                                hbox.getChildren().add(label);
                                hbox.getChildren().add(button);
                                HBox.setHgrow(label, Priority.ALWAYS);
                                label.setMaxWidth(Double.MAX_VALUE);
                                label.setStyle("-fx-font-size: 22px;");

                                setGraphic(hbox);
                            }
                        }

                        private Button getButton(FinishedGame item) {
                            Button button = new Button("Replay");
                            button.setPrefWidth(80);
                            button.setPrefHeight(60);
                            button.setOnAction(event -> {
                                gameList.setVisible(false);
                                initializeBoard(item.gameID());
                                player1Label.setText(item.player1());
                                player2Label.setText(item.player2());
                                player1Label.setVisible(true);
                                player2Label.setVisible(true);
                                gameOverLabel.setText(item.winner() + " WINS");
                            });
                            return button;
                        }
                    };
                }
            });
        }else{
            gameList.setStyle("-fx-background-color: tan");
            Label placeHolder = new Label("No Games Available");
            placeHolder.setStyle("-fx-font-size: 25");
            gameList.setPlaceholder(placeHolder);
        }
    }

    /**
     * Initializes the board and fetches the possibleMoves from the server
     *
     * @param gameID - the game to fetch the possibleMoves of
     */
    private void initializeBoard(UUID gameID){
        Piece[][] board = Utils.initializeBoard();

        tileBoard = new StackPane[NUM_OF_ROWS][NUM_OF_COLUMNS];

        Platform.runLater(()-> {
            double tile_width = UIBoard.getWidth() / NUM_OF_COLUMNS;
            double tile_height = UIBoard.getHeight() / NUM_OF_ROWS;
            double piece_radius = Math.min(tile_width, tile_height);

            for (int row = 0; row < NUM_OF_ROWS; row++) {
                for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                    StackPane tempPane = new StackPane();
                    Rectangle tile = new Rectangle(tile_width, tile_height);

                    PieceUI newUI = null;

                    if ((row + col) % 2 == 1) {
                        tile.setFill(Color.web("#704731"));
                        if (row < 3) {
                            newUI = new PieceUI(board[row][col], piece_radius, piece_radius);
                        } else if (row > 4) {
                            newUI = new PieceUI(board[row][col], piece_radius, piece_radius);
                        }
                        tempPane.getChildren().add(TILE_INDEX, tile);

                        if (newUI != null) {
                            tempPane.getChildren().add(PIECE_INDEX, newUI);
                            StackPane.setAlignment(newUI, Pos.CENTER);
                        }
                        tileBoard[row][col] = tempPane;
                        UIBoard.add(tempPane, col, row);
                    } else {
                        tile.setFill(Color.web("#efc59d"));
                        UIBoard.add(tile, col, row);
                        tileBoard[row][col] = null;
                    }
                }
            }
        });

        UIBoard.setVisible(true);
        try {
            possibleMoves = Client.getInstance().getHomeStub().getReplayMoves(gameID);
            if(possibleMoves != null && !possibleMoves.isEmpty()){
                arrow.setVisible(true);
                playerTurnLabel.setText("TURN: BLACK");
                playerTurnLabel.setVisible(true);
                return;
            }
        } catch (RemoteException e) {
            possibleMoves = null;
        }
        gameOverLabel.setVisible(true);
    }

    /**
     * Calls onto nextMove to display the next move in possibleMoves<br>
     *
     * Updates the UI according to the current state of the game
     */
    @FXML
    private void arrowOnClick(MouseEvent event) {
        nextMove(possibleMoves.remove(0));
        if (possibleMoves.isEmpty()){
            arrow.setVisible(false);
            playerTurnLabel.setVisible(false);
            gameOverLabel.setVisible(true);
            return;
        }
        currentTurn = currentTurn.equals(Turn.WHITE) ? Turn.BLACK : Turn.WHITE;
        playerTurnLabel.setText(currentTurn.equals(Turn.WHITE) ? "TURN: RED" : "TURN: BLACK");
    }

    /**
     * Calculates and displays the next move in the list of moves in the replay<br>
     *
     * <p>If the move is a step - calls onto swap</p><br>
     * <p>If the move is a capture - calls onto swap and removes the captured piece, multiple captures could be included in move
     * and will be executed consecutively</p><br>
     * <p>In both scenarios promotes according to position</p>
     *
     * @param move - a list of moves to be made(can consist of one or more moves)
     */
    private void nextMove(LinkedList<MoveInfo> move) {
        if(move.size() == 1 && Math.abs(move.getFirst().currentCol() - move.getFirst().newCol()) == 1){
            MoveInfo temp = move.getFirst();
            swap(temp);

            if (temp.newRow() == 0 || temp.newRow() == NUM_OF_ROWS - 1)
                ((PieceUI)(tileBoard[temp.newRow()][temp.newCol()].getChildren().get(PIECE_INDEX))).promote();
        }else{
            for(MoveInfo m : move){
                swap(m);
                removePiece((m.currentRow() + m.newRow()) / 2, (m.currentCol() + m.newCol()) / 2);
                if (m.newRow() == 0 || m.newRow() == NUM_OF_ROWS - 1)
                    ((PieceUI)(tileBoard[m.newRow()][m.newCol()].getChildren().get(PIECE_INDEX))).promote();
            }
        }
    }

    /**
     * Remove the board piece at position row,col
     * @param row - row position
     * @param col - col position
     */
    private void removePiece(int row, int col){
        tileBoard[row][col].getChildren().remove(PIECE_INDEX);
    }

    /**
     * Moves the piece at the current position given in move object, to the new position given in move object
     * @param move - the indexes to be swapped
     */
    private void swap(MoveInfo move){
        try {
            PieceUI temp = (PieceUI) tileBoard[move.currentRow()][move.currentCol()].getChildren().remove(PIECE_INDEX);
            tileBoard[move.newRow()][move.newCol()].getChildren().add(temp);
        }catch (ClassCastException e){
            System.out.println(move);
        }
    }

    /**
     * Switches to home screen when homeScreen button is fired
     */
    @FXML
    void homeScreenOnAction(ActionEvent event) throws IOException {
        assert SceneManager.getInstance() != null;
        SceneManager.getInstance().switchScene("HomePage.fxml");
    }
}