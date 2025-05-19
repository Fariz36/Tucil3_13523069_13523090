package gui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;

public class MatrixInputWindowController {
    @FXML private GridPane matrixGrid;
    @FXML private Button okButton;

    private int rows;
    private int cols;
    private List<TextField> matrixCells = new ArrayList<>();
    private Stage stage;

    public TextField selectedExitCell = null;
    public final String EXIT_STYLE = "-fx-background-color: #ffe082;"; // yellow highlight
    public final String NORMAL_STYLE = "";  // reset to default
    public String EXIT_POS = "";
    public int EXIT_ROW;
    public int EXIT_COL;

    String FinalString = "";

    public void initialize() {
        okButton.setOnAction(e -> {
        System.err.println("EXIT_POS " + EXIT_POS);
        System.err.println("EXIT_ROW " + EXIT_ROW);
        System.err.println("EXIT_COL " + EXIT_COL);

        if (selectedExitCell == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Exit");
            alert.setHeaderText("No Exit Selected");
            alert.setContentText("Please click one of the border cells to mark it as the exit (K).");
            alert.showAndWait();
            return;
        }

        if (EXIT_POS == "TOP") {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < EXIT_COL - 1; i++) {
                sb.append(" ");
            }
            sb.append("K");
            for (int i = EXIT_COL + 1; i < cols; i++) {
                sb.append(" ");
            }
            sb.append('\n');

            for (int i = 0; i < rows; i++) {
                for (TextField cell : matrixCells.subList(i * cols, (i + 1) * cols)) {
                    String text = cell.getText();
                    if (text.isEmpty()) {
                        sb.append(".");
                    } else {
                        sb.append(text);
                    }
                }
                sb.append('\n');
            }
            FinalString = sb.toString();
        }
        else if (EXIT_POS == "BOTTOM") {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rows; i++) {
                for (TextField cell : matrixCells.subList(i * cols, (i + 1) * cols)) {
                    String text = cell.getText();
                    if (text.isEmpty()) {
                        sb.append(".");
                    } else {
                        sb.append(text);
                    }
                }
                sb.append('\n');
            }
            for (int i = 0; i < EXIT_COL - 1; i++) {
                sb.append(" ");
            }
            sb.append("K");
            for (int i = EXIT_COL + 1; i < cols; i++) {
                sb.append(" ");
            }
            FinalString = sb.toString();
        }
        else if (EXIT_POS == "LEFT") {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rows; i++) {
                if (i == EXIT_ROW) {
                    sb.append("K");
                }
                else {
                    sb.append(" ");
                }

                for (TextField cell : matrixCells.subList(i * cols, (i + 1) * cols)) {
                    String text = cell.getText();
                    if (text.isEmpty()) {
                        sb.append(".");
                    } else {
                        sb.append(text);
                    }
                }
                sb.append('\n');
            }
            FinalString = sb.toString();
        }
        else if (EXIT_POS == "RIGHT") {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rows; i++) {
                for (TextField cell : matrixCells.subList(i * cols, (i + 1) * cols)) {
                    String text = cell.getText();
                    if (text.isEmpty()) {
                        sb.append(".");
                    } else {
                        sb.append(text);
                    }
                }
                if (i == EXIT_ROW) {
                    sb.append("K");
                }
                sb.append('\n');
            }
            FinalString = sb.toString();
        }

        System.out.println("Final String: " + FinalString);

        // Exit is selected — close the window
        stage.close();
    });
    }

    private void handleExitClick(TextField cell, String exitPos, int row, int col) {
        if (selectedExitCell != null) {
            selectedExitCell.setStyle("-fx-background-color: #eeeeee;");
            selectedExitCell.setText(""); // clear old exit
        }

        selectedExitCell = cell;
        selectedExitCell.setStyle(EXIT_STYLE);
        selectedExitCell.setText("K");

        EXIT_POS = exitPos;
        EXIT_ROW = row;
        EXIT_COL = col;
    }

    public void initMatrix(int rows, int cols, Stage stage) {
        int paddedRows = rows + 2;
        int paddedCols = cols + 2;

        this.rows = rows;
        this.cols = cols;
        this.stage = stage;

        matrixGrid.getChildren().clear();
        matrixCells.clear();

        for (int i = 0; i < paddedRows; i++) {
            for (int j = 0; j < paddedCols; j++) {

                // Skip corners
                if ((i == 0 || i == paddedRows - 1) && (j == 0 || j == paddedCols - 1)) {
                    continue;
                }

                TextField cell = new TextField();
                cell.setPrefWidth(40);
                cell.setPrefHeight(40);
                cell.setAlignment(Pos.CENTER);
                cell.setTextFormatter(new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (newText.length() <= 1 && (newText.isEmpty() || 
                        newText.matches("[A-Z.]") || !newText.equals("K"))) {
                        return change;
                    }
                    return null;
                }));

                // Top, bottom, left, or right edges (not corners): make them clickable exits
                boolean isTop = i == 0 && j > 0 && j < paddedCols - 1;
                boolean isBottom = i == paddedRows - 1 && j > 0 && j < paddedCols - 1;
                boolean isLeft = j == 0 && i > 0 && i < paddedRows - 1;
                boolean isRight = j == paddedCols - 1 && i > 0 && i < paddedRows - 1;
                if (isTop) {
                    EXIT_POS = "TOP";
                } else if (isBottom) {
                    EXIT_POS = "BOTTOM";
                } else if (isLeft) {
                    EXIT_POS = "LEFT";
                } else if (isRight) {
                    EXIT_POS = "RIGHT";
                }

                if (isTop || isBottom || isLeft || isRight) {
                    cell.setEditable(false);
                    cell.setStyle("-fx-background-color: #eeeeee;");  // lighter background
                    final int exitRow = (isTop) ? i : i-1;
                    final int exitCol = j;
                    String exitPos = isTop ? "TOP" : isBottom ? "BOTTOM" : isLeft ? "LEFT" : "RIGHT";
                    cell.setOnMouseClicked(e -> handleExitClick(cell, exitPos, exitRow, exitCol));
                } else {
                    matrixCells.add(cell); // only center cells are actual matrix input
                }

                matrixGrid.add(cell, j, i);
            }
        }
    }

    public String getFinalString() {
        return FinalString;
    }
}
