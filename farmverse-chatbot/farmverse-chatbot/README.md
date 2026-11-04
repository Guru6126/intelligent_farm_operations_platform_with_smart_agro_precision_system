# FarmVerse Nexus Chatbot

Farmer chatbot with a dashboard frontend and a Spring Boot backend, one project, one server (`http://localhost:8080`).

It answers two kinds of questions, in the language of the question (English, Telugu, Hindi, or Telugu/Hindi typed in English letters):

| Question | Answered from | `sourceType` in the JSON reply |
|---|---|---|
| About one farmer's records: *"Kumaran last month harvest"* | MySQL farmer tables (`farmverse_db` by default, `agro.datasource.url`) (`users -> farms -> crops -> harvests`) | `farm-records` |
| General farming: *"What is crop rotation?"* | MySQL database **`farmverse_db`**, table `agriculture_qa` | `database` |
| General farming, not in the database | **Ollama** (local AI) using the closest database Q&A as context (RAG) | `ollama-rag` / `ollama` |

## 1. Set up MySQL (once)

By default both the Q&A table and the farmer tables (users, farms, crops, harvests) are read from `farmverse_db`. If your farmer tables are in another database (e.g. `agroprecision`), change `agro.datasource.url` in `application.properties`. Run the scripts you need (MySQL Workbench: File > Open SQL Script > Run, or command line):

```
mysql -u root -p < database/farmverse_setup.sql          # farmverse_db.agriculture_qa  (2331 Q&A rows)
mysql -u root -p < database/agroprecision_sample.sql     # agroprecision: sample farmers (skip if you already have real data)
```

Sample farmers: **Kumaran** (2 harvests last month + 1 older), **Priya Sharma** (crop, no harvest), **Arjun Rao** (farm, no crops), **Ravi Kumar** (harvest 2 months ago). **Geetha Rani** (cotton harvest last month). **Ramesh, Rahul Sharma, Suresh Kumar do not exist** (used for the "User not found" tests).

Set your MySQL login as environment variables. Do not write the password in files or on GitHub:

- Windows PowerShell: `$env:DB_USERNAME="root"; $env:DB_PASSWORD="your_password"`
- Mac/Linux: `export DB_USERNAME=root DB_PASSWORD=your_password`

## 2. Run

Requirements: Java 21, MySQL 8, (optional but recommended) Ollama.

```
ollama pull nomic-embed-text     # semantic search
ollama pull qwen2.5:1.5b         # English AI fallback
ollama pull gemma3:4b            # Telugu / Hindi
ollama serve
```

```
./mvnw spring-boot:run           # Windows: mvnw.cmd spring-boot:run
```

Open **http://localhost:8080**. The app starts even if Ollama or `agroprecision` is down (general questions still work).
The green dot in the chat header shows status; hover for details (answers loaded, search mode, AI, farm records).

## 3. The five steps (farmer records)

| Step | Where |
|---|---|
| 1. Database check: are there real farmer records? | `GET /api/chatbot/db-check` shows connection, how the tables/columns were understood, row counts and one line per farmer. SQL version: `database/agroprecision_check.sql` |
| 2. User verification | Name is matched as a whole word against `users`. Not found => `User not found. Registered name checked: <name>.` |
| 3. Harvest history | `users -> farms -> crops -> harvests` for that user |
| 4. Last month | crops, harvest dates, quantities for the previous calendar month (+ total). Also "this month" and full history |
| 5. Testing | `GET /api/chatbot/farmer-selftest` runs the three required cases plus languages and "general questions are not hijacked" |

The three required cases, and what the chatbot says (sample data):

| Case | Question | Answer |
|---|---|---|
| User not found | `Show last month harvest of Ramesh` | `User not found. Registered name checked: Ramesh.` |
| No harvest records | `Priya Sharma last month harvest` | `No harvest records for Priya Sharma in last month (October 2026).` |
| Records present | `Kumaran last month harvest` | crops, dates and kg listed, with the total |

More questions it answers from MySQL (sample data):

