#!/bin/bash
echo "Starting backend..."
cd backend && ./mvnw spring-boot:run &
BACKEND_PID=$!

echo "Starting frontend..."
cd ../frontend && npm run dev &
FRONTEND_PID=$!

echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:3000"
wait
