package src;

import java.io.File;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MusicVisualizer extends Application {
    private String audioPath = "sample.wav";
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private Canvas canvas;
    private GraphicsContext gc;
    private double[] currentMagnitudes;
    private Label statusLabel;
    private AnimationTimer animationTimer;

    @Override
    public void start(Stage stage) throws Exception {
        // Create canvas for drawing
        canvas = new Canvas(800, 400);
        gc = canvas.getGraphicsContext2D();
        
        // Initialize magnitude array
        currentMagnitudes = new double[64];

        // Create controls
        Button playButton = new Button("Play/Pause");
        playButton.setOnAction(e -> togglePlayback());
        
        statusLabel = new Label("Ready to play");
        statusLabel.setTextFill(Color.WHITE);

        VBox root = new VBox(10, playButton, statusLabel, canvas);
        root.setStyle("-fx-background-color: #0a1428; -fx-padding: 10;");
        Scene scene = new Scene(root, 820, 500);

        // Initialize media player
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            statusLabel.setText("ERROR: Audio file not found: " + audioFile.getAbsolutePath());
            statusLabel.setTextFill(Color.RED);
            System.err.println("Audio file not found: " + audioFile.getAbsolutePath());
        } else {
            try {
                Media media = new Media(audioFile.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                
                // Configure spectrum analyzer
                mediaPlayer.setAudioSpectrumInterval(0.05);   // 20 updates per second
                mediaPlayer.setAudioSpectrumNumBands(64);     // 64 frequency bands
                mediaPlayer.setAudioSpectrumThreshold(-60);   // minimum threshold
                
                // Event handlers
                mediaPlayer.setOnReady(() -> {
                    statusLabel.setText("Media loaded - Duration: " + 
                        String.format("%.1f", mediaPlayer.getTotalDuration().toSeconds()) + "s");
                    statusLabel.setTextFill(Color.GREEN);
                });
                
                mediaPlayer.setOnError(() -> {
                    statusLabel.setText("ERROR: " + mediaPlayer.getError().getMessage());
                    statusLabel.setTextFill(Color.RED);
                });
                
                mediaPlayer.setOnEndOfMedia(() -> {
                    isPlaying = false;
                    statusLabel.setText("Playback finished");
                });

                // Spectrum listener - this captures the audio data
                mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
                    // Update our magnitude array
                    System.arraycopy(magnitudes, 0, currentMagnitudes, 0, 
                        Math.min(magnitudes.length, currentMagnitudes.length));
                });
                
                // Start animation timer for smooth rendering
                animationTimer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        drawVisualization();
                    }
                };
                animationTimer.start();
                
            } catch (Exception e) {
                statusLabel.setText("ERROR loading media: " + e.getMessage());
                statusLabel.setTextFill(Color.RED);
                e.printStackTrace();
            }
        }

        stage.setTitle("Music Visualizer - Fixed Version");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (animationTimer != null) animationTimer.stop();
            if (mediaPlayer != null) mediaPlayer.dispose();
        });
        stage.show();
        
        // Draw initial empty visualization
        drawVisualization();
    }
    
    private void drawVisualization() {
        // Clear canvas with dark background
        gc.setFill(Color.rgb(5, 10, 20));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw grid lines for reference
        gc.setStroke(Color.rgb(30, 30, 50));
        gc.setLineWidth(0.5);
        
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
        double maxHeight = canvas.getHeight() - 20; // Leave some margin
        
        for (int i = 0; i < bandCount; i++) {
            // Convert dB magnitude to height (magnitudes are typically -60 to 0 dB)
            double magnitude = currentMagnitudes[i];
            double normalizedMag = Math.max(0, (magnitude + 60) / 60.0); // Normalize -60 to 0 dB to 0-1
            double height = normalizedMag * maxHeight;
            
            // Enhanced height for better visibility
            height = Math.pow(height / maxHeight, 0.5) * maxHeight; // Square root scaling for better visual
            height = Math.max(height, 2); // Minimum height for visibility
            
            // Color based on frequency (low = red, mid = green, high = blue)
            double hue = (i / (double) bandCount) * 240; // 0-240 degrees (red to blue)
            double saturation = 0.8 + (normalizedMag * 0.2); // More intense colors for louder sounds
            double brightness = 0.5 + (normalizedMag * 0.5); // Brighter for louder sounds
            
            Color barColor = Color.hsb(hue, saturation, brightness);
            
            // Draw the bar
            double barX = i * bandWidth;
            double barY = canvas.getHeight() - height;
            
            // Main bar with gradient effect
            gc.setFill(barColor);
            gc.fillRect(barX + 1, barY, bandWidth - 2, height);
            
            // Top highlight
            gc.setFill(barColor.brighter());
            gc.fillRect(barX + 1, barY, bandWidth - 2, Math.min(3, height));
            
            // Side glow effect for taller bars
            if (height > 50) {
                gc.setFill(barColor.deriveColor(0, 1, 1, 0.3));
                gc.fillRect(barX, barY - 2, bandWidth, height + 4);
            }
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

    private void togglePlayback() {
        if (mediaPlayer == null) {
            statusLabel.setText("No media loaded");
            return;
        }
        
        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            statusLabel.setText("Paused");
        } else {
            mediaPlayer.play();
            isPlaying = true;
            statusLabel.setText("Playing...");
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}