package cli;

import java.io.*;
import java.util.*;

/**
 * FileParser class for parsing Rush Hour puzzle files with specific exit placement rules
 */
public class FileParser {
    
    public static ParsedBoard parseFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        
        try {
            // Dimensions
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException("File is empty");
            }
            firstLine = firstLine.trim();
            
            String[] dimensions = firstLine.split("\\s+");
            if (dimensions.length != 2) {
                throw new IOException("First line must contain exactly 2 integers for board dimensions");
            }
            
            int rows, cols;
            try {
                rows = Integer.parseInt(dimensions[0]);
                cols = Integer.parseInt(dimensions[1]);
            } catch (NumberFormatException e) {
                throw new IOException("First line must contain valid integers for board dimensions");
            }
            
            // Validate dimensions
            if (rows <= 0) {
                throw new IOException("Number of rows must be greater than 0, got: " + rows);
            }
            if (cols <= 0) {
                throw new IOException("Number of columns must be greater than 0, got: " + cols);
            }
            
            if (rows == 1 && cols == 1) {
                throw new IOException("Board size must be at least 1x2 or 2x1. Current size: " + rows + "x" + cols);
            }
            
            // Read number of pieces
            String secondLine = reader.readLine();
            if (secondLine == null) {
                throw new IOException("Missing second line for number of pieces");
            }
            secondLine = secondLine.trim();
            
            int numPieces;
            try {
                numPieces = Integer.parseInt(secondLine);
            } catch (NumberFormatException e) {
                throw new IOException("Second line must contain a valid integer for number of pieces");
            }
            
            // Validate number of pieces
            if (numPieces < 1) {
                throw new IOException("Number of pieces cannot be less than 1, got: " + numPieces);
            }
            
            // Read all remaining lines
            List<String> allLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
            
            // Exit position and count
            Position exitPosition = null;
            Orientation exitOrientation = null;
            int exitCount = 0;
            List<String> exitPositionDescriptions = new ArrayList<>();
            boolean hasLeftK = false;
            int leftKRow = -1;
            
