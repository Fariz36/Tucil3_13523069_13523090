package cli;

import java.io.*;
import java.util.*;

/**
 * Board class representing the Rush Hour puzzle grid state
 */
public class Board {
    private int width;
    private int height;
    private char[][] grid;
    private List<Piece> pieces;
    private Piece primaryPiece;
    private Position exitPosition;
    private Exit exitSide;
    
    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new char[height][width];
        this.pieces = new ArrayList<>();
        
        // Initialize grid with empty cells
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                grid[i][j] = '.';
            }
        }
    }
    
    // Copy constructor for creating board states
    public Board(Board other) {
        this.width = other.width;
        this.height = other.height;
        this.grid = new char[height][width];
        this.pieces = new ArrayList<>();
        this.exitSide = other.exitSide;
        
        // Copy grid
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                grid[i][j] = other.grid[i][j];
            }
        }
        
        // Copy pieces
        for (Piece piece : other.pieces) {
            Piece newPiece = new Piece(piece);
            pieces.add(newPiece);
            if (piece == other.primaryPiece) {
                this.primaryPiece = newPiece;
            }
        }
        
        this.exitPosition = new Position(other.exitPosition);
    }
    
    public static Board readFromFile(String filename) throws IOException {
        FileParser.ParsedBoard parsed = FileParser.parseFile(filename);
        
        Board board = new Board(parsed.cols, parsed.rows);
        
        // Copy grid
        for (int i = 0; i < parsed.rows; i++) {
            for (int j = 0; j < parsed.cols; j++) {
                board.grid[i][j] = parsed.grid[i][j];
            }
        }
        
        // Set exit position (normalize to board coordinates for algorithm)
        if (parsed.exitPosition.row == -1) {
            // Top exit
            board.exitPosition = new Position(0, parsed.exitPosition.col);
            board.exitSide = Exit.TOP;
        } else if (parsed.exitPosition.row == parsed.rows) {
            // Bottom exit
            board.exitPosition = new Position(parsed.rows - 1, parsed.exitPosition.col);
            board.exitSide = Exit.BOTTOM;
        } else if (parsed.exitPosition.col == -1) {
            // Left exit
            board.exitPosition = new Position(parsed.exitPosition.row, 0);
            board.exitSide = Exit.LEFT;
        } else if (parsed.exitPosition.col == parsed.cols) {
            // Right exit
            board.exitPosition = new Position(parsed.exitPosition.row, parsed.cols - 1);
            board.exitSide = Exit.RIGHT;
        } else {
            // Exit is within the grid (should not normally happen)
            board.exitPosition = new Position(parsed.exitPosition);
            board.exitSide = Exit.NONE;
        }
        
        // Parse pieces from grid
        Map<Character, List<Position>> piecePositions = new HashMap<>();
        
        for (int i = 0; i < parsed.rows; i++) {
            for (int j = 0; j < parsed.cols; j++) {
                char c = board.grid[i][j];
                if (c != '.' && c != 'K') {  // Exclude K and dots from pieces
                    // Validate character
                    if (!Character.isLetter(c)) {
                        throw new IOException("Invalid character '" + c + "' at position (" + i + ", " + j + "). Only letters are allowed for pieces.");
                    }
                    if (!Character.isUpperCase(c)) {
                        throw new IOException("Invalid character '" + c + "' at position (" + i + ", " + j + "). Only uppercase letters (A-Z) are allowed for pieces.");
                    }
                    piecePositions.computeIfAbsent(c, k -> new ArrayList<>())
                                 .add(new Position(i, j));
                }
            }
        }
        
        // Create pieces from positions
        int actualPieceCount = 0;
        for (Map.Entry<Character, List<Position>> entry : piecePositions.entrySet()) {
            char id = entry.getKey();
            List<Position> positions = entry.getValue();
            
            try {
                // Piece constructor performs validation
                Piece piece = new Piece(id, positions);
                board.pieces.add(piece);
                actualPieceCount++;
                
                if (id == 'P') {
                    board.primaryPiece = piece;
                }
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid piece configuration: " + e.getMessage());
            }
        }
        
        // Validate
        if (board.primaryPiece == null) {
            throw new IOException("No primary piece (P) found in the board configuration");
        }
        
        // Validate number of pieces (excluding primary piece)
        int actualNonPrimaryPieces = actualPieceCount - 1;
        if (actualNonPrimaryPieces != parsed.numPieces) {
            throw new IOException("Number of non-primary pieces mismatch. Expected " + parsed.numPieces + ", but found " + actualNonPrimaryPieces);
        }
        
        System.out.println("Board loaded successfully:");
        System.out.println("- Primary piece: " + board.primaryPiece.getId());
        System.out.println("- Exit position: " + board.exitPosition);
        System.out.println("- Exit on: " + board.exitSide);
        System.out.println("- Total pieces: " + board.pieces.size());
        
        return board;
    }
    
    public List<Move> getPossibleMoves() {
        List<Move> moves = new ArrayList<>();
        
        for (Piece piece : pieces) {
            // Try moving forward (down for vertical, right for horizontal)
            if (canMovePiece(piece, 1)) {
                moves.add(new Move(piece, piece.getOrientation() == Orientation.HORIZONTAL ? "right" : "down"));
            }
            
            // Try moving backward (up for vertical, left for horizontal)
            if (canMovePiece(piece, -1)) {
                moves.add(new Move(piece, piece.getOrientation() == Orientation.HORIZONTAL ? "left" : "up"));
            }
        }
        
        return moves;
    }
    
