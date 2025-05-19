package gui.controllers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cli.Board;
import cli.Exit;
import cli.Move;
import cli.Piece;
import cli.Position;
import cli.Solution;
import cli.Solver;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;

/**
 * MainController - Combined controller for inputs and visualization
 * Handles both the sidebar inputs and the board visualization
 * With consistent parsing for all input methods
 */
public class MainController {
    
    // Sidebar input elements
    @FXML private BorderPane rootContainer;
    @FXML private VBox sidebarContainer;
    @FXML private TextField filePathField;
    @FXML private Button browseButton;
    @FXML private Button loadFileButton;
    @FXML private TextArea configTextArea;
    @FXML private Button parseTextButton;
    @FXML private Spinner<Integer> rowSpinner;
    @FXML private Spinner<Integer> colSpinner;
    @FXML private Spinner<Integer> piecesSpinner;
    @FXML private GridPane matrixGrid;
    @FXML private Button createMatrixButton;
    @FXML private Button solveMatrixButton;
    @FXML private VBox matrixInputContainer;
    @FXML private ComboBox<String> algorithmComboBox;
    @FXML private ComboBox<String> heuristicComboBox;
    @FXML private Button solveButton;
    @FXML private Text statusText;
    @FXML private ProgressBar progressBar;
    @FXML private Button compoundButton;
    
    // Board visualization elements
    @FXML private StackPane canvasContainer;
    @FXML private VBox boardDisplay;
    @FXML private VBox welcomePane;
    @FXML private Canvas boardCanvas;
    @FXML private HBox animationControls;
    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button resetButton;
    @FXML private Button saveButton;
    @FXML private Slider speedSlider;
    @FXML private Label moveLabel;
    @FXML private Label statsLabel;
    @FXML private ComboBox<String> songComboBox;
    @FXML private Button playSelectedSongButton;
    @FXML private Button pauseSongButton;
    
    // Board state and data
    private Board currentBoard;
    private List<TextField> matrixCells = new ArrayList<>();
    
    // Solution and animation state
    private Solution solution;
    private List<Board> boardStates;
    private List<Move> moves;
    private int currentStateIndex = 0;
    private Timeline animation;
    private boolean isPlaying = false;
    private long executionTime;
    private int nodesExamined;
    private boolean isCompound = true;

    
    // Visualization constants
    private static final int CELL_SIZE = 60;
    private static final int PADDING = 40;
    private final Map<Character, Color> pieceColors = new HashMap<>();

    // Misc
    private MediaPlayer mediaPlayer;
    private final String SONG_PATH = "src/resources/song/";
    private String playedSong = "If_I_could_be_a_constellation.mp3";
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        ObservableList<String> songs = FXCollections.observableArrayList(
            "If_I_could_be_a_constellation.mp3",
            "Re_Re_.mp3",
            "Rockn'_Roll,_Morning_Light_Falls_on_You.mp3",
            "seisyun_complex.mp3",
            "Color_Your_Night.mp3",
            "Rubia.mp3"
        );
        songComboBox.setItems(songs);
        songComboBox.getSelectionModel().selectFirst();

        // optionally play the first song by default
        playBackgroundMusic(songs.get(0));

        updateCompoundButtonText();

        // Initialize UI components
        initializeInputs();
        
