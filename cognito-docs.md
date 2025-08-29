# AWS Cognito Middleware Documentation

This document provides comprehensive documentation for the AWS Cognito Middleware, including application creation, configuration, authentication processes, and security mechanisms.

## Table of Contents

1. [Overview](#overview)
2. [Application Creation and Configuration](#application-creation-and-configuration)
   - [Creating a New Application](#creating-a-new-application)
   - [Application Configuration Parameters](#application-configuration-parameters)
   - [Managing Applications](#managing-applications)
3. [Authentication Flow](#authentication-flow)
   - [Registration Process](#registration-process)
   - [Email Verification](#email-verification)
   - [Login Process](#login-process)
   - [Password Reset](#password-reset)
4. [Security Mechanisms](#security-mechanisms)
   - [X-APP-KEY Authentication](#x-app-key-authentication)
   - [Client Secret Encryption](#client-secret-encryption)
   - [Security Best Practices](#security-best-practices)
5. [API Reference](#api-reference)
   - [Authentication Endpoints](#authentication-endpoints)
   - [Admin Endpoints](#admin-endpoints)
6. [Troubleshooting](#troubleshooting)

## Overview

The AWS Cognito Middleware acts as a proxy between client applications and AWS Cognito. It routes authentication requests to the appropriate Cognito User Pool based on the `X-APP-KEY` header, which contains a UUID that maps to a configuration stored in a PostgreSQL database.

Key features include:
- Dynamic routing to different AWS Cognito User Pools
- Header-based client identification (`X-APP-KEY`)
- Support for custom user attributes during registration
- Secure storage of client secrets using Jasypt encryption
- Standardized API responses
- Comprehensive error handling

## Application Creation and Configuration

### Creating a New Application

Before using the middleware, you need to create an application configuration that maps to your AWS Cognito User Pool. This can be done through the Admin API:

1. **Create AWS Cognito User Pool**: First, create a User Pool in AWS Cognito with your desired settings.

2. **Create Application Configuration**: Use the Admin API to create a new application configuration:

```
POST /api/admin/apps
```

Request body:
```json
{
  "appName": "MyApplication",
  "awsRegion": "us-east-1",
  "userPoolId": "us-east-1_abcdefghi",
  "clientId": "1234567890abcdefghij",
  "clientSecret": "abcdefghijklmnopqrstuvwxyz1234567890"
}
```

Response:
```json
{
  "success": true,
  "message": "App configuration created successfully",
  "data": {
    "id": 1,
    "appKey": "550e8400-e29b-41d4-a716-446655440000",
    "appName": "MyApplication",
    "awsRegion": "us-east-1",
    "userPoolId": "us-east-1_abcdefghi",
    "clientId": "1234567890abcdefghij",
    "enabled": true
  }
}
```

3. **Note the App Key**: The `appKey` in the response is a UUID that will be used as the `X-APP-KEY` header value for all authentication requests.

### Application Configuration Parameters

When creating or updating an application configuration, the following parameters are required:

- `appName`: A descriptive name for the application
- `awsRegion`: The AWS region where the Cognito User Pool is located (e.g., "us-east-1")
- `userPoolId`: The ID of the Cognito User Pool (e.g., "us-east-1_abcdefghi")
- `clientId`: The App Client ID from the Cognito User Pool

Optional parameters:

- `clientSecret`: The App Client Secret from the Cognito User Pool (if applicable)
  - Note: This is optional as some Cognito operations only require userPoolId and clientId

### Managing Applications

The Admin API provides endpoints for managing application configurations:

#### Get All Applications

```
GET /api/admin/apps
```

Returns a list of all application configurations.

#### Get Application by ID

```
GET /api/admin/apps/{id}
```

Returns a specific application configuration by ID.

#### Update Application

```
PUT /api/admin/apps/{id}
```

Request body:
```json
{
  "appName": "UpdatedAppName",
  "awsRegion": "us-east-1",
  "userPoolId": "us-east-1_abcdefghi",
  "clientId": "1234567890abcdefghij",
  "clientSecret": "abcdefghijklmnopqrstuvwxyz1234567890"
}
```

Updates an existing application configuration.

#### Enable/Disable Application

```
PUT /api/admin/apps/{id}/enable
PUT /api/admin/apps/{id}/disable
```

Enables or disables an application configuration. Disabled applications will be rejected by the middleware.

#### Delete Application

```
DELETE /api/admin/apps/{id}
```

Deletes an application configuration.

## Authentication Flow

All authentication endpoints require the `X-APP-KEY` header to be present with a valid UUID that corresponds to an enabled application configuration.

### Registration Process

The registration process allows you to create new users in AWS Cognito with custom attributes.

#### Registration Endpoint

```
POST /api/auth/register
```

Headers:
- `Content-Type: application/json` (required)
- `X-APP-KEY: <UUID>` (required) - The UUID identifying the client application

Request body:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "attributes": {
    "name": "John Doe",
    "custom:phone_number": "+1234567890",
    "custom:National_ID": "ID12345",
    "custom:passport_number": "PASS12345",
    "custom:dob": "1990-01-01",
    "custom:nationality": "USA",
    "custom:country_of_origin": "USA",
    "custom:verification_status": "pending"
  }
}
```

Required fields:
- `email`: The user's email address (must be valid)
- `password`: The user's password

Optional fields:
- `attributes`: A map of custom attributes for the user

Custom attributes must be prefixed with `custom:` if they are not standard Cognito attributes.

Success response:
```json
{
  "success": true,
  "message": "User registered successfully. Please check your email for verification code",
  "data": {
    "username": "user@example.com",
    "status": "UNCONFIRMED",
    "userConfirmationNecessary": "true"
  }
}
```

#### Registration Implementation Details

The registration process works as follows:

1. The client sends a POST request to `/api/auth/register` with the required headers and request body.
2. The `AppKeyInterceptor` validates the `X-APP-KEY` header and fetches the corresponding Cognito configuration.
3. The `AuthController` validates the request body and calls the `CognitoService.registerUser` method.
4. The `CognitoService` creates a new user in the Cognito User Pool with the provided attributes.
5. AWS Cognito sends a verification code to the user's email address.
6. The response includes the username and status "UNCONFIRMED" to indicate that email verification is required.
7. The user must verify their email using the verification code before they can log in.

### Email Verification

After registration, users must verify their email address before they can log in.

#### Verification Endpoint

```
POST /api/auth/verify
```

Headers:
- `Content-Type: application/json` (required)
- `X-APP-KEY: <UUID>` (required) - The UUID identifying the client application

Request body:
```json
{
  "email": "user@example.com",
  "confirmationCode": "123456"
}
```

Success response:
```json
{
  "success": true,
  "message": "User verified successfully",
  "data": {
    "status": "CONFIRMED"
  }
}
```

#### Verification Implementation Details

1. When a user registers, AWS Cognito automatically sends a verification code to their email address.
2. The user must use this code with the `/api/auth/verify` endpoint to confirm their account.
3. The `CognitoService` calls the Cognito API to verify the user's email address.
4. Only after successful verification can the user log in to the system.

### Login Process

The login process authenticates a user and returns JWT tokens that can be used for subsequent API calls.

#### Login Endpoint

```
POST /api/auth/login
```

Headers:
- `Content-Type: application/json` (required)
- `X-APP-KEY: <UUID>` (required) - The UUID identifying the client application

Request body:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

Success response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJraWQiOiJxYWZcL3...",
    "refreshToken": "eyJjdHkiOiJKV1...",
    "idToken": "eyJraWQiOiJxYWZcL...",
    "expiresIn": "3600"
  }
}
```

#### Login Implementation Details

1. The client sends a POST request to `/api/auth/login` with the required headers and request body.
2. The `AppKeyInterceptor` validates the `X-APP-KEY` header and fetches the corresponding Cognito configuration.
3. The `AuthController` validates the request body and calls the `CognitoService.authenticateUser` method.
4. The `CognitoService` authenticates the user with the Cognito User Pool.
5. If authentication is successful, Cognito returns JWT tokens (access token, refresh token, and ID token).
6. The response includes these tokens and their expiration time.

### Password Reset

The password reset process allows users to reset their password if they forget it.

#### Forgot Password Endpoint

```
POST /api/auth/forgot-password
```

Headers:
- `Content-Type: application/json` (required)
- `X-APP-KEY: <UUID>` (required) - The UUID identifying the client application

Request body:
```json
{
  "email": "user@example.com"
}
```

Success response:
```json
{
  "success": true,
  "message": "Password reset code sent to email",
  "data": {
    "deliveryMedium": "EMAIL",
    "destination": "u***@e***.com"
  }
}
```

#### Confirm Forgot Password Endpoint

```
POST /api/auth/confirm-forgot-password
```

Headers:
- `Content-Type: application/json` (required)
- `X-APP-KEY: <UUID>` (required) - The UUID identifying the client application

Request body:
```json
{
  "email": "user@example.com",
  "confirmationCode": "123456",
  "newPassword": "NewSecurePass123!"
}
```

Success response:
```json
{
  "success": true,
  "message": "Password reset successful",
  "data": {}
}
```

#### Password Reset Implementation Details

1. The user initiates the password reset process by calling the `/api/auth/forgot-password` endpoint.
2. AWS Cognito sends a confirmation code to the user's email address.
3. The user uses this code along with their new password to call the `/api/auth/confirm-forgot-password` endpoint.
4. The `CognitoService` confirms the password reset with the Cognito User Pool.
5. If successful, the user can now log in with their new password.

## Security Mechanisms

### X-APP-KEY Authentication

The middleware uses the `X-APP-KEY` header to identify the client application and route requests to the appropriate Cognito User Pool.

#### How X-APP-KEY Works

1. Each application configuration in the database has a unique UUID `appKey`.
2. Clients must include this UUID in the `X-APP-KEY` header for all authentication requests.
3. The `AppKeyInterceptor` intercepts all requests to `/api/auth/**` endpoints.
4. It validates the `X-APP-KEY` header and fetches the corresponding Cognito configuration.
5. If the header is missing, invalid, or corresponds to a disabled application, the request is rejected.
6. If the header is valid, the Cognito configuration is stored in the request attributes for use by the service layer.

#### Security Considerations for X-APP-KEY

- The `X-APP-KEY` should be treated as a secret and not exposed to end users.
- It should be stored securely in your client application's configuration.
- Consider implementing additional security measures such as IP whitelisting or API key rotation.

### Client Secret Encryption

Client secrets are encrypted in the database using Jasypt encryption.

#### How Client Secret Encryption Works

1. The client secret is encrypted using Jasypt before being stored in the database.
2. The encryption password is set as an environment variable: `JASYPT_ENCRYPTOR_PASSWORD`.
3. When the client secret is needed, it is automatically decrypted by the `AttributeEncryptor`.

#### Encrypting a Client Secret

To encrypt a client secret:

1. Set the encryption password as an environment variable:
   ```
   export JASYPT_ENCRYPTOR_PASSWORD=your-secure-password
   ```

2. Use the Jasypt CLI to encrypt the client secret:
   ```
   java -cp ~/.m2/repository/org/jasypt/jasypt/1.9.3/jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI input="your-client-secret" password=your-secure-password algorithm=PBEWithMD5AndDES
   ```

3. Store the encrypted value in the database.

### Security Best Practices

When using the AWS Cognito Middleware, follow these security best practices:

1. **Use HTTPS**: Always use HTTPS in production to encrypt data in transit.
2. **Secure Encryption Password**: Store the Jasypt encryption password securely (e.g., using environment variables or a secrets manager).
3. **Rotate App Keys**: Regularly rotate app keys and client secrets.
4. **Implement Rate Limiting**: Consider implementing rate limiting to prevent abuse.
5. **Secure Admin Endpoints**: Secure the admin endpoints with appropriate authentication and authorization.
6. **Monitor and Audit**: Implement logging and monitoring to detect and respond to security incidents.
7. **Validate Input**: Always validate user input to prevent injection attacks.
8. **Minimize Data Exposure**: Only expose the minimum data necessary in API responses.

## API Reference

### Authentication Endpoints

All authentication endpoints require the `X-APP-KEY` header to be present.

#### Registration

```
POST /api/auth/register
```

Registers a new user in the Cognito User Pool.

#### Verification

```
POST /api/auth/verify
```

Verifies a user's email address with a confirmation code.

#### Login

```
POST /api/auth/login
```

Authenticates a user and returns JWT tokens.

#### Forgot Password

```
POST /api/auth/forgot-password
```

Initiates the password reset process.

#### Confirm Forgot Password

```
POST /api/auth/confirm-forgot-password
```

Confirms a new password with a confirmation code.

### Admin Endpoints

#### Get All Applications

```
GET /api/admin/apps
```

Returns a list of all application configurations.

#### Get Application by ID

```
GET /api/admin/apps/{id}
```

Returns a specific application configuration by ID.

#### Create Application

```
POST /api/admin/apps
```

Creates a new application configuration.

#### Update Application

```
PUT /api/admin/apps/{id}
```

Updates an existing application configuration.

#### Enable Application

```
PUT /api/admin/apps/{id}/enable
```

Enables an application configuration.

#### Disable Application

```
PUT /api/admin/apps/{id}/disable
```

Disables an application configuration.

#### Delete Application

```
DELETE /api/admin/apps/{id}
```

Deletes an application configuration.

## Troubleshooting

### Common Issues and Solutions

1. **Invalid X-APP-KEY**: Ensure that the `X-APP-KEY` header contains a valid UUID that corresponds to an enabled application configuration in the database.

2. **User Already Exists**: If you receive a "User with this email already exists" error during registration, the user is already registered. They should use the login endpoint or reset their password if they forgot it.

3. **Invalid Confirmation Code**: If you receive an "Invalid confirmation code" error during verification or password reset, ensure that the code is correct and has not expired.

4. **AWS Credentials Error**: If you receive an "AWS credentials error", ensure that the AWS region, user pool ID, client ID, and client secret (if applicable) are correct in the application configuration.

5. **Configuration Error**: If you receive a "Configuration error", ensure that the application configuration is complete and valid.

### Error Response Format

All error responses follow this format:

```json
{
  "success": false,
  "message": "Error message",
  "data": {
    "errorType": "ErrorType",
    "errorMessage": "Detailed error message"
  }
}
```

The `errorType` field can be used to determine the type of error and handle it appropriately.