/**
 * Check if a piece can move in a given direction
 * With improved exit detection
 */
    private boolean canMovePiece(Piece piece, int direction) {
        List<Position> positions = piece.getPositions();
        Set<Position> currentPositionsSet = new HashSet<>(positions);
        
        // Calculate all new positions
        List<Position> newPositions = new ArrayList<>();
        for (Position pos : positions) {
            Position newPos;
            
            if (piece.getOrientation() == Orientation.HORIZONTAL) {
                newPos = new Position(pos.row, pos.col + direction);
            } else {
                newPos = new Position(pos.row + direction, pos.col);
            }
            
            newPositions.add(newPos);
        }
        
        // Check if any new position is invalid
        for (Position newPos : newPositions) {
            // Special case: primary piece exiting
            if (piece == primaryPiece && exitPosition != null) {
                // Check if this move would reach or exit through the exit
                if (piece.getOrientation() == Orientation.HORIZONTAL) {
                    if (newPos.row == exitPosition.row) {
                        if ((exitSide == Exit.RIGHT && newPos.col >= width) ||
                            (exitSide == Exit.LEFT && newPos.col < 0)) {
                            return true; // Allow exit
                        }
                    }
                } else { // VERTICAL
                    if (newPos.col == exitPosition.col) {
                        if ((exitSide == Exit.BOTTOM && newPos.row >= height) ||
                            (exitSide == Exit.TOP && newPos.row < 0)) {
                            return true; // Allow exit
                        }
                    }
                }
            }
            
            // Normal bounds check
            if (newPos.row < 0 || newPos.row >= height || 
                newPos.col < 0 || newPos.col >= width) {
                return false;
            }
            
            // Check collision with other pieces
            char cellContent = grid[newPos.row][newPos.col];
            
            // Cell must be empty or occupied by the current piece
            if (cellContent != '.' && cellContent != piece.getId()) {
                return false;
            }
            
            // If occupied by the same piece, make sure it's a position that will be vacated
            if (cellContent == piece.getId() && !currentPositionsSet.contains(newPos)) {
                return false;
            }
        }
        
        return true;
    }
    
