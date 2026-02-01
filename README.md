# EchoNote

A minimal, offline-first voice journaling app for Android. Record your thoughts in seconds with no typing, no login, and no cloud storage.

## Features

### Core Functionality
- **Quick Recording**: Tap the large center button to start/stop recording
- **Waveform Visualization**: Real-time animated waveform display during recording
- **Auto-organized**: Voice notes automatically grouped by Today, Yesterday, and Older
- **Playback Controls**: Music-player style controls with seek bar, current time, and duration
- **Scrub Audio**: Drag the slider to jump to any position in the recording
- **Pause/Resume**: Pause and resume playback at any time
- **Share**: Export voice notes to WhatsApp, Drive, or any app
- **Delete**: Remove notes you no longer need
- **Dark Mode**: Automatic system theme support

### Technical Highlights
- **Offline-first**: All data stored locally on device
- **No accounts**: Zero authentication required
- **Privacy-focused**: No cloud, no tracking, no data collection
- **Modern UI**: Built with Jetpack Compose and Material 3
- **Clean architecture**: MVVM pattern with Repository layer

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Database**: Room (for metadata)
- **Storage**: Local file system (for audio files)
- **Audio**: MediaRecorder & MediaPlayer
- **Architecture**: MVVM + Repository pattern

## Requirements

- Android 12 (API 31) or higher
- Microphone permission

## Project Structure

```
app/src/main/java/com/example/dailyvoicejournalapp/
├── audio/
│   ├── AudioRecorder.kt      # MediaRecorder wrapper
│   └── AudioPlayer.kt        # MediaPlayer wrapper
├── data/
│   ├── VoiceNote.kt          # Room entity
│   ├── VoiceNoteDao.kt       # Database queries
│   └── AppDatabase.kt        # Room database
├── repository/
│   └── VoiceNoteRepository.kt # Data layer
├── ui/
│   ├── MainViewModel.kt       # State management
│   ├── components/
│   │   ├── RecordButton.kt        # Animated record button
│   │   ├── VoiceNoteItem.kt       # Note list item with audio controls
│   │   ├── AudioController.kt     # Music-style playback controller
│   │   ├── DateHeader.kt          # Section headers
│   │   └── WaveformVisualizer.kt  # Real-time waveform display
│   └── screens/
│       └── MainScreen.kt      # Main UI
└── MainActivity.kt            # Entry point
```

## Building & Running

1. Open the project in Android Studio
2. Sync Gradle files
3. Run on emulator or physical device (API 31+)

## Play Store Listing

**Title**: EchoNote – Minimal Voice Recorder

**Description**: Record your thoughts in seconds. No login. No cloud. Just you.

**Category**: Productivity

**Tags**: voice journal, voice notes, audio diary, minimal, offline, privacy

## Future Enhancements (Optional)

- Daily reminder notifications
- Mood tags before recording
- Auto-stop at 60 seconds
- Audio playback speed control (0.5x, 1x, 1.5x, 2x)
- Export all notes as ZIP
- Playback waveform (show recorded waveform during playback)
- Background playback with notification controls

## License

Built as a portfolio project. Feel free to use and modify.
