# MusicVisualizer

## Requirements
- Java 21+ (matching your Mac architecture: arm64 for Apple Silicon, x64 for Intel)
- JavaFX 21 SDK (matching architecture, JARs and native libs in `lib/`)
- Place your audio file as `sample.wav` in the project root.

## Compile
javac -d bin --module-path lib --add-modules javafx.controls,javafx.media src/MusicVisualizer.java src/AudioProcessor.java

## Run (macOS fix)
java --enable-native-access=ALL-UNNAMED -Dprism.order=sw --module-path lib --add-modules javafx.controls,javafx.media -Djava.library.path=lib -cp bin src.MusicVisualizer

## Troubleshooting
- If you see errors about missing JavaFX classes, double-check that all JavaFX JARs are in the `lib` folder.
- If you see "Audio file not found", make sure `sample.wav` is in the project root.
- If you see "Cannot find or load main class", ensure you are running the command from the project root and the `bin` folder exists.
- If you see "Error initializing QuantumRenderer: no suitable pipeline found", make sure you are using the correct JavaFX SDK for your Mac (arm64 for Apple Silicon, x64 for Intel), and add `-Djava.library.path=lib` to your run command.