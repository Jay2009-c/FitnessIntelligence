# FitnessIntelligence 🏃‍♂️📊

**FitnessIntelligence** is a high-performance Android application built to bridge the gap between consumer-grade fitness tracking and professional sports science. By integrating directly with **Google Health Connect**, it extracts high-frequency data Fitness devices and applies advanced physiological models to provide insights typically reserved for elite athletes.

---

## 🏗 Core Architecture & Mechanics

The application is built on **Clean Architecture** principles, ensuring a strict separation between data acquisition, mathematical processing, and UI presentation.

### 1. The Data Pipeline (Health Connect Integration)
Unlike standard apps that read simple summaries, FitnessIntelligence performs deep-data extraction:
*   **Multi-Record Synchronization:** The app concurrently fetches `ExerciseSessionRecord`, `HeartRateRecord`, `DistanceRecord`, `StepsRecord`, and `PowerRecord`.
*   **High-Frequency Sampling:** It retrieves individual heart rate samples (intraday data) to calculate HRV-derived metrics and cardiac drift, rather than relying on session averages.
*   **Scientific Resting HR:** Instead of a static user input, the app calculates a "Scientific Resting HR" by analyzing heart rate samples between **2:00 AM and 6:00 AM** over a rolling 30-day window, using the 1st percentile of samples to filter out sleep disturbances.

### 2. Physiological Calculation Engine
The `CalculateAdvancedMetricsUseCase` is the heart of the application, implementing peer-reviewed formulas:

#### **A. VO2 Max Comparative Analysis**
The app calculates VO2 Max using four distinct models to provide a high-confidence estimate:
*   **Uth-Sørensen:** `VO2 Max = 15.3 * (HR_max / HR_rest)` (Reliable for general fitness).
*   **Cooper Test (D12):** Based on the distance-to-time ratio in sustained efforts (>5 mins).
*   **ACSM Metabolic Equations:** Velocity-based calculations for running and walking.
*   **Friend Model (Primary):** A sophisticated model that weights metabolic speed against **Heart Rate Reserve (HRR)** utilization.

#### **B. Training Impulse (TRIMP) & Load**
Quantifying internal stress is handled via three methodologies:
*   **Banister’s Exponential TRIMP:** Uses an exponential weighting factor so that high-intensity minutes are weighted significantly more than low-intensity minutes.
    *   *Formula (Male):* `w = Duration * ΔHR * 0.64 * exp(1.92 * ΔHR)`
*   **Edwards’ Zone TRIMP:** Multiplies time spent in each of the 5 Karvonen zones by their respective zone number (1x to 5x).
*   **Lucía’s TRIMP:** Groups time into three physiological thresholds (below VT1, between VT1-VT2, and above VT2).

#### **C. Recovery & Readiness**
*   **Cardiac Drift (Aerobic Decoupling):** By comparing the efficiency (Pace:HR ratio) of the first and second halves of a workout, the app identifies cardiovascular decoupling. A drift >5% indicates a breakdown in aerobic efficiency.
*   **EPOC & Recovery:** Estimated recovery hours are calculated using a non-linear power function of the Banister TRIMP score: `Recovery Hours ∝ TRIMP ^ 1.03`.
*   **Heart Rate Recovery (HRR):** Analyzes the delta between peak HR and HR 60 seconds post-exercise to assess autonomic nervous system recovery.

---

## 🛠 Technical Stack

### **Modern Android Development (MAD)**
*   **Jetpack Compose:** A fully declarative UI for a fluid, reactive dashboard.
*   **Kotlin Coroutines & Flow:** Manages complex asynchronous sync operations across multiple Health Connect APIs without blocking the UI.
*   **Room Persistence:** An offline-first approach that stores analyzed sessions locally, ensuring user privacy and instant data access.
*   **Hilt/Dagger:** Dependency injection for modular and testable code.

### **Data Safety & Privacy**
*   **Health Connect:** Uses the latest Android 14 standard for secure, permission-based health data sharing.
*   **Local Processing:** All physiological calculations are performed on-device. No health data is ever transmitted to external servers.

---

## 📈 Key Performance Indicators (KPIs)

| Metric | Scientific Utility |
| :--- | :--- |
| **Aerobic Load** | Measures volume in Zones 2-3 to build the "Aerobic Base". |
| **Anaerobic Load** | Measures time above the lactate threshold (Zones 4-5). |
| **Readiness Score** | A daily 0-100% score balancing recent strain against recovery. |
| **Efficiency Factor** | Your pace per beat of heart rate—the ultimate measure of fitness progress. |

---

## 🚀 How It Works
1.  **Permission:** Grant Health Connect permissions for Heart Rate, Exercise, and Steps.
2.  **Sync:** The `DataSyncWorker` triggers a background sync to fetch new data.
3.  **Analyze:** The engine processes raw samples, applies the TRIMP and VO2 Max models, and determines your training effect.
4.  **Optimize:** Use the recovery and decoupling data to decide whether to train hard or take a rest day.

---
*Disclaimer: Fitness Analyzer is a tool for athletes and enthusiasts. It is not intended for medical diagnosis. Always consult a physician before beginning a high-intensity training program.*

This is partially vibe coded using gemini-3.0-flash preview and gpt-5.5
