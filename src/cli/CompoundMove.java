package cli;

/**
 * Represents a compound move (multi-cell movement in one direction)
 * This is used to represent moves like "A+3" in the solution sequence
 */
public class CompoundMove extends Move {
    private int distance;
    
    /**
     * Create a compound move
     * @param piece The piece to move
     * @param direction The direction ("up", "down", "left", "right")
     * @param distance The number of cells to move
     */
    public CompoundMove(Piece piece, String direction, int distance) {
        super(piece, direction);
        this.distance = distance;
    }
    
    /**
     * Get the distance of this compound move
     */
    public int getDistance() {
        return distance;
    }
    
    /**
     * Format the move as a string like "UP×3" or "DOWN×2"
     */
    @Override
    public String toString() {
        return getDirection().toUpperCase() + " (" + distance  + ")";
    }
}