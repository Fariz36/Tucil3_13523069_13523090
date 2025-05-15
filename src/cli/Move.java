package cli;

public class Move {
    private Piece piece;
    private String direction;
    
    public Move(Piece piece, String direction) {
        this.piece = piece;
        this.direction = direction;
    }
    
    public Piece getPiece() { return piece; }
    public String getDirection() { return direction; }
    
    public int getDirectionValue() {
        if (direction.equals("right") || direction.equals("down")) {
            return 1;
        } else {
            return -1;
        }
    }
    
    @Override
    public String toString() {
        return piece.getId() + "-" + direction;
    }
}
