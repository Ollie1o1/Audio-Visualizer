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
    private double[] smoothedMagnitudes; // For smoother animation
    private AnimationTimer animationTimer;
    
    // Sensitivity and responsiveness settings
    private static final double SENSITIVITY_MULTIPLIER = 50.0;
    private static final double SMOOTHING_FACTOR = 0.3;
    private static final double NOISE_THRESHOLD = 0.001;

    @Override
    public void start(Stage stage) throws Exception {
        // Create canvas for drawing
        canvas = new Canvas(800, 400);
        gc = canvas.getGraphicsContext2D();
        
        // Initialize magnitude arrays
        currentMagnitudes = new double[64];
        smoothedMagnitudes = new double[64];

        VBox root = new VBox(canvas);
        root.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 0;");
        Scene scene = new Scene(root, 800, 400);

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

        stage.setTitle("Audio Visualizer");
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
        
        int bands = currentMagnitudes.length;
        int samplesPerBand = samples.length / bands;
        
        for (int band = 0; band < bands; band++) {
            double sum = 0;
            int start = band * samplesPerBand;
            int end = Math.min(start + samplesPerBand, samples.length);
            
            // Calculate RMS (Root Mean Square) for better amplitude representation
            for (int i = start; i < end; i++) {
                sum += samples[i] * samples[i];
            }
            
            double rms = Math.sqrt(sum / (end - start));
            
            // Apply noise threshold - if below threshold, set to 0
            if (rms < NOISE_THRESHOLD) {
                rms = 0;
            }
            
            // More aggressive scaling for better responsiveness
            double magnitude = rms * SENSITIVITY_MULTIPLIER;
            
            // Apply logarithmic scaling for better visual representation
            if (magnitude > 0) {
                magnitude = Math.log10(magnitude * 10 + 1) * 0.5;
            }
            
            // Smooth the magnitude changes for fluid animation
            smoothedMagnitudes[band] = smoothedMagnitudes[band] * (1 - SMOOTHING_FACTOR) + 
                                     magnitude * SMOOTHING_FACTOR;
            
            currentMagnitudes[band] = smoothedMagnitudes[band];
        }
    }
    
    private void drawVisualization() {
        // Clear canvas with dark greyish-black background
        gc.setFill(Color.rgb(26, 26, 26));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw frequency bars
        int bandCount = currentMagnitudes.length;
        double bandWidth = canvas.getWidth() / (double) bandCount;
        double maxHeight = canvas.getHeight();
        double baselineY = canvas.getHeight(); // Bars start from bottom
        
        for (int i = 0; i < bandCount; i++) {
            double magnitude = currentMagnitudes[i];
            
            // Only draw bars if there's actual signal above threshold
            if (magnitude > 0.001) {
                // Scale height more aggressively
                double height = Math.min(magnitude * maxHeight * 2, maxHeight);
                
                // Apply power curve for more dramatic response
                height = Math.pow(height / maxHeight, 0.6) * maxHeight;
                
                // Calculate bar position
                double barX = i * bandWidth;
                double barY = baselineY - height;
                double barWidth = bandWidth * 0.8; // Slight gap between bars
                
                // Use white tones with slight transparency based on intensity
                double alpha = Math.min(0.3 + (height / maxHeight) * 0.7, 1.0);
                Color barColor = Color.rgb(255, 255, 255, alpha);
                
                // Draw the bar
                gc.setFill(barColor);
                gc.fillRect(barX + bandWidth * 0.1, barY, barWidth, height);
                
                // Add subtle glow effect for higher bars
                if (height > maxHeight * 0.3) {
                    gc.setFill(Color.rgb(255, 255, 255, 0.1));
                    gc.fillRect(barX + bandWidth * 0.05, barY - 2, barWidth * 1.1, height + 4);
                }
            }
        }
        
        // Draw subtle baseline
        gc.setStroke(Color.rgb(60, 60, 60));
        gc.setLineWidth(1);
        gc.strokeLine(0, baselineY - 1, canvas.getWidth(), baselineY - 1);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}