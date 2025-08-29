# AWS Cognito Middleware

A Spring Boot middleware that dynamically routes authentication requests to specific AWS Cognito User Pools based on the `X-APP-KEY` header.

## Overview

This middleware acts as a proxy between client applications and AWS Cognito. It routes authentication requests to the appropriate Cognito User Pool based on the `X-APP-KEY` header, which contains a UUID that maps to a configuration stored in a PostgreSQL database.

## Key Features

- Dynamic routing to different AWS Cognito User Pools
- Header-based client identification (`X-APP-KEY`)
- Support for custom user attributes during registration
- Secure storage of client secrets using Jasypt encryption
- Standardized API responses
- Comprehensive error handling

## API Endpoints

### Authentication Endpoints

All authentication endpoints require the `X-APP-KEY` header to be present.

### Registration

```
POST /api/auth/register
```

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "attributes": {
    "name": "John Doe",
    "custom:phone_number": "+1234567890"
  }
}
```

Additional custom attributes can be added to the `attributes` object.

For more details on custom attributes, see [CUSTOM_ATTRIBUTES.md](CUSTOM_ATTRIBUTES.md).

### Verification

```
POST /api/auth/verify
```

**Request:**
```json
{
  "email": "user@example.com",
  "confirmationCode": "123456"
}
```

### Login

```
POST /api/auth/login
```

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

### Forgot Password

```
POST /api/auth/forgot-password
```

**Request:**
```json
{
  "email": "user@example.com"
}
```

### Confirm Forgot Password

```
POST /api/auth/confirm-forgot-password
```

**Request:**
```json
{
  "email": "user@example.com",
  "confirmationCode": "123456",
  "newPassword": "NewSecurePass123!"
}
```

## Configuration

### Database Schema

The middleware uses a PostgreSQL database with the following schema:

```sql
CREATE TABLE app_config (
    id SERIAL PRIMARY KEY,
    app_key UUID UNIQUE NOT NULL,
    app_name VARCHAR(255) NOT NULL,
    aws_region VARCHAR(50) NOT NULL,
    user_pool_id VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255) NOT NULL
);
```

### Encrypting Client Secrets

Client secrets are encrypted using Jasypt. To encrypt a client secret:

1. Set the encryption password as an environment variable:
   ```
   export JASYPT_ENCRYPTOR_PASSWORD=your-secure-password
   ```

2. Use the Jasypt CLI or Spring Boot Jasypt to encrypt the client secret:
   ```
   java -cp ~/.m2/repository/org/jasypt/jasypt/1.9.3/jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI input="your-client-secret" password=your-secure-password algorithm=PBEWithMD5AndDES
   ```

3. Store the encrypted value in the database.

## Running the Application

1. Set up a PostgreSQL database and configure the connection in `application.properties`.
2. Set the Jasypt encryption password:
   ```
   export JASYPT_ENCRYPTOR_PASSWORD=your-secure-password
   ```
3. Run the application:
   ```
   ./mvnw spring-boot:run
   ```

### Admin Endpoints

The following endpoints are available for managing app configurations:

#### Get All Apps

```
GET /api/admin/apps
```

Returns a list of all app configurations.

#### Get App by ID

```
GET /api/admin/apps/{id}
```

Returns a specific app configuration by ID.

#### Create App

```
POST /api/admin/apps
```

**Request:**
```json
{
  "appName": "MyApp",
  "awsRegion": "us-east-1",
  "userPoolId": "us-east-1_abcdefghi",
  "clientId": "1234567890abcdefghij",
  "clientSecret": "abcdefghijklmnopqrstuvwxyz1234567890"
}
```

Creates a new app configuration and returns the created configuration with a generated UUID app key.

#### Update App

```
PUT /api/admin/apps/{id}
```

**Request:**
```json
{
  "appName": "UpdatedAppName",
  "awsRegion": "us-east-1",
  "userPoolId": "us-east-1_abcdefghi",
  "clientId": "1234567890abcdefghij",
  "clientSecret": "abcdefghijklmnopqrstuvwxyz1234567890"
}
```

Updates an existing app configuration.

#### Enable App

```
PUT /api/admin/apps/{id}/enable
```

Enables an app configuration.

#### Disable App

```
PUT /api/admin/apps/{id}/disable
```

Disables an app configuration. Disabled apps will be rejected by the interceptor.

#### Delete App

```
DELETE /api/admin/apps/{id}
```

Deletes an app configuration.

## Security Considerations

- Always use HTTPS in production
- Store the Jasypt encryption password securely (e.g., using environment variables or a secrets manager)
- Regularly rotate client secrets
- Consider implementing rate limiting to prevent abuse
- Secure the admin endpoints with appropriate authentication and authorization