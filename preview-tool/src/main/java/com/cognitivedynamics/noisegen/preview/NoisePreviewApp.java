package com.cognitivedynamics.noisegen.preview;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main JavaFX application for the FastNoiseLite Nouveau Preview Tool.
 *
 * <p>This tool provides an interactive GUI for exploring and visualizing
 * all noise types and features in FastNoiseLite Nouveau.
 *
 * <h2>Running the Application</h2>
 * <pre>
 * # From preview-tool directory:
 * mvn javafx:run
 *
 * # From root directory:
 * mvn -pl preview-tool javafx:run
 * </pre>
 */
public class NoisePreviewApp extends Application {

    private MainController controller;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("FastNoiseLite Nouveau Preview");

        // Create controller and build UI
        controller = new MainController(primaryStage);
        BorderPane root = controller.buildUI();

        // Create scene with dark theme
        Scene scene = new Scene(root, 1200, 800);

        // Load CSS if available
        String css = getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        } else {
            // Apply inline dark theme if CSS not found
            applyInlineDarkTheme(root);
        }

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Cleanup on close
        primaryStage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.shutdown();
            }
        });

        primaryStage.show();
    }

    private void applyInlineDarkTheme(BorderPane root) {
        root.setStyle("""
                -fx-background-color: #2b2b2b;
                -fx-font-family: 'Segoe UI', 'SF Pro Display', system;
                """);
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
