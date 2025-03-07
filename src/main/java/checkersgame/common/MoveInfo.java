package checkersgame.common;

import java.io.Serializable;

/**
 * <b>This class represents a move record</b><br>
 *
 * The record consists of the current position's coordinates and the new position's coordinates
 *
 * @author Ilay Zvi
 */
public record MoveInfo(int currentRow, int currentCol, int newRow, int newCol) implements Serializable {

    private static final int DATABASE_STRING_LENGTH = 3; //a coordinate is represented as x,y which is 3 chars long

    /**
     * Returns a string representation of this MoveInfo record
     * @param from - a string represented in the form of x,y
     * @param to - a string represented in the form of x,y
     * @return null if from or to are not properly defined, a MoveInfo record otherwise
     */
    public static MoveInfo fromString(String from, String to) {
        if(from.length() == DATABASE_STRING_LENGTH && to.length() == DATABASE_STRING_LENGTH) {
            String[] currPos = from.split(",");
            String[] newPos = to.split(",");
            return new MoveInfo(Integer.parseInt(currPos[0]), Integer.parseInt(currPos[1]),
                    Integer.parseInt(newPos[0]), Integer.parseInt(newPos[1]));
        }
        return null;
    }
}
