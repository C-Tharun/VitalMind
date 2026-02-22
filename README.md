# 🧠 VitalMind

An AI-Powered Wearable-Based Digital Health Twin for Personalized Wellness Monitoring

## 📌 Overview

VitalMind is a cutting-edge Android application that serves as your personal digital health twin, seamlessly integrating wearable health data from Google Fit to deliver real-time insights, AI-powered recommendations, and comprehensive wellness monitoring.

The app combines traditional health tracking with advanced machine learning features including **real-time stress detection**, **personalized health deviation analysis (PHBD-Net)**, **AI-powered health coaching**, and **geospatial stress mapping**—all built on a production-grade, offline-first architecture.

## 🎯 Key Objectives

- 🔗 Seamless integration with wearable health data (Google Fit)
- 🤖 AI-powered personalized health insights and recommendations
- 📊 Real-time physiological stress detection using ML models
- 🧬 Personalized baseline health deviation analysis
- 👤 User-specific, persistent health profiles
- ⚡ Fast, offline-first data access
- 📈 Rich visual analytics (daily, weekly, historical)
- 🗺️ Geospatial stress terrain visualization
- 🎨 Clean, modern UI using Jetpack Compose + Material 3

---

## ✨ Features

### 🏠 Dashboard (Home Screen)

The central hub for all your health metrics:

#### **Real-Time Health Metrics**
- **Steps** with progress rings and customizable goals
- **Calories** burned tracking
- **Distance** traveled (km)
- **Heart Rate** (average & real-time)
- **Sleep Duration** (hours & minutes)
- **Floors Climbed**
- **Move Minutes** (active time)
- **Weight** tracking
- **Last Recorded Activity** with timestamp

#### **Interactive Goal Tracking**
- Customizable daily goals for steps, calories, and distance
- Visual progress indicators with multi-metric rings
- Goal achievement celebrations

#### **Weekly Trend Charts**
- Visual representation of weekly health patterns
- Real-time UI updates as new data syncs from Google Fit

---

### 🧠 AI-Powered Features

#### 🤖 **VitalMind AI Assistant**

A conversational health chatbot powered by **Groq LLaMA 3.1 (70B)**:

- **Context-Aware Recommendations**: Analyzes your complete health profile
- **Personalized Insights** about:
  - Activity patterns
  - Sleep quality
  - Calorie expenditure
  - Heart rate trends
  - Weekly comparisons
- **Intelligent Conversation History**: Maintains context across queries
- **Quick-Start Suggestions**: Common health queries like:
  - "How was my sleep this week?"
  - "Am I meeting my activity goals?"
  - "What's my heart rate trend?"
  - "Suggest improvements for today"

#### 📊 **Baseline Insights & Smart Recommendations**

**7-Day Baseline Analysis**:
- Automatically computes your personal health baseline
- Tracks deviations from your normal patterns
- Monitors:
  - Steps vs. baseline
  - Sleep quality vs. baseline
  - Distance vs. baseline
  - Heart rate vs. baseline

**Weather-Integrated Recommendations**:
- Real-time weather data integration via WeatherAPI
- Context-aware activity suggestions based on:
  - Current weather conditions
  - Air Quality Index (AQI)
  - Temperature suitability
  - Time of day
  - Your activity baseline

**AI-Generated Explanations**:
- On-demand AI explanations for health deviations
- Personalized insights about why metrics changed
- Actionable recommendations to improve health

---

### 🔴 Stress Analysis & Monitoring

#### 📈 **Real-Time Stress Score**

Powered by a custom backend ML model:

**How It Works**:
- Sends physiological data to `/stress_analysis` endpoint
- Analyzes:
  - Heart rate patterns (average & max)
  - Activity levels and sedentary time
  - Step count and distance
  - Sleep duration
  - Calorie expenditure
  - Time of day correlation

**Stress Metrics Displayed**:
- **Stress Score**: Numerical value (0-100)
- **Stress Level**: Low / Medium / High / Extreme
- **Stress Status**: Normal / Elevated / Critical
- **Stress Stability**: Stable / Variable
- **Mood Indicator**: Derived from stress patterns

**Visual Representation**:
- Color-coded stress levels
- Real-time calculation on demand
- Retry mechanism for failed requests
- 30-second timeout with graceful error handling

#### 📜 **Stress Score History**

Track your stress patterns over time:

- **Persistent Storage**: All stress calculations saved locally
- **Historical Tracking**: View past stress scores with timestamps
- **Date-Based Filtering**: Focus on specific time periods
- **Visual Charts**: Trend analysis using Vico charts
- **Detailed Logs**: Request/response JSON for debugging

#### 🗺️ **Stress Terrain Map**

Geospatial visualization of your stress patterns:

