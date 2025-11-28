# Build Stage
FROM node:14-alpine AS build

# Set working directory
WORKDIR /app

# Copy package files and install dependencies
COPY package.json package-lock.json ./
RUN npm install

# Copy source files
COPY src ./src

# Runtime Stage
FROM node:14-alpine

# Set working directory
WORKDIR /app

# Copy built application from build stage
COPY --from=build /app .

# Use a non-root user
USER node

# Expose the application port
EXPOSE 3000

# Start the application
CMD ["npm", "start"]