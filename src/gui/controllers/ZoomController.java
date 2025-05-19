package gui.controllers;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.application.Platform;

/**
 * Enhanced controller for handling zoom and pan functionality on a canvas
 * Works with existing FXML layout and supports more intuitive dragging/panning
 * with special optimizations for very large boards (24x24 and above)
 */
public class ZoomController {
    private final Canvas canvas;
    private final StackPane canvasContainer;
    private final Button zoomInButton;
    private final Button zoomOutButton;
    private final Slider zoomSlider;
    
    // Zoom state
    private double zoomScale = 1.0;
    private double minZoom = 0.25;
    private double maxZoom = 3.0;
    private double zoomFactor = 1.1;
    
    // Pan state
    private double translateX = 0;
    private double translateY = 0;
    private Point2D lastMousePosition;
    private boolean isPanning = false;
    
    // Mouse states
    private boolean leftButtonDragging = false;
    private boolean hasResizedCanvas = false;
    
    /**
     * Create a new zoom controller
     * @param canvas The canvas to be zoomed
     * @param canvasContainer The container holding the canvas
     * @param zoomInButton Button for zooming in (optional, can be null)
     * @param zoomOutButton Button for zooming out (optional, can be null)
     * @param zoomSlider Slider for zoom control (optional, can be null)
     */
    public ZoomController(Canvas canvas, StackPane canvasContainer, 
                         Button zoomInButton, Button zoomOutButton, Slider zoomSlider) {
        this.canvas = canvas;
        this.canvasContainer = canvasContainer;
        this.zoomInButton = zoomInButton;
        this.zoomOutButton = zoomOutButton;
        this.zoomSlider = zoomSlider;
        
        setupEventHandlers();
        setupButtons();
        setupSlider();
    }
    
