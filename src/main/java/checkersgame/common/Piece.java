package checkersgame.common;

import java.io.Serializable;
import checkersgame.common.Utils.PieceColor;

/**
 * <b>This class represents a piece on the game board</b><br>
 *
 * <p>The Piece class is responsible for storing the color, and rank of the piece</p>
 *
 * @author Ilay Zvi
 */
public class Piece implements Serializable {

    private PieceColor color;
    private boolean isKing = false;

    /**
     * Creates a new instance of Piece with the specified color
     *
     * @param color - the color of the piece
     */
    public Piece(PieceColor color) {
        this.color = color;
    }

    /**
     * Creates a new instance of Piece with no color
     */
    public Piece() {
        this.color = null;
    }

    /**
     * Copy constructor of Piece, copies the rank and color of the other piece
     *
     * @param other - the piece to copy
     */
    public Piece(Piece other){
        this(other.getColor());
        this.isKing = other.isKing();
    }

    /**
     * Gets the value of the property color.
     */
    public PieceColor getColor() {
        return color;
    }

    /**
     * Sets the value of the property color.
     */
    public void setColor(PieceColor color) {
        this.color = color;
    }

    /**
     * Gets the value of the property isKing.
     * <p>
     * <b>Default value:</b><br>
     * false
     * </p>
     */
    public boolean isKing() {
        return isKing;
    }

    /**
     * Checks if the piece is empty or not based on its color field
     * @return true if the color field is null, false otherwise
     */
    public boolean isEmpty(){
        return this.color == null;
    }

    /**
     * Promotes the rank of the piece, setting isKing to true
     */
    public void promote() {
        isKing = true;
    }

    /**
     * Demotes the rank of the piece, setting isKing to false
     */
    public void demote(){
        isKing = false;
    }
}
