# Gesture Recognition App with Android and Mediapipe

This Android application utilizes Google's Mediapipe library to recognize gestures in images captured from the camera or selected from device storage. Once a gesture is recognized, the application displays its name and plays it back using Text-to-Speech (TTS).

## Features

- **Image Selection**: Allows users to select an image from device storage.
- **Image Capture**: Enables users to capture an image using the device's camera.
- **Gesture Recognition**: Uses Mediapipe for recognizing gestures in selected or captured images.
- **Gesture Display and Playback**: Shows the recognized gesture's name in a `TextView` and plays it using Text-to-Speech in English.

## Technologies Used

- **Android SDK**: Developed using Kotlin and Android libraries.
- **Mediapipe**: Google's library for image processing and gesture recognition.
- **Text-to-Speech (TTS)**: Used to play back the recognized gesture's name in English.

## Requirements

- Android device running Android 5.0 (API level 21) or higher.
- Camera (`CAMERA`) and internet access (`INTERNET`) permissions for full functionality.


## Documentation
- **Google Mediapipe : https://ai.google.dev/edge/mediapipe/solutions/guide
- **Gesture Recognition : https://ai.google.dev/edge/mediapipe/solutions/vision/gesture_recognizer
