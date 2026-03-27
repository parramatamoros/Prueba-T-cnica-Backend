# 📋 Diary API — Spring Boot + JWT + PostgreSQL

API RESTful para gestión de tareas con autenticación JWT y roles de usuario.

---

## 🏗️ Arquitectura del Proyecto

```
src/
├── main/java/com/diary/
│   ├── config/               # SecurityConfig, JwtProperties
│   ├── controller/           # AuthController, TaskController
│   ├── dto/
│   │   ├── request/          # RegisterRequest, LoginRequest, TaskRequest
│   │   └── response/         # AuthResponse, TaskResponse, ApiError
│   ├── entity/               # User, Task
│   │   └── enums/            # Role, TaskStatus
│   ├── exception/            # Excepciones custom + GlobalExceptionHandler
│   ├── repository/           # UserRepository, TaskRepository
│   ├── security/             # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
│   └── service/              # AuthService, TaskService
└── resources/
    ├── application.yml
    └── db/migration/         # Flyway: V1__init_schema.sql
```

---

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 17+
- Docker
- Maven 3.9+

# Compila y ejecuta
mvn spring-boot:run
```

---

## 📡 Endpoints de la API

### Autenticación (públicos)

| Método | Endpoint                  | Descripción               |
|--------|---------------------------|---------------------------|
| POST   | `/api/v1/auth/register`   | Registrar nuevo usuario   |
| POST   | `/api/v1/auth/login`      | Obtener token JWT         |

### Tareas (requieren `Authorization: Bearer <token>`)

| Método | Endpoint              | Rol    | Descripción                      |
|--------|-----------------------|--------|----------------------------------|
| GET    | `/api/v1/tasks`       | ANY    | Listar tareas (paginado + filtro)|
| GET    | `/api/v1/tasks/{id}`  | ANY    | Obtener tarea por ID             |
| POST   | `/api/v1/tasks`       | ANY    | Crear nueva tarea                |
| PUT    | `/api/v1/tasks/{id}`  | OWNER  | Actualizar tarea propia          |
| DELETE | `/api/v1/tasks/{id}`  | OWNER/ADMIN | Eliminar tarea             |

### Parámetros de paginación y filtro

```
GET /api/v1/tasks?status=PENDING&page=0&size=10&sort=createdAt,desc
```

| Parámetro | Valores                              | Default       |
|-----------|--------------------------------------|---------------|
| `status`  | `PENDING`, `IN_PROGRESS`, `COMPLETED`| (todos)       |
| `page`    | número entero ≥ 0                    | `0`           |
| `size`    | número entero                        | `10`          |
| `sort`    | `createdAt,desc` / `title,asc`       | `createdAt,desc`|

---

## 🔐 Ejemplos de Uso

### 1. Registrar usuario

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juan",
    "email": "juan@mail.com",
    "password": "Password1"
  }'
```

**Respuesta 201:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "juan",
  "role": "USER",
  "expiresIn": 86400000
}
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "juan", "password": "Password1"}'
```

### 3. Crear tarea

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Aprender Spring Security",
    "description": "Estudiar JWT y OAuth2",
    "status": "IN_PROGRESS"
  }'
```

### 4. Listar tareas filtradas

```bash
curl "http://localhost:8080/api/v1/tasks?status=PENDING&page=0&size=5" \
  -H "Authorization: Bearer <token>"
```

### Credenciales ADMIN por defecto
```
username: admin
password: Admin123!
```

---

## ❌ Respuestas de Error

Todos los errores siguen la misma estructura JSON:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "No tienes permisos para modificar la tarea con id: 5",
  "path": "/api/v1/tasks/5"
}
```

| Código | Causa                                     |
|--------|-------------------------------------------|
| 400    | Validación fallida (campos requeridos)    |
| 401    | Token JWT ausente, inválido o expirado    |
| 403    | Intentar acceder a recurso de otro usuario|
| 404    | Tarea o usuario no encontrado             |
| 409    | Username o email ya registrado            |
| 500    | Error interno del servidor                |

---

## 🧪 Ejecutar Tests

```bash
# Todos los tests
mvn test

# Solo una clase
mvn test -Dtest=TaskServiceTest

# Con reporte de cobertura (si tienes JaCoCo configurado)
mvn verify
```

---

## 🐳 Variables de Entorno

| Variable        | Default   | Descripción                     |
|-----------------|-----------|---------------------------------|
| `DB_HOST`       | localhost | Host de PostgreSQL              |
| `DB_PORT`       | 5432      | Puerto de PostgreSQL            |
| `DB_NAME`       | postgres  | Nombre de la base de datos      |
| `DB_USER`       | postgres  | Usuario de BD                   |
| `DB_PASSWORD`   | postgres  | Contraseña de BD                |
| `JWT_SECRET`    | (hex str) | Secreto HMAC para firmar tokens |
| `JWT_EXPIRATION`| 12345678  | Expiración JWT en milisegundos  |

---

## 🔧 Stack Técnico

| Tecnología           | Versión  | Uso                              |
|----------------------|----------|----------------------------------|
| Java                 | 17       | Lenguaje principal               |
| Spring Boot          | 3.2.5    | Framework base                   |
| Spring Security      | 6.x      | AuthN/AuthZ + filtros            |
| Spring Data JPA      | 3.x      | ORM + repositorios               |
| PostgreSQL           | 16       | Base de datos relacional         |
| Flyway               | 9.x      | Migraciones de esquema           |
| JJWT                 | 0.12.5   | Generación/validación JWT        |
| Lombok               | 1.18.x   | Reducción de boilerplate         |
| JUnit 5 + Mockito    | latest   | Pruebas unitarias                |
| Docker               | -        | Contenedorización                |
