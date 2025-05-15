package cli;

import java.io.*;
import java.util.*;

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
            if (numPieces <= 0) {
                throw new IOException("Number of pieces cannot be less than or equal to 0, got: " + numPieces);
            }
            
            // Read all remaining lines
            List<String> allLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
            
            // Initialize variables
            char[][] grid = new char[rows][cols];
            Position exitPosition = null;
            Orientation exitOrientation = null;
            int currentRow = 0;
            int lineIndex = 0;
            
            // Check for top exit (K in a line by itself before grid)
            if (lineIndex < allLines.size() && allLines.get(lineIndex).contains("K") && 
                !allLines.get(lineIndex).contains("A") && !allLines.get(lineIndex).contains("B")) {
                String topLine = allLines.get(lineIndex);
                for (int j = 0; j < topLine.length(); j++) {
                    if (topLine.charAt(j) == 'K') {
                        // Validate that K is within the board's column bounds
                        if (j >= cols) {
                            throw new IOException("Exit position 'K' at TOP is outside board boundaries. Column " + j + " is beyond board width " + cols);
                        }
                        exitPosition = new Position(-1, j);
                        exitOrientation = Orientation.VERTICAL;
                        System.out.println("Found exit K at TOP, column " + j);
                        break;
                    }
                }
                lineIndex++; // Skip this line for grid processing
            }
            
            // Process grid lines
            while (currentRow < rows && lineIndex < allLines.size()) {
                String currentLine = allLines.get(lineIndex);
                String processedLine = currentLine;
                
                // Check for K at the beginning (left exit)
                if (currentLine.startsWith("K") && currentLine.length() == cols + 1) {
                    if (exitPosition == null) {
                        exitPosition = new Position(currentRow, -1);
                        exitOrientation = Orientation.HORIZONTAL;
                        System.out.println("Found exit K at LEFT, row " + currentRow);
                    }
                    processedLine = currentLine.substring(1);
                }
                // Check for K at the end (right exit)
                else if (currentLine.length() == cols + 1 && currentLine.charAt(cols) == 'K') {
                    if (exitPosition == null) {
                        exitPosition = new Position(currentRow, cols);
                        exitOrientation = Orientation.HORIZONTAL;
                        System.out.println("Found exit K at RIGHT, row " + currentRow);
                    }
                    processedLine = currentLine.substring(0, cols);
                }
                
                // Validate processed line length
                if (processedLine.length() != cols) {
                    throw new IOException("Row " + currentRow + " has incorrect length. Expected " + cols + ", got " + processedLine.length());
                }
                
                // Validate characters in processed line
                for (int j = 0; j < processedLine.length(); j++) {
                    char c = processedLine.charAt(j);
                    if (c != '.' && !Character.isLetter(c)) {
                        throw new IOException("Invalid character '" + c + "' at row " + currentRow + ", column " + j + ". Only letters (A-Z), dots (.), are allowed.");
                    }
                    // Additional check for uppercase letters only (optional)
                    if (Character.isLetter(c) && !Character.isUpperCase(c)) {
                        throw new IOException("Invalid character '" + c + "' at row " + currentRow + ", column " + j + ". Only uppercase letters (A-Z) are allowed.");
                    }
                    grid[currentRow][j] = c;
                }
                
                currentRow++;
                lineIndex++;
            }
            
            // Check for bottom exit (K in a line by itself after grid)
            if (lineIndex < allLines.size() && exitPosition == null) {
                String bottomLine = allLines.get(lineIndex);
                for (int j = 0; j < bottomLine.length(); j++) {
                    if (bottomLine.charAt(j) == 'K') {
                        // Validate that K is within the board's column bounds
                        if (j >= cols) {
                            throw new IOException("Exit position 'K' at BOTTOM is outside board boundaries. Column " + j + " is beyond board width " + cols);
                        }
                        exitPosition = new Position(rows, j);
                        exitOrientation = Orientation.VERTICAL;
                        System.out.println("Found exit K at BOTTOM, column " + j);
                        break;
                    }
                }
            }
            
            if (exitPosition == null) {
                throw new IOException("No exit position (K) found in the board configuration");
            }
            
            // Additional validation: check if exit position is detached from board
            if (exitPosition.row == -1 || exitPosition.row == rows) {
                // Top or bottom exit - check if there are any empty lines between K and the board
                if (exitPosition.row == -1 && lineIndex > 1) {
                    // There might be empty lines before the board
                    for (int i = 0; i < lineIndex - 1; i++) {
                        if (allLines.get(i).trim().isEmpty()) {
                            throw new IOException("Exit position 'K' is detached from the board. There cannot be empty lines between K and the board.");
                        }
                    }
                }
                if (exitPosition.row == rows && lineIndex < allLines.size() - 1) {
                    // Check if there are lines after the exit
                    for (int i = lineIndex + 1; i < allLines.size(); i++) {
                        if (!allLines.get(i).trim().isEmpty()) {
                            throw new IOException("Exit position 'K' is not attached to the board. Found non-empty lines after exit.");
                        }
                    }
                }
            }
            
            // Validate that we have enough rows
            if (currentRow < rows) {
                throw new IOException("Not enough rows in file. Expected " + rows + ", got " + currentRow);
            }
            
            return new ParsedBoard(rows, cols, grid, exitPosition, exitOrientation, numPieces);
            
        } finally {
            reader.close();
        }
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
