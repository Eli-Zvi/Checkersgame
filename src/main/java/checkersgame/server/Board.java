package checkersgame.server;

import checkersgame.common.Utils;
import checkersgame.common.MoveInfo;
import checkersgame.common.Piece;
import javafx.util.Pair;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static checkersgame.common.Utils.*;

/**
 * <p>This class is responsible for every part of the checkersgame which includes:</p>
 * <ul>
 *     <li>The game board - initialization, updating</li>
 *     <li>The current player turn</li>
 *     <li>The game state - if the game is still being played or if either player has won</li>
 *     <li>The current possible moves according to the state of the game</li>
 *     <li>Performing requested moves</li>
 * </ul>
 */
public class Board {
    private final Piece[][] board;
    private Turn currentTurn = Utils.Turn.BLACK;
    private final Map<LinkedList<MoveInfo>, int[]> possibleMoves = new ConcurrentHashMap<>();
    private final Direction [] directions = {Direction.UP_RIGHT, Direction.UP_LEFT, Direction.DOWN_RIGHT, Direction.DOWN_LEFT};

    public enum GameState{
        ONGOING,
        PLAYER1WIN,
        PLAYER2WIN
    }

    /**
     * The default constructor of the Board class
     */
    public Board(){
        board = Utils.initializeBoard();
    }

    /**
     * copy constructor of the Board class
     * @param board a board to copy
     */
    private Board(Piece[][] board){
        this.board = new Piece[NUM_OF_ROWS][NUM_OF_COLUMNS]; // Create a new 2D array

        for (int i = 0; i < NUM_OF_ROWS; i++) {
            for (int j = 0; j < NUM_OF_COLUMNS; j++) {
                if (board[i][j] != null) {
                    this.board[i][j] = new Piece(board[i][j]); // Use Piece copy constructor
                }
            }
        }
    }

    /**
     * Gets the value of the property board.
     * @return the game board
     */
    public Piece[][] getBoard(){
        return this.board;
    }

    /**
     * Gets the value of the property currentTurn.
     * @return the current turn in the game
     */
    public Turn getCurrentTurn(){
        return this.currentTurn;
    }

    /**
     * Gets the value of the property possibleMoves
     * @return the moves stored in possibleMoves
     */
    public Map<LinkedList<MoveInfo>, int[]> getPossibleMoves() {
        return possibleMoves;
    }

