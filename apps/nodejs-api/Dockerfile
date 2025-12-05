# Use the official Node.js 18 Alpine image
FROM node:18-alpine

# Create and change to the app directory
WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci --only=production

# Copy application files
COPY src/ ./src/

# Create a non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Change ownership of the app directory
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

# Expose the port the app runs on
EXPOSE 3000

# Run the application
CMD ["node", "src/index.js"]
