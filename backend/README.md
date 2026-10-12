# Dhanova AI — Backend Setup (Phase 1: Foundation + Auth)

## 1. Prerequisites
- Python 3.10+
- MySQL Server + MySQL Workbench installed and running

## 2. Create the database
Open MySQL Workbench (or `mysql` CLI) and run the file:
```
database/schema.sql
```
This creates the `dhanova_ai` database with all core tables and seed data
(default categories + currencies).

## 3. Set up the backend
```bash
cd backend
python -m venv venv

# Activate:
# Windows:
venv\Scripts\activate
# Mac/Linux:
source venv/bin/activate

pip install -r requirements.txt
```

## 4. Configure environment variables
Copy `.env.example` to `.env` and fill in your real MySQL password:
```bash
cp .env.example .env
```
Edit `.env`:
```
MYSQL_PASSWORD=your_actual_mysql_password
JWT_SECRET=make-up-any-long-random-string
```

## 5. Run the server
```bash
python app.py
```
You should see Flask running on `http://0.0.0.0:5000`.

## 6. Test it (checkpoint before moving on!)

### Test Flask ↔ MySQL connection
```bash
curl http://localhost:5000/ping
```
Expected: `{"status": "ok", "flask": "running", "mysql": {"ok": 1}}`

If this fails, check:
- Is MySQL running?
- Is `MYSQL_PASSWORD` in `.env` correct?
- Does the `dhanova_ai` database exist? (Did you run schema.sql?)

### Test Registration
```bash
curl -X POST http://localhost:5000/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Swara Desai","email":"swara@example.com","password":"test123"}'
```
Expected: JSON with a `token` and `user` object.

### Test Login
```bash
curl -X POST http://localhost:5000/login \
  -H "Content-Type: application/json" \
  -d '{"email":"swara@example.com","password":"test123"}'
```
Expected: JSON with a `token`.

### Test protected route
Copy the token from login, then:
```bash
curl http://localhost:5000/me \
  -H "Authorization: Bearer PASTE_TOKEN_HERE"
```
Expected: your user details.

## ✅ Once all four checks above pass, Stage 1 (Foundation) and
Stage 2 (Authentication) are done. Next: Family Management module
(`routes/family.py`) — ask Claude to build it next.

## Project structure
```
backend/
├── app.py              # Entry point, registers routes
├── config.py            # Reads .env settings
├── utils.py              # JWT helpers + @token_required decorator
├── requirements.txt
├── .env.example
├── database/
│   └── db.py            # MySQL connection pool + query helper
├── routes/
│   └── auth.py           # /register, /login, /me
└── models/                # (reserved for later — ORM-style models if needed)
```