    /**
     * Set up mouse event handlers for zoom and pan
     */
    private void setupEventHandlers() {
        // Scroll zoom
        canvas.setOnScroll(this::handleScroll);
        
        // Pan with mouse drag
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        
        // Set cursor based on board size
        updateCursor();
        
        // Listen for canvas size changes
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateCursor();
            // For very large boards, automatically set an initial zoom level
            if (newVal.doubleValue() > 1000 && !hasResizedCanvas) {
                hasResizedCanvas = true;
                Platform.runLater(() -> {
                    // Use a lower initial zoom for very large boards
                    double initialZoom = calculateOptimalInitialZoom();
                    zoom(initialZoom);
                });
            }
        });
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> updateCursor());
    }
    
    /**
     * Calculate optimal initial zoom level based on canvas size and container
     */
    private double calculateOptimalInitialZoom() {
        // Safety check - if we can't get container dimensions, use a default
        if (canvasContainer == null || canvasContainer.getWidth() <= 0 || canvasContainer.getHeight() <= 0) {
            // Default small scale for very large boards
            if (canvas.getWidth() > 1400 || canvas.getHeight() > 1400) {
                return 0.3;
            } else if (canvas.getWidth() > 1000 || canvas.getHeight() > 1000) {
                return 0.5;
            } else {
                return 0.8;
            }
        }
        
        // Calculate zoom that would fit the canvas in the container with some padding
        double containerWidth = canvasContainer.getWidth() - 40; // 20px padding on each side
        double containerHeight = canvasContainer.getHeight() - 40;
        
        double widthRatio = containerWidth / canvas.getWidth();
        double heightRatio = containerHeight / canvas.getHeight();
        
        // Choose the smaller ratio to ensure complete visibility
        double fitZoom = Math.min(widthRatio, heightRatio);
        
        // Limit to a reasonable range
        return Math.max(0.25, Math.min(fitZoom, 1.0));
    }
    
    /**
     * Update cursor based on board size and zoom level
     */
    private void updateCursor() {
        boolean isVeryLargeBoard = canvas.getWidth() > 1000 || canvas.getHeight() > 1000;
        
        if (zoomScale > 1.0 || isLargeBoard() || isVeryLargeBoard) {
            canvas.setCursor(Cursor.HAND); // Indicate draggable
        } else {
            canvas.setCursor(Cursor.DEFAULT);
        }
    }
    
    /**
     * Set up zoom buttons
     */
    private void setupButtons() {
        if (zoomInButton != null) {
            zoomInButton.setOnAction(e -> zoomIn());
        }
        
        if (zoomOutButton != null) {
            zoomOutButton.setOnAction(e -> zoomOut());
        }
    }
    
    /**
     * Set up zoom slider
     */
    private void setupSlider() {
        if (zoomSlider != null) {
            // Set slider range and initial value
            zoomSlider.setMin(minZoom);
            zoomSlider.setMax(maxZoom);
            zoomSlider.setValue(zoomScale);
            
            // Update zoom when slider changes
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                // Only update if the change is significant
                if (Math.abs(newVal.doubleValue() - zoomScale) > 0.01) {
                    zoom(newVal.doubleValue());
                }
            });
        }
    }
    
    /**
     * Handle scroll events for zooming
     */
    private void handleScroll(ScrollEvent event) {
        // Get mouse position relative to canvas
        double mouseX = event.getX();
        double mouseY = event.getY();
        
        // Calculate new scale with adaptive zoom rate for large boards
        boolean isVeryLargeBoard = canvas.getWidth() > 1000 || canvas.getHeight() > 1000;
        
        // Use a smaller zoom factor for very large boards - makes zooming more gradual
        double effectiveZoomFactor = isVeryLargeBoard ? 1.05 : zoomFactor;
        
        double delta = event.getDeltaY() > 0 ? effectiveZoomFactor : 1 / effectiveZoomFactor;
        double newScale = zoomScale * delta;
        newScale = Math.max(minZoom, Math.min(maxZoom, newScale));
        
        // Only apply zoom if scale has changed
        if (newScale != zoomScale) {
            // Calculate zoom center (mouse position)
            Point2D mousePoint = new Point2D(mouseX, mouseY);
            zoom(newScale, mousePoint);
        }
        
        event.consume();
    }
    
    /**
     * Handle mouse pressed event for panning
     */
    private void handleMousePressed(MouseEvent event) {
        // Special handling for very large boards - always allow dragging
        boolean isVeryLargeBoard = canvas.getWidth() > 1000 || canvas.getHeight() > 1000;
        
        // Start panning with middle button or right button
        if (event.isMiddleButtonDown() || event.isSecondaryButtonDown()) {
            lastMousePosition = new Point2D(event.getX(), event.getY());
            isPanning = true;
            canvas.setCursor(Cursor.MOVE);
        }
        
        // Start panning with left button based on board size and zoom
        else if (event.isPrimaryButtonDown()) {
            // Always enable left-button dragging for large boards, very large boards, or when zoomed
            if (zoomScale > 1.0 || isLargeBoard() || isVeryLargeBoard) {
                lastMousePosition = new Point2D(event.getX(), event.getY());
                leftButtonDragging = true;
                canvas.setCursor(Cursor.MOVE);
            }
        }
    }
    
    /**
     * Check if the board is considered "large" and should allow dragging
     */
    private boolean isLargeBoard() {
        // Always consider boards with dimensions over 10x10 as large
        return canvas.getWidth() > 400 || canvas.getHeight() > 400;
    }
    
    /**
     * Handle mouse dragged event for panning
     */
    private void handleMouseDragged(MouseEvent event) {
        // Handle traditional panning with middle/right mouse button
        if (isPanning) {
            handlePanning(event);
        }
        
        // Handle left-button drag panning 
        else if (leftButtonDragging) {
            // Always allow dragging once initiated
            handlePanning(event);
        }
    }
    
    /**
     * Common panning logic for any button drag
     */
    private void handlePanning(MouseEvent event) {
        if (lastMousePosition == null) {
            lastMousePosition = new Point2D(event.getX(), event.getY());
            return;
        }
        
        // Calculate delta in screen space
        double deltaX = event.getX() - lastMousePosition.getX();
        double deltaY = event.getY() - lastMousePosition.getY();
        
        // Special handling for very large boards - apply a higher sensitivity factor
        boolean isVeryLargeBoard = canvas.getWidth() > 1000 || canvas.getHeight() > 1000;
        double sensitivityFactor = isVeryLargeBoard ? 2.0 : 1.0;
        
        // Convert delta to canvas space (accounting for zoom)
        // We divide by zoomScale because when zoomed in, a small mouse movement
        // translates to a larger canvas movement
        double canvasDeltaX = (deltaX / zoomScale) * sensitivityFactor;
        double canvasDeltaY = (deltaY / zoomScale) * sensitivityFactor;
        
        // Update translation
        translateX += canvasDeltaX;
        translateY += canvasDeltaY;
        
        lastMousePosition = new Point2D(event.getX(), event.getY());
        
        // Apply transformation
        applyTransform();
    }
    
    /**
     * Handle mouse released event for panning
     */
    private void handleMouseReleased(MouseEvent event) {
        if (isPanning) {
            isPanning = false;
            updateCursor();
        }
        
        if (leftButtonDragging) {
            leftButtonDragging = false;
            updateCursor();
        }
    }
    
    /**
     * Zoom in by one step
     */
    public void zoomIn() {
        double newScale = zoomScale * zoomFactor;
        newScale = Math.min(newScale, maxZoom);
        zoom(newScale);
    }
    
    /**
     * Zoom out by one step
     */
    public void zoomOut() {
        double newScale = zoomScale / zoomFactor;
        newScale = Math.max(newScale, minZoom);
        zoom(newScale);
    }
    
    /**
     * Reset zoom and pan to default
     */
    public void resetZoom() {
        zoomScale = 1.0;
        translateX = 0;
        translateY = 0;
        
        // Update slider if available
        if (zoomSlider != null) {
            zoomSlider.setValue(zoomScale);
        }
        
        // Update cursor based on zoom level
        updateCursor();
        
        // Apply transformation
        applyTransform();
    }
    
    /**
     * Zoom to specific scale
     */
    public void zoom(double newScale) {
        // Zoom to center of canvas
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() / 2;
        zoom(newScale, new Point2D(centerX, centerY));
    }
    
    /**
     * Zoom to specific scale at a specific point
     */
    public void zoom(double newScale, Point2D center) {
        if (newScale < minZoom || newScale > maxZoom) {
            return;
        }
        
        // Get relative position of zoom center
        double centerX = center.getX();
        double centerY = center.getY();
        
        // Calculate new translation to maintain center point
        double factor = newScale / zoomScale;
        translateX = factor * (translateX - centerX) + centerX;
        translateY = factor * (translateY - centerY) + centerY;
        
        // Update zoom scale
        zoomScale = newScale;
        
        // Update slider if available
        if (zoomSlider != null) {
            zoomSlider.setValue(zoomScale);
        }
        
        // Update cursor based on new zoom level
        updateCursor();
        
        // Apply transformation
        applyTransform();
    }
    
    /**
     * Apply current zoom and translation to canvas
     */
    private void applyTransform() {
        // Store the canvas dimensions before transform
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        
        // Reset canvas transform
        canvas.setScaleX(1);
        canvas.setScaleY(1);
        canvas.setTranslateX(0);
        canvas.setTranslateY(0);
        
        // Apply scale transform
        canvas.setScaleX(zoomScale);
        canvas.setScaleY(zoomScale);
        
        // Try to get container dimensions
        double containerWidth = 0;
        double containerHeight = 0;
        
        if (canvasContainer != null) {
            containerWidth = canvasContainer.getWidth();
            containerHeight = canvasContainer.getHeight();
            
            // If container dimensions are not available yet, use reasonable defaults
            if (containerWidth <= 0) containerWidth = 800;
            if (containerHeight <= 0) containerHeight = 600;
        } else {
            // Default fallback values if container is null
            containerWidth = 800;
            containerHeight = 600;
        }
        
        // Calculate bounds to prevent excessive dragging for very large boards
        double scaledWidth = canvasWidth * zoomScale;
        double scaledHeight = canvasHeight * zoomScale;
        
        // For very large boards, use a more aggressive bounding approach
        boolean isVeryLargeBoard = canvasWidth > 1000 || canvasHeight > 1000;
        double boundingFactor = isVeryLargeBoard ? 0.3 : 0.5; // Tighter bounds for very large boards
        
        // Limit translation to keep canvas mostly within view
        if (scaledWidth > containerWidth) {
            // Calculate max translation with extra padding for large boards
            double extraPadding = isVeryLargeBoard ? 100 : 0;
            double maxTranslateX = (scaledWidth - containerWidth + extraPadding) / 2 / zoomScale * boundingFactor;
            translateX = Math.max(-maxTranslateX, Math.min(maxTranslateX, translateX));
        } else {
            // Center smaller canvas horizontally
            translateX = 0;
        }
        
        if (scaledHeight > containerHeight) {
            // Calculate max translation with extra padding for large boards
            double extraPadding = isVeryLargeBoard ? 100 : 0;
            double maxTranslateY = (scaledHeight - containerHeight + extraPadding) / 2 / zoomScale * boundingFactor;
            translateY = Math.max(-maxTranslateY, Math.min(maxTranslateY, translateY));
        } else {
            // Center smaller canvas vertically
            translateY = 0;
        }
        
        // Apply translation with adaptive scaling based on board size
        double translationFactor = 1 - zoomScale;
        canvas.setTranslateX(translateX * translationFactor);
        canvas.setTranslateY(translateY * translationFactor);
    }
    
    /**
     * Set zoom limits
     */
    public void setZoomLimits(double minZoom, double maxZoom) {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
        
        // Update slider limits if available
        if (zoomSlider != null) {
            zoomSlider.setMin(minZoom);
            zoomSlider.setMax(maxZoom);
        }
    }
    
    /**
     * Get current zoom scale
     */
    public double getZoomScale() {
        return zoomScale;
    }
    
    /**
     * Transform a point in canvas space to zoomed space
     */
    public Point2D canvasToZoomed(double x, double y) {
        double zoomedX = x * zoomScale + translateX * (1 - zoomScale);
        double zoomedY = y * zoomScale + translateY * (1 - zoomScale);
        return new Point2D(zoomedX, zoomedY);
    }
    
    /**
     * Transform a point in zoomed space to canvas space
     */
    public Point2D zoomedToCanvas(double x, double y) {
        double canvasX = (x - translateX * (1 - zoomScale)) / zoomScale;
        double canvasY = (y - translateY * (1 - zoomScale)) / zoomScale;
        return new Point2D(canvasX, canvasY);
    }
}