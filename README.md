# Intelligent Farm Operations Platform with Smart Agro Precision System

## Project Overview
This repository contains a multi‑module Maven project that combines:

- **AgroPrecision Backend** – Spring Boot services for farm management, user auth, etc.
- **FarmVerse Chatbot** – A standalone Spring Boot chatbot providing AI‑driven assistance to farmers.
- **Frontend** – A Vite/React UI (`frontend/`).

The goal is to run **both** backend services in a single Spring Boot JVM so the UI can interact with the chatbot via a floating widget **or** a dedicated `/assistant` page.

---

## Repository Structure
```
/ (root)                – Aggregator pom.xml (multi‑module)
/backend/                – AgroPrecision Spring Boot module
/farmverse-chatbot/      – FarmVerse chatbot Spring Boot module
/frontend/               – React/Vite UI
```

## Prerequisites
- Java 17 (or higher) – JDK installed and `JAVA_HOME` set.
- Maven 3.8+ (`./mvnw` wrapper is included).
- Node 18+ and npm (for the UI).
- Git (for version control).

---

## Build & Run (Development)
### 1. Install backend & chatbot dependencies
```bash
# From the project root
./mvnw clean install
```
### 2. Start the combined Spring Boot server
```bash
./mvnw spring-boot:run   # Serves both AgroPrecision and FarmVerse on the same port (default 8081)
```
The server will expose:
- API endpoints under `/api/**` (AgroPrecision).
- Chatbot endpoints under `/api/chatbot/**`.
- Static chatbot UI assets (e.g. `/static/app.js`) served from the classpath.

### 3. Run the React development server (UI)
```bash
cd frontend
npm install
npm run dev
```
The UI is available at `http://localhost:5173`. It reads the following environment variables (see `.env.example`):
- `VITE_CHATBOT_STATIC` – Base URL for static chatbot assets (e.g. `http://localhost:8081/static`).
- `VITE_CHATBOT_API` – Base URL for chatbot API calls (e.g. `http://localhost:8081/api`).

### 4. Using the Chatbot in the UI
- **Floating widget** – The `ChatbotWidget` component loads the static `app.js` from `VITE_CHATBOT_STATIC` and calls `VITE_CHATBOT_API/chatbot/ask`.
- **Dedicated page** – Navigate to `/assistant` (handled by `AssistantRedirectController`) to see the full chatbot UI.

---

## Production Build
1. Build the React UI and copy the assets into the backend JAR:
```bash
cd frontend
npm run build   # outputs to frontend/dist
# Maven resources plugin (configured in backend/pom.xml) will copy `frontend/dist` into `backend/src/main/resources/static`
```
2. Package the application:
```bash
./mvnw clean package -DskipTests
```
3. Run the packaged JAR:
```bash
java -jar backend/target/agroprecision-backend-1.0.0.jar
```
The JAR contains both backend APIs and the chatbot UI, ready for deployment.

---

## Key Files Added / Modified
- `backend/src/main/java/com/agroprecision/AgroPrecisionApplication.java` – Updated `@SpringBootApplication` to scan both packages.
- `backend/src/main/java/com/agroprecision/controller/AssistantRedirectController.java` – New controller that forwards `/assistant` to the static chatbot UI.
- `backend/pom.xml` – Added Maven Resources Plugin to copy the built React assets into `src/main/resources/static` during the `process-resources` phase.
- `.env` (frontend) – Added `VITE_CHATBOT_STATIC` and `VITE_CHATBOT_API` variables.
- `README.md` – This file, providing full build and run instructions.

---

## Next Steps / Optional Enhancements
- Add unit/integration tests for the new controller.
- Configure Spring Boot `application.yml` to externalise the server port if needed.
- Set up CI/CD pipelines to automatically build and publish the combined JAR.

---

**Enjoy building smarter farms!**
