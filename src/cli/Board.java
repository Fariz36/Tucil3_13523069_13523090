package cli;

import java.io.*;
import java.util.*;

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
        } else if (parsed.exitPosition.row == parsed.rows) {
            // Bottom exit
            board.exitPosition = new Position(parsed.rows - 1, parsed.exitPosition.col);
        } else if (parsed.exitPosition.col == -1) {
            // Left exit
            board.exitPosition = new Position(parsed.exitPosition.row, 0);
        } else if (parsed.exitPosition.col == parsed.cols) {
            // Right exit
            board.exitPosition = new Position(parsed.exitPosition.row, parsed.cols - 1);
        } else {
            // Exit is within the grid
            board.exitPosition = new Position(parsed.exitPosition);
        }
        
        // Store the actual exit side for display purposes
        if (parsed.exitPosition.row == -1) {
            board.exitSide = Exit.TOP;
        } else if (parsed.exitPosition.row == parsed.rows) {
            board.exitSide = Exit.BOTTOM;
        } else if (parsed.exitPosition.col == -1) {
            board.exitSide = Exit.LEFT;
        } else if (parsed.exitPosition.col == parsed.cols) {
            board.exitSide = Exit.RIGHT;
        } else {
            board.exitSide = Exit.NONE;
        }
        
        // Parse pieces from grid
        Map<Character, List<Position>> piecePositions = new HashMap<>();
        Set<Character> validPieceChars = new HashSet<>();
        
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
                    validPieceChars.add(c);
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
                // Piece constructor now performs validation automatically
                Piece piece = new Piece(id, positions);
                board.pieces.add(piece);
                actualPieceCount++;
                
                if (id == 'P') {
                    board.primaryPiece = piece;
                }
            } catch (IllegalArgumentException e) {
                // Convert IllegalArgumentException to IOException for consistency
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
        System.out.println("- Exit on: " + getExitSide(parsed.exitPosition, parsed.rows, parsed.cols));
        System.out.println("- Total pieces: " + board.pieces.size());
        
        return board;
    }
    
    private static String getExitSide(Position exitPos, int rows, int cols) {
        if (exitPos.row == -1) return "TOP";
        if (exitPos.row == rows) return "BOTTOM";
        if (exitPos.col == -1) return "LEFT";
        if (exitPos.col == cols) return "RIGHT";
        return "UNKNOWN";
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
                if (piece.getOrientation() == Orientation.HORIZONTAL && newPos.row == exitPosition.row) {
                    if ((exitPosition.col == width - 1 && newPos.col == width) ||
                        (exitPosition.col == 0 && newPos.col == -1)) {
                        continue; // Allow exit
                    }
                } else if (piece.getOrientation() == Orientation.VERTICAL && newPos.col == exitPosition.col) {
                    if ((exitPosition.row == height - 1 && newPos.row == height) ||
                        (exitPosition.row == 0 && newPos.row == -1)) {
                        continue; // Allow exit
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
    
    private Position getFrontPosition(Piece piece, int direction) {
        List<Position> positions = piece.getPositions();
        if (piece.getOrientation() == Orientation.HORIZONTAL) {
            return direction > 0 ? positions.get(positions.size() - 1) : positions.get(0);
        } else {
            return direction > 0 ? positions.get(positions.size() - 1) : positions.get(0);
        }
    }
    
    private Position getBackPosition(Piece piece, int direction) {
        List<Position> positions = piece.getPositions();
        if (piece.getOrientation() == Orientation.HORIZONTAL) {
            return direction > 0 ? positions.get(0) : positions.get(positions.size() - 1);
        } else {
            return direction > 0 ? positions.get(0) : positions.get(positions.size() - 1);
        }
    }
    
    public Board makeMove(Move move) {
        Board newBoard = new Board(this);
        Piece piece = newBoard.getPieceById(move.getPiece().getId());
        int direction = move.getDirectionValue();
        
        // Clear current positions
        for (Position pos : piece.getPositions()) {
            if (pos.row >= 0 && pos.row < height && pos.col >= 0 && pos.col < width) {
                newBoard.grid[pos.row][pos.col] = '.';
            }
        }
        
        // Update piece positions
        piece.move(direction);
        
        // Set new positions (only if within bounds)
        for (Position pos : piece.getPositions()) {
            if (pos.row >= 0 && pos.row < height && pos.col >= 0 && pos.col < width) {
                newBoard.grid[pos.row][pos.col] = piece.getId();
            }
        }
        
        return newBoard;
    }
    
    public boolean isSolved() {
        if (primaryPiece == null || exitPosition == null) {
            return false;
        }
        
        // Check if primary piece has reached or passed the exit
        for (Position pos : primaryPiece.getPositions()) {
            // Check if piece is at the exit position
            if (pos.equals(exitPosition)) {
                return true;
            }
            
            // Check if piece has passed through the exit
            if (exitPosition.row == 0 && pos.row < 0 && pos.col == exitPosition.col) {
                return true; // Exited top
            } else if (exitPosition.row == height - 1 && pos.row >= height && pos.col == exitPosition.col) {
                return true; // Exited bottom
            } else if (exitPosition.col == 0 && pos.col < 0 && pos.row == exitPosition.row) {
                return true; // Exited left
            } else if (exitPosition.col == width - 1 && pos.col >= width && pos.row == exitPosition.row) {
                return true; // Exited right
            }
        }
        
        return false;
    }
    
    private boolean isAtExit(Position pos) {
        if (exitPosition == null) {
            return false;
        }
        
        // Exit position rules based on primary piece orientation
        if (primaryPiece.getOrientation() == Orientation.HORIZONTAL) {
            // For horizontal pieces, exit must be on the left or right edge
            if ((exitPosition.col == 0 || exitPosition.col == width - 1) && 
                exitPosition.row == pos.row) {
                
                // Check if the piece is adjacent to exit
                if (exitPosition.col == 0) {
                    // Exit on left - check if piece can exit
                    return pos.col == 0;
                } else {
                    // Exit on right - check if piece can exit
                    return pos.col == width - 1;
                }
            }
        } else {
            // For vertical pieces, exit must be on the top or bottom edge
            if ((exitPosition.row == 0 || exitPosition.row == height - 1) && 
                exitPosition.col == pos.col) {
                
                // Check if the piece is adjacent to exit
                if (exitPosition.row == 0) {
                    // Exit on top - check if piece can exit
                    return pos.row == 0;
                } else {
                    // Exit on bottom - check if piece can exit
                    return pos.row == height - 1;
                }
            }
        }
        
        return false;
    }
    
    public void display() {
        // Use ANSI colors for better visualization
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";     // Primary piece
        String GREEN = "\u001B[32m";   // Exit
        String YELLOW = "\u001B[33m";  // Moving piece (will be used in solution display)
        
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
    
    
    public Piece getPrimaryPiece() { return primaryPiece; }
    public Position getExitPosition() { return exitPosition; }
    public Exit getExitSide() { return exitSide; }
    
    // Debug methods
    public boolean canMovePieceDebug(Piece piece, int direction) {
        System.out.println("DEBUG: Checking move for " + piece.getId() + " direction: " + direction);
        boolean result = canMovePiece(piece, direction);
        System.out.println("DEBUG: Result: " + result);
        return result;
    }
    
    public Piece getPieceByIdDebug(char id) {
        for (Piece piece : pieces) {
            if (piece.getId() == id) {
                return piece;
            }
        }
        return null;
    }
    
    public char getGridAt(int row, int col) {
        if (row >= 0 && row < height && col >= 0 && col < width) {
            return grid[row][col];
        }
        return ' ';
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
    
    // Original getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<Piece> getPieces() { return pieces; }
    public char[][] getGrid() { return grid; }
}