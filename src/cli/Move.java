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
    
    /**
     * Returns the direction as an integer value
     * @return 1 for right/down, -1 for left/up
     */
    public int getDirectionValue() {
        if (direction.equals("right") || direction.equals("down")) {
            return 1;
        } else {
            return -1;
        }
    }
    
    @Override
    public String toString() {
        // Convert the direction to uppercase
        return direction.toUpperCase();
    }
}