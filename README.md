# Gradion Assessment

This is a take-home assessment for the Intern Fullstack Developer role at Gradion.

## Architecture

The application is a full-stack web application with a React frontend and a Spring Boot backend.

- **Frontend:** React with Vite
- **Backend:** Spring Boot with Maven
- **Database:** H2 (in-memory)

## Setup

### Prerequisites

- Java 21
- Node.js
- Gemini API Key

### Environment Variables

Create a `.env` file in the `backend` directory with the following content:

```
GEMINI_API_KEY=your_api_key
```

### Running the Application

1. **Start the backend:**
   ```bash
   ./start.sh
   ```

2. **Start the frontend:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Testing

To run the tests, execute the following command:

```bash
./test.sh
```
