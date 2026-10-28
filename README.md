# 🏏 12THMAN – Cricket Players & Umpires Booking and Tournament Organizing App

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange.svg)](https://kotlinlang.org/)
[![MVVM](https://img.shields.io/badge/Architecture-MVVM-blue.svg)](https://developer.android.com/topic/architecture)
[![Material 3](https://img.shields.io/badge/UI-Material%203-green.svg)](https://material.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**12THMAN** is a full-featured, native Android application built for cricket enthusiasts, players, umpires, and tournament organizers. It streamlines player scouting, professional umpire hiring, corporate/club tournament hosting, team registrations, fixture scheduling, and real-time live match scoring.

---

## ✨ Key Features

- **Player Scouting & Booking**: Browse, search, and filter verified cricket players (Batsmen, Bowlers, All-Rounders, Wicket Keepers) by role, location, rating, and match fees.
- **Umpire Hiring**: Hire certified umpires (State Level, National Certified, ICC Accredited) for matches and tournaments.
- **Tournament Organization**: Host and manage cricket leagues (T20, ODI, Box Cricket), set entry fees, prize pools, and venue details.
- **Team Registration**: Register team squads with captain details and player lineups.
- **Interactive Live Match Scoring**: Conduct matches with a full-featured cricket scoring keypad (0-6 runs, Wickets, Wides, No Balls, Innings Switching, and automated match result calculation).
- **Ball-by-Ball Commentary**: Real-time match commentary log updated instantly during live scoring.
- **User Profile & Advanced Settings**: Account management, Push Notifications toggle, Dark Theme toggle, Location Services toggle, Currency Preference (`INR (₹)`, `USD ($)`, etc.), Clear Cache, and Terms & Conditions.

---

## 🏛️ Architecture & Tech Stack

- **Language**: 100% Kotlin
- **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern
- **UI Framework**: XML Layouts with ViewBinding & Material Design 3 Components
- **Navigation**: Android Jetpack Navigation Component with Safe Args & Bottom Navigation
- **Concurrency**: Kotlin Coroutines & `LiveData` / `StateFlow`
- **Testing**: JUnit & AndroidX Instrumentation Testing

---

## 📱 App Screens & Navigation Flow

1. **Splash Screen**: Brand logo, tagline, and quick entry actions.
2. **Sign In / Registration**: Secure user authentication and sign-up with input validation.
3. **Role Selection**: Tailored onboarding for **Cricket Player**, **Cricket Umpire**, **Tournament Organizer**, and **Scorer**.
4. **Home Dashboard**: Quick action tiles, featured players, top-rated umpires, and active tournament banners.
5. **Players & Umpires Directory**: Search bars, filter chips, rating badges, and direct booking forms.
6. **Tournaments & Fixtures**: Overview, team registrations, and match schedules grouped by stages.
7. **Live Scoring Console**: Real-time scoreboard, scoring controls, and commentary feed.
8. **Profile & Settings**: Account preferences, dark mode, currency selector, and cache management.

---

## 🎨 Visual Identity & Design System

12THMAN follows a sleek, professional dark sports aesthetic inspired by modern Figma designs:
- **Primary Background**: Deep Navy (`#07111F`)
- **Card Surfaces**: Dark Blue (`#112033`) with subtle borders (`#334155`)
- **Primary Accents**: Mint Green (`#45E6A4`) for buttons and active indicators
- **Typography**: High-contrast Pure White (`#FFFFFF`) headings and Bright Slate (`#CBD5E1`) secondary text

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 11 or higher
- Android SDK 24 (Min) to SDK 35 (Target)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/akashdeva2008-lang/12TH-MAN.git
   ```
2. Open the project in Android Studio.
3. Let Gradle sync dependencies.
4. Run the app on an Android Emulator or physical device (`Shift + F10`).

---

## 📦 Project Structure

```text
com.example.twelveman/
├── MainActivity.kt
├── navigation/
│   └── AppNavigator.kt
├── ui/
│   ├── splash/
│   ├── auth/
│   ├── home/
│   ├── players/
│   ├── umpires/
│   ├── bookings/
│   ├── tournaments/
│   └── profile/
├── data/
│   ├── model/
│   ├── repository/
│   └── local/
└── utils/
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
