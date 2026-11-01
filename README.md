# Lastwell

Lastwell is a Spring Boot API for tracking cobbler repair tickets. Log footwear, leather, issue, and cobbler, then move each pair from intake through lasting, stitching, and finishing to pickup.

## Stack

- Java 21
- Spring Boot 4
- Bean Validation
- In-memory service layer
- Maven Wrapper (`mvnw`)

## Features

- CRUD for repair jobs
- Status workflow: `INTAKE` → `LASTING` → `STITCHING` → `FINISHING` → `READY` → `PICKED_UP`
- Dedicated last, stitch, finish, ready, and pickup actions
- Footwear: `oxford`, `boot`, `loafer`, `pump`, `sneaker`
- Optional notes and tags
- Filter by status, footwear, tag, or free-text query
- Problem Details responses for validation and not-found errors
- Seeded demo jobs on startup

## Architecture

```
Client
  │
  ▼
JobController / HealthController
  │
  ▼
JobService
  │
  └── ConcurrentHashMap store
```

## Project layout

```
lastwell/
├── pom.xml
├── mvnw / mvnw.cmd
├── README.md
├── LICENSE
├── requests.http
└── src/
    ├── main/java/com/lastwell/
    │   ├── job/
    │   ├── error/
    │   └── health/
    └── test/java/com/lastwell/
```

## Requirements

- JDK 21+

Maven is optional — use the included wrapper.

## Setup

```bash
git clone https://github.com/<your-username>/lastwell.git
cd lastwell
```

## Run

```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

- Health: http://localhost:8080/health
- Jobs: http://localhost:8080/api/v1/jobs

## API overview

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness check |
| `GET` | `/api/v1/jobs` | List jobs (`status`, `footwear`, `tag`, `q`, `limit`, `offset`) |
| `POST` | `/api/v1/jobs` | Create a job |
| `GET` | `/api/v1/jobs/{id}` | Get one job |
| `PUT` | `/api/v1/jobs/{id}` | Update a job |
| `DELETE` | `/api/v1/jobs/{id}` | Delete a job |
| `POST` | `/api/v1/jobs/{id}/last` | intake → lasting |
| `POST` | `/api/v1/jobs/{id}/stitch` | lasting → stitching |
| `POST` | `/api/v1/jobs/{id}/finish` | stitching → finishing |
| `POST` | `/api/v1/jobs/{id}/ready` | finishing → ready |
| `POST` | `/api/v1/jobs/{id}/pickup` | ready → picked_up |

### Example

```bash
# Create an intake job
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"LW-210\",\"customer\":\"Sam Ortega\",\"footwear\":\"pump\",\"leather\":\"patent\",\"issue\":\"Broken heel stem\",\"cobbler\":\"Jordan Lee\",\"dueOn\":\"2026-09-01\",\"status\":\"INTAKE\",\"tags\":[\"dress\"]}"

# Start lasting
curl -X POST http://localhost:8080/api/v1/jobs/<job-id>/last

# Filter ready loafers
curl "http://localhost:8080/api/v1/jobs?status=READY&footwear=loafer"
```

## Tests

```bash
# Windows
.\mvnw.cmd test

# macOS / Linux
./mvnw test
```

## License

MIT
