# DocAI Test Monorepo

## Project Overview
This is a monorepo containing multiple applications in different languages for testing DocAI Dockerfile generation.

## Repository Structure
```
├── apps/
│   ├── nodejs-api/      # Node.js Express REST API
│   ├── python-flask/    # Python Flask API
│   ├── java-spring/     # Java Spring Boot API
│   └── go-api/          # Go REST API
├── .github/
│   └── copilot-instructions.md
└── README.md
```

## Individual App Structures

### Node.js API (`apps/nodejs-api/`)
```
├── src/
│   ├── controllers/    # Route controllers
│   ├── routes/         # API routes
│   ├── middleware/     # Custom middleware
│   ├── config/         # Configuration files
│   └── index.js        # Application entry point
├── Dockerfile
├── package.json
└── README.md
```

## Development Guidelines

### General
- Each app is independent and self-contained
- Each app has its own Dockerfile
- Run commands from within the specific app directory

### Node.js Apps
- Use ES6+ JavaScript features
- Follow RESTful API conventions
- Use async/await for asynchronous operations
- Implement proper error handling
- Use environment variables for configuration

### Python Apps
- Use Python 3.9+
- Follow PEP 8 style guide
- Use virtual environments

### Java Apps
- Use Java 17+
- Follow Spring Boot conventions
- Use Maven for dependency management

### Go Apps
- Use Go 1.21+
- Follow Go conventions
- Use Go modules

## Running Individual Apps

### Node.js API
```bash
cd apps/nodejs-api
npm install
npm run dev
```

## DocAI Usage
When changes are made to a specific app directory, DocAI should:
1. Detect the changed path
2. Analyze only that specific app
3. Generate/update the Dockerfile for that app