            // First pass: Check for exit positions and count total exits
            for (int i = 0; i < allLines.size(); i++) {
                String currentLine = allLines.get(i);
                if (currentLine.trim().isEmpty()) continue;
                
                if (currentLine.contains("K")) {
                    // Check if this is a top K (before board)
                    if (i == 0) {
                        // If the first line has only K or starts with K and has no other letters, it's a top exit
                        if (currentLine.trim().equals("K") || 
                            (currentLine.indexOf('K') >= 0 && !containsAnyPiece(currentLine))) {
                            
                            int kPosition = currentLine.indexOf('K');
                            
                            // Check if K is within the board's column range (0 to cols-1)
                            if (kPosition >= cols) {
                                throw new IOException("Invalid exit position 'K' at position (" + 
                                                    i + ", " + kPosition + "). Top K must be within the board's column range (0 to " + 
                                                    (cols-1) + ").");
                            }
                            
                            exitCount++;
                            exitPositionDescriptions.add("TOP (column " + kPosition + ")");
                            exitPosition = new Position(-1, kPosition);
                            exitOrientation = Orientation.VERTICAL;
                        } 
                        // Otherwise, treat it as a normal board row that could have left or right K
                        else {
                            if (currentLine.startsWith("K")) {
                                exitCount++;
                                exitPositionDescriptions.add("LEFT (row " + i + ")");
                                exitPosition = new Position(i, -1);
                                exitOrientation = Orientation.HORIZONTAL;
                                hasLeftK = true;
                                leftKRow = i;
                            }
                            // Check for right K (including corner cases)
                            else if (currentLine.endsWith("K") || 
                                    (currentLine.length() > cols && currentLine.charAt(cols) == 'K') ||
                                    (currentLine.length() >= cols && currentLine.charAt(currentLine.length() - 1) == 'K')) {
                                
                                exitCount++;
                                // Check if it's a corner K
                                if (currentLine.length() >= cols && currentLine.charAt(currentLine.length() - 1) == 'K') {
                                    String cornerType = "UPPER RIGHT CORNER";
                                    exitPositionDescriptions.add(cornerType + " (row " + i + ")");
                                } else {
                                    exitPositionDescriptions.add("RIGHT (row " + i + ")");
                                }
                                
                                exitPosition = new Position(i, cols);
                                exitOrientation = Orientation.HORIZONTAL;
                            }
                            // K inside the board (invalid)
                            else {
                                int kPosition = currentLine.indexOf('K');
                                throw new IOException("Invalid exit position 'K' found inside the board at position (" + 
                                                    i + ", " + kPosition + "). Exit must be placed on the edge of the board.");
                            }
                        }
                    }
                    // Check if this is a bottom K (after all board rows)
                    else if (i >= rows) {
                        int kPosition = currentLine.indexOf('K');
                        
                        // Check if K is within the board's column range (0 to cols-1)
                        if (kPosition >= cols) {
                            throw new IOException("Invalid exit position 'K' at position (" + 
                                                i + ", " + kPosition + "). Bottom K must be within the board's column range (0 to " + 
                                                (cols-1) + ").");
                        }
                        
                        exitCount++;
                        exitPositionDescriptions.add("BOTTOM (column " + kPosition + ")");
                        exitPosition = new Position(rows, kPosition);
                        exitOrientation = Orientation.VERTICAL;
                    }
                    // Check for left K
                    else if (currentLine.startsWith("K")) {
                        exitCount++;
                        exitPositionDescriptions.add("LEFT (row " + i + ")");
                        exitPosition = new Position(i, -1);
                        exitOrientation = Orientation.HORIZONTAL;
                        hasLeftK = true;
                        leftKRow = i;
                    }
                    // Check for right K (including corner cases)
                    else if (currentLine.endsWith("K") || 
                            (currentLine.length() > cols && currentLine.charAt(cols) == 'K') ||
                            (currentLine.length() == cols && currentLine.charAt(cols - 1) == 'K')) {
                        
                        exitCount++;
                        // Check if it's a corner K
                        if (currentLine.length() == cols && currentLine.charAt(cols - 1) == 'K') {
                            String cornerType = (i == 0) ? "UPPER RIGHT CORNER" : 
                                              (i == rows - 1) ? "LOWER RIGHT CORNER" : "RIGHT CORNER";
                            exitPositionDescriptions.add(cornerType + " (row " + i + ")");
                        } else {
                            exitPositionDescriptions.add("RIGHT (row " + i + ")");
                        }
                        
                        exitPosition = new Position(i, cols);
                        exitOrientation = Orientation.HORIZONTAL;
                    }
                    // K inside the board (invalid)
                    else {
                        int kPosition = currentLine.indexOf('K');
                        throw new IOException("Invalid exit position 'K' found inside the board at position (" + 
                                            i + ", " + kPosition + "). Exit must be placed on the edge of the board.");
                    }
                }
            }
            
            // Check if we found an exit
            if (exitPosition == null) {
                throw new IOException("No exit position (K) found in the board configuration");
            }
            
            // Check if we have more than one exit
            if (exitCount > 1) {
                throw new IOException("Multiple exit positions (K) found: " + String.join(", ", exitPositionDescriptions) + 
                                    ". The Rush Hour puzzle must have exactly one exit position.");
            }
            
            // Second pass: Process the board
            char[][] grid = new char[rows][cols];
            int currentRow = 0;
            int lineIndex = 0;
            
            // Skip top exit line if it exists
            if (exitPosition.row == -1) {
                lineIndex = 1;
            }
            
