# 🤖 AI Chat Assistant — Android App

An AI-powered conversational chat app built with **Kotlin**, **Google Gemini API**, and **Room Database** as part of the Codec Technologies Android Internship.

---

## ✨ Features

| Feature | Details |
|---|---|
| 💬 AI Chat | Real-time streaming responses via Google Gemini Pro |
| 🎙️ Voice Input | Speak your message using Android SpeechRecognizer |
| 🗂️ Chat History | All conversations saved locally with Room Database |
| 📋 Session Management | Multiple chat sessions, create/delete/resume any |
| ⏹️ Stop Streaming | Cancel a response mid-stream |
| 🌓 Material Design 3 | Clean purple-themed UI with chat bubbles |

---

## 🏗️ Architecture

```
MVVM + Repository Pattern
─────────────────────────────────────────────────
 UI Layer      →  ChatActivity, HistoryActivity
 Adapters      →  MessageAdapter, SessionAdapter
 ViewModel     →  ChatViewModel (StateFlow + Coroutines)
 Repository    →  ChatRepository (Room + Gemini)
 Data Layer    →  Room DB (Message, ChatSession, MessageDao)
 Remote        →  Gemini API (generativeai SDK)
```

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM + Repository
- **AI:** Google Gemini Pro via `generativeai` SDK 0.7.0
- **Database:** Room 2.6.1 with Kotlin Flow
- **Async:** Kotlin Coroutines + StateFlow
- **UI:** RecyclerView, ViewBinding, Material Components
- **Voice:** Android `SpeechRecognizer`

---

## 🚀 Setup & Run

### Step 1 — Clone the repo
```bash
git clone https://github.com/AdityaKaradbhajne/ai-chat-assistant.git
cd ai-chat-assistant
```

### Step 2 — Get a free Gemini API key
Go to https://aistudio.google.com/app/apikey and create a key.

### Step 3 — Add the key to local.properties
```
cp local.properties.template local.properties
```
Edit `local.properties`:
```
sdk.dir=/Users/AdityaKaradbhajne/Library/Android/sdk
GEMINI_API_KEY=your_actual_key_here
```

> ⚠️ `local.properties` is in `.gitignore` — it will never be committed.

### Step 4 — Build and run
Open in Android Studio and click **Run**, or use:
```bash
./gradlew assembleDebug
```
Minimum SDK: **26 (Android 8.0)**

---

## 📁 Project Structure

```
app/src/main/
├── java/com/example/aichat/
│   ├── data/
│   │   ├── Message.kt            ← Room entity + MessageRole enum
│   │   ├── ChatSession.kt        ← Session entity
│   │   ├── MessageDao.kt         ← Room DAO queries
│   │   ├── ChatDatabase.kt       ← Singleton Room DB
│   │   └── ChatRepository.kt    ← Bridges Room + Gemini API
│   └── ui/
│       ├── ChatViewModel.kt      ← Core logic (streaming, sessions)
│       ├── ChatActivity.kt       ← Main chat screen + voice input
│       ├── MessageAdapter.kt     ← RecyclerView adapter (3 view types)
│       └── history/
│           ├── HistoryActivity.kt
│           └── SessionAdapter.kt
├── res/
│   ├── layout/                   ← All XML layouts
│   ├── drawable/                 ← Vector icons + shape drawables
│   └── values/                   ← Colors, strings, themes
└── AndroidManifest.xml
```

---

## 💡 Key Implementation Details

**Streaming Responses** — The ViewModel collects Gemini's token stream and updates the same Room DB row in real-time so partial responses appear as the AI types.

**Conversation Context** — Every `sendMessage()` passes the full session history to Gemini, ensuring coherent multi-turn conversations.

**API Key Security** — Key is read from `local.properties` via `BuildConfig`. Never hardcoded, excluded from Git.

---

## 👨‍💻 Author

Built by **Aditya Karadbhajne** during the Android Development Internship at **Codec Technologies**