        // Set initial status
        updateStatus("Ready to load configuration");
    }

    /**
     * Initialize input elements
     */
    private void initializeInputs() {
        // Initialize spinners with a minimum value of 1
        rowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 6));
        colSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 6));
        piecesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 12));
        
        // Add a listener to prevent 1x1 boards
        rowSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == 1 && colSpinner.getValue() == 1) {
                colSpinner.getValueFactory().setValue(2);
                updateStatus("Board size must be at least 1x2 or 2x1", true);
            }
        });
        
        colSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == 1 && rowSpinner.getValue() == 1) {
                rowSpinner.getValueFactory().setValue(2);
                updateStatus("Board size must be at least 1x2 or 2x1", true);
            }
        });
        
        // Initialize algorithm dropdown
        algorithmComboBox.getItems().addAll(
            "Uniform Cost Search (UCS)",
            "Greedy Best First Search",
            "A* Search",
            "Dijkstra's Algorithm"
        );
        algorithmComboBox.getSelectionModel().selectFirst();
        
        // Initialize heuristic dropdown
        heuristicComboBox.getItems().addAll(
            "Manhattan Distance",
            "Direct Distance",
            "Blocking Count"
        );
        heuristicComboBox.getSelectionModel().selectFirst();
        
        // Initially disable heuristic for UCS
        heuristicComboBox.setDisable(true);
        algorithmComboBox.setOnAction(e -> {
            String selected = algorithmComboBox.getValue();
            boolean needsHeuristic = selected.contains("Greedy") || selected.contains("A*");
            heuristicComboBox.setDisable(!needsHeuristic);
        });
        
        // Setup text area with sample configuration
        configTextArea.setText("6 6\n11\nAAB..F\n..BCDF\nGPPCDFK\nGH.III\nGHJ...\nLLJMM.");
        
        // Hide matrix input initially
        matrixInputContainer.setVisible(false);
        
        // Disable solve button initially
        solveButton.setDisable(true);
    }
    
    /**
     * Initialize canvas and animation
     */
    private void initializeCanvas() {
        // Setup animation
        animation = new Timeline(
            new KeyFrame(Duration.seconds(1.0 / speedSlider.getValue()), e -> {
                if (currentStateIndex < boardStates.size() - 1) {
                    currentStateIndex++;
                    drawBoard(boardStates.get(currentStateIndex));
                    updateMoveInfo();
                    
                    // If solved
                    if (currentStateIndex == boardStates.size() - 1) {
                        updateStatus("Puzzle solved successfully!");
                    }
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
    
    /**
     * Handle browse button click
     */
    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Rush Hour Configuration File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        // Set initial directory to test/input if it exists
        File testInputDir = new File("test/input");
        if (testInputDir.exists() && testInputDir.isDirectory()) {
            fileChooser.setInitialDirectory(testInputDir);
        }
        
        File file = fileChooser.showOpenDialog(rootContainer.getScene().getWindow());
        if (file != null) {
            filePathField.setText(file.getAbsolutePath());
            handleLoadFile(); // Auto-load when file is selected
        }
    }
    
    /**
     * Handle load file button click
     */
    @FXML
    private void handleLoadFile() {
        String filePath = filePathField.getText();
        if (filePath.isEmpty()) {
            updateStatus("Error: Please select a file first", true);
            return;
        }
        
        updateStatus("Loading file...");
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        
        // Run loading in background
        new Thread(() -> {
            try {
                currentBoard = Board.readFromFile(filePath);
                
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    
                    // Check if primary piece is aligned with exit
                    if (!currentBoard.isPrimaryPieceAlignedWithExit()) {
                        updateStatus("Warning: Primary piece and exit are not aligned. The puzzle may not be solvable.", true);
                    } else {
                        updateStatus("Board loaded successfully!");
                    }
                    
                    // Enable solve button
                    solveButton.setDisable(false);
                    
                    // Show the board
                    displayInitialBoard(currentBoard);
                });
                
            } catch (IOException e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("Error loading file: " + e.getMessage(), true);
                    currentBoard = null;
                    solveButton.setDisable(true);
                });
            }
        }).start();
    }
    
    /**
     * Handle parse text button click - Uses FileParser for consistent parsing
     */
    @FXML
    private void handleParseText() {
        String configText = configTextArea.getText();
        if (configText.trim().isEmpty()) {
            updateStatus("Error: Please enter a configuration", true);
            return;
        }
        
        // Make sure we have at least 3 lines (dimensions, pieces count, and at least one board row)
        String[] lines = configText.split("\\n");
        if (lines.length < 3) {
            updateStatus("Error: Configuration must have at least 3 lines", true);
            return;
        }
        
        // Parse dimensions to determine board size
        String[] dimensions = lines[0].trim().split("\\s+");
        if (dimensions.length != 2) {
            updateStatus("Error: First line must contain exactly 2 integers for board dimensions", true);
            return;
        }
        
        int rows, cols;
        try {
            rows = Integer.parseInt(dimensions[0]);
            cols = Integer.parseInt(dimensions[1]);
        } catch (NumberFormatException e) {
            updateStatus("Error: Invalid board dimensions, must be integers", true);
            return;
        }
        
        // Validate minimum board size (at least 1x2 or 2x1)
        if (rows < 1 || cols < 1 || (rows == 1 && cols == 1)) {
            updateStatus("Error: Board size must be at least 1x2 or 2x1. Current size: " + rows + "x" + cols, true);
            return;
        }
        
        updateStatus("Parsing configuration...");
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        
        // Run parsing in background
        new Thread(() -> {
            try {
                // Save to temporary file and parse using FileParser
                File tempFile = File.createTempFile("rushHour", ".txt");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(configText);
                }
                
                // Use the same FileParser logic used in Board.readFromFile
                currentBoard = Board.readFromFile(tempFile.getAbsolutePath());
                tempFile.delete();
                
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    
                    // Check if primary piece is aligned with exit
                    if (!currentBoard.isPrimaryPieceAlignedWithExit()) {
                        updateStatus("Warning: Primary piece and exit are not aligned. The puzzle may not be solvable.", true);
                    } else {
                        updateStatus("Configuration parsed successfully!");
                    }
                    
                    // Enable solve button
                    solveButton.setDisable(false);
                    
                    // Show the board
                    displayInitialBoard(currentBoard);
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("Error parsing configuration: " + e.getMessage(), true);
                    currentBoard = null;
                    solveButton.setDisable(true);
                });
            }
        }).start();
    }

    
    private void updateCompoundButtonText() {
        if (compoundButton != null) {
            compoundButton.setText(isCompound ? "ON" : "OFF");
        }
    }

    @FXML
    private void handleToggleCompound() {
        isCompound = !isCompound;
        updateCompoundButtonText();
    }
    
    /**
     * Handle create matrix button click
     */
    @FXML
    private void handleCreateMatrix() {
        int rows = rowSpinner.getValue();
        int cols = colSpinner.getValue();
        
        // Validate minimum board size (at least 1x2 or 2x1)
        if (rows < 1 || cols < 1 || (rows == 1 && cols == 1)) {
            updateStatus("Error: Board size must be at least 1x2 or 2x1. Current size: " + rows + "x" + cols, true);
            return;
        }
        
        matrixGrid.getChildren().clear();
        matrixCells.clear();
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                TextField cell = new TextField();
                cell.setPrefWidth(40);
                cell.setPrefHeight(40);
                cell.getStyleClass().add("matrix-cell");
                cell.setAlignment(javafx.geometry.Pos.CENTER);
                cell.setTextFormatter(new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (newText.length() <= 1 && (newText.isEmpty() || 
                        newText.matches("[A-Z.]") || newText.equals("K"))) {
                        return change;
                    }
                    return null;
                }));
                
                matrixGrid.add(cell, j, i);
                matrixCells.add(cell);
            }
        }
        
        matrixInputContainer.setVisible(true);
        solveMatrixButton.setDisable(false);
    }
    
    /**
     * Handle solve matrix button click - Uses FileParser for consistent parsing
     */
    @FXML
    private void handleSolveMatrix() {
        int rows = rowSpinner.getValue();
        int cols = colSpinner.getValue();
        int numPieces = piecesSpinner.getValue();
        
        // Validate minimum board size (at least 1x2 or 2x1)
        if (rows < 1 || cols < 1 || (rows == 1 && cols == 1)) {
            updateStatus("Error: Board size must be at least 1x2 or 2x1. Current size: " + rows + "x" + cols, true);
            return;
        }
        
        // Build configuration string from matrix
        StringBuilder config = new StringBuilder();
        config.append(rows).append(" ").append(cols).append("\n");
        config.append(numPieces).append("\n");
        
        boolean foundP = false;
        boolean foundK = false;
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int index = i * cols + j;
                String value = matrixCells.get(index).getText();
                if (value.isEmpty()) value = ".";
                
                if (value.equals("P")) foundP = true;
                if (value.equals("K")) foundK = true;
                
                config.append(value);
            }
            if (i < rows - 1) config.append("\n");
        }
        
        if (!foundP) {
            updateStatus("Error: Primary piece 'P' not found in matrix", true);
            return;
        }
        
        if (!foundK) {
            updateStatus("Error: Exit 'K' not found in matrix", true);
            return;
        }
        
        updateStatus("Processing matrix configuration...");
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        
        // Parse the configuration in background using FileParser
        new Thread(() -> {
            try {
                File tempFile = File.createTempFile("rushHourMatrix", ".txt");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(config.toString());
                }
                
                // Use the same FileParser logic used in Board.readFromFile
                currentBoard = Board.readFromFile(tempFile.getAbsolutePath());
                tempFile.delete();
                
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    
                    if (!currentBoard.isPrimaryPieceAlignedWithExit()) {
                        updateStatus("Warning: Primary piece and exit are not aligned. The puzzle may not be solvable.", true);
                    } else {
                        updateStatus("Matrix configuration loaded successfully!");
                    }
                    
                    solveButton.setDisable(false);
                    displayInitialBoard(currentBoard);
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("Error parsing matrix: " + e.getMessage(), true);
                    currentBoard = null;
                    solveButton.setDisable(true);
                });
            }
        }).start();
    }
    
    /**
     * Handle solve button click
     */
    @FXML
    private void handleSolve() {
        if (currentBoard == null) {
            updateStatus("Error: Please load a board configuration first", true);
            return;
        }
        
        String algorithm = algorithmComboBox.getValue();
        String heuristic = heuristicComboBox.getValue();
        
        // Disable UI during solving
        solveButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        updateStatus("Solving puzzle using " + algorithm + "...");
        
        // Run solving in background thread
        new Thread(() -> {
            try {
                Solver solver = new Solver();
                Solution solution = null;
                
                long startTime = System.currentTimeMillis();
                
                // Select algorithm based on combo box selection
                if (algorithm.contains("UCS")) {
                    solution = solver.solveUCS(currentBoard, isCompound);
                } else if (algorithm.contains("Dijkstra")) {
                    solution = solver.solveDijkstra(currentBoard, isCompound);
                } else if (algorithm.contains("Greedy")) {
                    solution = solver.solveGreedy(currentBoard, heuristic, isCompound);
                } else if (algorithm.contains("A*")) {
                    solution = solver.solveAStar(currentBoard, heuristic, isCompound);
                }
                
                long endTime = System.currentTimeMillis();
                final long executionTime = endTime - startTime;
                
                final Solution finalSolution = solution;
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    solveButton.setDisable(false);
                    
                    // Store the execution time and solution as class members
                    this.executionTime = executionTime;
                    this.solution = finalSolution;
                    
                    if (finalSolution != null) {
                        displaySolution(finalSolution, algorithm, heuristic, executionTime);
                    } else {
                        // Even when no solution is found, show examined nodes and time
                        int nodesExamined = solver.getLastNodesExamined();
                        updateStatus("No solution found for this configuration. Examined " + nodesExamined + " states in " + executionTime + " ms.", true);
                        
                        // Store the nodes examined for saving
                        this.nodesExamined = nodesExamined;
                        
                        // Enable save button to allow saving the no-solution result
                        animationControls.setVisible(true);
                        playButton.setDisable(true);
                        pauseButton.setDisable(true);
                        resetButton.setDisable(true);
                        
                        // Update stats label with the info
                        statsLabel.setText(String.format(
                            "Algorithm: %s | Heuristic: %s | States Examined: %d | Time: %d ms | No Solution Found",
                            algorithm, 
                            (algorithm.contains("UCS") || algorithm.contains("Dijkstra")) ? "-" : heuristic,
                            nodesExamined,
                            executionTime
                        ));
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    solveButton.setDisable(false);
                    updateStatus("Error solving puzzle: " + e.getMessage(), true);
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    /**
     * Display initial board
     */
    private void displayInitialBoard(Board board) {
        // Show canvas, hide welcome pane
        welcomePane.setVisible(false);
        boardCanvas.setVisible(true);
        
        // Initialize colors
        initializePieceColors(board);
        
        // Draw the board
        drawBoard(board);
        
        // Hide animation controls
        animationControls.setVisible(false);
        
        // Update move info
        moveLabel.setText("Initial Board Configuration");
        statsLabel.setText("");
    }
    
    /**
     * Display solution with animation controls
     */
    private void displaySolution(Solution solution, String algorithm, String heuristic, long executionTime) {
        this.solution = solution;
        this.boardStates = solution.getStates();
        this.moves = solution.getMoves();
        this.currentStateIndex = 0;
        
        // Initialize canvas animation if needed
        if (animation == null) {
            initializeCanvas();
        }
        
        // Show animation controls
        animationControls.setVisible(true);
        
        // Reset controls state
        playButton.setDisable(false);
        pauseButton.setDisable(true);
        
        // Draw initial state
        drawBoard(boardStates.get(0));
        
        // For UCS and Dijkstra, display "-" as the heuristic since they don't use heuristics
        String displayHeuristic = heuristic;
        if (algorithm.contains("UCS") || algorithm.contains("Dijkstra")) {
            displayHeuristic = "-";
        }
        
        // Update stats and info
        statsLabel.setText(String.format(
            "Algorithm: %s | Heuristic: %s | States Examined: %d | Moves: %d | Time: %d ms",
            algorithm, 
            displayHeuristic,
            solution.getStatesExamined(),
            moves.size(),
            executionTime
        ));
        
        updateMoveInfo();
        updateStatus("Solution found! Use controls to view the steps.");
    }
    
    /**
     * Initialize colors for pieces
     */
    private void initializePieceColors(Board board) {
        pieceColors.clear();
        
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
        for (Piece piece : board.getPieces()) {
            if (piece.getId() == 'P') {
                pieceColors.put('P', colors[0]); // Red for primary piece
            } else {
                pieceColors.put(piece.getId(), colors[colorIndex % colors.length]);
                colorIndex++;
            }
        }
    }
    
    /**
     * Draw the board on canvas
     */
    private void drawBoard(Board board) {
        GraphicsContext gc = boardCanvas.getGraphicsContext2D();
        int width = board.getWidth();
        int height = board.getHeight();
        
        // Ensure canvas is large enough
        if (boardCanvas.getWidth() < width * CELL_SIZE + PADDING * 2 ||
            boardCanvas.getHeight() < height * CELL_SIZE + PADDING * 2) {
            boardCanvas.setWidth(width * CELL_SIZE + PADDING * 2);
            boardCanvas.setHeight(height * CELL_SIZE + PADDING * 2);
        }
        
        // Clear canvas
        gc.clearRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());
        
        // Draw board background with rounded corners
        gc.setFill(Color.rgb(240, 240, 240, 0.9));
        gc.fillRoundRect(PADDING - 10, PADDING - 10, 
                        width * CELL_SIZE + 20, height * CELL_SIZE + 20, 15, 15);
        
        // Draw grid background
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRoundRect(PADDING, PADDING, width * CELL_SIZE, height * CELL_SIZE, 10, 10);
        
        // Draw grid lines
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(1);
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
        
        // Draw exit arrow
        drawExitArrow(gc, board);
        
        // Draw pieces
        for (Piece piece : board.getPieces()) {
            drawPiece(gc, piece);
        }
    }
    
    /**
     * Draw exit arrow
     */
    private void drawExitArrow(GraphicsContext gc, Board board) {
        Position exitPos = board.getExitPosition();
        gc.setFill(Color.DARKGREEN);
        
        double arrowSize = 15;
        
        switch (board.getExitSide()) {
            case TOP:
                // Draw arrow pointing up
                double xTop = PADDING + exitPos.col * CELL_SIZE + CELL_SIZE / 2;
                double yTop = PADDING - 10;
                gc.fillPolygon(
                    new double[] {xTop, xTop - arrowSize, xTop + arrowSize},
                    new double[] {yTop - arrowSize, yTop, yTop},
                    3
                );
                break;
            case BOTTOM:
                // Draw arrow pointing down
                double xBottom = PADDING + exitPos.col * CELL_SIZE + CELL_SIZE / 2;
                double yBottom = PADDING + board.getHeight() * CELL_SIZE + 10;
                gc.fillPolygon(
                    new double[] {xBottom, xBottom - arrowSize, xBottom + arrowSize},
                    new double[] {yBottom + arrowSize, yBottom, yBottom},
                    3
                );
                break;
            case LEFT:
                // Draw arrow pointing left
                double xLeft = PADDING - 10;
                double yLeft = PADDING + exitPos.row * CELL_SIZE + CELL_SIZE / 2;
                gc.fillPolygon(
                    new double[] {xLeft - arrowSize, xLeft, xLeft},
                    new double[] {yLeft, yLeft - arrowSize, yLeft + arrowSize},
                    3
                );
                break;
            case RIGHT:
                // Draw arrow pointing right
                double xRight = PADDING + board.getWidth() * CELL_SIZE + 10;
                double yRight = PADDING + exitPos.row * CELL_SIZE + CELL_SIZE / 2;
                gc.fillPolygon(
                    new double[] {xRight + arrowSize, xRight, xRight},
                    new double[] {yRight, yRight - arrowSize, yRight + arrowSize},
                    3
                );
                break;
        }
    }
    
    /**
     * Draw piece with 3D effect
     */
    private void drawPiece(GraphicsContext gc, Piece piece) {
        Color color = pieceColors.get(piece.getId());
        if (color == null) {
            color = Color.GRAY; // Default color if not found
        }
        
        gc.setFill(color);
        gc.setStroke(color.darker());
        gc.setLineWidth(3);
        
        List<Position> positions = piece.getPositions();
        
        // Calculate bounding box
        int minRow = positions.stream().mapToInt(p -> p.row).min().orElse(0);
        int maxRow = positions.stream().mapToInt(p -> p.row).max().orElse(0);
        int minCol = positions.stream().mapToInt(p -> p.col).min().orElse(0);
        int maxCol = positions.stream().mapToInt(p -> p.col).max().orElse(0);
        
        // Draw piece as rounded rectangle with shadow
        double x = PADDING + minCol * CELL_SIZE + 5;
        double y = PADDING + minRow * CELL_SIZE + 5;
        double width = (maxCol - minCol + 1) * CELL_SIZE - 10;
        double height = (maxRow - minRow + 1) * CELL_SIZE - 10;
        
        // Draw shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillRoundRect(x + 3, y + 3, width, height, 15, 15);
        
        // Draw piece
        gc.setFill(color);
        gc.fillRoundRect(x, y, width, height, 15, 15);
        gc.setStroke(color.darker());
        gc.strokeRoundRect(x, y, width, height, 15, 15);
        
        // Add highlight effect
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        gc.fillRoundRect(x + 3, y + 3, width - 6, height/2 - 6, 10, 10);
        
        // Draw piece ID
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 24));
        double textX = x + width / 2 - 10;
        double textY = y + height / 2 + 8;
        gc.fillText(String.valueOf(piece.getId()), textX, textY);
    }
    
    /**
     * Update move information
     */
    private void updateMoveInfo() {
        if (currentStateIndex == 0) {
            moveLabel.setText("Initial Board Configuration");
        } else if (currentStateIndex > moves.size()) {
            moveLabel.setText("Puzzle Solved!");
        } else {
            Move currentMove = moves.get(currentStateIndex - 1);
            moveLabel.setText(String.format("Move %d of %d: %s", 
                currentStateIndex, moves.size(), currentMove.toString()));
        }
    }
    
    /**
     * Update status message
     */
    private void updateStatus(String message) {
        updateStatus(message, false);
    }
    
    /**
     * Update status message with error flag
     */
    private void updateStatus(String message, boolean isError) {
        statusText.setText(message);
        if (isError) {
            statusText.setStyle("-fx-fill: #e74c3c;"); // Red for errors
        } else {
            statusText.setStyle("-fx-fill: #80d8ff;"); // Normal color
        }
    }

    /**
     * Handle play button click
     */
    @FXML
    private void handlePlay() {
        if (!isPlaying && currentStateIndex < boardStates.size() - 1) {
            animation.play();
            isPlaying = true;
            playButton.setDisable(true);
            pauseButton.setDisable(false);
        }
    }
    
    /**
     * Handle pause button click
     */
    @FXML
    private void handlePause() {
        pauseAnimation();
    }
    
    /**
     * Pause animation
     */
    private void pauseAnimation() {
        if (isPlaying) {
            animation.pause();
            isPlaying = false;
            playButton.setDisable(false);
            pauseButton.setDisable(true);
        }
    }
    
    /**
     * Handle reset button click
     */
    @FXML
    private void handleReset() {
        pauseAnimation();
        currentStateIndex = 0;
        drawBoard(boardStates.get(0));
        updateMoveInfo();
        playButton.setDisable(false);
        updateStatus("Reset to initial configuration");
    }
    
    /**
     * Handle save button click
     */
    @FXML
    private void handleSave() {
        // Make sure we have either a solution or at least examined some nodes
        if (solution == null && nodesExamined == 0) {
            updateStatus("No results to save", true);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Solution");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        // Use algorithm name in suggested filename
        String algorithm = algorithmComboBox.getValue().split(" ")[0].toLowerCase();
        String resultType = (solution != null) ? "solution" : "no_solution";
        fileChooser.setInitialFileName("rush_hour_" + algorithm + "_" + resultType + ".txt");
        
        // Try to set initial directory to test/output if it exists
        File outputDir = new File("test/output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        fileChooser.setInitialDirectory(outputDir);
        
        File file = fileChooser.showSaveDialog(rootContainer.getScene().getWindow());
        if (file != null) {
            try {
                saveSolution(file);
                if (solution != null) {
                    updateStatus("Solution saved to: " + file.getName());
                } else {
                    updateStatus("No solution results saved to: " + file.getName());
                }
            } catch (IOException e) {
                updateStatus("Error saving solution: " + e.getMessage(), true);
            }
        }
    }
    
    /**
     * Save solution to file
     */
    private void saveSolution(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // For UCS and Dijkstra, display "-" as the heuristic
            String displayHeuristic = heuristicComboBox.getValue();
            if (algorithmComboBox.getValue().contains("UCS") || 
                algorithmComboBox.getValue().contains("Dijkstra")) {
                displayHeuristic = "-";
            }
            
            writer.write("=== Rush Hour Solution ===\n");
            writer.write("Algorithm: " + algorithmComboBox.getValue() + "\n");
            
            // Write heuristic info if applicable
            writer.write("Heuristic: " + displayHeuristic + "\n");
            
            // Check if we have a solution
            if (solution != null) {
                writer.write("Number of states examined: " + solution.getStatesExamined() + "\n");
                writer.write("Number of moves: " + solution.getMoves().size() + "\n");
            } else {
                // No solution found case
                writer.write("Number of states examined: " + nodesExamined + "\n");
                writer.write("Number of moves: NO SOLUTION FOUND\n");
            }
            
            writer.write("Execution time: " + executionTime + " ms\n");
            writer.write("\n");
            
            // Only write solution steps if a solution was found
            if (solution != null) {
                writer.write("Papan Awal\n");
                writeBoard(writer, boardStates.get(0));
                writer.write("\n");
                
                for (int i = 0; i < moves.size(); i++) {
                    writer.write("Gerakan " + (i + 1) + ": " + moves.get(i) + "\n");
                    writeBoard(writer, boardStates.get(i + 1));
                    writer.write("\n");
                }
                
                writer.write("[Primary piece has reached the exit!]\n");
            } else {
                // Write initial board only
                writer.write("Papan Awal\n");
                writeBoard(writer, currentBoard);
                writer.write("\n");
                writer.write("[No solution found for this configuration]\n");
            }
        }
    }
    
    /**
     * Write board to file
     */
    private void writeBoard(FileWriter writer, Board board) throws IOException {
        int width = board.getWidth();
        int height = board.getHeight();
        
        // Write top exit if exists
        Exit exitSide = board.getExitSide();
        Position exitPosition = board.getExitPosition();
        
        if (exitSide == Exit.TOP) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.write("K");
                } else {
                    writer.write(" ");
                }
            }
            writer.write("\n");
        }
        
        // Write board content
        for (int i = 0; i < height; i++) {
            // Write left exit
            if (exitSide == Exit.LEFT && i == exitPosition.row) {
                writer.write("K");
            }
            
            // Write board cells
            for (int j = 0; j < width; j++) {
                writer.write(board.getGridAt(i, j));
            }
            
            // Write right exit
            if (exitSide == Exit.RIGHT && i == exitPosition.row) {
                writer.write("K");
            }
            
            writer.write("\n");
        }
        
        // Write bottom exit if exists
        if (exitSide == Exit.BOTTOM) {
            for (int j = 0; j < width; j++) {
                if (j == exitPosition.col) {
                    writer.write("K");
                } else {
                    writer.write(" ");
                }
            }
            writer.write("\n");
        }
    }

    @FXML
    private void handlePlaySelectedSong() {
        String selectedSong = songComboBox.getValue();
        if (selectedSong != null && !selectedSong.isEmpty()) {
            playBackgroundMusic(selectedSong);
        }
    }

    @FXML
    private void handlePauseSong() {
        if (mediaPlayer != null) {
        MediaPlayer.Status status = mediaPlayer.getStatus();
            if (status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                pauseSongButton.setText("Play");
            } 
            else if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.STOPPED) {
                if (!songComboBox.getValue().equals(playedSong)) {
                    mediaPlayer.stop();
                    playBackgroundMusic(songComboBox.getValue());
                    pauseSongButton.setText("Pause");
                    playedSong = songComboBox.getValue();

                    return;
                }
                mediaPlayer.play();
                pauseSongButton.setText("Pause");
            }
    }
}

    public void playBackgroundMusic(String filename) {
        try {
            System.out.println("Playing background music: " + filename);
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            File file = new File(SONG_PATH + filename);
            if (!file.exists()) {
                System.err.println("Audio file not found: " + file.getAbsolutePath());
                return;
            }

            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop the song
            mediaPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}