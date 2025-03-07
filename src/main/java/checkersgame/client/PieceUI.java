package checkersgame.client;

import checkersgame.common.Utils.PieceColor;
import checkersgame.common.Piece;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * <b>This class represents a UI piece on the user's board</b><br>
 *
 * @author Ilay Zvi
 */
public class PieceUI extends ImageView {
    //preload images
    private static final Image redKing = new Image(PieceUI.class.getResource("/checkersgame/client/red_king.png").toExternalForm());
    private static final Image redPiece = new Image(PieceUI.class.getResource("/checkersgame/client/red_piece.png").toExternalForm());
    private static final Image blackPiece = new Image(PieceUI.class.getResource("/checkersgame/client/black_piece.png").toExternalForm());
    private static final Image blackKing = new Image(PieceUI.class.getResource("/checkersgame/client/black_king.png").toExternalForm());
    private final Piece piece;


    /**
     * Creates a new instance of PieceUI with the specified piece,width,height
     *
     * @param piece - the piece that is represented by the UI piece
     * @param width - the width of the image
     * @param height - the height of the image
     */
    public PieceUI(Piece piece, double width, double height) {
        super(piece.getColor() == PieceColor.WHITE ? redPiece : blackPiece);
        this.setFitHeight(height);
        this.setFitWidth(width);
        this.setPreserveRatio(true);
        this.piece = piece;
    }

    /**
     * Retrieves the underlying Piece object
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * Calls onto the underlying Piece's promote and changes from a regular piece's image to a king version
     */
    public void promote(){
        this.piece.promote();
        this.setImage(piece.getColor() == PieceColor.WHITE ? redKing : blackKing);
    }

    /**
     * Calls onto the underlying Piece's demote and changes from a king piece's image to a regular version
     */
    public void demote(){
        this.piece.demote();
        this.setImage(piece.getColor() == PieceColor.WHITE ? redPiece : blackPiece);
    }
}
