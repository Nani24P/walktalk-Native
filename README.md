# WalkTalk Native Starter

This repo contains:

- `android/` — Native Kotlin + Jetpack Compose Android app.
- `server/` — Small Node.js WebSocket signaling server for Build 2.
- `.github/workflows/android-debug-apk.yml` — GitHub Actions APK builder.

## Build 1 included

- Native Android shell
- WalkTalk-style dark UI
- Notification Center screen
- Local Android notification test
- Microphone permission request
- Android 12+ support: `minSdk 31`

## Build 2 included

- WebSocket signaling client in Android app
- Connect / join room / send chat / send ping / send alert
- Minimal Node.js signaling server

## Build APK on GitHub

1. Upload this repo to GitHub.
2. Open **Actions**.
3. Run **Build Android Debug APK**.
4. Download artifact **WalkTalk-debug-apk**.
5. Install APK on Android phone.

## Run signaling server locally

```bash
cd server
npm install
npm start
```

Default server URL:

```text
ws://localhost:8787
```

For a real Android phone, `localhost` means the phone itself. Deploy the server online or use your computer/local network IP.
