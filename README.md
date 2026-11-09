# RideTogether

Helps employees of small companies find colleagues to share the ride to work and back.

## Requirements

- JDK 21
- Node.js 22+

## Run the backend

```bash
cd ride-together
JAVA_HOME=/path/to/jdk-21 ./mvnw spring-boot:run
```

The API runs on http://localhost:8080. Data is stored in `ride-together/data/` (H2, file based).

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app runs on http://localhost:5173.

## Run the tests

```bash
cd ride-together
JAVA_HOME=/path/to/jdk-21 ./mvnw test
```