- **Google Maps Integration**: Visualize stress zones on a map
- **Location-Based Clustering**: Groups health data by location
- **Dual Visualization Modes**:
  - **Stress Zones**: Areas with high HR deviation
  - **Calming Zones**: Areas with low, stable HR
- **Interactive Features**:
  - Toggle between zone types
  - Marker-based cluster visualization
  - Info dialog explaining methodology
  - Real-time user location tracking

---

### 🏥 Health Deviation Analysis (PHBD-Net)

**Personalized Baseline Deviation Network** — a cutting-edge ML feature for detecting anomalies in your health patterns.

#### 🧬 **Personalized Baseline Modeling**

**10-Day Baseline Collection**:
- Automatically collects daily aggregated health metrics
- Minimum **10 days** required for personalized baseline
- Stored locally per user using Room database

**Baseline Metrics Computed**:
1. **Average Heart Rate**: Mean of all HR samples
2. **Resting Heart Rate**: Derived from sleep or low-activity periods (lowest 10-15% percentile)
3. **Heart Rate Variance**: Statistical variance in HR
4. **Steps Total**: Daily step count
5. **Total Sleep Minutes**: Aggregated sleep duration
6. **Calories Burned**: Total energy expenditure
7. **Sedentary Ratio**: Proportion of sedentary time
8. **Movement Variance**: Variability in activity intensity
9. **Activity Load Index**: Composite metric of activity intensity
10. **Sleep Consistency**: Rolling standard deviation of sleep duration

**Baseline Status States**:
- **COLLECTING** (days < 10):
  - Shows "🔄 Personalizing your health baseline"
  - Displays countdown: "Available in X days"
  - Analyze button disabled
- **READY** (days ≥ 10):
  - Enables "Analyze Health Deviation" button
  - Shows "✅ Baseline Ready" indicator

#### 🔬 **Real-Time Deviation Analysis**

**Backend ML Integration**:
- POST `/health_deviation` endpoint
- Sends only **TODAY's** computed metrics + `userId`
- Does **NOT** send historical baseline data (privacy-first)

**Response Metrics**:
- **Health Deviation Score**: Quantified deviation from your personal baseline
- **Stress Drift Level**: Low 🟢 / Medium 🟠 / High 🔴
- **Confidence**: Percentage indicating data completeness
- **Top Contributors**: Metrics causing the most deviation (e.g., "total_sleep_minutes, steps_total")

**Physiologically Accurate Calculations**:
- **Resting HR**: Derived from sleep sessions or consecutive low-activity periods (step count = 0)
- **Variance**: Computed using statistical standard deviation
- **Activity Load**: Indexed based on movement patterns and intensity
- **Sleep Consistency**: Measured as rolling standard deviation over 7 days

**Smart Data Collection**:
- Baseline collection waits for sufficient daily data
- Guards against incomplete Google Fit sync
- Logs when baseline is skipped due to insufficient data
- Safe from race conditions and premature data collection

#### 🎨 **UI Integration**

**Health Deviation Card** (displayed below Stress Score on Home screen):
- **Deviation Score**: Large, formatted display (e.g., "3.2")
- **Color-Coded Drift Level**:
  - 🟢 **Low** → Green
  - 🟠 **Medium** → Orange
  - 🔴 **High** → Red
- **Confidence Percentage**: Shows data completeness
- **Top Contributors**: Human-readable labels (e.g., "Sleep, Steps, Heart Rate")
- **Partial Data Caption**: "Based on partial data" if confidence < 1.0
- **Fallback Message**: "Health deviation unavailable today" on API failure

**Material 3 Styling**:
- Consistent with app theme
- Rounded corners, elevation, and spacing
- Non-blocking error states with retry options

---

### 📊 Metric History & Trends

Detailed drill-down analytics for all health metrics:

**Supported Metrics**:
- Steps
- Heart Rate
- Calories
- Distance
- Sleep
- Activity

**History Screen Features**:
- **Date Picker**: View data up to 30 days back
- **Summary Statistics**: Total, average, max values
- **Interactive Vico Charts**: Visual trend analysis
- **Raw Data Lists**: Timestamped entries
- **Day Comparison**: Today vs. selected date

---

### 💤 Accurate Sleep Handling

Advanced sleep tracking with midnight-crossing support:

- Correctly handles sleep sessions that cross midnight
- Calculates only the **overlapping portion** for each day
- Prevents over-counting or under-counting sleep duration
- Sleep duration displayed in **hours and minutes**
- Aggregates multiple sleep sessions per day

---

### 👤 Multi-User Support

Secure, user-specific health profiles:

- **Google OAuth 2.0** authentication
- All health data scoped to the signed-in user
- Multiple users can safely use the same device
- User profile display with avatar and name
- Secure sign-out functionality
- Data isolation per user ID

