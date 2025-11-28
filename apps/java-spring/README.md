# Java Spring Boot API

A RESTful API application built with Java and Spring Boot.

## Features

- RESTful API endpoints for user management
- Health check endpoint with Spring Actuator
- Spring Boot 3.2 with Java 17

## Project Structure

```
├── src/
│   └── main/
│       ├── java/com/example/api/
│       │   ├── Application.java
│       │   ├── controller/
│       │   │   ├── HealthController.java
│       │   │   └── UserController.java
│       │   └── model/
│       │       └── User.java
│       └── resources/
│           └── application.properties
├── pom.xml
├── Dockerfile
└── README.md
```

## Prerequisites

- Java 17+
- Maven 3.8+

## Running the Application

### Development Mode
```bash
./mvnw spring-boot:run
```

### Build and Run
```bash
./mvnw clean package
java -jar target/java-spring-api-1.0.0.jar
```

## Docker

### Build the Image
```bash
docker build -t java-spring-api .
```

### Run the Container
```bash
docker run -p 8081:8081 java-spring-api
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| GET | `/api/health` | API health check |
| GET | `/api/users` | Get all users |
| GET | `/api/users/:id` | Get user by ID |
| POST | `/api/users` | Create new user |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| SERVER_PORT | Server port | 8081 |
