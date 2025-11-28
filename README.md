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
├── .env                # Environment variables
├── package.json
└── README.md
```

## Prerequisites

- Node.js (v18 or higher recommended)
- npm

## Installation

1. Install dependencies:
   ```bash
   npm install
   ```

2. Configure environment variables:
   - Copy `.env.example` to `.env` (or use the existing `.env` file)
   - Modify values as needed

## Running the Application

### Development Mode (with auto-reload)

```bash
npm run dev
```

### Production Mode

```bash
npm start
```

The server will start on `http://localhost:3000` by default.

## API Endpoints

### Health Check

| Method | Endpoint       | Description        |
|--------|----------------|--------------------|
| GET    | `/api/health`  | Health check       |

### Users

| Method | Endpoint          | Description        |
|--------|-------------------|--------------------|
| GET    | `/api/users`      | Get all users      |
| GET    | `/api/users/:id`  | Get user by ID     |
| POST   | `/api/users`      | Create new user    |
| PUT    | `/api/users/:id`  | Update user        |
| DELETE | `/api/users/:id`  | Delete user        |

## Example Requests

### Get all users

```bash
curl http://localhost:3000/api/users
```

### Get user by ID

```bash
curl http://localhost:3000/api/users/1
```

### Create a new user

```bash
curl -X POST http://localhost:3000/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "New User", "email": "newuser@example.com"}'
```

### Update a user

```bash
curl -X PUT http://localhost:3000/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Name"}'
```

### Delete a user

```bash
curl -X DELETE http://localhost:3000/api/users/1
```

## Environment Variables

| Variable   | Description              | Default       |
|------------|--------------------------|---------------|
| PORT       | Server port              | 3000          |
| NODE_ENV   | Environment mode         | development   |

## License

ISC
