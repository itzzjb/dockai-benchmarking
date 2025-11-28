# Build Stage
FROM golang:1.21-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Go module files and source code
COPY go.mod ./
COPY main.go ./

# Build the Go application
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o server .

# Runtime Stage
FROM alpine:3.19

# Create a non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled binary from the builder stage
COPY --from=builder /app/server ./

# Change ownership of the binary to the non-root user
RUN chown appuser:appgroup server

# Switch to the non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Command to run the application
CMD ["./server"]