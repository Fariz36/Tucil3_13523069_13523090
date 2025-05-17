package gui.controllers;

import cli.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import java.util.*;
import java.util.List;

/**
 * Controller for smooth animation of Rush Hour puzzle states
 * Implements state-by-state animation with interpolation between states
 * and special handling for primary piece exiting the board
 */
public class AnimationController {
    private Canvas boardCanvas;
    private GraphicsContext gc;
    private Timeline animation;
    private List<Board> states;
    private List<Move> moves;
    private int currentStateIndex = 0;
    private int animationSteps = 10; // Number of interpolation steps between states
    private int currentAnimationStep = 0;
    private boolean isPlaying = false;
    private Label moveLabel;
    private Label statsLabel;
    private Slider speedSlider;
    private Button playButton;
    private Button pauseButton;
    private Button resetButton;
    private int width;
    private int height;
    
    // Constants for visualization
    private static final int CELL_SIZE = 60;
    private static final int PADDING = 40;
    private Map<Character, Color> pieceColors = new HashMap<>();
    
    /**
     * Initialize the controller
     */
    public AnimationController(Canvas canvas, Label moveLabel, Label statsLabel, 
                              Slider speedSlider, Button playButton, Button pauseButton, 
                              Button resetButton, Solution solution) {
        this.boardCanvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.moveLabel = moveLabel;
        this.statsLabel = statsLabel;
        this.speedSlider = speedSlider;
        this.playButton = playButton;
        this.pauseButton = pauseButton;
        this.resetButton = resetButton;
        this.states = solution.getStates();
        this.moves = solution.getMoves();
        
        // Initialize piece colors
        initializePieceColors();
        
        // Set up the canvas size based on board dimensions
        Board initialBoard = states.get(0);
        this.width = initialBoard.getWidth();
        this.height = initialBoard.getHeight();
        boardCanvas.setWidth(width * CELL_SIZE + PADDING * 2);
        boardCanvas.setHeight(height * CELL_SIZE + PADDING * 2);
        
        // Set up animation
        setupAnimation();
        
        // Draw initial state
        drawBoard(initialBoard, null, 0);
        updateInfo();
    }
    
    /**
     * Initialize colors for pieces
     */
    private void initializePieceColors() {
        // Define a set of visually distinct colors
        Color[] colors = {
            Color.CRIMSON,           // Primary piece (P)
            Color.ROYALBLUE,
            Color.LIMEGREEN,
            Color.GOLD,
            Color.DARKORANGE,
            Color.MEDIUMPURPLE,
            Color.DEEPSKYBLUE,
            Color.HOTPINK,
            Color.FORESTGREEN,
            Color.CHOCOLATE,
            Color.SLATEBLUE,
            Color.TOMATO,
            Color.TEAL,
            Color.INDIANRED,
            Color.MEDIUMSEAGREEN,
            Color.STEELBLUE,
            Color.DARKVIOLET,
            Color.SANDYBROWN,
            Color.CORAL,
            Color.DARKORANGE,
            Color.DARKCYAN,
            Color.MEDIUMORCHID,
            Color.LIGHTCORAL,
            Color.LIGHTSEAGREEN,
            Color.LIGHTSLATEGRAY,
            Color.LIGHTSTEELBLUE
        };
        
        // Assign colors to pieces in initial board
        Board initialBoard = states.get(0);
        int colorIndex = 1; // Start at 1, reserve 0 for primary piece
        
        // Assign primary piece color first
        pieceColors.put('P', colors[0]);
        
        // Assign colors to other pieces
        for (Piece piece : initialBoard.getPieces()) {
            if (piece.getId() != 'P') {
                pieceColors.put(piece.getId(), colors[colorIndex % colors.length]);
                colorIndex++;
            }
        }
    }
    
