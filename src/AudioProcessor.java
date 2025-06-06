package src;

import javax.sound.sampled.*;
import java.io.File;

public class AudioProcessor {
    public static float[] getAudioSamples(String filePath) throws Exception {
        // Open the audio file
        File audioFile = new File(filePath);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        
        // Convert to standard PCM format
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
        
        // Read all bytes
        byte[] bytes = pcmStream.readAllBytes();
        pcmStream.close();
        
        // Convert to float samples (-1.0 to 1.0)
        float[] samples = new float[bytes.length / 2];
        for (int i = 0; i < samples.length; i++) {
            short sample = (short)((bytes[i*2] & 0xFF) | (bytes[i*2+1] << 8));
            samples[i] = sample / 32768.0f;
        }
        return samples;
    }
}
