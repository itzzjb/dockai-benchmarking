# Node.js API Application

A RESTful API application built with Node.js and Express.js.

## Features

- RESTful API endpoints for user management
- Health check endpoint
- Request logging middleware
- Error handling middleware
- CORS support
- Environment configuration with dotenv

## Project Structure

```
├── src/
│   ├── config/         # Configuration files
│   │   └── index.js
│   ├── controllers/    # Route controllers
│   │   ├── healthController.js
│   │   └── userController.js
│   ├── middleware/     # Custom middleware
│   │   ├── errorHandler.js
│   │   └── requestLogger.js
│   ├── routes/         # API routes
│   │   ├── healthRoutes.js
│   │   ├── userRoutes.js
│   │   └── index.js
│   └── index.js        # Application entry point
├── Dockerfile
├── package.json
└── README.md
```

## Prerequisites

- Node.js (v18 or higher recommended)
- npm

## Installation

```bash
npm install
```

## Running the Application

### Development Mode
```bash
npm run dev
```

### Production Mode
```bash
npm start
```

## Docker

### Build the Image
```bash
docker build -t nodejs-api .
```

### Run the Container
```bash
docker run -p 3000:3000 nodejs-api
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/users` | Get all users |
| GET | `/api/users/:id` | Get user by ID |
| POST | `/api/users` | Create new user |
| PUT | `/api/users/:id` | Update user |
| DELETE | `/api/users/:id` | Delete user |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| PORT | Server port | 3000 |
| NODE_ENV | Environment | development |
