package checkersgame.server;

import checkersgame.common.MoveInfo;
import checkersgame.common.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static checkersgame.common.Utils.*;

import java.rmi.RemoteException;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        // Initialize the Board instance before each test
        board = new Board();
        board.checkPossibleCaptures();
        if(board.getPossibleMoves().isEmpty())
            board.checkPossibleMoves();
    }

    @Test
    void testInitialBoardSetup() {
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
    void testAttemptMove() throws RemoteException {
        // Test regular move
        LinkedList<MoveInfo> moves = board.getPossibleMoves().keySet().iterator().next(); // example valid move
        board.attemptMove(moves);
        assertNotNull(board.getBoard()[moves.getLast().newRow()][moves.getLast().newCol()], "The move should be valid.");
    }

    @Test
    void testAttemptCapture() throws RemoteException {
        // Test a capture move
        LinkedList<MoveInfo> captureMoves = board.getPossibleMoves().keySet().iterator().next();
        board.attemptCapture(captureMoves);
        assertNotNull(board.getBoard()[captureMoves.getLast().newRow()][captureMoves.getLast().newCol()],
                "The capture move should be valid.");
    }
}
