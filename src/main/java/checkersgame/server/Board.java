package checkersgame.server;

import checkersgame.common.Utils;
import checkersgame.common.MoveInfo;
import checkersgame.common.Piece;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static checkersgame.common.Utils.*;
//TODO ADD DOCUMENTATION MAYBE LOCKS
public class Board {
    private final Piece[][] board;
    private Turn currentTurn = Utils.Turn.BLACK;
    //when piece is clicked: lookup piece -> cycle through moveinfo and display the highlight on the last moveinfo in the array, can also highlight all the pieces
    //when turn switches, checked forced capture, calculate possible moves or captures and send to player
    //make a map on player side between piece -> pieceui -> maybe even StackPane
    private final Map<LinkedList<MoveInfo>, int[]> possibleMoves = new ConcurrentHashMap<>();
    private final Direction [] directions = {Direction.UP_RIGHT, Direction.UP_LEFT, Direction.DOWN_RIGHT, Direction.DOWN_LEFT};

    public enum GameState{
        ONGOING,
        PLAYER1WIN,
        PLAYER2WIN
    }

    public Board(){
        board = Utils.initializeBoard();
        //printBoard();
    }

    public Board(Piece[][] board){
        this.board = new Piece[NUM_OF_ROWS][NUM_OF_COLUMNS]; // Create a new 2D array

        for (int i = 0; i < NUM_OF_ROWS; i++) {
            for (int j = 0; j < NUM_OF_COLUMNS; j++) {
                if (board[i][j] != null) {
                    this.board[i][j] = new Piece(board[i][j]); // Use Piece copy constructor
                }
            }
        }
    }

    public Piece[][] getBoard(){
        return this.board;
    }

    public Turn getCurrentTurn(){
        return this.currentTurn;
    }

    public void setCurrentTurn(Turn turn){
        this.currentTurn = turn;
    }

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

    public Map<LinkedList<MoveInfo>, int[]> getPossibleMoves() {
        return possibleMoves;
    }

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

    public void removePiece(int row, int col) {
        board[row][col].setColor(null);
        board[row][col].demote();
    }

    public void swap(int row1, int col1, int row2, int col2) {
        Piece temp = board[row1][col1];
        board[row1][col1] = board[row2][col2];
        board[row2][col2] = temp;
    }

    public boolean attemptMove(LinkedList<MoveInfo> moves){
        try {
            if (possibleMoves.containsKey(moves)) {
                MoveInfo move = moves.getFirst();

                swap(move.currentRow(), move.currentCol(), move.newRow(), move.newCol());

                setCurrentTurn(Turn.BLACK == getCurrentTurn() ? Turn.WHITE : Turn.BLACK);

                return checkPromotion(move.newRow(), move.newCol());
            }
        }catch (IndexOutOfBoundsException e){
            System.out.println("attemptMove " +e.getMessage());
        }
        return false;
    }

    public boolean movePiece(MoveInfo move) {
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

    public void checkPossibleMoves() {
        possibleMoves.clear();
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                if((row + col) % 2 == 1 && board[row][col] != null && !board[row][col].isEmpty()){
                    Piece currentPiece = board[row][col];
                    PieceColor tempColor = currentPiece.getColor();

                    if(tempColor.equals(currentTurn.getColor())){
                        for(Direction d : directions){
                            LinkedList<MoveInfo> moves = new LinkedList<>();
                            MoveInfo move = new MoveInfo(row, col, row + d.getRowChange(), col + d.getColChange());
                            if(movePiece(move)){
                                moves.add(move);
                                possibleMoves.put(moves, new int[]{row, col});
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean capturePiece(MoveInfo move){
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

    public void checkPossibleCaptures(){
        possibleMoves.clear();
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                if((row + col) % 2 == 1 && board[row][col] != null && !board[row][col].isEmpty()){
                    Piece currentPiece = board[row][col];
                    PieceColor tempColor = currentPiece.getColor();

                    if(tempColor.equals(currentTurn.getColor())){
                        for(Direction d : directions){
                            MoveInfo move = new MoveInfo(row, col, row + 2 * d.getRowChange(), col + 2 * d.getColChange());
                            if(capturePiece(move)){
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

    public void checkMultipleCaptures(){
        //use recursion - moveinfo, board as parameters, create a new board for every direction
        Queue<Pair<LinkedList<MoveInfo>, Board>> infoBoard = new LinkedList<>();
        Map<LinkedList<MoveInfo>, int[]> toAdd = new HashMap<>();

        for (Map.Entry<LinkedList<MoveInfo>, int[]> pair : possibleMoves.entrySet()) {
            Board boardCopy = new Board(board);
            LinkedList<MoveInfo> initialMove = pair.getKey();
            boardCopy.capture(initialMove.getLast());
            infoBoard.add(new Pair<>(initialMove, boardCopy));

            while (!infoBoard.isEmpty()) {
                Pair<LinkedList<MoveInfo>, Board> info = infoBoard.poll();
                LinkedList<MoveInfo> moves = info.getKey();
                boardCopy = info.getValue();
                boolean madeNewCapture = false;
                MoveInfo currentPos = moves.getLast();

                for (Direction d : directions) {
                    int currRow = currentPos.newRow(), currCol = currentPos.newCol();
                    MoveInfo move = new MoveInfo(currRow, currCol, currRow + 2 * d.getRowChange(), currCol + 2 * d.getColChange());

                    if (boardCopy.capturePiece(move)) { // <- need to create a new board after because it changes the board
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

    public void capture(MoveInfo move){
        try {
            if (capturePiece(move)) {
                swap(move.currentRow(), move.currentCol(), move.newRow(), move.newCol());
                removePiece((move.currentRow() + move.newRow()) / 2, (move.currentCol() + move.newCol()) / 2);
                checkPromotion(move.newRow(), move.newCol());
            }
        }catch (IndexOutOfBoundsException e){
            System.out.println("attemptCapture " +e.getMessage());
        }
    }

    public boolean attemptCapture(LinkedList<MoveInfo> moves){
        boolean promotion = false;
        try {
            for (MoveInfo move : moves) {
                swap(move.currentRow(), move.currentCol(), move.newRow(), move.newCol());
                removePiece((move.currentRow() + move.newRow()) / 2, (move.currentCol() + move.newCol()) / 2);
                promotion = promotion || checkPromotion(move.newRow(), move.newCol());
            }
            setCurrentTurn(Turn.BLACK == getCurrentTurn() ? Turn.WHITE : Turn.BLACK);
        }catch (Exception e) { // if it fails revert moves - implement later
        }
        return promotion;
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