            // Initialize grid with empty cells
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    grid[i][j] = '.';
                }
            }
            
            // Process the board rows
            while (currentRow < rows && lineIndex < allLines.size()) {
                String currentLine = allLines.get(lineIndex);
                
                // Skip empty lines
                if (currentLine.trim().isEmpty()) {
                    lineIndex++;
                    continue;
                }
                
                String processedLine = currentLine;
                boolean hasRightK = false;
                
                // Special handling for left K
                if (hasLeftK) {
                    // If this is the row with K, remove the K for grid processing
                    if (lineIndex == leftKRow) {
                        processedLine = currentLine.substring(1);
                    }
                    // All other rows should have exactly one leading space
                    else {
                        if (!currentLine.startsWith(" ") || currentLine.startsWith("  ")) {
                            throw new IOException("Invalid format: When K is at the left, all other rows must have exactly one leading space. Row " + 
                                                (lineIndex + 1) + " has incorrect spacing.");
                        }
                        processedLine = currentLine.substring(1); // Remove leading space
                    }
                }
                
                // If this line has a right K, remove it for grid processing
                if (currentLine.endsWith("K")) {
                    processedLine = currentLine.substring(0, currentLine.length() - 1);
                    hasRightK = true;
                } else if (currentLine.length() > cols && currentLine.charAt(cols) == 'K') {
                    processedLine = currentLine.substring(0, cols);
                    hasRightK = true;
                }
                
                // Special case: Check for corner K (K at the end of line)
                if (!hasRightK && currentLine.length() == cols && currentLine.charAt(cols - 1) == 'K') {
                    // This is a corner K - it's treated as a right edge exit
                    processedLine = processedLine.substring(0, cols - 1) + ".";
                    hasRightK = true;
                }
                
                // Validate processed line length
                if (processedLine.length() > cols) {
                    processedLine = processedLine.substring(0, cols); // Truncate if too long
                }
                else if (processedLine.length() < cols) {
                    throw new IOException("Row " + (lineIndex + 1) + " has incorrect length. Expected " + cols + 
                                        ", got " + processedLine.length() + ". Line: \"" + processedLine + "\"");
                }
                
                // Map the grid cells
                for (int j = 0; j < processedLine.length(); j++) {
                    char c = processedLine.charAt(j);
                    
                    // Validate character
                    if (c != '.' && !Character.isLetter(c)) {
                        throw new IOException("Invalid character '" + c + "' at row " + (lineIndex + 1) + 
                                            ", column " + (j + 1) + ". Only letters (A-Z) and dots (.) are allowed.");
                    }
                    
                    // Ensure uppercase letters
                    if (Character.isLetter(c) && !Character.isUpperCase(c)) {
                        throw new IOException("Invalid character '" + c + "' at row " + (lineIndex + 1) + 
                                            ", column " + (j + 1) + ". Only uppercase letters (A-Z) are allowed.");
                    }
                    
                    // K should have been handled earlier (replaced with .)
                    if (c == 'K' && !hasRightK) {
                        throw new IOException("Invalid exit position 'K' found inside the board at position (" + 
                                            (lineIndex + 1) + ", " + (j + 1) + "). Exit must be placed on the edge of the board.");
                    }
                    
                    grid[currentRow][j] = c;
                }
                
                currentRow++;
                lineIndex++;
            }
            
            // Validate that we have enough rows
            if (currentRow < rows) {
                throw new IOException("Not enough rows in file. Expected " + rows + ", got " + currentRow);
            }
            
            // No additional validation for K attachment to pieces needed
            
            return new ParsedBoard(rows, cols, grid, exitPosition, exitOrientation, numPieces);
            
        } finally {
            reader.close();
        }
    }
    
    /**
     * Helper method to check if a line contains any piece character
     */
    private static boolean containsAnyPiece(String line) {
        for (char c : line.toCharArray()) {
            if (Character.isLetter(c) && c != 'K') {
                return true;
            }
        }
        return false;
    }
    
    // Inner class to hold parsed data
    public static class ParsedBoard {
        public final int rows;
        public final int cols;
        public final char[][] grid;
        public final Position exitPosition;
        public final Orientation exitOrientation;
        public final int numPieces;
        
        public ParsedBoard(int rows, int cols, char[][] grid, Position exitPosition, 
                          Orientation exitOrientation, int numPieces) {
            this.rows = rows;
            this.cols = cols;
            this.grid = grid;
            this.exitPosition = exitPosition;
            this.exitOrientation = exitOrientation;
            this.numPieces = numPieces;
        }
    }
}