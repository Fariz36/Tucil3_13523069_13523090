package gui.controllers;

import cli.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private List<Board> boardStates;
    private List<Move> moves;
    private int currentStateIndex = 0;
    private Timeline animation;
    private boolean isPlaying = false;
    
    private static final int CELL_SIZE = 60;
    private static final int PADDING = 40;
    private final Map<Character, Color> pieceColors = new HashMap<>();
    
    public void initialize(Solution solution, Board initialBoard, String algorithm, 
                         String heuristic, long executionTime) {
        this.solution = solution;
        this.initialBoard = initialBoard;
        this.boardStates = solution.getStates();
        this.moves = solution.getMoves();
        
        // Initialize colors
        initializePieceColors();
        
        // Set up stats
        statsLabel.setText(String.format(
            "Algorithm: %s\nHeuristic: %s\nStates Examined: %d\nMoves: %d\nExecution Time: %d ms",
            algorithm, 
            heuristic != null ? heuristic : "N/A",
            solution.getStatesExamined(),
            moves.size(),
            executionTime
        ));
        
        // Set up speed slider
        speedSlider.setMin(0.5);
        speedSlider.setMax(3.0);
        speedSlider.setValue(1.0);
        
        // Initial draw
        drawBoard(boardStates.get(0));
        updateStatus();
        
        // Set up animation
        setupAnimation();
    }
    
    private void initializePieceColors() {
        Color[] colors = {
            Color.CRIMSON,      // Primary piece (P)
            Color.ROYALBLUE,
            Color.LIMEGREEN,
            Color.GOLD,
            Color.DARKORANGE,
            Color.MEDIUMPURPLE,
            Color.DEEPPINK,
            Color.TURQUOISE,
            Color.CORAL,
            Color.DARKGREEN,
            Color.INDIGO,
            Color.MAROON,
            Color.OLIVE,
            Color.TEAL,
            Color.NAVY,
            Color.CHOCOLATE
        };
        
        // Assign colors to pieces
        int colorIndex = 1; // Start from 1, reserve 0 for primary piece
        for (Piece piece : initialBoard.getPieces()) {
            if (piece.getId() == 'P') {
                pieceColors.put('P', colors[0]); // Red for primary piece
            } else {
                pieceColors.put(piece.getId(), colors[colorIndex % colors.length]);
                colorIndex++;
            }
        }
    }
    
    private void setupAnimation() {
        animation = new Timeline(
            new KeyFrame(Duration.seconds(1.0 / speedSlider.getValue()), e -> {
                if (currentStateIndex < boardStates.size() - 1) {
                    currentStateIndex++;
                    drawBoard(boardStates.get(currentStateIndex));
                    updateStatus();
                } else {
                    pauseAnimation();
                }
            })
        );
        animation.setCycleCount(Timeline.INDEFINITE);
        
        // Update animation speed when slider changes
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            animation.setRate(newVal.doubleValue());
        });
    }
    
    private void drawBoard(Board board) {
        GraphicsContext gc = boardCanvas.getGraphicsContext2D();
        int width = board.getWidth();
        int height = board.getHeight();
        
        // Clear canvas
        gc.clearRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());
        
        // Draw background
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(PADDING, PADDING, width * CELL_SIZE, height * CELL_SIZE);
        
        // Draw grid
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(2);
        for (int i = 0; i <= width; i++) {
            gc.strokeLine(PADDING + i * CELL_SIZE, PADDING, 
                         PADDING + i * CELL_SIZE, PADDING + height * CELL_SIZE);
        }
        for (int i = 0; i <= height; i++) {
            gc.strokeLine(PADDING, PADDING + i * CELL_SIZE, 
                         PADDING + width * CELL_SIZE, PADDING + i * CELL_SIZE);
        }
        
        // Draw exit
        Position exitPos = board.getExitPosition();
        gc.setFill(Color.LIGHTGREEN);
        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(3);
        
        switch (board.getExitSide()) {
            case TOP:
                gc.fillRect(PADDING + exitPos.col * CELL_SIZE, PADDING - 20, CELL_SIZE, 20);
                gc.strokeRect(PADDING + exitPos.col * CELL_SIZE, PADDING - 20, CELL_SIZE, 20);
                break;
            case BOTTOM:
                gc.fillRect(PADDING + exitPos.col * CELL_SIZE, PADDING + height * CELL_SIZE, 
                          CELL_SIZE, 20);
                gc.strokeRect(PADDING + exitPos.col * CELL_SIZE, PADDING + height * CELL_SIZE, 
                            CELL_SIZE, 20);
                break;
            case LEFT:
                gc.fillRect(PADDING - 20, PADDING + exitPos.row * CELL_SIZE, 20, CELL_SIZE);
                gc.strokeRect(PADDING - 20, PADDING + exitPos.row * CELL_SIZE, 20, CELL_SIZE);
                break;
            case RIGHT:
                gc.fillRect(PADDING + width * CELL_SIZE, PADDING + exitPos.row * CELL_SIZE, 
                          20, CELL_SIZE);
                gc.strokeRect(PADDING + width * CELL_SIZE, PADDING + exitPos.row * CELL_SIZE, 
                            20, CELL_SIZE);
                break;
        }
        
        // Draw pieces
        for (Piece piece : board.getPieces()) {
            drawPiece(gc, piece);
        }
    }
    
    private void drawPiece(GraphicsContext gc, Piece piece) {
        Color color = pieceColors.get(piece.getId());
        gc.setFill(color);
        gc.setStroke(color.darker());
        gc.setLineWidth(3);
        
        List<Position> positions = piece.getPositions();
        
        // Calculate bounding box
        int minRow = positions.stream().mapToInt(p -> p.row).min().orElse(0);
        int maxRow = positions.stream().mapToInt(p -> p.row).max().orElse(0);
        int minCol = positions.stream().mapToInt(p -> p.col).min().orElse(0);
        int maxCol = positions.stream().mapToInt(p -> p.col).max().orElse(0);
        
        // Draw piece as rounded rectangle
        double x = PADDING + minCol * CELL_SIZE + 5;
        double y = PADDING + minRow * CELL_SIZE + 5;
        double width = (maxCol - minCol + 1) * CELL_SIZE - 10;
        double height = (maxRow - minRow + 1) * CELL_SIZE - 10;
        
        gc.fillRoundRect(x, y, width, height, 15, 15);
        gc.strokeRoundRect(x, y, width, height, 15, 15);
        
        // Draw piece ID
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        double textX = x + width / 2 - 10;
        double textY = y + height / 2 + 8;
        gc.fillText(String.valueOf(piece.getId()), textX, textY);
    }
    
    private void updateStatus() {
        if (currentStateIndex >= moves.size()) {
            statusLabel.setText("Puzzle Solved!");
            currentMoveLabel.setText("Primary piece has reached the exit!");
        } else {
            statusLabel.setText("Move " + (currentStateIndex + 1) + " of " + moves.size());
            currentMoveLabel.setText("Current Move: " + moves.get(currentStateIndex));
        }
    }
    
    @FXML
    private void handlePlay() {
        if (!isPlaying && currentStateIndex < boardStates.size() - 1) {
            animation.play();
            isPlaying = true;
            playButton.setDisable(true);
            pauseButton.setDisable(false);
        }
    }
    
    @FXML
    private void handlePause() {
        pauseAnimation();
    }
    
    private void pauseAnimation() {
        if (isPlaying) {
            animation.pause();
            isPlaying = false;
            playButton.setDisable(false);
            pauseButton.setDisable(true);
        }
    }
    
    @FXML
    private void handleReset() {
        pauseAnimation();
        currentStateIndex = 0;
        drawBoard(boardStates.get(0));
        updateStatus();
        playButton.setDisable(false);
    }
    
    @FXML
    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Solution");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        fileChooser.setInitialFileName("rush_hour_solution.txt");
        
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
    
    private void saveSolution(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Rush Hour Solution\n");
            writer.write("==================\n\n");
            writer.write(statsLabel.getText());
            writer.write("\n\nSolution Steps:\n");
            writer.write("===============\n\n");
            
            writer.write("Initial Board:\n");
            writeBoard(writer, boardStates.get(0));
            writer.write("\n");
            
            for (int i = 0; i < moves.size(); i++) {
                writer.write("Move " + (i + 1) + ": " + moves.get(i) + "\n");
                writeBoard(writer, boardStates.get(i + 1));
                writer.write("\n");
            }
            
            writer.write("Puzzle Solved!\n");
        }
    }
    
    private void writeBoard(FileWriter writer, Board board) throws IOException {
        for (int i = 0; i < board.getHeight(); i++) {
            for (int j = 0; j < board.getWidth(); j++) {
                writer.write(board.getGridAt(i, j));
            }
            writer.write("\n");
        }
    }
    
    private void showInfo(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}