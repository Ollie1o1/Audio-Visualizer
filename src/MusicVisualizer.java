package src;

import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MusicVisualizer extends Application {
    private String audioPath = "sample.wav";
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    
    @Override
    public void start(Stage stage) throws Exception {
        // Create canvas for drawing
        Canvas canvas = new Canvas(800, 500);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Create play/pause button
        Button playButton = new Button("Play");
        playButton.setOnAction(e -> togglePlayback());
        
        VBox root = new VBox(10, playButton, canvas);
        Scene scene = new Scene(root, 800, 600);
        
        // Initialize media player
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            System.err.println("Audio file not found: " + audioFile.getAbsolutePath());
            Platform.exit();
            return;
        }

        Media media = new Media(audioFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        
        // Setup visualization
        mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setFill(Color.BLUE);
            
            for (int i = 0; i < magnitudes.length; i++) {
                double height = Math.max(0, -magnitudes[i] - 60) * 5;
                gc.fillRect(i * 10, 400 - height, 8, height);
            }
        });

        stage.setTitle("Music Visualizer");
        stage.setScene(scene);
        stage.show();
    }

    private void togglePlayback() {
        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
        } else {
            mediaPlayer.play();
            isPlaying = true;
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