| Question | Answer |
|---|---|
| `Which farmers have harvested their crops?` | farmers with harvest count and total kg |
| `Which farmers have not harvested yet?` | farmers with no harvest rows |
| `Give me the total harvest quantity from all farmers.` | SUM of all harvests in kg, with a per-crop breakdown (also `... last month`, `... cotton ...`) |
| `Show all farmers registered in the system` / `How many farmers are registered?` | names / count from `users` |
| `Is Arjun Rao registered in the system?` | `Arjun Rao is registered in the system.` |
| `Show the details of a farmer named Rahul Sharma.` | `User not found. Registered name checked: Rahul Sharma.` |
| `గీతా రాణి ఎంత పత్తి పండించింది?` | Telugu/Hindi script names are matched to the English names in the database |

Names do not need exact spelling or capital letters, so voice input works: `geetha rani`, `geeta rani`, `gita rani`, `kumar an`, `arjun row`, `priya sarma` all find the right farmer. A name that is not close to any registered farmer still gives `User not found. Registered name checked: ...`.

| `How many farms are registered?` / `How many crops are growing?` / `Number of harvest records` | counts straight from the tables |
| `Highest harvest farmer` / `Lowest harvest farmer` | ranking by total kg |

Telugu / Hindi examples: `Kumaran ki gata nela harvest details cheppu`, `Kumaran ka pichle mahine ka harvest batao`.
Replies for farmer records are fixed templates filled with database values, so crop names, dates and kg are never altered by an AI model.

### If your table or column names are different
The app detects names such as `users.full_name`, `farms.owner_id`, `crops.crop_name`, `harvests.quantity_kg`, `harvests.harvest_date`
(and common variants like `farmer_id`, `crop_type`, `yield_kg`, or foreign keys declared in the database).
If something is not found, `/api/chatbot/db-check` lists exactly what is missing. Fix it with a line in `application.properties`, for example:

```
agro.map.users.name=farmer_name
agro.map.harvests.qty=yield_in_kg
agro.default-unit=kg
```

## 4. Project layout

```
database/                      farmverse_setup.sql, agroprecision_sample.sql, agroprecision_check.sql
src/main/java/.../agro/        farmer-records engine (schema detection, SQL, parser, replies, self-test). No Spring inside: easy to test
src/main/java/.../             controllers, AgricultureDataService (MySQL Q&A + search), OllamaService, TranslationService, LanguageSupport
src/main/resources/static/     dashboard (index.html, style.css, app.js)
src/main/resources/application.properties
```

API: `POST /api/chatbot/ask` body `{"question": "...", "language": "en|te|hi"}` (language optional), `GET /api/chatbot/health`,
`GET /api/chatbot/db-check`, `GET /api/chatbot/farmer-selftest`, `GET /api/chatbot/selftest` (languages, slow).

## 5. Language behaviour
- Telugu/Hindi script, or Telugu/Hindi typed in English letters, is detected from the question itself; the dropdown is only a hint when the text gives no clue.
- A Telugu/Hindi question is translated to English, searched in the database, and the answer translated back; if the database has no answer, Ollama answers directly in that language.
- The reply copies the writing style of the question (English letters in, English letters out).
- `ollama.multilingual-model` (default `gemma3:4b`) must be installed for good Telugu/Hindi.

## 6. Tuning and troubleshooting
- `farmverse.match-threshold` (0.65) and `farmverse.translated-match-threshold` (0.72): raise if wrong database answers appear, lower if good ones are skipped. The console prints `best=... (need ...)` for every question.
- A database answer is used only if its question shares a specific topic word with yours and is about the same crop (asking about rice never returns a maize entry).
- `Table 'farmverse_db.users' doesn't exist` (error 1146): the farmer tables are in the **other** database. This version reads them from `agroprecision` through `agro.datasource.*`.
- `Access denied`: set `DB_USERNAME` / `DB_PASSWORD` (step 1) and restart.
- `/api/chatbot/health` shows `"farmerDb": false`: open `/api/chatbot/db-check` for the reason.
- Frontend served from another port (Live Server): add `?api=http://localhost:8080` to the URL.
