package src;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MusicVisualizer extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Create canvas for drawing
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Pane pane = new Pane(canvas);
        Scene scene = new Scene(pane, 800, 600);
        stage.setTitle("Music Visualizer");
        stage.setScene(scene);

        try {
            // Load audio samples using direct file path
            float[] samples = AudioProcessor.getAudioSamples("src/sample.wav");

            // Draw waveform with better scaling
            gc.beginPath();
            gc.moveTo(0, 300);
            for (int i = 0; i < Math.min(samples.length, 800); i++) {
                gc.lineTo(i, 300 + samples[i] * 200); // Increased amplitude scaling
            }
            gc.stroke();
        } catch (Exception e) {
            System.err.println("Error loading audio: " + e.getMessage());
            Platform.exit();
        }

        stage.show();
    }

    public static void main(String[] args) {
        // Initialize JavaFX with module path
        System.setProperty("javafx.version", "17");
        Application.launch(MusicVisualizer.class, args);
    }
}
