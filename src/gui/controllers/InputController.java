package gui.controllers;

import cli.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InputController {
    
    @FXML private TabPane inputTabPane;
    @FXML private Tab fileTab;
    @FXML private Tab textTab;
    @FXML private Tab matrixTab;
    
    // File input components
    @FXML private TextField filePathField;
    @FXML private Button browseButton;
    @FXML private Button loadFileButton;
    
    // Text input components
    @FXML private TextArea configTextArea;
    @FXML private Button parseTextButton;
    
    // Matrix input components
    @FXML private Spinner<Integer> rowSpinner;
    @FXML private Spinner<Integer> colSpinner;
    @FXML private Spinner<Integer> piecesSpinner;
    @FXML private GridPane matrixGrid;
    @FXML private Button createMatrixButton;
    @FXML private Button solveMatrixButton;
    @FXML private VBox matrixInputContainer;
    
    // Algorithm selection
    @FXML private ComboBox<String> algorithmComboBox;
    @FXML private ComboBox<String> heuristicComboBox;
    @FXML private Button solveButton;
    
    private Board currentBoard;
    private List<TextField> matrixCells;
    
    @FXML
    public void initialize() {
        // Initialize spinners
        rowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 10, 6));
        colSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 10, 6));
        piecesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 12));
        
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
        
        matrixCells = new ArrayList<>();
        matrixInputContainer.setVisible(false);
        
        // Add sample configuration
        configTextArea.setText("6 6\n12\nAAB..F\n..BCDF\nGPPCDFK\nGH.III\nGHJ...\nLLJMM.");
    }
    
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
        
        File file = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (file != null) {
            filePathField.setText(file.getAbsolutePath());
            handleLoadFile(); // Auto-load when file is selected
        }
    }
    
    @FXML
    private void handleLoadFile() {
        String filePath = filePathField.getText();
        if (filePath.isEmpty()) {
            showAlert("Error", "Please select a file first", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            currentBoard = Board.readFromFile(filePath);
            
            // Check if primary piece is aligned with exit
            if (!currentBoard.isPrimaryPieceAlignedWithExit()) {
                boolean shouldContinue = showConfirmation(
                    "Warning",
                    "Primary piece and exit are not aligned. The puzzle may not be solvable. Continue anyway?"
                );
                if (!shouldContinue) {
                    currentBoard = null;
                    return;
                }
            }
            
            showAlert("Success", "Board loaded successfully!", Alert.AlertType.INFORMATION);
            solveButton.setDisable(false);
        } catch (IOException e) {
            showAlert("Error", "Error loading file: " + e.getMessage(), Alert.AlertType.ERROR);
            currentBoard = null;
            solveButton.setDisable(true);
        }
    }
    
    @FXML
    private void handleParseText() {
        String configText = configTextArea.getText();
        if (configText.trim().isEmpty()) {
            showAlert("Error", "Please enter a configuration", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            // Save to temporary file and parse
            File tempFile = File.createTempFile("rushHour", ".txt");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(configText);
            }
            
            currentBoard = Board.readFromFile(tempFile.getAbsolutePath());
            tempFile.delete();
            
            // Check if primary piece is aligned with exit
            if (!currentBoard.isPrimaryPieceAlignedWithExit()) {
                boolean shouldContinue = showConfirmation(
                    "Warning",
                    "Primary piece and exit are not aligned. The puzzle may not be solvable. Continue anyway?"
                );
                if (!shouldContinue) {
                    currentBoard = null;
                    return;
                }
            }
            
            showAlert("Success", "Configuration parsed successfully!", Alert.AlertType.INFORMATION);
            solveButton.setDisable(false);
        } catch (IOException e) {
            showAlert("Error", "Error parsing configuration: " + e.getMessage(), Alert.AlertType.ERROR);
            currentBoard = null;
            solveButton.setDisable(true);
        }
    }
    
    @FXML
    private void handleCreateMatrix() {
        int rows = rowSpinner.getValue();
        int cols = colSpinner.getValue();
        
        matrixGrid.getChildren().clear();
        matrixCells.clear();
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                TextField cell = new TextField();
                cell.setPrefWidth(40);
                cell.setPrefHeight(40);
                cell.setMaxWidth(40);
                cell.setMaxHeight(40);
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
    
    @FXML
    private void handleSolveMatrix() {
        int rows = rowSpinner.getValue();
        int cols = colSpinner.getValue();
        int numPieces = piecesSpinner.getValue();
        
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
            showAlert("Error", "Primary piece 'P' not found in matrix", Alert.AlertType.ERROR);
            return;
        }
        
        if (!foundK) {
            showAlert("Error", "Exit 'K' not found in matrix", Alert.AlertType.ERROR);
            return;
        }
        
        // Parse the configuration
        try {
            File tempFile = File.createTempFile("rushHourMatrix", ".txt");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(config.toString());
            }
            
            currentBoard = Board.readFromFile(tempFile.getAbsolutePath());
            tempFile.delete();
            
            showAlert("Success", "Matrix configuration loaded successfully!", Alert.AlertType.INFORMATION);
            solveButton.setDisable(false);
        } catch (IOException e) {
            showAlert("Error", "Error parsing matrix: " + e.getMessage(), Alert.AlertType.ERROR);
            currentBoard = null;
            solveButton.setDisable(true);
        }
    }
    
    @FXML
    private void handleSolve() {
        if (currentBoard == null) {
            showAlert("Error", "Please load a board configuration first", Alert.AlertType.ERROR);
            return;
        }
        
        String algorithm = algorithmComboBox.getValue();
        String heuristic = heuristicComboBox.getValue();
        
        // Disable UI during solving
        solveButton.setDisable(true);
        
        // Run solving in background thread
        new Thread(() -> {
            try {
                Solver solver = new Solver();
                Solution solution = null;
                
                long startTime = System.currentTimeMillis();
                
                // Select algorithm based on combo box selection
                if (algorithm.contains("UCS")) {
                    solution = solver.solveUCS(currentBoard);
                } else if (algorithm.contains("Dijkstra")) {
                    solution = solver.solveDijkstra(currentBoard);
                } else if (algorithm.contains("Greedy")) {
                    solution = solver.solveGreedy(currentBoard, heuristic);
                } else if (algorithm.contains("A*")) {
                    solution = solver.solveAStar(currentBoard, heuristic);
                }
                
                long endTime = System.currentTimeMillis();
                
                final Solution finalSolution = solution;
                final long executionTime = endTime - startTime;
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    solveButton.setDisable(false);
                    
                    if (finalSolution != null) {
                        openVisualization(finalSolution, currentBoard, algorithm, heuristic, executionTime);
                    } else {
                        showAlert("No Solution", "No solution found for this configuration", Alert.AlertType.WARNING);
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    solveButton.setDisable(false);
                    showAlert("Error", "Error solving puzzle: " + e.getMessage(), Alert.AlertType.ERROR);
                    e.printStackTrace();
                });
            }
        }).start();
    }
    
    private void openVisualization(Solution solution, Board initialBoard, String algorithm, 
                                 String heuristic, long executionTime) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/fxml/VisualizationView.fxml"));
            Parent root = loader.load();
            
            VisualizationController controller = loader.getController();
            controller.initialize(solution, initialBoard, algorithm, heuristic, executionTime);
            
            Stage stage = new Stage();
            stage.setTitle("Rush Hour Solution Visualization");
            Scene scene = new Scene(root, 800, 650);
            
            // Try to load CSS
            try {
                scene.getStylesheets().add(getClass().getResource("/resources/styles.css").toExternalForm());
            } catch (Exception e) {
                System.out.println("Could not load styles.css");
            }
            
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(650);
            stage.show();
            
        } catch (IOException e) {
            showAlert("Error", "Error opening visualization: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        return result == ButtonType.OK;
    }
}