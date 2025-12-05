# Stage 1: Builder
FROM golang:1.21-alpine AS builder

# Set environment variables for static linking
ENV CGO_ENABLED=0

# Set the working directory
WORKDIR /app

# Copy go.mod and source files
COPY go.mod ./
COPY main.go ./

# Build the Go application
RUN go build -o app

# Stage 2: Runtime
FROM scratch

# Copy the compiled binary from the builder stage
COPY --from=builder /app/app /app/app

# Use a non-root user for security
USER 1001

# Set the entrypoint to run the binary
ENTRYPOINT ["/app/app"]