/**
 * Make a move on the board, with improved exit handling
 */
    public Board makeMove(Move move) {
        Board newBoard = new Board(this);
        Piece piece = newBoard.getPieceById(move.getPiece().getId());
        int direction = move.getDirectionValue();
        
        // Check if this is an exit move for the primary piece
        boolean isExitMove = false;
        if (piece == newBoard.primaryPiece) {
            if (piece.getOrientation() == Orientation.HORIZONTAL) {
                if ((exitSide == Exit.RIGHT && direction > 0) || 
                    (exitSide == Exit.LEFT && direction < 0)) {
                    // Check if this move would position the primary piece at the exit
                    for (Position pos : piece.getPositions()) {
                        Position newPos = new Position(pos.row, pos.col + direction);
                        if ((exitSide == Exit.RIGHT && newPos.col >= width && pos.row == exitPosition.row) ||
                            (exitSide == Exit.LEFT && newPos.col < 0 && pos.row == exitPosition.row)) {
                            isExitMove = true;
                            break;
                        }
                    }
                }
            } else { // VERTICAL
                if ((exitSide == Exit.BOTTOM && direction > 0) || 
                    (exitSide == Exit.TOP && direction < 0)) {
                    // Check if this move would position the primary piece at the exit
                    for (Position pos : piece.getPositions()) {
                        Position newPos = new Position(pos.row + direction, pos.col);
                        if ((exitSide == Exit.BOTTOM && newPos.row >= height && pos.col == exitPosition.col) ||
                            (exitSide == Exit.TOP && newPos.row < 0 && pos.col == exitPosition.col)) {
                            isExitMove = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // Clear current positions
        for (Position pos : piece.getPositions()) {
            if (pos.row >= 0 && pos.row < height && pos.col >= 0 && pos.col < width) {
                newBoard.grid[pos.row][pos.col] = '.';
            }
        }
        
        // Update piece positions
        piece.move(direction);
        
        // Only set new positions if it's not an exit move for primary piece
        if (!isExitMove) {
            // Set new positions (only if within bounds)
            for (Position pos : piece.getPositions()) {
                if (pos.row >= 0 && pos.row < height && pos.col >= 0 && pos.col < width) {
                    newBoard.grid[pos.row][pos.col] = piece.getId();
                }
            }
        } else {
            // For exit moves, leave primary piece positions empty
            // Remove primary piece from the list to ensure it won't be considered again
            newBoard.pieces.remove(piece);
            newBoard.primaryPiece = null; // Clear primary piece reference
        }
        
        return newBoard;
    }
    
    public boolean isSolved() {
        if (primaryPiece == null || exitPosition == null) {
            return false;
        }
        
        // Get positions of the primary piece
        List<Position> primaryPositions = primaryPiece.getPositions();
        
        // Check based on exit side and primary piece orientation
        if (primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
            // For horizontal primary piece, check if it's at exit
            if (exitSide == Exit.RIGHT) {
                // Find rightmost position of the piece
                Position rightmost = primaryPositions.stream()
                    .max(Comparator.comparingInt(p -> p.col))
                    .orElse(primaryPositions.get(0));
                
                // Primary piece solved if rightmost position is at the rightmost cell AND same row as exit
                return rightmost.col == width - 1 && rightmost.row == exitPosition.row;
            } 
            else if (exitSide == Exit.LEFT) {
                // Find leftmost position of the piece
                Position leftmost = primaryPositions.stream()
                    .min(Comparator.comparingInt(p -> p.col))
                    .orElse(primaryPositions.get(0));
                
                // Primary piece solved if leftmost position is at the leftmost cell AND same row as exit
                return leftmost.col == 0 && leftmost.row == exitPosition.row;
            }
        } 
        else if (primaryPiece.getOrientation() == Orientation.VERTICAL) {
            // For vertical primary piece, check if it's at exit
            if (exitSide == Exit.BOTTOM) {
                // Find bottommost position of the piece
                Position bottommost = primaryPositions.stream()
                    .max(Comparator.comparingInt(p -> p.row))
                    .orElse(primaryPositions.get(0));
                
                // Primary piece solved if bottommost position is at the bottom cell AND same column as exit
                return bottommost.row == height - 1 && bottommost.col == exitPosition.col;
            } 
            else if (exitSide == Exit.TOP) {
                // Find topmost position of the piece
                Position topmost = primaryPositions.stream()
                    .min(Comparator.comparingInt(p -> p.row))
                    .orElse(primaryPositions.get(0));
                
                // Primary piece solved if topmost position is at the top cell AND same column as exit
                return topmost.row == 0 && topmost.col == exitPosition.col;
            }
        }
        
        return false;
    }
    
    public void display() {
        // Use ANSI colors for better visualization
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";     // Primary piece
        String GREEN = "\u001B[32m";   // Exit
        
        // Display top exit if exists
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    System.out.print(GREEN + "K" + RESET);
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Display board content
        for (int i = 0; i < height; i++) {
            // Left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                System.out.print(GREEN + "K" + RESET);
            }
            
            // Board content
            for (int j = 0; j < width; j++) {
                char c = grid[i][j];
                if (c == 'P') {
                    System.out.print(RED + c + RESET);
                } else {
                    System.out.print(c);
                }
            }
            
            // Right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                System.out.print(GREEN + "K" + RESET);
            }
            
            System.out.println();
        }
        
        // Display bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    System.out.print(GREEN + "K" + RESET);
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }
    
    public void displayWithMove(Move lastMove) {
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";     // Primary piece
        String GREEN = "\u001B[32m";   // Exit
        String YELLOW = "\u001B[33m";  // Moving piece
        
        char movingPieceId = lastMove != null ? lastMove.getPiece().getId() : '\0';
        
        // Display top exit if exists
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    System.out.print(GREEN + "K" + RESET);
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Display board content
        for (int i = 0; i < height; i++) {
            // Left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                System.out.print(GREEN + "K" + RESET);
            }
            
            // Board content
            for (int j = 0; j < width; j++) {
                char c = grid[i][j];
                if (c == 'P') {
                    // Primary piece always stays red
                    System.out.print(RED + c + RESET);
                } else if (c == movingPieceId) {
                    // Other moving pieces are yellow
                    System.out.print(YELLOW + c + RESET);
                } else {
                    System.out.print(c);
                }
            }
            
            // Right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                System.out.print(GREEN + "K" + RESET);
            }
            
            System.out.println();
        }
        
        // Display bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    System.out.print(GREEN + "K" + RESET);
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }
    
    public String getStateString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                sb.append(grid[i][j]);
            }
        }
        return sb.toString();
    }
    
    private Piece getPieceById(char id) {
        for (Piece piece : pieces) {
            if (piece.getId() == id) {
                return piece;
            }
        }
        return null;
    }
    
    public boolean isPrimaryPieceAlignedWithExit() {
        if (primaryPiece == null || exitPosition == null) {
            return false;
        }
        
        // Check alignment based on primary piece orientation and exit position
        if (primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
            // For horizontal primary piece, exit must be on left or right AND same row
            if ((exitSide == Exit.LEFT || exitSide == Exit.RIGHT)) {
                // Check if primary piece shares the same row with exit
                for (Position pos : primaryPiece.getPositions()) {
                    if (pos.row == exitPosition.row) {
                        return true;
                    }
                }
            }
        } else { // VERTICAL
            // For vertical primary piece, exit must be on top or bottom AND same column
            if ((exitSide == Exit.TOP || exitSide == Exit.BOTTOM)) {
                // Check if primary piece shares the same column with exit
                for (Position pos : primaryPiece.getPositions()) {
                    if (pos.col == exitPosition.col) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
 * Display board with improved visualization for exit state
 */
    public void displayWithFinalState() {
        // Use ANSI colors for better visualization
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";     // Primary piece
        String GREEN = "\u001B[32m";   // Exit
        
        // Display top exit if exists
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    // Show primary piece in the exit
                    if (primaryPiece != null && primaryPiece.getOrientation() == Orientation.VERTICAL) {
                        System.out.print(RED + "P" + RESET);
                    } else {
                        System.out.print(GREEN + "K" + RESET);
                    }
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Display board content
        for (int i = 0; i < height; i++) {
            // Left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                // Show primary piece in the exit
                if (primaryPiece != null && primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
                    System.out.print(RED + "P" + RESET);
                } else {
                    System.out.print(GREEN + "K" + RESET);
                }
            }
            
            // Board content
            for (int j = 0; j < width; j++) {
                char c = grid[i][j];
                if (c == 'P') {
                    System.out.print(RED + c + RESET);
                } else {
                    System.out.print(c);
                }
            }
            
            // Right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                // Show primary piece in the exit for the final state
                if (this.isSolved() || primaryPiece == null) {
                    System.out.print(RED + "P" + RESET);
                } else {
                    System.out.print(GREEN + "K" + RESET);
                }
            }
            
            System.out.println();
        }
        
        // Display bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    // Show primary piece in the exit
                    if (primaryPiece != null && primaryPiece.getOrientation() == Orientation.VERTICAL) {
                        System.out.print(RED + "P" + RESET);
                    } else {
                        System.out.print(GREEN + "K" + RESET);
                    }
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }

    /**
     * Display board with primary piece shown next to exit K
     */
    public void displayWithExitedPiece() {
        // Use ANSI colors for better visualization
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";     // Primary piece
        String GREEN = "\u001B[32m";   // Exit
        
        // Display top exit if exists
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    // Show K with P outside
                    System.out.print(GREEN + "K" + RED + "P" + RESET);
                    j++; // Skip one column as we printed two characters
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        
        // Display board content
        for (int i = 0; i < height; i++) {
            // Left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                // Show P outside, then K
                System.out.print(RED + "P" + GREEN + "K" + RESET);
            }
            
            // Board content
            for (int j = 0; j < width; j++) {
                char c = grid[i][j];
                // Don't show P in the board for final state (it's exited)
                if (c == 'P' && this.isSolved()) {
                    System.out.print('.');
                } else if (c == 'P') {
                    System.out.print(RED + c + RESET);
                } else {
                    System.out.print(c);
                }
            }
            
            // Right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                // Show K then P outside
                System.out.print(GREEN + "K" + RED + "P" + RESET);
            }
            
            System.out.println();
        }
        
        // Display bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    // Show K with P outside
                    System.out.print(GREEN + "K" + RED + "P" + RESET);
                    j++; // Skip one column as we printed two characters
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }
    
    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Piece> getPieces() { return pieces; }
    public Piece getPrimaryPiece() { return primaryPiece; }
    public Position getExitPosition() { return exitPosition; }
    public Exit getExitSide() { return exitSide; }
    public char[][] getGrid() { return grid; }
    
    public char getGridAt(int row, int col) {
        if (row >= 0 && row < height && col >= 0 && col < width) {
            return grid[row][col];
        }
        return ' ';
    }
}