---

### 📶 Offline-First Architecture

Robust, cache-first design for reliability:

- **Room Database**: Single source of truth
- **UI**: Always reads from local storage
- **Cache-Then-Network Strategy**:
  1. Instant screen loading from cache
  2. Background sync from Google Fit
  3. Automatic UI refresh on data update
- **Network Resilience**:
  - Graceful handling of timeouts (30s)
  - Retry mechanisms for failed API calls
  - Offline mode with cached data
  - Network availability detection

---

## 🏗️ Architecture

```
Wearable / Phone Sensors
        ↓
    Google Fit API
        ↓
 GoogleFitManager (Data Sync)
        ↓
HealthDataRepository (Business Logic)
        ↓
Room Database (Local Cache - Single Source of Truth)
        ↓
ViewModel (StateFlow - Reactive State)
        ↓
Jetpack Compose UI (Material 3)
```

**Architectural Highlights**:
- **MVVM Pattern**: Clean separation of concerns
- **Repository Pattern**: Abstracted data layer
- **Reactive UI**: StateFlow for real-time updates
- **Coroutines**: Asynchronous operations without blocking
- **Dependency Injection**: Manual DI for simplicity
- **Modular Design**: Feature-based organization

---

## 🛠️ Tech Stack

### **Android**
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Async Programming**: Coroutines, Flow, StateFlow
- **Local Storage**: Room Database (SQLite)
- **Charts**: Vico (Compose-native charting library)
- **Navigation**: Jetpack Compose Navigation
- **Image Loading**: Coil

### **APIs & Services**
- **Google Fit API**: Health data integration
- **Google Sign-In**: OAuth 2.0 authentication
- **Google Maps API**: Geospatial visualization
- **WeatherAPI**: Real-time weather data
- **Groq AI API**: LLaMA 3.1 (70B) for chatbot
- **Custom Backend**: Stress analysis & health deviation ML models
  - Stress Analysis: `https://vitalmind-stress-api.onrender.com/stress_analysis`
  - Health Deviation: `https://vitalmind-stress-api.onrender.com/health_deviation`
  - Baseline Training: `https://vitalmind-stress-api.onrender.com/train_baseline_model`

### **Networking**
- **Retrofit**: REST API communication (stress & health deviation)
- **Ktor Client**: HTTP client for AI & weather APIs
- **Gson**: JSON serialization
- **Kotlinx Serialization**: Kotlin-native JSON handling

### **Third-Party Libraries**
- **Vico Charts**: Data visualization
- **Coil**: Async image loading
- **Maps Compose**: Google Maps integration
- **Play Services**: Fitness, Auth, Location, Maps

---

## 🧠 AI / ML Features

### **Implemented**
✅ **Real-Time Stress Detection**
  - Backend ML model analyzing physiological data
  - Multi-factor stress calculation (HR, activity, sleep, time of day)
  - Historical stress tracking

✅ **Health Deviation Analysis (PHBD-Net)**
  - Personalized baseline modeling (10-day minimum)
  - **Autoencoder-based per-user baseline training**
  - Automatic model training on server when baseline ready
  - Monthly retraining (30-day cycle)
  - Real-time anomaly detection
  - Drift level classification (Low/Medium/High)
  - Confidence scoring
  - Training status tracking via SharedPreferences

✅ **AI Health Assistant**
  - Groq LLaMA 3.1 (70B) integration
  - Context-aware recommendations
  - Conversational health insights

✅ **Weather-Aware Recommendations**
  - Real-time weather + AQI integration
  - Activity suggestions based on environmental factors

✅ **Stress Terrain Mapping**
  - Geospatial stress visualization
  - Location-based health clustering

### **Architecture Readiness**
The app is designed to easily integrate additional ML features:
- Structured, time-series health data
- User-scoped historical records
- Offline inference capability
- Retrofit/Ktor APIs for model serving

---

## 🔐 Privacy & Ethics

- ✅ Uses Google-provided OAuth and permission systems
- ✅ No medical diagnosis is performed (wellness-oriented only)
- ✅ All insights are informational, not prescriptive
- ✅ Data stored locally on device by default
- ✅ Backend communication uses secure HTTPS
- ✅ User ID is stable but anonymized (Google Account ID)
- ✅ No third-party data sharing
- ✅ Baseline data never sent to backend (privacy-first)

---

## 🚀 Getting Started

### **Prerequisites**
- Android Studio (latest stable version)
- Android device with **Google Fit** installed
- Google account with health data permissions
- Minimum Android SDK: **29** (Android 10)
- Target SDK: **36**

### **API Keys Required**

Create a `local.properties` file in the project root with:

```properties
GROQ_API_KEY=your_groq_api_key_here
WEATHER_API_KEY=your_weatherapi_key_here
GOOGLE_MAPS_API_KEY=your_google_maps_key_here
```

