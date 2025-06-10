package src;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.sound.sampled.LineUnavailableException;

public class MusicVisualizer extends Application {
    private Canvas canvas;
    private GraphicsContext gc;
    private double[] currentMagnitudes;
    private double[] smoothedMagnitudes; // For smoother animation
    private AnimationTimer animationTimer;
    private StackPane root;
    private Button toggleButton;
    private boolean isCircularMode = false;
    
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

        // Create toggle button
        toggleButton = new Button("●");
        toggleButton.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(255,255,255,0.4); " +
            "-fx-border-radius: 15; " +
            "-fx-background-radius: 15; " +
            "-fx-font-size: 16px; " +
            "-fx-min-width: 30; " +
            "-fx-min-height: 30; " +
            "-fx-max-width: 30; " +
            "-fx-max-height: 30;"
        );
        
        toggleButton.setOnMouseEntered(e -> toggleButton.setStyle(
            "-fx-background-color: rgba(255,255,255,0.3); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(255,255,255,0.6); " +
            "-fx-border-radius: 15; " +
            "-fx-background-radius: 15; " +
            "-fx-font-size: 16px; " +
            "-fx-min-width: 30; " +
            "-fx-min-height: 30; " +
            "-fx-max-width: 30; " +
            "-fx-max-height: 30;"
        ));
        
        toggleButton.setOnMouseExited(e -> toggleButton.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2); " +
            "-fx-text-fill: white; " +
            "-fx-border-color: rgba(255,255,255,0.4); " +
            "-fx-border-radius: 15; " +
            "-fx-background-radius: 15; " +
            "-fx-font-size: 16px; " +
            "-fx-min-width: 30; " +
            "-fx-min-height: 30; " +
            "-fx-max-width: 30; " +
            "-fx-max-height: 30;"
        ));
        
        toggleButton.setOnAction(e -> {
            isCircularMode = !isCircularMode;
            toggleButton.setText(isCircularMode ? "▪" : "●");
        });

        // Use StackPane for better resizing behavior
        root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #1a1a1a;");
        
        // Add button to top-left corner
        StackPane.setAlignment(toggleButton, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(toggleButton, new javafx.geometry.Insets(10, 0, 0, 10));
        root.getChildren().add(toggleButton);
        
        Scene scene = new Scene(root, 800, 400);

        // Bind canvas size to scene size for responsive resizing
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());
        
        // Redraw when canvas size changes
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> drawVisualization());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> drawVisualization());

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
        stage.setMinWidth(400);
        stage.setMinHeight(200);
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
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        
        // Clear canvas with dark greyish-black background
        gc.setFill(Color.rgb(26, 26, 26));
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
        
        if (isCircularMode) {
            drawCircularVisualization(canvasWidth, canvasHeight);
        } else {
            drawBarVisualization(canvasWidth, canvasHeight);
        }
    }
    
    private void drawBarVisualization(double canvasWidth, double canvasHeight) {
        // Draw frequency bars
        int bandCount = currentMagnitudes.length;
        double bandWidth = canvasWidth / (double) bandCount;
        double maxHeight = canvasHeight;
        double baselineY = canvasHeight; // Bars start from bottom
        
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
        gc.setLineWidth(Math.max(1, canvasHeight * 0.002));
        gc.strokeLine(0, baselineY - 1, canvasWidth, baselineY - 1);
    }
    
    private void drawCircularVisualization(double canvasWidth, double canvasHeight) {
        double centerX = canvasWidth / 2;
        double centerY = canvasHeight / 2;
        double baseRadius = Math.min(canvasWidth, canvasHeight) * 0.15;
        double maxRadius = Math.min(canvasWidth, canvasHeight) * 0.4;
        
        int bandCount = currentMagnitudes.length;
        double angleStep = 2 * Math.PI / bandCount;
        
        // Draw center circle
        gc.setFill(Color.rgb(60, 60, 60, 0.3));
        gc.fillOval(centerX - baseRadius * 0.5, centerY - baseRadius * 0.5, 
                   baseRadius, baseRadius);
        
        // Draw circular waves
        for (int i = 0; i < bandCount; i++) {
            double magnitude = currentMagnitudes[i];
            
            if (magnitude > 0.001) {
                double angle = i * angleStep;
                
                // Calculate wave parameters
                double waveHeight = magnitude * (maxRadius - baseRadius) * 1.5;
                waveHeight = Math.pow(waveHeight / (maxRadius - baseRadius), 0.7) * (maxRadius - baseRadius);
                double currentRadius = baseRadius + waveHeight;
                
                // Calculate points for smooth wave segments
                double x1 = centerX + Math.cos(angle - angleStep * 0.4) * baseRadius;
                double y1 = centerY + Math.sin(angle - angleStep * 0.4) * baseRadius;
                double x2 = centerX + Math.cos(angle) * currentRadius;
                double y2 = centerY + Math.sin(angle) * currentRadius;
                double x3 = centerX + Math.cos(angle + angleStep * 0.4) * baseRadius;
                double y3 = centerY + Math.sin(angle + angleStep * 0.4) * baseRadius;
                
                // Create gradient effect
                double intensity = Math.min(waveHeight / (maxRadius - baseRadius), 1.0);
                double alpha = Math.min(0.4 + intensity * 0.6, 0.9);
                
                // Draw wave segment
                gc.setFill(Color.rgb(255, 255, 255, alpha));
                gc.setStroke(Color.rgb(255, 255, 255, alpha * 0.7));
                gc.setLineWidth(Math.max(2, Math.min(canvasWidth, canvasHeight) * 0.005));
                
                // Draw as a triangle/wave segment
                gc.beginPath();
                gc.moveTo(x1, y1);
                gc.lineTo(x2, y2);
                gc.lineTo(x3, y3);
                gc.closePath();
                gc.fill();
                
                // Add glow effect for high intensity
                if (intensity > 0.5) {
                    gc.setFill(Color.rgb(255, 255, 255, intensity * 0.2));
                    double glowRadius = currentRadius + 5;
                    double gx1 = centerX + Math.cos(angle - angleStep * 0.5) * baseRadius;
                    double gy1 = centerY + Math.sin(angle - angleStep * 0.5) * baseRadius;
                    double gx2 = centerX + Math.cos(angle) * glowRadius;
                    double gy2 = centerY + Math.sin(angle) * glowRadius;
                    double gx3 = centerX + Math.cos(angle + angleStep * 0.5) * baseRadius;
                    double gy3 = centerY + Math.sin(angle + angleStep * 0.5) * baseRadius;
                    
                    gc.beginPath();
                    gc.moveTo(gx1, gy1);
                    gc.lineTo(gx2, gy2);
                    gc.lineTo(gx3, gy3);
                    gc.closePath();
                    gc.fill();
                }
                
                // Draw connecting line from center
                gc.setStroke(Color.rgb(255, 255, 255, alpha * 0.3));
                gc.setLineWidth(1);
                gc.strokeLine(centerX + Math.cos(angle) * baseRadius * 0.5, 
                             centerY + Math.sin(angle) * baseRadius * 0.5, x2, y2);
            }
        }
        
        // Draw center dot
        gc.setFill(Color.rgb(255, 255, 255, 0.8));
        double dotSize = Math.min(canvasWidth, canvasHeight) * 0.01;
        gc.fillOval(centerX - dotSize, centerY - dotSize, dotSize * 2, dotSize * 2);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}