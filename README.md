# Yaqazah - يقظة 🚗💤

> **Real-time Driver Monitoring & Sleep Prediction System**

![Flutter](https://img.shields.io/badge/Mobile-Flutter-blue)
![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-green)
![TensorFlow Lite](https://img.shields.io/badge/AI-TFLite-orange)
![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-336791)

## 📖 Overview

**Yaqazah** is a real-time driver monitoring system that predicts drowsiness using computer vision and lightweight deep
learning.  
By analyzing facial landmarks and ocular metrics on standard mobile devices, Yaqazah eliminates the need for specialized
hardware.

The system shifts road safety from reactive detection to proactive prevention by identifying early signs of fatigue
before they become critical.

---

## 🌟 Key Features

### 📱 For Drivers (Mobile Application)

- **Real-Time Monitoring:** Detects eye closure, yawning, and head movement using the front camera.
- **Sleep Prediction:** Uses LSTM models to estimate drowsiness probability.
- **Smart Alerts:** Customizable alerts (sound, vibration, visual).
- **Offline Capability:** Runs fully offline using on-device AI inference.
- **Session Reports:** Export driving sessions as PDF/CSV with full history tracking.

### 💻 For Fleet Managers (Web Dashboard)

- **Fleet Visibility:** Search drivers by username or email.
- **Analytics Dashboard:** View fleet-level statistics and trends.
- **Report Management:** Filter and analyze reports by date, driver, or alert type.

---

## 🏗️ System Architecture

Yaqazah follows a layered architecture:

1. **Presentation Layer:** Mobile Driver App + Web Admin Dashboard
2. **Business Logic Layer:** Session, User, Analytics, Detection, Alert, and Reporting services
3. **Data Layer:** PostgreSQL for persistent storage and Redis for caching

### AI Pipeline

The system uses a dual-layer AI pipeline:

**Extraction Layer (Computer Vision):**

- MediaPipe — facial landmarks, EAR & MAR
- YOLOv8 — external distraction detection (mobile usage, smoking)

**Classification Layer (Machine Learning):**

- LSTM — temporal modeling of fatigue progression
- TensorFlow Lite — optimized mobile inference

---

## 🛠️ Tech Stack

| Component       | Technology      |
|-----------------|-----------------|
| Mobile          | Flutter         |
| Backend         | Spring Boot     |
| Database        | PostgreSQL      |
| Caching         | Redis           |
| AI/ML           | TensorFlow Lite |
| Computer Vision | MediaPipe       |

---

## ⚙️ Backend Setup (Spring Boot)

### 1. Clone the Repository

```bash
git clone git@github.com:YAQAZA/Yaqazah.git
cd Yaqazah
```

---

### 2. Prerequisites

Make sure you have installed:

* Java 21
* Maven
* PostgreSQL

[//]: # (* Redis)

---

### 3. Create PostgreSQL Database

Open PostgreSQL and run:

```sql
CREATE DATABASE yaqazah_db;
```

[//]: # (---)

[//]: # ()
[//]: # (### 4. Configure Environment)

[//]: # ()
[//]: # (Update `src/main/resources/application.properties`:)

[//]: # ()
[//]: # (```properties)

[//]: # (spring.datasource.url=jdbc:postgresql://localhost:5432/yaqazah_db)

[//]: # (spring.datasource.username=YOUR_USERNAME)

[//]: # (spring.datasource.password=YOUR_PASSWORD)

[//]: # ()
[//]: # (spring.jpa.hibernate.ddl-auto=update)

[//]: # (spring.jpa.show-sql=true)

[//]: # ()
[//]: # (spring.redis.host=localhost)

[//]: # (spring.redis.port=6379)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (### 5. Run Redis)

[//]: # ()
[//]: # (Make sure Redis server is running:)

[//]: # ()
[//]: # (```bash)

[//]: # (redis-server)

[//]: # (```)

---

### 4. Build & Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

---

### 5. API Documentation

After running, access Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

---

## 🔐 Authentication & Security

* Spring Security with OAuth2 client support
* JWT-based authentication using `jjwt`
* Role-based access control for users and admins

---

## 📊 Data & Reporting

* Export reports using:

    * CSV (Apache Commons CSV)
* Stores session history and fatigue analytics per driver

---

[//]: # (## 🧪 Testing)

[//]: # ()
[//]: # (Run tests using:)

[//]: # ()
[//]: # (```bash)

[//]: # (mvn test)

[//]: # (```)

[//]: # ()
[//]: # (Includes:)

[//]: # ()
[//]: # (* JPA tests)

[//]: # (* Security tests)

[//]: # (* Redis tests)

[//]: # (* Web layer tests)

[//]: # ()
[//]: # (---)

[//]: # (## 🚀 Notes)

[//]: # ()
[//]: # (* Ensure PostgreSQL and Redis are running before starting the backend)

[//]: # (* Default server port: `8080`)

[//]: # (* Change port if needed:)

[//]: # ()
[//]: # (```properties)

[//]: # (server.port=8081)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## 📌 Future Improvements)

[//]: # ()
[//]: # (* Real-time alerts via WebSockets)

[//]: # (* Advanced ML models for higher accuracy)

[//]: # (* Cloud deployment &#40;AWS / Kubernetes&#41;)

[//]: # (* Integration with vehicle systems &#40;IoT&#41;)

[//]: # ()
[//]: # (---)

## 👥 Contributors

* Yaqazah Team

---

