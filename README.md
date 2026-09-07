# 🎲 Ludo Classic — Android

A redesigned, native Android Ludo game by **Harsh Nagar**, built for local pass-and-play on one phone.

## ✨ What's new in 2.0

- 👥 **2, 3, or 4 human players** on one device
- 📱 Pass-and-play turn system — no internet or account required
- 🎨 Completely refreshed game menu and board presentation
- 🎯 Four tokens per player
- 🎲 Random dice with a clear turn panel
- 🚪 Six required to leave the yard
- ⭐ Safe squares
- 💥 Capture opponent tokens on non-safe squares
- 🧱 Blocked-square movement rule
- 🏠 Home lane and exact finishing at 56
- 🔁 Rolling a six gives an extra turn
- 🏆 Full winner screen and instant new game
- 💾 Player names are remembered locally
- 🔒 No network permission or online service is needed

## 🎮 How to play

1. Choose **2–4 players**.
2. Enter each player's name.
3. Tap **START GAME**.
4. The active player taps **ROLL**.
5. When a token is highlighted, tap it to move.
6. Roll a **6** to bring a token out of the yard.
7. Capture opponents to send their token back to the yard.
8. Get all four tokens home first to win.

## 🛠 Tech

- Kotlin
- Android SDK 35
- Custom Canvas-based board
- Minimum Android API 23
- JVM 17
- Fully offline runtime

## 📦 Build

Open the project in Android Studio and build the `app` module.

With Gradle installed:

```bash
gradle assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

## 👨‍💻 Developer

**Harsh Nagar**
