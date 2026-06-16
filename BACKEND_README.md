# SmartATS Backend

Spring Boot backend for SmartATS with Gemini-first AI routing, OpenRouter fallback, and SmartATS local fallback protection.

## AI Architecture

```
Frontend -> Spring Boot Backend -> AI Gateway Layer
                                      |- GeminiProvider
                                      |- OpenRouterProvider
                                      '- SmartATSFallbackProvider
```

The frontend never receives Gemini or OpenRouter API keys, provider-specific payloads, or provider-switch details.

## Environment Variables

Required for Gemini:

```powershell
$env:GEMINI_API_KEY="your-gemini-api-key"
```

Required for OpenRouter fallback:

```powershell
$env:OPENROUTER_API_KEY="your-openrouter-api-key"
```

Optional Gemini environment variables:

```powershell
$env:GEMINI_ENABLED="true"
$env:GEMINI_MODEL="gemini-2.5-flash"
$env:GEMINI_BASE_URL="https://generativelanguage.googleapis.com"
$env:GEMINI_CONNECT_TIMEOUT_MILLIS="5000"
$env:GEMINI_READ_TIMEOUT_MILLIS="15000"
$env:GEMINI_MAX_RETRIES="2"
$env:GEMINI_RETRY_DELAY_MILLIS="500"
$env:GEMINI_REQUESTS_PER_MINUTE="30"
$env:GEMINI_MAX_INPUT_CHARS="6000"
$env:GEMINI_MAX_RESUME_CHARS="24000"
$env:GEMINI_MAX_JOB_DESCRIPTION_CHARS="12000"
$env:GEMINI_MAX_SKILLS="12"
```

Optional OpenRouter environment variables:

```powershell
$env:OPENROUTER_ENABLED="true"
$env:OPENROUTER_BASE_URL="https://openrouter.ai"
$env:OPENROUTER_PRIMARY_MODEL="deepseek/deepseek-chat-v3-0324:free"
$env:OPENROUTER_SECONDARY_MODEL="qwen/qwen-2.5-72b-instruct:free"
$env:OPENROUTER_CONNECT_TIMEOUT_MILLIS="5000"
$env:OPENROUTER_READ_TIMEOUT_MILLIS="20000"
$env:OPENROUTER_MAX_RETRIES="1"
$env:OPENROUTER_RETRY_DELAY_MILLIS="750"
$env:OPENROUTER_QUEUE_SPACING_MILLIS="300"
$env:OPENROUTER_RATE_LIMIT_COOLDOWN_MILLIS="12000"
$env:OPENROUTER_MAX_BACKOFF_MILLIS="10000"
```

