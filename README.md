# My Shogi

A modern, Jetpack Compose-based Shogi (Japanese Chess) application for Android.

![Shogi Board](app/src/main/res/drawable/shogi_icon_main.png)

## Features

- **Full Shogi Rules**: Supports all standard moves, including piece promotion and capturing.
- **Modern UI**: Built entirely with Jetpack Compose for a smooth and responsive experience.
- **Traditional Aesthetics**: Features a beautiful wooden board texture (`mokume`) and traditional piece designs.
- **Two-Player Local Play**: Play against a friend on the same device.
- **Move History & Undo**: Supports the "Matta" (undo) feature (up to 3 times per player).
- **Game State Management**: Automatically saves game progress using `rememberSaveable`.

## Screenshots

*(Screenshots will be placed here)*

## Project Structure

- `app/`: The main Android application module.
    - `src/main/java/com/example/myshogi/`: Contains the Kotlin source code.
        - `MainActivity.kt`: Entry point of the app.
        - `ShogiUi.kt`: UI components and screen definitions.
        - `ShogiModel.kt`: Game logic and rules implementation.
- `gradle/`: Contains the Version Catalog (`libs.versions.toml`) for dependency management.

## Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Minimum SDK**: 34
- **Target SDK**: 35
- **Build System**: Gradle with Version Catalog

## Getting Started

### Prerequisites

- Android Studio Ladybug or later.
- Android SDK 35.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/MyShogi.git
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Run the app on an emulator or a physical device (API 34+).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
