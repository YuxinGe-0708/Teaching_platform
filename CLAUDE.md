# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```powershell
# Recommended: one-click script (prompts for MySQL password)
.\run-dev.ps1 -DbPassword "yourpassword" -SeedTestData

# With AI and Judge0
$env:QWEN_API_KEY="sk-xxx"
.\run-dev.ps1 -DbPassword "yourpassword" -SeedTestData -UseQwen

# Manual
$env:DB_URL="jdbc:mysql://localhost:3306/teaching_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="yourpassword"
mvn spring-boot:run

# Compile only
mvn compile
```

Default test accounts (password always `123456`):
- Teacher: `teacher_demo`, `teacher_algo`
- Student: `student_001` through `student_006`
- Admin: `admin` / `admin123456`

## Architecture

**Stack**: Java 8, Spring Boot 2.7.18, Thymeleaf (SSR), MyBatis (annotation-only, no XML mappers), MySQL 8.x, Vue 3 via CDN for embedded interactivity on a few pages.

**Main class**: `TeachingPlatformApplication` (not `Main.java` — that is a stale IntelliJ template).

**Package layout**:
```
org.example/
  controller/   — ~15 @Controller classes, one per functional area
  entity/       — POJOs matching DB tables
  mapper/       — MyBatis @Mapper interfaces (annotation-only SQL)
  service/      — business logic (UserService, CourseService, TaskService, AiService, JudgeService)
  dto/          — ApiResponse, LoginRequest, RegisterRequest
  config/       — WebConfig (CORS + BCrypt bean), DatabaseInitializer, TestDataSeeder
  util/         — MarkdownUtils, TaskMetadataUtils
```

## Routing

Every `@Controller` method returns a Thymeleaf template name (string), not JSON. There is **no separate REST API** — the only `@ResponseBody` endpoints are async AI chat and code judge calls.

Template variables are injected via `Model` in controller methods. All templates receive `currentUser` (from `@ControllerAdvice GlobalModelAdvice` — reads session).

## Auth & Permissions

Session-based authentication (no JWT/token). The pattern is:
```java
User user = UserController.requireUser(session);
if (user == null) return "redirect:/login";
```

There is **no role/permission table or annotation-based access control**. Each controller method checks `user.getRole()` manually. Roles: `student`, `teacher`, `admin`.

## Database

- MySQL, database `teaching_platform`, charset `utf8mb4`
- **`DatabaseInitializer.java`** runs at startup via `@PostConstruct`: adds missing columns to existing tables, creates new tables (`discussion_post`, `discussion_reply`, `study_note`, `resource_progress`), inserts default admin user
- **`TestDataSeeder.java`** seeds demo data when `APP_SEED_TEST_DATA=true`
- Schema DDL is in `docs/schema.sql` (14 tables), but runtime init may diverge from it
- MyBatis maps underscore column names to camelCase entity fields automatically (`map-underscore-to-camel-case: true`)

## Frontend

**Templates**: `src/main/resources/templates/` — 37+ `.html` files, organized by role (`student/`, `teacher/`, `admin/`, `discussion/`, `fragments/`).

**Styling**: `static/css/cyberpunk.css` (~750 lines) — single CSS file. Uses CSS custom properties (`--bg-header`, `--border-color`, etc.). Dark green header, cream background, serif fonts. Borders and box-shadows give a "stamped" look.

**Key template patterns**:
- `sidebar.html` renders role-based nav using `th:if="${currentUser.role == 'student'}"` etc.
- Vue 3 is loaded via CDN `<script>` only on pages that need reactivity (AI chat, code editor, video learning). Most pages are pure server-rendered.
- No frontend build tooling, no `package.json`

## Key Controllers

| Controller | Routes | Notes |
|---|---|---|
| `UserController` | `/`, `/login`, `/register`, `/home`, `/profile`, `/logout`, `/resource-square`, `/help` | Auth, landing, profile |
| `StudentController` | `/student/**` | Course selection, task submission, grades, notes |
| `TeacherController` | `/teacher/**` | Course CRUD, task management, grading, class management |
| `AdminController` | `/admin/**` | User list, notification broadcast, operation logs, dashboard stats |
| `TeachingResourceController` | `/teacher/resource/**`, `/student/resource/**` | File upload/download, resource browsing within courses |
| `AiController` | API endpoints | Chat completions, AI note generation (returns JSON) |
| `JudgeController` | API endpoints | Code submission to Judge0, result polling (returns JSON) |
| `DiscussionController` | `/discussion/**` | Discussion posts with anonymous option and AI-assisted replies |

## AI Integration

Default provider is DeepSeek (`api.deepseek.com/v1/chat/completions`), overridable via `AI_API_URL`/`AI_API_KEY`/`AI_MODEL` env vars. `AiService.java` uses `RestTemplate` to call OpenAI-compatible chat API. Conversation context is held in an in-memory `ConcurrentHashMap` (lost on restart — a known gap).

## Judge0 Integration

`JudgeService.java` submits code to Judge0 API, polls for results, and maps status to AC/WA/CE/RE/TLE. Configured via `JUDGE0_*` env vars in `application.yml`.

## Important Caveats

- **No build-time schema management**: Columns are added by `DatabaseInitializer` at runtime. `docs/schema.sql` is the reference DDL but may lag behind runtime state.
- **No uploads directory in repo**: `/uploads` is git-ignored. Resources upload to `uploads/resources/`.
- **Logout destroys the entire session**: `session.invalidate()` in `UserController.logout()`.
- **Admin dashboard auto-redirect**: UserController `/home` redirects admin role to `/admin/dashboard`.
