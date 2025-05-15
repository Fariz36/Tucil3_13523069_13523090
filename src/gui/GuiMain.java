package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class GuiMain extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Try different ways to load FXML
            URL fxmlUrl = getClass().getResource("/gui/fxml/MainView.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("fxml/MainView.fxml");
            }
            if (fxmlUrl == null) {
                System.err.println("Could not find MainView.fxml");
                System.err.println("Trying alternative paths...");
                
                // List available resources
                System.err.println("Available resources in classpath:");
                System.err.println(getClass().getResource("/"));
                
                throw new RuntimeException("Cannot find MainView.fxml. Make sure it's in src/gui/fxml/");
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 900, 700);
            
            // Try to load CSS
            URL cssUrl = getClass().getResource("/resources/styles.css");
            if (cssUrl == null) {
                cssUrl = getClass().getResource("/styles.css");
            }
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("Warning: Could not find styles.css");
            }
            
            primaryStage.setTitle("Rush Hour Puzzle Solver");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(700);
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("Error loading GUI:");
            e.printStackTrace();
            throw e;
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}