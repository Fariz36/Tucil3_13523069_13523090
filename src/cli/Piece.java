package cli;

public class Piece {
    private char id;
    private java.util.List<Position> positions;
    private Orientation orientation;
    
    public Piece(char id, java.util.List<Position> positions) {
        this.id = id;
        this.positions = new java.util.ArrayList<>(positions);
        
        // Validate piece shape before determining orientation
        validatePieceShape();
        
        determineOrientation();
        sortPositions();
    }
    
    /**
     * Validates that the piece has a proper elongated form (1×N or N×1)
     * @throws IllegalArgumentException if the piece shape is invalid
     */
    private void validatePieceShape() {
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("Piece '" + id + "' has no positions");
        }
        
        if (positions.size() < 2) {
            throw new IllegalArgumentException("Piece '" + id + "' must occupy at least 2 cells, but only has " + positions.size());
        }
        
        // Get the bounding box
        int minRow = positions.stream().mapToInt(p -> p.row).min().orElse(0);
        int maxRow = positions.stream().mapToInt(p -> p.row).max().orElse(0);
        int minCol = positions.stream().mapToInt(p -> p.col).min().orElse(0);
        int maxCol = positions.stream().mapToInt(p -> p.col).max().orElse(0);
        
        int rowSpan = maxRow - minRow + 1;
        int colSpan = maxCol - minCol + 1;
        
        // Check if piece is linear (either horizontal or vertical)
        boolean isHorizontal = (rowSpan == 1 && colSpan > 1);
        boolean isVertical = (colSpan == 1 && rowSpan > 1);
        
        // Special case: single cell allowed only for primary piece (P)
        if (positions.size() == 1 && id == 'P') {
            return; // Allow single cell for primary piece (for testing)
        }
        
        if (!isHorizontal && !isVertical) {
            throw new IllegalArgumentException("Piece '" + id + "' is not linear. It spans " + 
                rowSpan + " rows and " + colSpan + " columns. Pieces must be either 1×N or N×1.");
        }
        
        // Verify that all positions are contiguous
        if (isHorizontal) {
            // For horizontal pieces, all positions should have the same row
            int expectedRow = positions.get(0).row;
            for (Position pos : positions) {
                if (pos.row != expectedRow) {
                    throw new IllegalArgumentException("Piece '" + id + "' is not properly aligned horizontally. " +
                        "Position (" + pos.row + "," + pos.col + ") is not in row " + expectedRow);
                }
            }
            
            // Check for gaps in columns
            if (positions.size() != colSpan) {
                throw new IllegalArgumentException("Piece '" + id + "' has gaps. Expected " + 
                    colSpan + " consecutive cells but found " + positions.size());
            }
            
            // Verify no gaps between min and max columns
            java.util.Set<Integer> columns = new java.util.HashSet<>();
            for (Position pos : positions) {
                columns.add(pos.col);
            }
            for (int col = minCol; col <= maxCol; col++) {
                if (!columns.contains(col)) {
                    throw new IllegalArgumentException("Piece '" + id + "' has a gap at column " + col);
                }
            }
            
        } else { // isVertical
            // For vertical pieces, all positions should have the same column
            int expectedCol = positions.get(0).col;
            for (Position pos : positions) {
                if (pos.col != expectedCol) {
                    throw new IllegalArgumentException("Piece '" + id + "' is not properly aligned vertically. " +
                        "Position (" + pos.row + "," + pos.col + ") is not in column " + expectedCol);
                }
            }
            
            // Check for gaps in rows
            if (positions.size() != rowSpan) {
                throw new IllegalArgumentException("Piece '" + id + "' has gaps. Expected " + 
                    rowSpan + " consecutive cells but found " + positions.size());
            }
            
            // Verify no gaps between min and max rows
            java.util.Set<Integer> rows = new java.util.HashSet<>();
            for (Position pos : positions) {
                rows.add(pos.row);
            }
            for (int row = minRow; row <= maxRow; row++) {
                if (!rows.contains(row)) {
                    throw new IllegalArgumentException("Piece '" + id + "' has a gap at row " + row);
                }
            }
        }
    }
    
    private void determineOrientation() {
        if (positions.size() < 2) {
            this.orientation = Orientation.HORIZONTAL;
            return;
        }

        // Since validation has already been done, we know the piece is linear
        // Just check if it's horizontal or vertical based on the span
        int minRow = positions.stream().mapToInt(p -> p.row).min().orElse(0);
        int maxRow = positions.stream().mapToInt(p -> p.row).max().orElse(0);
        int minCol = positions.stream().mapToInt(p -> p.col).min().orElse(0);
        int maxCol = positions.stream().mapToInt(p -> p.col).max().orElse(0);
        
        int rowSpan = maxRow - minRow + 1;
        int colSpan = maxCol - minCol + 1;

        // Single cell piece defaults to horizontal (shouldn't happen in normal cases)
        if (rowSpan == 1 && colSpan == 1) {
            this.orientation = Orientation.HORIZONTAL;
        } else if (rowSpan == 1) {
            this.orientation = Orientation.HORIZONTAL;
        } else {
            this.orientation = Orientation.VERTICAL;
        }
    }

    /**
     * Copy constructor
     */
    public Piece(Piece other) {
        this.id = other.id;
        this.positions = new java.util.ArrayList<>();
        for (Position pos : other.positions) {
            this.positions.add(new Position(pos));
        }
        this.orientation = other.orientation;
    }
    
    /**
     * Move the piece in the specified direction
     * @param direction 1 for forward (right/down), -1 for backward (left/up)
     */
    public void move(int direction) {
        for (Position pos : positions) {
            if (orientation == Orientation.HORIZONTAL) {
                pos.col += direction;
            } else {
                pos.row += direction;
            }
        }
        sortPositions();
    }
    
    /**
     * Sort positions for consistent ordering
     */
    private void sortPositions() {
        positions.sort((p1, p2) -> {
            if (orientation == Orientation.HORIZONTAL) {
                if (p1.row != p2.row) {
                    return Integer.compare(p1.row, p2.row);
                }
                return Integer.compare(p1.col, p2.col);
            } else {
                if (p1.col != p2.col) {
                    return Integer.compare(p1.col, p2.col);
                }
                return Integer.compare(p1.row, p2.row);
            }
        });
    }
    
    // Getters
    public char getId() { return id; }
    public java.util.List<Position> getPositions() { return positions; }
    public Orientation getOrientation() { return orientation; }
    public int getSize() { return positions.size(); }
    
    @Override
    public String toString() {
        return "Piece{id=" + id + ", orientation=" + orientation + ", positions=" + positions + "}";
    }
}