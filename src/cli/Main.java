package cli;

import java.io.*;
import java.util.*;

/**
 * Main class for the Rush Hour puzzle solver
 * This version supports compound moves as shown in the sample image
 */
public class Main {
    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Rush Hour Puzzle Solver ===");
        System.out.print("Enter the filename (without .txt extension): ");
        String filename = scanner.nextLine().trim();
        
        // Add .txt extension if not present
        if (!filename.endsWith(".txt")) {
            filename = filename + ".txt";
        }
        
        // Use relative path to test/input folder
        String filepath = "test/input/" + filename;
        
        try {
            // Read the puzzle configuration
            System.out.println("Reading file: " + filepath);
            Board board = Board.readFromFile(filepath);
            System.out.println("\nInitial Board State:");
            board.display();
            
            // Check if primary piece is aligned with exit
            if (!board.isPrimaryPieceAlignedWithExit()) {
                System.out.println();
                System.out.println(YELLOW + "WARNING: Primary piece (P) and exit position (K) are not aligned!" + RESET);
                System.out.println(YELLOW + "The primary piece may not be able to reach the exit with this configuration." + RESET);
                System.out.print("Do you want to continue anyway? (y/n): ");
                
                String continueChoice = getYesNoChoice(scanner);
                
                if (!continueChoice.equalsIgnoreCase("y")) {
                    System.out.println("Exiting program...");
                    scanner.close();
                    return;
                }
            }
            
            System.out.println("\nChoose algorithm:");
            System.out.println("1. Uniform Cost Search (UCS)");
            System.out.println("2. Greedy Best First Search");
            System.out.println("3. A* Search");
            System.out.print("Enter your choice (1-3): ");
            
            int algorithmChoice = getAlgorithmChoice(scanner);
            
            // Ask for heuristic if using Greedy or A*
            String heuristic = "manhattan";
            if (algorithmChoice == 2 || algorithmChoice == 3) {
                System.out.println("\nChoose heuristic:");
                System.out.println("1. Manhattan Distance");
                System.out.println("2. Direct Distance");
                System.out.println("3. Blocking Count");
                System.out.print("Enter your choice (1-3): ");
                
                int heuristicChoice = getHeuristicChoice(scanner);
                switch (heuristicChoice) {
                    case 1: heuristic = "manhattan"; break;
                    case 2: heuristic = "direct"; break;
                    case 3: heuristic = "blocking"; break;
                    default: heuristic = "manhattan";
                }
            }
            
            // Solve the puzzle
            Solver solver = new Solver();
            Solution solution = null;
            String algorithmUsed = "";
            
            long startTime = System.currentTimeMillis();
            
            switch (algorithmChoice) {
                case 1:
                    solution = solver.solveUCS(board, false);
                    algorithmUsed = "Uniform Cost Search (UCS)";
                    break;
                case 2:
                    solution = solver.solveGreedy(board, heuristic);
                    algorithmUsed = "Greedy Best First Search with " + heuristic + " heuristic";
                    break;
                case 3:
                    solution = solver.solveAStar(board, heuristic);
                    algorithmUsed = "A* Search with " + heuristic + " heuristic";
                    break;
                default:
                    System.out.println("Using UCS as default.");
                    solution = solver.solveUCS(board, false);
                    algorithmUsed = "Uniform Cost Search (UCS)";
            }
            
            long endTime = System.currentTimeMillis();
            
            // Display results
            if (solution != null) {
                System.out.println("\nSolution found!");
                System.out.println("Algorithm used: " + algorithmUsed);
                System.out.println("Number of states examined: " + solution.getStatesExamined());
                System.out.println("Number of moves: " + solution.getMoves().size());
                System.out.println("Execution time: " + (endTime - startTime) + " ms");
                
                System.out.println("\nSolution steps:");
                solution.displaySolution();
                
                // Ask if user wants to save the solution
                System.out.print("\nDo you want to save the solution to a file? (y/n): ");
                String saveChoice = getYesNoChoice(scanner);
                
                if (saveChoice.equalsIgnoreCase("y")) {
                    // Save solution to file
                    String outputFilename = filename.replace(".txt", "");
                    String algoPrefix = algorithmChoice == 1 ? "ucs_" : 
                                       (algorithmChoice == 2 ? "greedy_" : "astar_");
                    String outputPath = "test/output/" + algoPrefix + "output_" + outputFilename + ".txt";
                    
                    // Check if file already exists
                    File outputFile = new File(outputPath);
                    boolean shouldSave = true;
                    
                    if (outputFile.exists()) {
                        System.out.print("File " + outputPath + " already exists. Do you want to overwrite it? (y/n): ");
                        String overwriteChoice = getYesNoChoice(scanner);
                        
                        if (!overwriteChoice.equalsIgnoreCase("y")) {
                            shouldSave = false;
                            System.out.println("File not saved.");
                        }
                    }
                    
                    if (shouldSave) {
                        try {
                            saveSolutionToFile(board, solution, endTime - startTime, outputPath, algorithmUsed);
                            System.out.println("Solution saved to: " + outputPath);
                        } catch (IOException e) {
                            System.err.println("Error saving solution to file: " + e.getMessage());
                        }
                    }
                } else {
                    System.out.println("Solution not saved.");
                }
            } else {
                System.out.println("\nNo solution found!");
            }
            
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("No such file or directory") || errorMessage.contains("The system cannot find the file")) {
                System.err.println("Error: File not found - " + filepath);
                System.err.println("Make sure the file exists in the test/input/ directory");
            } else {
                System.err.println("Error reading file: " + errorMessage);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        scanner.close();
    }
    
