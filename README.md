# AuthMaster 🔐

A production-ready Spring Boot authentication service featuring JWT-based authentication with refresh token rotation, rate limiting, and secure cookie handling.

## Features

- **JWT Authentication**: Secure access and refresh token implementation
- **Refresh Token Rotation**: Automatic token rotation for enhanced security
- **HTTPOnly Cookies**: Refresh tokens stored in secure HTTPOnly cookies
- **CSRF Protection**: Configurable CSRF protection for sensitive endpoints
- **Rate Limiting**: Built-in rate limiting to prevent abuse
- **Role-Based Access Control (RBAC)**: Support for user roles and permissions
- **BCrypt Password Hashing**: Strong password encryption (cost factor: 12)
- **CORS Configuration**: Pre-configured for cross-origin requests
- **PostgreSQL & H2 Support**: Production database with in-memory testing option
- **Input Validation**: Request validation using Jakarta Validation

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.0**
- **Spring Security**
- **Spring Data JPA**
- **PostgreSQL** (Primary database)
- **H2** (Testing database)
- **JWT** (io.jsonwebtoken 0.12.6)
- **Lombok**
- **Maven**

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 12+ (for production)
- IDE (IntelliJ IDEA, Eclipse, or VS Code recommended)

## Installation

### 1. Clone the repository

```bash
git clone <repository-url>
cd authmaster
```

### 2. Configure PostgreSQL

Create a PostgreSQL database:

```sql
CREATE DATABASE user_auth_db;
```

### 3. Update application.properties

Edit `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/user_auth_db
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT Secret (Generate a new secure key for production!)
app.jwt.secret=your-256-bit-secret-key-here
app.jwt.access-expiration-ms=900000       # 15 minutes
app.jwt.refresh-expiration-ms=1209600000  # 14 days

# Server Configuration
server.address=localhost
server.port=8080
```

> ⚠️ **Security Warning**: Never commit sensitive credentials to version control. Use environment variables or a secrets manager for production.

### 4. Build the project

```bash
./mvnw clean install
```

### 5. Run the application

```bash
./mvnw spring-boot:run
```

The server will start at `http://localhost:8080`

## API Endpoints

### Authentication Endpoints

#### Register a new user
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response:**
```json
"Registered: johndoe"
```

---

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "username": "johndoe"
}
```

**Set-Cookie:** `refreshToken=<token>; HttpOnly; Secure; SameSite=Strict; Max-Age=1209600`

---

#### Refresh Access Token
```http
POST /api/auth/refresh
Cookie: refreshToken=<refresh-token>
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "username": "johndoe"
}
```

> 💡 The refresh token is automatically rotated with each refresh request.

---

#### Logout
```http
POST /api/auth/logout
Cookie: refreshToken=<refresh-token>
```

**Response:**
```json
"Logged out"
```

---

### Protected Endpoints

For protected endpoints, include the access token in the Authorization header:

```http
GET /api/protected-resource
Authorization: Bearer <access-token>
```

## Architecture

### Project Structure

```
src/main/java/com/authutil/authmaster/
├── config/
│   ├── SecurityConfig.java       # Security & CORS configuration
│   └── WebConfig.java             # Web MVC configuration
├── controller/
│   ├── AuthController.java        # Authentication endpoints
│   └── DashboardController.java   # Protected endpoints
├── dto/
│   ├── AuthResponse.java          # Authentication response DTO
│   ├── LoginRequest.java          # Login request DTO
│   ├── RefreshRequest.java        # Refresh token request DTO
│   ├── RegisterRequest.java       # Registration request DTO
│   └── TokenResponse.java         # Token response DTO
├── model/
│   ├── RefreshToken.java          # Refresh token entity
│   ├── Role.java                  # User role enum
│   └── User.java                  # User entity
├── repository/
│   ├── RefreshTokenRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java  # JWT filter
│   ├── JwtUtils.java                 # JWT utility methods
│   └── RateLimitInterceptor.java     # Rate limiting
├── service/
│   ├── AuthService.java              # Authentication business logic
│   └── CustomUserDetailsService.java # User details service
└── util/
    └── CookieUtil.java                # Cookie management utilities
```

### Security Features

#### 1. JWT Token Strategy
- **Access Token**: Short-lived (15 minutes), sent in response body
- **Refresh Token**: Long-lived (14 days), stored in HTTPOnly cookie
- **Token Rotation**: New refresh token issued on each refresh

#### 2. Password Security
- BCrypt hashing with cost factor 12
- Automatic password validation
- Secure password storage

#### 3. CORS Configuration
Pre-configured allowed origins:
- `http://localhost:3000` (React default)
- `http://localhost:5173` (Vite default)
- Custom domains (configure in SecurityConfig.java)

#### 4. Rate Limiting
Built-in rate limiting interceptor to prevent brute-force attacks

## Configuration

### JWT Configuration

The JWT settings can be configured in `application.properties`:

```properties
# JWT Secret - Use a strong, randomly generated key
app.jwt.secret=your-secret-key-here

# Access token expiration (milliseconds) - Default: 15 minutes
app.jwt.access-expiration-ms=900000

# Refresh token expiration (milliseconds) - Default: 14 days
app.jwt.refresh-expiration-ms=1209600000
```

### CORS Configuration

Update allowed origins in [SecurityConfig.java](src/main/java/com/authutil/authmaster/config/SecurityConfig.java):

```java
config.setAllowedOrigins(List.of(
    "http://localhost:3000",
    "https://your-frontend-domain.com"
));
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true
);
```

### User Roles Table
```sql
CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    roles VARCHAR(50)
);
```

### Refresh Tokens Table
```sql
CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

## Testing

Run the test suite:

```bash
./mvnw test
```

## Security Best Practices

1. **Generate a Strong JWT Secret**
   ```bash
   # Use a cryptographically secure random generator
   openssl rand -base64 64
   ```

2. **Use Environment Variables**
   ```bash
   export JWT_SECRET=your-secret-key
   export DB_PASSWORD=your-db-password
   ```

3. **Enable HTTPS in Production**
   - Set `Secure` flag on cookies
   - Use TLS/SSL certificates

4. **Rotate Secrets Regularly**
   - Implement key rotation strategy
   - Update JWT secrets periodically

5. **Monitor and Log**
   - Enable security event logging
   - Monitor failed login attempts

## Deployment

### Using Docker (Recommended)

Create a `Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/authmaster-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
./mvnw clean package
docker build -t authmaster .
docker run -p 8080:8080 authmaster
```

### Using Maven

```bash
./mvnw clean package
java -jar target/authmaster-0.0.1-SNAPSHOT.jar
```

## Environment Variables

For production deployment, use environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/user_auth_db
SPRING_DATASOURCE_USERNAME=db_user
SPRING_DATASOURCE_PASSWORD=db_password
JWT_SECRET=your-production-secret
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=8080
```

## Troubleshooting

### Common Issues

**Issue**: `Connection refused` to PostgreSQL
- **Solution**: Ensure PostgreSQL is running and credentials are correct

**Issue**: `Invalid JWT token`
- **Solution**: Check if the token has expired or the secret key has changed

**Issue**: CORS errors
- **Solution**: Add your frontend URL to the allowed origins list

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

**Anurag**

## Acknowledgments

- Spring Security Team
- JWT.io
- Spring Boot Community

---

## 📚 Additional Resources

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JWT Introduction](https://jwt.io/introduction)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

## Support

For issues and questions, please open an issue in the GitHub repository.

---

⭐ If you find this project helpful, please consider giving it a star!
