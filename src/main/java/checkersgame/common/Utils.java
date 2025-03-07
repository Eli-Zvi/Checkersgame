package checkersgame.common;

/**
 * <b>This class represents the constants across the project</b><br>
 *
 * @author Ilay Zvi
 */
public class Utils {
    public static final int NUM_OF_ROWS = 8;
    public static final int NUM_OF_COLUMNS = 8;
    public static final PieceColor player1Color = PieceColor.BLACK, player2Color = PieceColor.WHITE; //player1 is black(top) player2 is white(bottom)
    //represents the index in the StackPane of the tile and piece
    public final static int TILE_INDEX = 0;
    public final static int PIECE_INDEX = 1;

    /**
     * Represents a turn in the game and the color that correlates to the respective turn
     */
    public enum Turn{
        BLACK(player1Color),
        WHITE(player2Color);

        private final PieceColor color;

        Turn(PieceColor color){
            this.color = color;
        }

        /**
         * Gets the value of the property color of the Turn.
         */
        public PieceColor getColor(){
            return color;
        }
    }

    /**
     * Represents the possible diagonals movements in the game.<br>
     * The values indicate row and column changes:
     * <ul>
     *     <li>UP_RIGHT(-1, 1): Move diagonally up-right.</li>
     *     <li>UP_LEFT(-1, -1): Move diagonally up-left.</li>
     *     <li>DOWN_RIGHT(1, 1): Move diagonally down-right.</li>
     *     <li>DOWN_LEFT(1, -1): Move diagonally down-left.</li>
     * </ul>
     */
    public enum Direction{
        UP_RIGHT(-1, 1),
        UP_LEFT(-1, -1),
        DOWN_RIGHT(1, 1),
        DOWN_LEFT(1, -1);

        private final int rowChange;
        private final int colChange;

        Direction(int rowChange, int colChange) {
            this.rowChange = rowChange;
            this.colChange = colChange;
        }

        /**
         * Gets the value of the property rowChange of the Direction.
         */
        public int getRowChange() {
            return rowChange;
        }

        /**
         * Gets the value of the property colChange of the Direction.
         */
        public int getColChange() {
            return colChange;
        }
    }

    /**
     * Represents the colors of the pieces
     */
    public enum PieceColor{
        BLACK,
        WHITE
    }

    /**
     * Initializes a new instance of a board
     *
     * <p>The method creates a new board, initializing each of its cells according to the rules of the game:</p>
     * <ul>
     *     <li>Black pieces at the top</li>
     *     <li>White pieces at the bottom</li>
     *     <li>Pieces are only positioned on black tiles</li>
     * </ul>
     *
     * @return A 2D array representing the newly initialized game board, where:
     * <ul>
     *      <li> null represents a white tile (non-playable).</li>
     *      <li> A Piece object represents a piece on a black tile.</li>
     *      <li> The top three rows contain black pieces.</li>
     *      <li> The bottom three rows contain white pieces.</li>
     * </ul>
     */
    public static Piece[][] initializeBoard(){
        Piece[][] board = new Piece[NUM_OF_ROWS][NUM_OF_COLUMNS];

        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                if((row + col) % 2 == 1){
                    if(row < 3){
                        board[row][col] = new Piece(player1Color);
                    }else if(row > 4){
                        board[row][col] = new Piece(player2Color);
                    }else{
                        board[row][col] = new Piece();
                    }
                }else{
                    board[row][col] = null;
                }
            }
        }

        return board;
    }
}
