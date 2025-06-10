# Live Audio Visualizer

## Features
- Real-time visualization of system audio input (YouTube, music players, microphone, etc.)
- Frequency spectrum display with color gradient (blue to red)
- Responsive visualization that updates at 60fps

## Requirements
- Java 21+ (matching your Mac architecture: arm64 for Apple Silicon, x64 for Intel)
- JavaFX 21 SDK (matching architecture, JARs and native libs in `lib/`)

## Build and Run

### Compile:
```bash
javac -d bin --module-path lib --add-modules javafx.controls,javafx.media src/*.java
```

### Run (macOS):
```bash
java --enable-native-access=ALL-UNNAMED -Dprism.order=sw \
     --module-path lib --add-modules javafx.controls,javafx.media \
     -Djava.library.path=lib -cp bin src.MusicVisualizer
```

## Usage
1. Run the program - a visualization window will open
2. Play any audio on your computer
3. The visualization will respond in real-time to the audio
4. Close the window to stop the program

## Troubleshooting
- If you see errors about missing JavaFX classes, double-check that all JavaFX JARs are in the `lib` folder
- If you see "Cannot find or load main class", ensure you are running the command from the project root and the `bin` folder exists
- If you see "Error initializing QuantumRenderer", make sure you are using the correct JavaFX SDK for your Mac (arm64 for Apple Silicon, x64 for Intel)
- If audio isn't being captured, check your system's audio settings and permissions
