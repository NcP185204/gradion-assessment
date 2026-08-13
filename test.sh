#!/bin/bash
echo "Running backend tests..."
cd backend && ./mvnw test

echo "Running frontend tests..."
cd ../frontend && npm test -- --watchAll=false