    /**
     * Checks if the piece at position row1,col1 and row2,col2 are of opposite color
     * @param row1 the row of the first piece
     * @param col1 the col of the first piece
     * @param row2 the row of the second piece
     * @param col2 the col of the second piece
     * @return true if the pieces are of opposite color false otherwise
     */
    private boolean isOppositeColor(int row1, int col1, int row2, int col2) {
        // check that middle piece is not empty
        // check that they are opposite colors
        try {
            return !board[(row1 + row2) / 2][(col1 + col2) / 2].isEmpty() &&
                    !board[row1][col1].getColor().equals(board[(row1 + row2) / 2][(col1 + col2) / 2].getColor());
        }catch (NullPointerException e){
            System.out.println("isOppositeColor" + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if either player has won by counting each side's pieces on the board
     * @return <ol>
     *              <li> {@code GameState.PLAYER2WIN} if white has won </li>
     *              <li> {@code GameState.PLAYER1WIN} if black has won </li>
     *              <li> {@code GameState.ONGOING} if neither has won </li>
     *         </ol>
     */
    public GameState checkWin(){
        int blackCount = 0;
        int whiteCount = 0;

        for(Piece[] row : this.board){
            for (Piece piece : row){
                if(piece == null || piece.getColor() == null) continue;

                if(blackCount > 0 && whiteCount > 0){
                    return GameState.ONGOING;
                }

                if(piece.getColor() == player1Color){
                    blackCount++;
                }else{
                    whiteCount++;
                }
            }
        }

        if(blackCount == 0){
            return GameState.PLAYER2WIN;
        }else if(whiteCount == 0){
            return GameState.PLAYER1WIN;
        }
        else return GameState.ONGOING;
    }

    /**
     * Check if the piece at position row,col can be promoted
     * @param row row of the piece to check
     * @param col col of the piece to check
     * @return {@code true} if the piece can be promoted {@code false} if the piece cant be promoted
     */
    public boolean checkPromotion(int row, int col){
        try{
            Piece piece = board[row][col];
            if(piece.getColor().equals(player1Color) && row == NUM_OF_ROWS - 1){ // check black promotion
                piece.promote();
                return true;
            }

            if(piece.getColor().equals(player2Color) && row == 0){ // check white promotion
                piece.promote();
                return true;
            }
        }catch (IndexOutOfBoundsException e){
            System.out.println("checkPromotion " +e.getMessage());
        }
        return false;
    }

    /**
     * Removes the piece at position row,col from the board
     * @param row the row of the piece
     * @param col the col of the piece
     */
    public void removePiece(int row, int col) {
        board[row][col].setColor(null);
        board[row][col].demote();
    }

    /**
     * Swap between the tile at row1,col1 and row2,col2
     * @param row1 the row of the first tile
     * @param col1 the col of the first tile
     * @param row2 the row of the second tile
     * @param col2 the col of the second tile
     */
    public void swap(int row1, int col1, int row2, int col2) {
        Piece temp = board[row1][col1];
        board[row1][col1] = board[row2][col2];
        board[row2][col2] = temp;
    }

    /**
     * Attempts to perform a given move
     * @param moves move to perform
     * @return true if the piece used to perform the move has been promoted false otherwise
     */
    public boolean attemptMove(LinkedList<MoveInfo> moves) throws RemoteException{
        try {
            if (possibleMoves.containsKey(moves)) {
                MoveInfo move = moves.getFirst();

                swap(move.currentRow(), move.currentCol(), move.newRow(), move.newCol());

                this.currentTurn = Turn.BLACK == getCurrentTurn() ? Turn.WHITE : Turn.BLACK;

                return checkPromotion(move.newRow(), move.newCol());
            }else{
                throw new RemoteException();
            }
        }catch (IndexOutOfBoundsException e){
            System.out.println("attemptMove " +e.getMessage());
        }
        return false;
    }

    /**
     * Attempts to perform a given move/moves
     * @param moves a list of moves to perform
     * @return true if the piece used to perform the move/s has been promoted false otherwise
     */
    public boolean attemptCapture(LinkedList<MoveInfo> moves) throws RemoteException {
        boolean promotion = false;
        if(possibleMoves.containsKey(moves)) {
            for (MoveInfo move : moves) {
                swap(move.currentRow(), move.currentCol(), move.newRow(), move.newCol());
                removePiece((move.currentRow() + move.newRow()) / 2, (move.currentCol() + move.newCol()) / 2);
                promotion = promotion || checkPromotion(move.newRow(), move.newCol());
            }
            this.currentTurn = Turn.BLACK == getCurrentTurn() ? Turn.WHITE : Turn.BLACK;
        }else{
            throw new RemoteException();
        }
        return promotion;
    }

    /**
     * Checks if a non capture move is valid
     * @param move move to check
     * @return true if the move is legal false otherwise
     */
    public boolean isMoveLegal(MoveInfo move) {
        int row = move.currentRow(), col = move.currentCol(), newRow = move.newRow(), newCol = move.newCol();
        if(newRow >= 0 && newCol >= 0 && newRow < NUM_OF_ROWS && newCol < NUM_OF_COLUMNS && board[row][col] != null &&
                board[newRow][newCol] != null && board[newRow][newCol].isEmpty() && Math.abs(newCol - col) == 1){
            if(board[row][col].isKing()){
                return Math.abs(newRow - row) == 1;
            }
            else if(board[row][col].getColor().equals(PieceColor.BLACK)){
                return (newRow - row) == 1;
            }
            else if(board[row][col].getColor().equals(PieceColor.WHITE)){
                return (newRow - row) == -1;
            }
        }
        return false;
    }

    /**
     * Checks for all possible moves for the current state of the board and current player turn
     */
    private void checkPossibleMoves() {
        possibleMoves.clear();
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                if((row + col) % 2 == 1 && board[row][col] != null && !board[row][col].isEmpty()){
                    Piece currentPiece = board[row][col];
                    PieceColor tempColor = currentPiece.getColor();

                    //check if the piece is of the same type as the current turn
                    if(tempColor.equals(currentTurn.getColor())){
                        for(Direction d : directions){ //check for each direction
                            LinkedList<MoveInfo> moves = new LinkedList<>();
                            MoveInfo move = new MoveInfo(row, col, row + d.getRowChange(), col + d.getColChange());
                            if(isMoveLegal(move)){
                                moves.add(move); //add move if its legal
                                possibleMoves.put(moves, new int[]{row, col}); //add move to possibleMoves
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>Checks for all possible captures for the current state of the board and the current player turn</p>
     * <ol>
     *     <li>First scans the board for all possible singular captures</li>
     *     <li>After scanning for all possible singular captures calls onto {@code checkPossibleCaptures}
     *     to scan if any of the possible singular captures could perform another capture or captures</li>
     * </ol>
     */
    private void checkPossibleCaptures(){
        possibleMoves.clear(); //clear possibleMoves

        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                if((row + col) % 2 == 1 && board[row][col] != null && !board[row][col].isEmpty()){

                    Piece currentPiece = board[row][col];
                    PieceColor tempColor = currentPiece.getColor();

                    if(tempColor.equals(currentTurn.getColor())){ //if the piece color matches the turn
                        for(Direction d : directions){ //check all 4 diagonals
                            MoveInfo move = new MoveInfo(row, col, row + 2 * d.getRowChange(), col + 2 * d.getColChange());
                            if(isCaptureLegal(move)){
                                //if the piece can perform the move, create a new MoveInfo list and add the move, then add to possibleMoves
                                LinkedList<MoveInfo> moves = new LinkedList<>();
                                moves.add(move);
                                possibleMoves.putIfAbsent(moves, new int[]{row, col});
                            }
                        }
                    }
                }
            }
        }

        if(!possibleMoves.isEmpty())
            checkMultipleCaptures();
    }

    /**
     * <p>Scans the possibleMoves map to check if any of the captures(if they exist) can be a multiple capture</p>
     * ex: possibleMoves has the move 2,2 -> 4,4 checks if captures can be done from 4,4 to a double diagonal tile
     */
    private void checkMultipleCaptures(){
        // key - the list of moves that lead to the board state, value - a board state
        Queue<Pair<LinkedList<MoveInfo>, Board>> infoBoard = new LinkedList<>();
        Map<LinkedList<MoveInfo>, int[]> toAdd = new HashMap<>();

        for (Map.Entry<LinkedList<MoveInfo>, int[]> pair : possibleMoves.entrySet()) {
            Board boardCopy = new Board(board);
            LinkedList<MoveInfo> initialMove = pair.getKey();
            boardCopy.capture(initialMove.getLast());
            infoBoard.add(new Pair<>(initialMove, boardCopy));

            while (!infoBoard.isEmpty()) { //similar to BFS
                Pair<LinkedList<MoveInfo>, Board> info = infoBoard.poll();
                LinkedList<MoveInfo> moves = info.getKey();
                boardCopy = info.getValue();
                boolean madeNewCapture = false;
                MoveInfo currentPos = moves.getLast();

                for (Direction d : directions) {
                    int currRow = currentPos.newRow(), currCol = currentPos.newCol();
                    MoveInfo move = new MoveInfo(currRow, currCol, currRow + 2 * d.getRowChange(), currCol + 2 * d.getColChange());

                    if (boardCopy.isCaptureLegal(move)) {
                        madeNewCapture = true;
                        LinkedList<MoveInfo> tempMoves = new LinkedList<>(moves);
                        Board tempBoard = new Board(boardCopy.getBoard());
                        tempMoves.add(move);
                        tempBoard.capture(move);
                        infoBoard.add(new Pair<>(tempMoves, tempBoard));
                    }
                }

                if (!madeNewCapture && !possibleMoves.containsKey(moves)) {
                    if(initialMove != null){
                        possibleMoves.remove(initialMove);
                        initialMove = null;
                    }
                    toAdd.put(moves, pair.getValue());
                }
            }
        }

        if(!toAdd.isEmpty()) {
            possibleMoves.putAll(toAdd);
        }
    }

    /**
     * If the game is still in play, checks if there are any possible(forced) captures,
     * if there aren't any, checks all possible moves
     */
    public void updatePossibleMoves(){
        if(checkWin() == GameState.ONGOING){
            checkPossibleCaptures(); //check for the possible captures

            if (possibleMoves.isEmpty()) { //if no possible captures were found, check for possible regular moves
                checkPossibleMoves();
            }
        }
    }

    /**
     * Checks if a given capture move is legal
     * @param move a move to check
     * @return true if move is legal false otherwise
     */
    public boolean isCaptureLegal(MoveInfo move){
        int row = move.currentRow(), col = move.currentCol(), newRow = move.newRow(), newCol = move.newCol();

        if(newRow >= 0 && newCol >= 0 && newRow < NUM_OF_ROWS && newCol < NUM_OF_COLUMNS &&
                board[row][col] != null && board[newRow][newCol] != null && board[newRow][newCol].isEmpty()
                && isOppositeColor(row, col, newRow, newCol)) {

            if(board[row][col].isKing()){
                return Math.abs(newRow - row) == 2 && Math.abs(newCol - col) == 2;
            }
            else if(board[row][col].getColor().equals(PieceColor.BLACK)){
                return (newRow - row) == 2 && Math.abs(newCol - col) == 2;
            }
            else if(board[row][col].getColor().equals(PieceColor.WHITE)){
                return (newRow - row) == -2 && Math.abs(newCol - col) == 2;
            }
        }
        return false;
    }

    /**
     * <p>Performs a singular capture move</p>
     * <p>Meant to be used by the checkMultipleCaptures function</p>
     * @param move a capture move to perform
     */
    public void capture(MoveInfo move){
        try {
            if (isCaptureLegal(move)) {
                swap(move.currentRow(), move.currentCol(), move.newRow(), move.newCol()); //swap tiles
                removePiece((move.currentRow() + move.newRow()) / 2, (move.currentCol() + move.newCol()) / 2); //remove piece
                checkPromotion(move.newRow(), move.newCol()); //check for promotion and promote
            }
        }catch (IndexOutOfBoundsException e){
            System.out.println("attemptCapture " +e.getMessage());
        }
    }

    public void printBoard(){
        for(Piece[] row : this.board){
            for(Piece piece : row){
                if(piece != null){
                    if(piece.getColor() != null)
                        System.out.print(piece.getColor() + " ");
                    else{
                        System.out.print("empty ");
                    }
                }else{
                    System.out.print("x ");
                }
            }
            System.out.println();
        }
    }
}