    /**
     * Set up animation timeline
     */
    private void setupAnimation() {
        animation = new Timeline(
            new KeyFrame(Duration.millis(1000 / (speedSlider.getValue() * animationSteps)), e -> {
                // Update animation step
                currentAnimationStep++;
                
                // If we completed all animation steps for this move
                if (currentAnimationStep >= animationSteps) {
                    currentAnimationStep = 0;
                    currentStateIndex++;
                    
                    // If we reached the end, stop
                    if (currentStateIndex >= states.size()) {
                        currentStateIndex = states.size() - 1;
                        pause();
                    }
                }
                
                // Update the visualization
                updateVisualization();
            })
        );
        animation.setCycleCount(Timeline.INDEFINITE);
        
        // Set up slider to adjust animation speed
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double millis = 1000 / (newVal.doubleValue() * animationSteps);
            animation.getKeyFrames().clear();
            animation.getKeyFrames().add(new KeyFrame(Duration.millis(millis), e -> {
                currentAnimationStep++;
                if (currentAnimationStep >= animationSteps) {
                    currentAnimationStep = 0;
                    currentStateIndex++;
                    if (currentStateIndex >= states.size()) {
                        currentStateIndex = states.size() - 1;
                        pause();
                    }
                }
                updateVisualization();
            }));
        });
    }
    
    /**
     * Update visualization based on current state and animation step
     */
    private void updateVisualization() {
        // Get current board state
        Board currentState = states.get(currentStateIndex);
        
        // Get the move that led to this state (if not the initial state)
        Move currentMove = null;
        if (currentStateIndex > 0 && currentStateIndex <= moves.size()) {
            currentMove = moves.get(currentStateIndex - 1);
        }
        
        // Draw the board with animation
        drawBoard(currentState, currentMove, currentAnimationStep);
        
        // Update info labels
        updateInfo();
    }
    
    /**
     * Update info labels
     */
    private void updateInfo() {
        // Update move information
        if (currentStateIndex == 0) {
            moveLabel.setText("Initial Board Configuration");
        } else if (currentStateIndex <= moves.size()) {
            Move move = moves.get(currentStateIndex - 1);
            moveLabel.setText(String.format("Move %d of %d: %s", 
                currentStateIndex, moves.size(), move.toString()));
        }
    }
    
    /**
     * Draw board with animation
     * @param board The current board state
     * @param move The current move (may be null for initial state)
     * @param animStep Animation step (0 to animationSteps-1)
     */
    private void drawBoard(Board board, Move move, int animStep) {
        // Clear canvas
        gc.clearRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());
        
        // Draw board background
        gc.setFill(Color.rgb(240, 240, 240));
        gc.fillRect(PADDING, PADDING, width * CELL_SIZE, height * CELL_SIZE);
        
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
        drawExit(board);
        
        // Draw pieces
        for (Piece piece : board.getPieces()) {
            // For the piece being moved, draw it with animation
            if (move != null && piece.getId() == move.getPiece().getId()) {
                drawAnimatedPiece(piece, move, animStep);
            } else {
                drawPiece(piece);
            }
        }
        
        // Special case: primary piece exiting in last move
        if (currentStateIndex == states.size() - 1 && 
            currentAnimationStep > 0 && 
            move != null && move.getPiece().getId() == 'P') {
            drawExitingPrimaryPiece(board, move, currentAnimationStep);
        }
    }
    
    /**
     * Draw exit on the board
     */
    private void drawExit(Board board) {
        Exit exitSide = board.getExitSide();
        Position exitPos = board.getExitPosition();
        
        gc.setFill(Color.LIGHTGREEN);
        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(2);
        
        switch (exitSide) {
            case TOP:
                gc.fillRect(PADDING + exitPos.col * CELL_SIZE, PADDING - 20, CELL_SIZE, 20);
                gc.strokeRect(PADDING + exitPos.col * CELL_SIZE, PADDING - 20, CELL_SIZE, 20);
                
                // Draw exit arrow
                gc.setFill(Color.DARKGREEN);
                double xTop = PADDING + exitPos.col * CELL_SIZE + CELL_SIZE / 2;
                double yTop = PADDING - 5;
                gc.fillPolygon(
                    new double[] {xTop, xTop - 10, xTop + 10},
                    new double[] {yTop - 10, yTop, yTop},
                    3
                );
                break;
                
            case BOTTOM:
                gc.fillRect(PADDING + exitPos.col * CELL_SIZE, 
                           PADDING + height * CELL_SIZE, CELL_SIZE, 20);
                gc.strokeRect(PADDING + exitPos.col * CELL_SIZE, 
                             PADDING + height * CELL_SIZE, CELL_SIZE, 20);
                             
                // Draw exit arrow
                gc.setFill(Color.DARKGREEN);
                double xBottom = PADDING + exitPos.col * CELL_SIZE + CELL_SIZE / 2;
                double yBottom = PADDING + height * CELL_SIZE + 15;
                gc.fillPolygon(
                    new double[] {xBottom, xBottom - 10, xBottom + 10},
                    new double[] {yBottom + 10, yBottom, yBottom},
                    3
                );
                break;
                
            case LEFT:
                gc.fillRect(PADDING - 20, PADDING + exitPos.row * CELL_SIZE, 20, CELL_SIZE);
                gc.strokeRect(PADDING - 20, PADDING + exitPos.row * CELL_SIZE, 20, CELL_SIZE);
                
                // Draw exit arrow
                gc.setFill(Color.DARKGREEN);
                double xLeft = PADDING - 10;
                double yLeft = PADDING + exitPos.row * CELL_SIZE + CELL_SIZE / 2;
                gc.fillPolygon(
                    new double[] {xLeft - 10, xLeft, xLeft},
                    new double[] {yLeft, yLeft - 10, yLeft + 10},
                    3
                );
                break;
                
            case RIGHT:
                gc.fillRect(PADDING + width * CELL_SIZE, 
                           PADDING + exitPos.row * CELL_SIZE, 20, CELL_SIZE);
                gc.strokeRect(PADDING + width * CELL_SIZE, 
                             PADDING + exitPos.row * CELL_SIZE, 20, CELL_SIZE);
                             
                // Draw exit arrow
                gc.setFill(Color.DARKGREEN);
                double xRight = PADDING + width * CELL_SIZE + 10;
                double yRight = PADDING + exitPos.row * CELL_SIZE + CELL_SIZE / 2;
                gc.fillPolygon(
                    new double[] {xRight + 10, xRight, xRight},
                    new double[] {yRight, yRight - 10, yRight + 10},
                    3
                );
                break;
        }
        
        // Draw exit label
        gc.setFill(Color.BLACK);
        gc.fillText("EXIT", PADDING + width * CELL_SIZE + 25, PADDING + 15);
    }
    
    /**
     * Draw a piece on the board
     */
    private void drawPiece(Piece piece) {
        List<Position> positions = piece.getPositions();
        
        // Skip if no positions
        if (positions.isEmpty()) return;
        
        // Get color for this piece
        Color color = pieceColors.getOrDefault(piece.getId(), Color.GRAY);
        
        // Calculate bounding box
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        
        for (Position pos : positions) {
            minRow = Math.min(minRow, pos.row);
            maxRow = Math.max(maxRow, pos.row);
            minCol = Math.min(minCol, pos.col);
            maxCol = Math.max(maxCol, pos.col);
        }
        
        // Calculate dimensions
        double x = PADDING + minCol * CELL_SIZE + 5;
        double y = PADDING + minRow * CELL_SIZE + 5;
        double width = (maxCol - minCol + 1) * CELL_SIZE - 10;
        double height = (maxRow - minRow + 1) * CELL_SIZE - 10;
        
        // Draw piece with rounded corners and shadows
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillRoundRect(x + 3, y + 3, width, height, 10, 10);
        
        gc.setFill(color);
        gc.fillRoundRect(x, y, width, height, 10, 10);
        
        gc.setStroke(color.darker());
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 10, 10);
        
        // Add a highlight effect
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        gc.fillRoundRect(x + 5, y + 5, width - 10, height / 3, 10, 10);
        
        // Draw piece ID
        gc.setFill(Color.WHITE);
        gc.fillText(String.valueOf(piece.getId()), x + width / 2 - 5, y + height / 2 + 5);
    }
    
    /**
     * Draw a piece with animation during movement
     */
    private void drawAnimatedPiece(Piece piece, Move move, int animStep) {
        List<Position> positions = piece.getPositions();
        
        // Skip if no positions
        if (positions.isEmpty()) return;
        
        // Get color for this piece
        Color color = pieceColors.getOrDefault(piece.getId(), Color.GRAY);
        
        // Calculate bounding box
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        
        for (Position pos : positions) {
            minRow = Math.min(minRow, pos.row);
            maxRow = Math.max(maxRow, pos.row);
            minCol = Math.min(minCol, pos.col);
            maxCol = Math.max(maxCol, pos.col);
        }
        
        // Calculate movement offset
        double offsetX = 0, offsetY = 0;
        int distance = 1; // Default distance is 1
        
        if (move instanceof CompoundMove) {
            distance = ((CompoundMove)move).getDistance();
        }
        
        // Calculate fraction of movement
        double fraction = (double)animStep / animationSteps;
        
        // Determine direction and apply offset
        if (move.getDirection().equals("right")) {
            offsetX = fraction * distance * CELL_SIZE;
        } else if (move.getDirection().equals("left")) {
            offsetX = -fraction * distance * CELL_SIZE;
        } else if (move.getDirection().equals("down")) {
            offsetY = fraction * distance * CELL_SIZE;
        } else if (move.getDirection().equals("up")) {
            offsetY = -fraction * distance * CELL_SIZE;
        }
        
        // Calculate dimensions with offset
        double x = PADDING + minCol * CELL_SIZE + 5 + offsetX;
        double y = PADDING + minRow * CELL_SIZE + 5 + offsetY;
        double width = (maxCol - minCol + 1) * CELL_SIZE - 10;
        double height = (maxRow - minRow + 1) * CELL_SIZE - 10;
        
        // Draw piece with rounded corners and shadows
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillRoundRect(x + 3, y + 3, width, height, 10, 10);
        
        gc.setFill(color);
        gc.fillRoundRect(x, y, width, height, 10, 10);
        
        gc.setStroke(color.darker());
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 10, 10);
        
        // Add a highlight effect
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        gc.fillRoundRect(x + 5, y + 5, width - 10, height / 3, 10, 10);
        
        // Draw piece ID
        gc.setFill(Color.WHITE);
        gc.fillText(String.valueOf(piece.getId()), x + width / 2 - 5, y + height / 2 + 5);
    }
    
    /**
     * Draw primary piece exiting the board
     */
    private void drawExitingPrimaryPiece(Board board, Move lastMove, int animStep) {
        // Only proceed if it's a primary piece move
        if (lastMove.getPiece().getId() != 'P') return;
        
        // Get information about the primary piece
        int pieceSize = lastMove.getPiece().getSize();
        Exit exitSide = board.getExitSide();
        Position exitPos = board.getExitPosition();
        
        // Get the distance moved
        int distance = 1;
        if (lastMove instanceof CompoundMove) {
            distance = ((CompoundMove)lastMove).getDistance();
        }
        
        // Calculate how much of the piece is outside
        double fractionOutside = (double)animStep / animationSteps;
        int cellsOutside = (int)Math.ceil(fractionOutside * Math.min(pieceSize, distance));
        
        // Draw the exiting piece based on exit side
        Color color = pieceColors.getOrDefault('P', Color.CRIMSON);
        
        switch (exitSide) {
            case RIGHT:
                for (int i = 0; i < cellsOutside; i++) {
                    double x = PADDING + board.getWidth() * CELL_SIZE + 5 + i * CELL_SIZE;
                    double y = PADDING + exitPos.row * CELL_SIZE + 5;
                    
                    // Draw exiting piece cell
                    gc.setFill(color);
                    gc.fillRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    gc.setStroke(color.darker());
                    gc.strokeRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    
                    // Add highlight
                    gc.setFill(Color.rgb(255, 255, 255, 0.3));
                    gc.fillRoundRect(x + 5, y + 5, CELL_SIZE - 20, (CELL_SIZE - 10) / 3, 8, 8);
                    
                    // Draw P if it's the first exiting cell
                    if (i == 0) {
                        gc.setFill(Color.WHITE);
                        gc.fillText("P", x + CELL_SIZE / 2 - 5, y + CELL_SIZE / 2 + 5);
                    }
                }
                break;
                
            case LEFT:
                for (int i = 0; i < cellsOutside; i++) {
                    double x = PADDING - (i + 1) * CELL_SIZE + 5;
                    double y = PADDING + exitPos.row * CELL_SIZE + 5;
                    
                    // Draw exiting piece cell
                    gc.setFill(color);
                    gc.fillRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    gc.setStroke(color.darker());
                    gc.strokeRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    
                    // Add highlight
                    gc.setFill(Color.rgb(255, 255, 255, 0.3));
                    gc.fillRoundRect(x + 5, y + 5, CELL_SIZE - 20, (CELL_SIZE - 10) / 3, 8, 8);
                    
                    // Draw P if it's the first exiting cell
                    if (i == 0) {
                        gc.setFill(Color.WHITE);
                        gc.fillText("P", x + CELL_SIZE / 2 - 5, y + CELL_SIZE / 2 + 5);
                    }
                }
                break;
                
            case BOTTOM:
                for (int i = 0; i < cellsOutside; i++) {
                    double x = PADDING + exitPos.col * CELL_SIZE + 5;
                    double y = PADDING + board.getHeight() * CELL_SIZE + 5 + i * CELL_SIZE;
                    
                    // Draw exiting piece cell
                    gc.setFill(color);
                    gc.fillRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    gc.setStroke(color.darker());
                    gc.strokeRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    
                    // Add highlight
                    gc.setFill(Color.rgb(255, 255, 255, 0.3));
                    gc.fillRoundRect(x + 5, y + 5, CELL_SIZE - 20, (CELL_SIZE - 10) / 3, 8, 8);
                    
                    // Draw P if it's the first exiting cell
                    if (i == 0) {
                        gc.setFill(Color.WHITE);
                        gc.fillText("P", x + CELL_SIZE / 2 - 5, y + CELL_SIZE / 2 + 5);
                    }
                }
                break;
                
            case TOP:
                for (int i = 0; i < cellsOutside; i++) {
                    double x = PADDING + exitPos.col * CELL_SIZE + 5;
                    double y = PADDING - (i + 1) * CELL_SIZE + 5;
                    
                    // Draw exiting piece cell
                    gc.setFill(color);
                    gc.fillRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    gc.setStroke(color.darker());
                    gc.strokeRoundRect(x, y, CELL_SIZE - 10, CELL_SIZE - 10, 10, 10);
                    
                    // Add highlight
                    gc.setFill(Color.rgb(255, 255, 255, 0.3));
                    gc.fillRoundRect(x + 5, y + 5, CELL_SIZE - 20, (CELL_SIZE - 10) / 3, 8, 8);
                    
                    // Draw P if it's the first exiting cell
                    if (i == 0) {
                        gc.setFill(Color.WHITE);
                        gc.fillText("P", x + CELL_SIZE / 2 - 5, y + CELL_SIZE / 2 + 5);
                    }
                }
                break;
        }
    }
    
    /**
     * Play the animation
     */
    public void play() {
        // Only play if not at the end
        if (currentStateIndex < states.size() - 1 || currentAnimationStep < animationSteps - 1) {
            animation.play();
            isPlaying = true;
            playButton.setDisable(true);
            pauseButton.setDisable(false);
        }
    }
    
    /**
     * Pause the animation
     */
    public void pause() {
        if (isPlaying) {
            animation.pause();
            isPlaying = false;
            playButton.setDisable(false);
            pauseButton.setDisable(true);
        }
    }
    
    /**
     * Reset the animation to the beginning
     */
    public void reset() {
        pause();
        currentStateIndex = 0;
        currentAnimationStep = 0;
        drawBoard(states.get(0), null, 0);
        updateInfo();
        playButton.setDisable(false);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (animation != null) {
            animation.stop();
        }
    }
}