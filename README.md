# AI Quizzer Backend

## Description
A Spring Boot microservice for an AI-powered Quiz Application. Supports:
- Mock authentication with JWT
- Quiz management (generation, submission, scoring)
- AI features: hint generation, result suggestions, adaptive difficulty
- History and retry support

---

## Setup Instructions

### 1. Clone repository
```bash
git clone <repo_url>
cd ai-quizzer

### 2. Use this values in docker cmd
export GROQ_API_KEY=<groq_secret_api_key>
export GROQ_API_URL="https://api.groq.com/openai/v1/chat/completions"

### 3. Run below docker cmd
docker build -t ai-quizzer .
docker run -p 8080:8080 -e GROQ_API_KEY=$GROQ_API_KEY -e GROQ_API_URL=$GROQ_API_URL ai-quizzer

