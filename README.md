# tsc-incident-reviewer

Local monorepo with:
- `backend/` (Java 17, Spring Boot, Maven)
- `frontend/` (React + TypeScript, Vite)

## Prerequisites
- Java 17
- Node.js 18+
- Maven 3.9+

## Run Backend
```bash
cd backend
mvn spring-boot:run
```
Backend runs on `http://localhost:8080`.

## Run Frontend
```bash
cd frontend
npm i
npm run dev
```
Frontend runs on `http://localhost:5173`.

## Verify
1. Open `http://localhost:5173`
2. Click `Ping backend`
3. Confirm `Backend response: ok`

## Notes
- Vite proxy forwards `/api/*` to backend (`http://localhost:8080`).
- Upload/parse logic is intentionally not implemented in Phase 1.
