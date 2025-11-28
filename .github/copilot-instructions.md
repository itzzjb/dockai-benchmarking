# Node.js API Application

## Project Overview
This is a Node.js REST API application built with Express.js.

## Project Structure
```
├── src/
│   ├── controllers/    # Route controllers
│   ├── routes/         # API routes
│   ├── middleware/     # Custom middleware
│   ├── config/         # Configuration files
│   └── index.js        # Application entry point
├── package.json
└── README.md
```

## Development Guidelines
- Use ES6+ JavaScript features
- Follow RESTful API conventions
- Use async/await for asynchronous operations
- Implement proper error handling
- Use environment variables for configuration

## Running the Project
- Development: `npm run dev`
- Production: `npm start`

## API Endpoints
- `GET /api/health` - Health check endpoint
- `GET /api/users` - Get all users
- `GET /api/users/:id` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/:id` - Update user
- `DELETE /api/users/:id` - Delete user
