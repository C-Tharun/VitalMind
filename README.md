# 🧠 VitalMind

An AI-Ready Wearable-Based Digital Health Twin for Personalized Wellness Monitoring

## 📌 Overview

VitalMind is a modern Android application that acts as a personal digital health twin, collecting, storing, analyzing, and visualizing health data from wearable devices via Google Fit.
It provides users with real-time insights, historical trends, and metric-wise drill-down analytics, built on a production-grade offline-first architecture.

The project is designed to be AI/ML-ready, enabling future extensions such as stress detection, anomaly detection, and personalized wellness insights.

## 🎯 Key Objectives

- Seamless integration with wearable health data (Google Fit)
- User-specific, persistent health profiles
- Fast, offline-first data access
- Rich visual analytics (daily, weekly, historical)
- Scalable architecture suitable for AI/ML extensions
- Clean, modern UI using Jetpack Compose

## ✨ Features

### 🏠 Dashboard (Home Screen)
- Displays today’s health summary:
  - Steps
  - Calories
  - Distance
  - Heart Rate
  - Sleep Duration
  - Floors Climbed
  - Move Minutes
  - Weight
  - Last Recorded Activity
- Weekly trend charts for key metrics
- Real-time UI updates as new data is synced

### 📊 Metric History & Trends
- Tap any metric to view detailed history
- Date picker (up to 30 days back)
- Supported metrics:
  - Steps
  - Heart Rate
  - Calories
  - Distance
  - Sleep
  - Activity
- Each history screen includes:
  - Summary statistics
  - Interactive charts
  - Raw data lists

### 💤 Accurate Sleep Handling
- Correctly handles sleep sessions that cross midnight
- Calculates only the overlapping portion for each day
- Prevents over-counting or under-counting sleep duration

### 👤 Multi-User Support
- Google Account–based sign-in
- All health data is scoped to the signed-in user
- Multiple users can safely use the same device

### 📶 Offline-First Architecture
- Local Room database acts as the single source of truth
- UI always reads from local storage
- Cache-then-network strategy:
  - Instant screen loading from cache
  - Background sync from Google Fit
  - Automatic UI refresh on data update

## 🏗️ Architecture

Wearable / Phone Sensors  
        ↓  
     Google Fit  
        ↓  
GoogleFitManager  
        ↓  
HealthDataRepository  
        ↓  
Room Database (Single Source of Truth)  
        ↓  
ViewModel (StateFlow)  
        ↓  
Jetpack Compose UI

**Architectural Highlights**
- Repository pattern for clean data abstraction
- Reactive UI using StateFlow
- Coroutines for asynchronous operations
- Modular, extensible codebase

## 🛠️ Tech Stack
- **Android**
  - Language: Kotlin
  - UI: Jetpack Compose + Material 3
  - Architecture: MVVM
  - Async: Coroutines, Flow / StateFlow
  - Local Storage: Room Database
  - Charts: Vico (Compose-native charts)
  - Navigation: Jetpack Compose Navigation
- **APIs & Services**
  - Google Fit API
  - Google Sign-In
  - OAuth 2.0

## 🧠 AI / ML Readiness

Although VitalMind currently focuses on robust data collection and analytics, the architecture is designed to support:
- Stress detection models
- Anomaly detection in vitals
- Trend-based predictions
- Personalized wellness recommendations

Clean, structured, user-specific historical data makes the system ideal for future ML pipelines.

## 🔐 Privacy & Ethics
- Uses Google-provided OAuth and permission systems
- No medical diagnosis is performed
- All insights are wellness-oriented
- Data is stored locally on the device unless explicitly extended

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable)
- Android device with Google Fit installed
- Google account with health data

### Steps
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle
4. Run the app on a physical device
5. Sign in with Google and grant Google Fit permissions

## 📄 Project Status

- ✅ Core features implemented
- ✅ Stable, offline-first architecture
- ✅ Ready for academic submission
- 🔜 AI/ML extensions (future work)

## 🎓 Academic Context

This project was developed as a final-year engineering project, demonstrating:
- Mobile systems design
- Wearable data integration
- Offline-first architecture
- Data aggregation & analytics
- Modern Android development practices

## 📬 Future Enhancements
- AI-generated daily/weekly health summaries
- Stress level prediction using ML
- Push notifications for anomalies
- Cloud sync & backup
- Exportable health reports (PDF)

## 👨‍💻 Author

Tharun Subramanian C, Tavish P
Final-Year Engineering Student  
Project: VitalMind