**Where to get API keys**:
- **Groq**: https://console.groq.com/
- **WeatherAPI**: https://www.weatherapi.com/
- **Google Maps**: https://console.cloud.google.com/

### **Installation Steps**

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/VitalMind.git
   cd VitalMind
   ```

2. **Open in Android Studio**:
   - File → Open → Select the VitalMind folder

3. **Add API keys**:
   - Create `local.properties` (see above)

4. **Sync Gradle**:
   - Click "Sync Now" when prompted

5. **Run on Device**:
   - Connect an Android device (emulator may not have Google Fit)
   - Click the "Run" button

6. **Grant Permissions**:
   - Sign in with Google
   - Grant Google Fit permissions when prompted

---

## 📊 Data Flow

### **Health Data Sync**
```
Google Fit → GoogleFitManager → HealthDataRepository → Room DB → ViewModel → UI
```

### **Stress Analysis**
```
Room DB → StressRepository → Backend ML API → StressViewModel → StressScoreCard UI
```

### **Health Deviation**
```
Room DB → HealthDeviationRepository → Baseline Calculation → Backend ML API → HealthDeviationViewModel → Health Deviation Card UI
```

### **AI Assistant**
```
User Input → VitalMindAIViewModel → Groq API (LLaMA 3.1) → AI Response → Chat UI
```

---

## 📄 Project Status

### **Completed Features**
- ✅ Core health data collection (Google Fit integration)
- ✅ Dashboard with real-time metrics
- ✅ Metric history & trends
- ✅ Real-time stress analysis
- ✅ Stress score history
- ✅ Stress terrain map
- ✅ AI health assistant (Groq LLaMA 3.1)
- ✅ Baseline insights with weather integration
- ✅ Health deviation analysis (PHBD-Net)
- ✅ Personalized baseline modeling
- ✅ Multi-user support
- ✅ Offline-first architecture
- ✅ Material 3 UI

### **In Progress / Future Work**
- 🔜 Push notifications for health anomalies
- 🔜 Cloud sync & backup (Firebase)
- 🔜 Exportable health reports (PDF/CSV)
- 🔜 Wearable device direct integration (Samsung Health, Fitbit)
- 🔜 Advanced ML models (sleep quality prediction, activity recommendations)
- 🔜 Social features (health challenges, leaderboards)

---

## 🎓 Academic Context

This project was developed as a **final-year engineering project**, demonstrating:

- ✅ Mobile systems design and architecture
- ✅ Wearable data integration and synchronization
- ✅ Offline-first architecture patterns
- ✅ Data aggregation & analytics
- ✅ Modern Android development practices
- ✅ Machine learning integration (REST APIs)
- ✅ AI-powered conversational interfaces
- ✅ Real-time data visualization
- ✅ Privacy-first design principles

**Technologies Demonstrated**:
- Kotlin (100%)
- Jetpack Compose (Modern Android UI)
- MVVM Architecture
- Coroutines & Flow (Reactive Programming)
- Room Database (Local Persistence)
- Retrofit & Ktor (Network Communication)
- Google Fit API (Health Data)
- Google Maps API (Geospatial Visualization)
- Machine Learning API Integration
- Generative AI (LLaMA 3.1)

---

## 📬 Future Enhancements

### **Short-Term**
- [ ] Daily/weekly AI-generated health summaries
- [ ] Custom notification system for health anomalies
- [ ] Export health reports (PDF, CSV)
- [ ] More detailed activity tracking (exercise types)

### **Medium-Term**
- [ ] Cloud backup and sync (Firebase)
- [ ] On-device ML inference (TensorFlow Lite)
- [ ] Integration with Samsung Health SDK
- [ ] Voice-based AI assistant
- [ ] Widget support for quick health overview

### **Long-Term**
- [ ] Predictive health analytics (forecast stress, sleep quality)
- [ ] Integration with medical IoT devices
- [ ] Social features (health challenges, groups)
- [ ] Gamification (achievements, streaks)
- [ ] Cross-platform (iOS, Web)

---

## 👨‍💻 Authors

**Tharun Subramanian C**  
**Tavish P**  

Final-Year Engineering Students  
**Project**: VitalMind - AI-Powered Digital Health Twin

---

## 📜 License

This project is developed for **academic purposes** as a final-year engineering project.

---

## 🙏 Acknowledgments

- **Google Fit API** for health data integration
- **Groq** for LLaMA 3.1 AI model access
- **WeatherAPI** for real-time weather data
- **Vico Charts** for Compose-native charting
- **Jetpack Compose** team for modern Android UI toolkit
- **VitalMind Backend Team** for ML model development


---

**Built with ❤️ using Kotlin & Jetpack Compose**

