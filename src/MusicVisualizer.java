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
        System.out.println("Canvas created with dimensions: " + canvas.getWidth() + "x" + canvas.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create play/pause button
        Button playButton = new Button("Play/Pause");
        playButton.setOnAction(e -> togglePlayback());

        VBox root = new VBox(10, playButton, canvas);
        root.setStyle("-fx-background-color: #0a1428;");
        Scene scene = new Scene(root, 800, 600);
        scene.setFill(Color.rgb(10, 20, 40));

        // Initialize media player
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            System.err.println("Audio file not found: " + audioFile.getAbsolutePath());
            Platform.exit();
            return;
        }

        Media media = new Media(audioFile.toURI().toString());
        System.out.println("Media loaded from: " + audioFile.getAbsolutePath());
        mediaPlayer = new MediaPlayer(media);

        // ✅ ADD THESE LINES to enable spectrum visualization
        mediaPlayer.setAudioSpectrumInterval(0.05);   // updates ~20 times per second
        mediaPlayer.setAudioSpectrumNumBands(64);     // number of bars
        mediaPlayer.setAudioSpectrumThreshold(-60);   // sensitivity baseline

        mediaPlayer.setOnReady(() -> System.out.println("MediaPlayer is ready"));
        mediaPlayer.setOnError(() -> System.out.println("MediaPlayer error: " + mediaPlayer.getError()));

        // Setup visualization
        mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setFill(Color.rgb(10, 20, 40, 0.3));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            int bandCount = 64;
            double bandWidth = canvas.getWidth() / (double) bandCount;

            for (int i = 0; i < bandCount; i++) {
                int magIndex = (int)((i / (double) bandCount) * magnitudes.length);
                double magnitude = -magnitudes[magIndex] - 60;
                double height = Math.max(0, magnitude * 8);

                double colorRatio = Math.min(1.0, height / 100.0);
                Color bandColor = Color.color(
                    0.0,
                    1.0 - colorRatio * 0.5,
                    1.0 - colorRatio * 0.3
                );

                gc.setFill(bandColor);
                gc.fillRect(
                    i * bandWidth,
                    canvas.getHeight() - height,
                    bandWidth - 2,
                    height
                );
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
// Note: Ensure you have the JavaFX libraries set up correctly in your project.