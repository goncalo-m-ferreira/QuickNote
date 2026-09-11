# QuickNote REST API

Backend REST API for the QuickNote Android mobile application. Built with Node.js, Express, and PostgreSQL.

---

## 🛠️ Tech Stack & Architecture

- **Runtime:** Node.js (>= 18.0.0)
- **Framework:** Express.js
- **Database:** PostgreSQL with connection pooling (`pg`)
- **Password Hashing:** `bcryptjs` (Salt rounds: 10)
- **Authentication:** Stateless JSON Web Tokens (`jsonwebtoken`)
- **Cross-Origin Handling:** `cors`

---

## 🔐 Security & Authorization Model

1. **Email Normalization:** All emails are trimmed and converted to lowercase prior to database queries to prevent case-sensitive duplicate accounts.
2. **Password Security:** Passwords are never stored in plain text and are excluded from all query returns (`password_hash` is never exposed).
3. **Stateless JWT Authorization:** Protected endpoints require `Authorization: Bearer <token>` header.
4. **Ownership-Based Access Control:**
   - `401 Unauthorized`: Missing or malformed `Authorization` header.
   - `403 Forbidden`: Invalid, corrupted, or expired JWT; or authenticated user attempts to read, modify, or delete a note owned by another user.
   - `404 Not Found`: Requested resource ID does not exist in the database.
5. **SQL Injection Prevention:** All database operations utilize parameterized queries (`$1`, `$2`).

---

## ⚙️ Environment Variables

Create an `api/.env` file based on `.env.example`:

```env
PORT=3000
NODE_ENV=development
DATABASE_URL=postgres://postgres:postgres@localhost:5432/quicknote
JWT_SECRET=your_jwt_secret_key_change_in_production
JWT_EXPIRES_IN=7d
```

---

## 🚀 Getting Started (Local Development)

1. Navigate to the API directory:
   ```bash
   cd api
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Set up the local `.env` configuration:
   ```bash
   cp .env.example .env
   ```
4. Start the development server (auto-reloads on file change):
   ```bash
   npm run dev
   ```
5. Run in production mode:
   ```bash
   npm start
   ```

---

## 📡 API Endpoints Reference

### 1. Health Check
- **`GET /health`**
  - **Auth:** Public
  - **Response (200 OK):**
    ```json
    {
      "status": "ok",
      "service": "QuickNote API",
      "timestamp": "2026-09-11T16:00:00.000Z"
    }
    ```

---

### 2. Authentication

- **`POST /auth/register`**
  - **Auth:** Public
  - **Request Body:**
    ```json
    {
      "email": "user@example.com",
      "password": "securepassword123"
    }
    ```
  - **Responses:**
    - `201 Created`: User registered successfully with signed JWT.
    - `400 Bad Request`: Missing fields, non-string types, email > 255 chars, invalid email format, or password < 6 characters.
    - `409 Conflict`: Email already registered.

- **`POST /auth/login`**
  - **Auth:** Public
  - **Request Body:**
    ```json
    {
      "email": "user@example.com",
      "password": "securepassword123"
    }
    ```
  - **Responses:**
    - `200 OK`: Login successful with signed JWT.
    - `400 Bad Request`: Missing email or password, non-string types, or email > 255 chars.
    - `401 Unauthorized`: Invalid credentials.

- **`POST /auth/logout`**
  - **Auth:** Bearer Token required
  - **Header:** `Authorization: Bearer <token>`
  - **Responses:**
    - `200 OK`:
      ```json
      {
        "message": "Logout successful. Token invalidated on client."
      }
      ```
    - `401 Unauthorized`: Missing or malformed `Authorization` header.
    - `403 Forbidden`: Invalid, corrupted, or expired token.

---

### 3. Users

- **`GET /users/me`**
  - **Auth:** Bearer Token required
  - **Header:** `Authorization: Bearer <token>`
  - **Responses:**
    - `200 OK`:
      ```json
      {
        "user": {
          "id": 1,
          "email": "user@example.com",
          "createdAt": "2026-09-11T16:00:00.000Z",
          "updatedAt": "2026-09-11T16:00:00.000Z"
        }
      }
      ```
    - `401 Unauthorized`: Missing or malformed `Authorization` header.
    - `403 Forbidden`: Invalid, corrupted, or expired token.

---

### 4. Notes CRUD

- **`GET /notes`**
  - **Auth:** Bearer Token required
  - **Header:** `Authorization: Bearer <token>`
  - **Responses:**
    - `200 OK`:
      ```json
      {
        "notes": [
          {
            "id": 1,
            "user_id": 1,
            "title": "Meeting Notes",
            "content": "Discuss project milestones",
            "created_at": "2026-09-11T16:00:00.000Z",
            "updated_at": "2026-09-11T16:00:00.000Z"
          }
        ]
      }
      ```
    - `401 Unauthorized`: Missing or malformed `Authorization` header.
    - `403 Forbidden`: Invalid, corrupted, or expired token.

- **`GET /notes/:id`**
  - **Auth:** Bearer Token required
  - **Responses:**
    - `200 OK`: Returns the requested note object.
    - `400 Bad Request`: Non-numeric or invalid ID format.
    - `401 Unauthorized`: Missing or malformed `Authorization` header.
    - `403 Forbidden`: Invalid/expired token OR note belongs to a different user.
    - `404 Not Found`: Note does not exist.

- **`POST /notes`**
  - **Auth:** Bearer Token required
  - **Request Body:**
    ```json
    {
      "title": "Shopping List",
      "content": "Milk, eggs, coffee"
    }
    ```
  - **Responses:**
    - `201 Created`: Note created successfully.
    - `400 Bad Request`: Missing, empty, non-string, or title > 255 chars; or missing, empty, or non-string content.
    - `401 Unauthorized`: Missing or malformed `Authorization` header.
    - `403 Forbidden`: Invalid, corrupted, or expired token.

- **`PUT /notes/:id`**
  - **Auth:** Bearer Token required
  - **Request Body:**
    ```json
    {
      "title": "Updated Title",
      "content": "Updated content text"
    }
    ```
  - **Responses:**
    - `200 OK`: Note updated successfully.
    - `400 Bad Request`: Invalid ID format; missing, empty, non-string, or title > 255 chars; or missing, empty, or non-string content.
    - `401 Unauthorized`: Missing or malformed `Authorization` header.
    - `403 Forbidden`: Invalid/expired token OR note belongs to another user.
    - `404 Not Found`: Note does not exist.

- **`DELETE /notes/:id`**
  - **Auth:** Bearer Token required
  - **Responses:**
    - `200 OK`: `{"message": "Note deleted successfully."}`
    - `400 Bad Request`: Non-numeric or invalid ID format.
    - `401 Unauthorized`: Missing or malformed `Authorization` header.
    - `403 Forbidden`: Invalid/expired token OR note belongs to another user.
    - `404 Not Found`: Note does not exist.
