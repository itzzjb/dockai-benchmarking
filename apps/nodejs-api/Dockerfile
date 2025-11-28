# Build Stage
FROM node:18-alpine AS build

# Set working directory
WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application code
COPY . .

# Runtime Stage
FROM node:18-alpine AS runtime

# Set working directory
WORKDIR /app

# Copy built application from build stage
COPY --from=build /app /app

# Use a non-root user
USER node

# Set environment variables
ENV PORT=3000

# Expose the port the app runs on
EXPOSE 3000

# Command to run the application
CMD ["npm", "start"]