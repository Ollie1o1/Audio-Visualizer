package src;

import javax.sound.sampled.*;
import java.io.File;

public class AudioProcessor {
    private static final int BUFFER_SIZE = 4096;
    private static TargetDataLine line;
    private static boolean isRunning = false;
    private static float[] currentSamples = new float[BUFFER_SIZE/2];
    
    public static void startAudioCapture() throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new Exception("Line not supported");
        }
        
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        isRunning = true;
        
        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isRunning) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    // Convert to float samples (-1.0 to 1.0)
                    for (int i = 0; i < bytesRead/2; i++) {
                        short sample = (short)((buffer[i*2] & 0xFF) | (buffer[i*2+1] << 8));
                        currentSamples[i] = sample / 32768.0f;
                    }
                }
            }
        }).start();
    }
    
    public static void stopAudioCapture() {
        isRunning = false;
        if (line != null) {
            line.stop();
            line.close();
        }
    }
    
    public static float[] getCurrentSamples() {
        return currentSamples;
    }
    
    // Old file-based method kept for compatibility
    public static float[] getAudioSamples(String filePath) throws Exception {
        File audioFile = new File(filePath);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        
        AudioFormat baseFormat = audioStream.getFormat();
        AudioFormat decodedFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.getSampleRate(),
            16,
            baseFormat.getChannels(),
            baseFormat.getChannels() * 2,
            baseFormat.getSampleRate(),
            false
        );
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);
        
        byte[] bytes = pcmStream.readAllBytes();
        pcmStream.close();
        
        float[] samples = new float[bytes.length / 2];
        for (int i = 0; i < samples.length; i++) {
            short sample = (short)((bytes[i*2] & 0xFF) | (bytes[i*2+1] << 8));
            samples[i] = sample / 32768.0f;
        }
        return samples;
    }
}