    /**
     * Write final board state to file with primary piece shown next to exit
     */
    private static void writeFinalBoardWithExitedPiece(PrintWriter writer, Board board) {
        int width = board.getWidth();
        int height = board.getHeight();
        
        Exit exitSide = board.getExitSide();
        Position exitPosition = board.getExitPosition();
        
        // Top exit if exists
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("KP"); // Show K with P outside
                    j++; // Skip one column as we printed two characters
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
        
        // Board content
        for (int i = 0; i < height; i++) {
            // Left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                writer.print("PK"); // Show P outside, then K
            }
            
            // Board cells
            for (int j = 0; j < width; j++) {
                char c = board.getGridAt(i, j);
                // Don't show P in the board for final state (it's exited)
                if (c == 'P') {
                    writer.print('.');
                } else {
                    writer.print(c);
                }
            }
            
            // Right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                writer.print("KP"); // Show K then P outside
            }
            
            writer.println();
        }
        
        // Bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("KP"); // Show K with P outside
                    j++; // Skip one column as we printed two characters
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
    }

    /**
     * Save solution to a file with P shown next to exit K
     */
    private static void saveSolutionToFile(Board initialBoard, Solution solution, long executionTime, 
                                        String outputPath, String algorithmUsed) throws IOException {
        // Create output directory if it doesn't exist
        File outputDir = new File("test/output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Write solution to file
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            // Write execution info
            writer.println("=== Rush Hour Solution ===");
            writer.println("Algorithm used: " + algorithmUsed);
            writer.println("Number of states examined: " + solution.getStatesExamined());
            writer.println("Number of moves: " + solution.getMoves().size());
            writer.println("Execution time: " + executionTime + " ms");
            writer.println();
            
            // Write move sequence
            writer.println("Move sequence:");
            writer.println(formatMoveSequence(solution.getMoves()));
            writer.println();
            
            // Write initial board
            writer.println("Papan Awal");
            writeBoard(writer, solution.getStates().get(0));
            
            // Write each move except the last
            List<Move> moves = solution.getMoves();
            List<Board> states = solution.getStates();
            
            for (int i = 0; i < moves.size() - 1; i++) {
                writer.println();
                writer.println("Gerakan " + (i + 1) + ": " + moves.get(i));
                writeBoard(writer, states.get(i + 1));
            }
            
            // Write the final move with special visualization
            if (moves.size() > 0) {
                int lastIndex = moves.size() - 1;
                writer.println();
                writer.println("Gerakan " + (lastIndex + 1) + ": " + moves.get(lastIndex));
                writeFinalBoardWithExitedPiece(writer, states.get(states.size() - 1));
            }
            
            writer.println();
            writer.println("[Primary piece has reached the exit!]");
        }
    }

    /**
     * Write final board state to file with primary piece shown exiting
     */
    public static void writeFinalBoard(PrintWriter writer, Board board) {
        int width = board.getWidth();
        int height = board.getHeight();
        
        Exit exitSide = board.getExitSide();
        Position exitPosition = board.getExitPosition();
        
        // Top exit if exists
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("P"); // Show primary piece exiting at top
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
        
        // Board content
        for (int i = 0; i < height; i++) {
            // Left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                writer.print("P"); // Show primary piece exiting at left
            }
            
            // Board cells
            for (int j = 0; j < width; j++) {
                writer.print(board.getGridAt(i, j));
            }
            
            // Right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                writer.print("P"); // Show primary piece exiting at right
            }
            
            writer.println();
        }
        
        // Bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("P"); // Show primary piece exiting at bottom
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
    }
    
    /**
     * Format move sequence for display
     */
    private static String formatMoveSequence(List<Move> moves) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        
        for (Move move : moves) {
            sb.append(move.toString()).append(" ");
            count++;
            
            // Add newline every 16 moves for readability
            if (count % 16 == 0) {
                sb.append("\n");
            }
        }
        
        sb.append("(").append(moves.size()).append(" moves)");
        return sb.toString();
    }
    
    /**
     * Write board to a file
     */
    private static void writeBoard(PrintWriter writer, Board board) {
        // Write board without colors (plain text for file)
        int width = board.getWidth();
        int height = board.getHeight();
        
        // Top exit if exists
        Exit exitSide = board.getExitSide();
        Position exitPosition = board.getExitPosition();
        
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("K");
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
        
        // Board content
        for (int i = 0; i < height; i++) {
            // Left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                writer.print("K");
            }
            
            // Board cells
            for (int j = 0; j < width; j++) {
                writer.print(board.getGridAt(i, j));
            }
            
            // Right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                writer.print("K");
            }
            
            writer.println();
        }
        
        // Bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("K");
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
    }
    
    /**
     * Get yes/no choice from user
     */
    private static String getYesNoChoice(Scanner scanner) {
        String choice = scanner.nextLine().trim();
        
        while (!choice.equalsIgnoreCase("y") && !choice.equalsIgnoreCase("n")) {
            System.out.print(RED + "Invalid input! Please enter 'y' or 'n': " + RESET);
            choice = scanner.nextLine().trim();
        }
        
        return choice;
    }
    
    /**
     * Get algorithm choice from user
     */
    private static int getAlgorithmChoice(Scanner scanner) {
        String input = scanner.nextLine().trim();
        
        while (true) {
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= 3) {
                    return choice;
                } else {
                    System.out.print(RED + "Invalid choice! Please enter a number between 1 and 3: " + RESET);
                    input = scanner.nextLine().trim();
                }
            } catch (NumberFormatException e) {
                System.out.print(RED + "Invalid input! Please enter a number (1-3): " + RESET);
                input = scanner.nextLine().trim();
            }
        }
    }
    
    /**
     * Get heuristic choice from user
     */
    private static int getHeuristicChoice(Scanner scanner) {
        String input = scanner.nextLine().trim();
        
        while (true) {
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= 3) {
                    return choice;
                } else {
                    System.out.print(RED + "Invalid choice! Please enter a number between 1 and 3: " + RESET);
                    input = scanner.nextLine().trim();
                }
            } catch (NumberFormatException e) {
                System.out.print(RED + "Invalid input! Please enter a number (1-3): " + RESET);
                input = scanner.nextLine().trim();
            }
        }
    }
}