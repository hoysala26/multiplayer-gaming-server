package client;

import javax.sound.sampled.*;

public class SoundEngine {

    public static void playShoot() {
        new Thread(() -> generateTone(800, 150, 100, true)).start(); // High pitch slide down
    }

    public static void playJump() {
        new Thread(() -> generateTone(300, 200, 100, false)).start(); // Low pitch slide up
    }

    public static void playHit() {
        new Thread(() -> generateNoise(150)).start(); // White noise (Explosion)
    }

    public static void playWin() {
        new Thread(() -> {
            generateTone(400, 100, 100, false);
            try { Thread.sleep(100); } catch(Exception e){}
            generateTone(600, 100, 100, false);
            try { Thread.sleep(100); } catch(Exception e){}
            generateTone(800, 300, 100, false);
        }).start();
    }

    public static void playSelect() {
        new Thread(() -> generateTone(1000, 50, 50, false)).start(); // Short blip
    }

    // --- SYNTHESIZER LOGIC ---
    
    // Generates a Sine Wave Tone
    private static void generateTone(int startFreq, int durationMs, int volume, boolean slideDown) {
        try {
            float sampleRate = 44100;
            byte[] buf = new byte[1];
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();

            for (int i = 0; i < durationMs * (sampleRate / 1000); i++) {
                // Calculate frequency (Slide effect)
                double angle;
                if (slideDown) {
                    angle = i / (sampleRate / (startFreq - (i / 10.0))); 
                } else {
                    angle = i / (sampleRate / (startFreq + (i / 10.0)));
                }
                
                // Math.sin generates the wave
                buf[0] = (byte) (Math.sin(2.0 * Math.PI * angle) * volume);
                sdl.write(buf, 0, 1);
            }
            sdl.drain();
            sdl.close();
        } catch (Exception e) {}
    }

    // Generates White Noise (Static)
    private static void generateNoise(int durationMs) {
        try {
            float sampleRate = 44100;
            byte[] buf = new byte[1];
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();

            for (int i = 0; i < durationMs * (sampleRate / 1000); i++) {
                buf[0] = (byte) (Math.random() * 200 - 100); // Random bytes = Noise
                sdl.write(buf, 0, 1);
            }
            sdl.drain();
            sdl.close();
        } catch (Exception e) {}
    }
}