package checkersgame.common;

import java.io.Serializable;
import java.util.UUID;

/**
 * <b>This class represents a finished game record</b><br>
 *
 * The record consists of the game's UUID, the username of the players that were part of the game, and the winner out of the two
 *
 * @author Ilay Zvi
 */
public record FinishedGame(UUID gameID, String player1, String player2, String winner) implements Serializable {

    @Override
    public String toString() {
        return player1 + " vs " + player2 + "  \tWinner: " + winner;
    }
}