Database setup can still be overridden with:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://..."
$env:SPRING_DATASOURCE_USERNAME="..."
$env:SPRING_DATASOURCE_PASSWORD="..."
```

## Centralized AI Layer

Production AI requests now flow through:

- `GeminiConfig`
- `OpenRouterConfig`
- `GeminiService`
- `OpenRouterService`
- `OpenRouterResponseParser`
- `PromptBuilderService`
- `AIResponseParser`
- `AIGatewayService`
- `AIProvider`
- `GeminiProvider`
- `OpenRouterProvider`
- `SmartATSFallbackProvider`
- `AIService`
- `AiInputSanitizer`
- `AiRateLimitService`

## Provider Strategy

Request order:

1. `GeminiProvider`
2. `OpenRouterProvider`
3. `SmartATSFallbackProvider`

Gemini falls through to OpenRouter when the gateway sees provider unavailability such as:

- `429`
- `500`
- `503`
- timeout / connection failures
- structured parsing failures

OpenRouter then uses:

1. `deepseek/deepseek-chat-v3-0324:free`
2. `qwen/qwen-2.5-72b-instruct:free`

The gateway keeps provider retries bounded, tracks cooldown windows, and avoids infinite retry loops by attempting each provider chain once per request.

## AI Endpoints

Candidate interview generation:

- `POST /api/ai/interview/generate`
- `POST /api/ai/interview/evaluate`

ATS analysis:

- `POST /api/ai/ats/analyze`

Job matching:

- `POST /api/ai/match/score`

Existing session-based interview APIs remain active:

- `POST /api/interview/sessions`
- `GET /api/interview/sessions/{sessionId}`
- `PUT /api/interview/sessions/{sessionId}/answers`
- `POST /api/interview/sessions/{sessionId}/submit`
- `GET /api/interview/sessions/{sessionId}/result`

Legacy ATS and match endpoints remain wired to the same backend services:

- `POST /api/resumes/analyze`
- `GET /api/match/{jobId}`

## Analytics Architecture

Analytics now uses backend aggregation and persisted snapshots:

- recruiter analytics aggregate jobs, applications, and interview schedules
- candidate analytics combine ATS snapshots, job-match snapshots, applications, and completed interview sessions
- ATS history is stored in `ats_analysis_snapshots`
- match history is stored in `job_match_snapshots`
- frontend charts consume normalized analytics DTOs instead of raw records

## Analytics Endpoints

- `GET /api/analytics/recruiter`
- `GET /api/analytics/recruiter/export.csv`
- `GET /api/analytics/candidate`

## Request Examples

Interview question generation:

```json
{
  "jobRole": "Java Developer",
  "difficulty": "Intermediate",
  "skills": ["Java", "Spring Boot", "SQL"],
  "interviewType": "Mixed",
  "jobDescription": "Build REST APIs and maintain backend services."
}
```

Interview answer evaluation:

```json
{
  "question": "Explain dependency injection.",
  "candidateAnswer": "Constructor injection improves testability and loose coupling.",
  "jobRole": "Java Developer",
  "expectedSkills": ["Java", "Spring Boot"]
}
```

ATS analysis:

```json
{
  "targetRole": "Backend Engineer",
  "jobDescription": "Need Java, Spring Boot, SQL, Docker, and communication skills."
}
```

Job match scoring:

```json
{
  "jobDescription": "Need Java, Spring Boot, SQL, and Docker experience.",
  "resumeText": "Java Spring Boot REST APIs SQL",
  "targetRole": "Backend Engineer"
}
```

## Security and Resilience

- Gemini and OpenRouter API keys are loaded from environment variables only.
- All AI inputs are validated, sanitized, and size-limited.
- Resume text, answers, and job descriptions are wrapped as untrusted prompt content.
- Gemini and OpenRouter requests use centralized timeout and retry handling.
- Provider health is tracked with cooldown windows after transient failures.
- OpenRouter requests use structured JSON schema output enforcement.
- AI endpoints are rate-limited with an in-memory guard.
- Backend responses are normalized before they reach the UI.
- The same DTO fields and score ranges are preserved across Gemini, OpenRouter, and local fallback.
- Graceful fallback messages are returned when remote providers are unavailable.
- Safe logs include provider used, fallback activation, failure reason, and timing, never API keys.

Analytics-specific guardrails:

- analytics exports are role-protected
- ATS and match histories are stored on the backend, not in frontend local storage
- analytics responses use private short-lived cache headers for future caching layers
- empty-state responses stay structured so charts can degrade safely

## Testing

Compile:

```powershell
.\mvnw.cmd -q -DskipTests compile
```

Run backend tests:

```powershell
.\mvnw.cmd -q test
```

The current automated coverage includes:

- Gemini success flow
- Gemini `429` fallback flow
- Gemini `503` fallback flow
- Gemini timeout to OpenRouter flow
- complete provider failure to SmartATS local fallback
- interview question generation
- interview answer evaluation
- ATS analysis
- job match scoring
- recruiter analytics endpoint coverage
- candidate analytics endpoint coverage
- legacy interview and match endpoint compatibility
- fallback behavior when Gemini is unavailable

Analytics verification also includes:

- ATS snapshot persistence through `/api/ai/ats/analyze`
- match snapshot persistence through `/api/match/{jobId}` and `/api/ai/match/score`
- recruiter analytics aggregation
- candidate analytics aggregation
