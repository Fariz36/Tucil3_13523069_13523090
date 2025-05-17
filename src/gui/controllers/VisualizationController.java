package gui.controllers;

import cli.*;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.List;

/**
 * Controller for the visualization view that integrates with the AnimationController
 * to display smooth animations of the Rush Hour puzzle solution
 */
public class VisualizationController {
    
    @FXML private Canvas boardCanvas;
    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;
    @FXML private Button saveButton;
    @FXML private Slider speedSlider;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;
    @FXML private Label currentMoveLabel;
    
    private Solution solution;
    private Board initialBoard;
    private AnimationController animationController;
    private String algorithmUsed;
    private String heuristicUsed;
    private long executionTime;
    
    /**
     * Initialize the controller with solution data
     */
    public void initialize(Solution solution, Board initialBoard, String algorithm, 
                        String heuristic, long executionTime) {
        this.solution = solution;
        this.initialBoard = initialBoard;
        this.algorithmUsed = algorithm;
        this.heuristicUsed = heuristic;
        this.executionTime = executionTime;
        
        // For UCS and Dijkstra, display "-" as the heuristic since they don't use heuristics
        String displayHeuristic = heuristic;
        if (algorithm.contains("UCS") || algorithm.contains("Dijkstra")) {
            displayHeuristic = "-";
        }
        
        // Set stats label with algorithm info
        statsLabel.setText(String.format(
            "Algorithm: %s | Heuristic: %s | States Examined: %d | Moves: %d | Time: %d ms",
            algorithm, 
            displayHeuristic,
            solution.getStatesExamined(),
            solution.getMoves().size(),
            executionTime
        ));
        
        // Initialize the animation controller
        animationController = new AnimationController(
            boardCanvas, currentMoveLabel, statusLabel, speedSlider,
            playButton, pauseButton, resetButton, solution
        );
        
        // Set button actions
        playButton.setOnAction(e -> {
            animationController.play();
        });
        
        pauseButton.setOnAction(e -> {
            animationController.pause();
        });
        
        resetButton.setOnAction(e -> {
            animationController.reset();
        });
        
        saveButton.setOnAction(e -> {
            handleSave();
        });
        
        // Initial state
        pauseButton.setDisable(true);
    }
    
    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Solution");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        fileChooser.setInitialFileName("rush_hour_solution.txt");
        
        // Try to use test/output directory if it exists
        File outputDir = new File("test/output");
        if (outputDir.exists() && outputDir.isDirectory()) {
            fileChooser.setInitialDirectory(outputDir);
        }
        
        File file = fileChooser.showSaveDialog(saveButton.getScene().getWindow());
        if (file != null) {
            try {
                saveSolution(file);
                showInfo("Solution saved successfully!");
            } catch (IOException e) {
                showError("Error saving solution: " + e.getMessage());
            }
        }
    }
    
    /**
     * Save solution to a file with visualization of primary piece exiting
     */
    private void saveSolution(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write header
            writer.println("Rush Hour Solution");
            writer.println("==================\n");
            
            // Write stats
            writer.println("Algorithm: " + algorithmUsed);
            
            // For UCS and Dijkstra, display "-" as the heuristic
            String displayHeuristic = heuristicUsed;
            if (algorithmUsed.contains("UCS") || algorithmUsed.contains("Dijkstra")) {
                displayHeuristic = "-";
            }
            
            if (displayHeuristic != null && !displayHeuristic.isEmpty() && !displayHeuristic.equals("-")) {
                writer.println("Heuristic: " + displayHeuristic);
            } else {
                writer.println("Heuristic: -");
            }
            
            writer.println("States examined: " + solution.getStatesExamined());
            writer.println("Total moves: " + solution.getMoves().size());
            writer.println("Execution time: " + executionTime + " ms");
            writer.println();
            
            // Write move sequence
            writer.println("Move Sequence:");
            writer.println(formatMoveSequence(solution.getMoves()));
            writer.println();
            
            // Write initial board
            writer.println("Papan Awal");
            writeBoard(writer, solution.getStates().get(0));
            
            // Write each move
            List<Move> moves = solution.getMoves();
            List<Board> states = solution.getStates();
            
            for (int i = 0; i < moves.size() - 1; i++) {
                writer.println();
                writer.println("Gerakan " + (i + 1) + ": " + moves.get(i));
                writeBoard(writer, states.get(i + 1));
            }
            
            // Write final move with visualization of exiting primary piece
            if (moves.size() > 0) {
                int lastIndex = moves.size() - 1;
                writer.println();
                writer.println("Gerakan " + (lastIndex + 1) + ": " + moves.get(lastIndex));
                writeFinalBoard(writer, states.get(states.size() - 1), moves.get(lastIndex));
            }
            
            writer.println();
            writer.println("[Primary piece has reached the exit!]");
        }
    }
    
    /**
     * Format move sequence as a compact string
     */
    private String formatMoveSequence(List<Move> moves) {
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
     * Write board to file
     */
    private void writeBoard(PrintWriter writer, Board board) {
        int width = board.getWidth();
        int height = board.getHeight();
        
        // Write top exit if exists
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
        
        // Write board content
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
        
        // Write bottom exit if exists
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
     * Write final board state with visualization of exiting primary piece
     */
    private void writeFinalBoard(PrintWriter writer, Board board, Move lastMove) {
        int width = board.getWidth();
        int height = board.getHeight();
        
        Exit exitSide = board.getExitSide();
        Position exitPosition = board.getExitPosition();
        
        // Write top exit with exiting piece if applicable
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("K");
                    // Show primary piece outside
                    if (lastMove.getPiece().getId() == 'P') {
                        writer.print("P");
                    }
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
        
        // Write board content
        for (int i = 0; i < height; i++) {
            // Left exit with exiting piece if applicable
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                // Show primary piece outside
                if (lastMove.getPiece().getId() == 'P') {
                    writer.print("P");
                }
                writer.print("K");
            }
            
            // Board cells - don't show primary piece as it's now outside
            for (int j = 0; j < width; j++) {
                char cell = board.getGridAt(i, j);
                if (cell == 'P' && lastMove.getPiece().getId() == 'P') {
                    writer.print('.');  // Primary piece has exited
                } else {
                    writer.print(cell);
                }
            }
            
            // Right exit with exiting piece if applicable
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                writer.print("K");
                // Show primary piece outside
                if (lastMove.getPiece().getId() == 'P') {
                    writer.print("P");
                }
            }
            
            writer.println();
        }
        
        // Write bottom exit with exiting piece if applicable
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.print("K");
                    // Show primary piece outside
                    if (lastMove.getPiece().getId() == 'P') {
                        writer.print("P");
                    }
                } else {
                    writer.print(" ");
                }
            }
            writer.println();
        }
    }
    
    /**
     * Show info message
     */
    private void showInfo(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show error message
     */
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Clean up resources before closing
     */
    public void cleanup() {
        if (animationController != null) {
            animationController.cleanup();
        }
    }
}