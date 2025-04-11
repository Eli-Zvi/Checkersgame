package checkersgame.server;

import checkersgame.common.MoveInfo;
import checkersgame.common.Piece;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static checkersgame.common.Utils.*;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        // Initialize the Board instance before each test
        board = new Board();
        board.printBoard();
        board.updatePossibleMoves();
    }

    @AfterEach
    void tearDown(){
        board = null;
    }

    @Test
    void shouldInitializeBoard() {
        // Test that the board is initialized correctly with non-null pieces
        Piece[][] pieces = board.getBoard();
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                // Check that pieces are either empty or correctly placed
                if ((row + col) % 2 == 1) { // Assuming black pieces are at odd indices initially
                    assertNotNull(pieces[row][col], "Piece should be initialized");
                    if(row < 3) {
                        assertEquals(pieces[row][col].getColor(), PieceColor.BLACK);
                    }else if(row > 4){
                        assertEquals(pieces[row][col].getColor(), PieceColor.WHITE);
                    }else{
                        assertNull(pieces[row][col].getColor());
                    }
                } else {
                    assertNull(pieces[row][col], "Empty space should be null");
                }
            }
        }
    }

    @Test
    void testSwitchTurn() throws RemoteException {
        // Test that the turn switches between players correctly
        assertEquals(Turn.BLACK, board.getCurrentTurn());
        board.attemptMove(board.getPossibleMoves().keySet().iterator().next());
        assertEquals(Turn.WHITE, board.getCurrentTurn());
    }

    @Test
    void shouldPerformMove() throws RemoteException {
        // Test regular move
        LinkedList<MoveInfo> moves = board.getPossibleMoves().keySet().iterator().next(); // example valid move
        board.attemptMove(moves);
        assertNotNull(board.getBoard()[moves.getLast().newRow()][moves.getLast().newCol()], "The move should be valid.");
    }

    @Test
    void shouldPerformCapture() throws RemoteException {
        // Test a capture move
        LinkedList<MoveInfo> captureMoves = board.getPossibleMoves().keySet().iterator().next();
        board.attemptCapture(captureMoves);
        assertNotNull(board.getBoard()[captureMoves.getLast().newRow()][captureMoves.getLast().newCol()],
                "The capture move should be valid.");
    }

    @Test
    void shouldPlayEntireGame() throws RemoteException{
        Random rand = new Random();
        int count = 0;
        Queue<LinkedList<MoveInfo>> player1Queue = new LinkedList<>();
        Queue<LinkedList<MoveInfo>> player2Queue = new LinkedList<>();
        Set<LinkedList<MoveInfo>> captureMoves;
        while(board.checkWin() == Board.GameState.ONGOING && !board.getPossibleMoves().isEmpty()) {
            count++;
            System.out.println("------------------------------------");

            captureMoves = board.getPossibleMoves().keySet();

            LinkedList<MoveInfo> move = captureMoves.stream().skip(rand.nextInt(captureMoves.size())).findFirst().orElse(null);

            if(player1Queue.size() == 5){
                player1Queue.remove();
            }
            if(player2Queue.size() == 5){
                player2Queue.remove();
            }

            while(((player1Queue.contains(move) && count % 2 == 0) || (player2Queue.contains(move) && count % 2 == 1))
                    && captureMoves.size() > 3 && move.size() == 1) {
                move = captureMoves.stream().skip(rand.nextInt(captureMoves.size())).findFirst().orElse(null);
                assertNotNull(move);
            }

            if(count % 2 == 0){
                player1Queue.offer(move);
            }else player2Queue.offer(move);

            if(Math.abs(move.getFirst().newCol() - move.getFirst().currentCol()) == 2)
                board.attemptCapture(move);
            else board.attemptMove(move);

            board.printBoard();
            board.updatePossibleMoves();
        }
        System.out.println(count);
        if (board.getPossibleMoves().isEmpty()){
            System.out.println("No Possible Moves");
            System.out.println(board.getCurrentTurn().equals(Turn.BLACK) ? "White Wins" : "Black Wins");
        }else{
            System.out.println(board.checkWin().equals(Board.GameState.PLAYER2WIN) ? "White Wins" : "Black Wins");
        }
    }
}
