package gui;

import java.io.File;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Main JavaFX Application class for the Rush Hour Puzzle Solver GUI
 */
public class GuiMain extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Create necessary directories
            createDirectories();
            
            // Try to load FXML
            URL fxmlUrl = findResource("gui/fxml/MainView.fxml");
            if (fxmlUrl == null) {
                System.err.println("Could not find MainView.fxml");
                System.err.println("Trying alternative paths...");
                
                // List available resources
                System.err.println("Available resources in classpath:");
                URL rootResource = getClass().getResource("/");
                if (rootResource != null) {
                    System.err.println("Root resource: " + rootResource);
                } else {
                    System.err.println("Root resource not found");
                }
                
                throw new RuntimeException("Cannot find MainView.fxml. Make sure it's in src/gui/fxml/");
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1100, 750);
            
            // Load CSS
            URL cssUrl = findResource("resources/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("Loaded CSS from: " + cssUrl.toExternalForm());
            } else {
                System.out.println("Warning: Could not find styles.css, using default styles");
                
                // Try to load from file directly
                File cssFile = new File("src/resources/styles.css");
                if (cssFile.exists()) {
                    scene.getStylesheets().add(cssFile.toURI().toString());
                    System.out.println("Loaded CSS from file: " + cssFile.getAbsolutePath());
                }
            }
            
            // Set application icon
            try {
                File iconFile = new File("src/resources/images/icon.png");
                if (iconFile.exists()) {
                    primaryStage.getIcons().add(new Image(iconFile.toURI().toString()));
                    System.out.println("Loaded icon from: " + iconFile.getAbsolutePath());
                } else {
                    System.out.println("Icon file not found, using default icon");
                }
            } catch (Exception e) {
                System.err.println("Error loading icon (non-critical): " + e.getMessage());
            }
            
            primaryStage.setTitle("Kessoku No Owari ♪");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Error loading GUI:");
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Find a resource file by trying multiple paths
     */
    private URL findResource(String path) {
        // Try different class loaders and paths
        URL resource = null;
        
        // Try with class loader
        resource = getClass().getClassLoader().getResource(path);
        if (resource != null) return resource;
        
        // Try with / prefix
        resource = getClass().getResource("/" + path);
        if (resource != null) return resource;
        
        // Try without / prefix
        resource = getClass().getResource(path);
        if (resource != null) return resource;
        
        // Try as file
        File file = new File("src/" + path);
        if (file.exists()) {
            try {
                return file.toURI().toURL();
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return null;
    }
    
    /**
     * Create necessary directories for resources
     */
    private void createDirectories() {
        try {
            // Create test/input and test/output directories
            new File("test/input").mkdirs();
            new File("test/output").mkdirs();
            
            // Create resources directories
            new File("src/resources/images").mkdirs();
            
            System.out.println("Created resource directories");
        } catch (Exception e) {
            System.err.println("Error creating directories (non-critical): " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}