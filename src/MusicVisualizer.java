package src;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.sound.sampled.LineUnavailableException;

public class MusicVisualizer extends Application {
    private Canvas canvas;
    private GraphicsContext gc;
    private double[] currentMagnitudes;
    private AnimationTimer animationTimer;

    @Override
    public void start(Stage stage) throws Exception {
        // Create canvas for drawing
        canvas = new Canvas(800, 400);
        gc = canvas.getGraphicsContext2D();
        
        // Initialize magnitude array
        currentMagnitudes = new double[64];

        VBox root = new VBox(10, canvas);
        root.setStyle("-fx-background-color: #0a1428; -fx-padding: 10;");
        Scene scene = new Scene(root, 820, 500);

        // Start audio capture
        try {
            AudioProcessor.startAudioCapture();
        } catch (Exception e) {
            System.err.println("Failed to start audio capture: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Start animation timer for smooth rendering
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                processAudio();
                drawVisualization();
            }
        };
        animationTimer.start();

        stage.setTitle("Live Audio Visualizer");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            animationTimer.stop();
            AudioProcessor.stopAudioCapture();
        });
        stage.show();
        
        // Draw initial empty visualization
        drawVisualization();
    }
    
    private void processAudio() {
        float[] samples = AudioProcessor.getCurrentSamples();
        if (samples == null || samples.length == 0) return;
        
        // Simple FFT-like processing (simplified for visualization)
        int bands = currentMagnitudes.length;
        int samplesPerBand = samples.length / bands;
        
        for (int band = 0; band < bands; band++) {
            double sum = 0;
            int start = band * samplesPerBand;
            int end = Math.min(start + samplesPerBand, samples.length);
            
            for (int i = start; i < end; i++) {
                sum += Math.abs(samples[i]);
            }
            
            // Convert to dB scale (simplified)
            double avg = sum / (end - start);
            currentMagnitudes[band] = 20 * Math.log10(avg * 100 + 1);
        }
    }
    
    private void drawVisualization() {
        // Clear canvas with dark background
        gc.setFill(Color.rgb(5, 10, 20));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw grid lines for reference
        gc.setStroke(Color.rgb(60, 60, 90));
        gc.setLineWidth(1);
        
        // Horizontal grid lines
        for (int i = 0; i <= 10; i++) {
            double y = (canvas.getHeight() / 10.0) * i;
            gc.strokeLine(0, y, canvas.getWidth(), y);
        }
        
        // Vertical grid lines
        for (int i = 0; i <= 16; i++) {
            double x = (canvas.getWidth() / 16.0) * i;
            gc.strokeLine(x, 0, x, canvas.getHeight());
        }
        
        // Draw frequency bars
        int bandCount = currentMagnitudes.length;
        double bandWidth = canvas.getWidth() / (double) bandCount;
        double maxHeight = canvas.getHeight() - 20;
        
        for (int i = 0; i < bandCount; i++) {
            // Convert dB magnitude to height
            double magnitude = currentMagnitudes[i];
            double normalizedMag = Math.max(0, (magnitude + 60) / 60.0);
            double height = normalizedMag * maxHeight;
            
            // Enhanced height for better visibility
            height = Math.pow(height / maxHeight, 0.3) * maxHeight;
            height = Math.max(height, 10);
            
            // Color gradient from blue to red based on frequency
            double hue = i / (double)bandCount * 0.7; // 0-0.7 (blue to red)
            Color barColor = Color.hsb(hue * 360, 0.9, 0.9);
            
            // Draw the bar
            double barX = i * bandWidth;
            double barY = canvas.getHeight() - height;
            
            gc.setFill(barColor);
            gc.fillRect(barX + 1, barY, bandWidth - 2, height);
        }
        
        // Draw center line
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeLine(0, canvas.getHeight() - 10, canvas.getWidth(), canvas.getHeight() - 10);
        
        // Add frequency labels
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("Low", 10, canvas.getHeight() - 5);
        gc.fillText("Mid", canvas.getWidth()/2 - 10, canvas.getHeight() - 5);
        gc.fillText("High", canvas.getWidth() - 35, canvas.getHeight() - 5);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
