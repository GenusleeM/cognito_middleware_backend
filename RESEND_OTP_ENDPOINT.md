# Resend OTP Endpoint Documentation

## Overview
This document describes the new `/api/auth/resend-otp` endpoint that allows users to request a new OTP/confirmation code if they didn't receive the initial one during registration.

## Endpoint Details

### URL
```
POST /api/auth/resend-otp
```

### Headers
- `Content-Type: application/json`
- `X-APP-KEY: <your-app-key>` (Required - Multi-tenant identifier)

### Request Body
```json
{
  "email": "user@example.com"
}
```

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Confirmation code resent successfully to email",
  "data": {
    "deliveryMedium": "EMAIL",
    "destination": "u***@e***.com"
  }
}
```

### Error Responses

#### User Not Found (400 Bad Request)
```json
{
  "success": false,
  "message": "User not found",
  "data": {
    "errorType": "UserNotFoundException",
    "errorMessage": "User does not exist."
  }
}
```

#### Configuration Error (400 Bad Request)
```json
{
  "success": false,
  "message": "Configuration error: ...",
  "data": {
    "errorType": "ConfigurationError",
    "errorMessage": "Cognito app configuration not found in request"
  }
}
```

#### Invalid Email Format (400 Bad Request)
```json
{
  "success": false,
  "message": "Validation error",
  "data": {
    "email": "Email should be valid"
  }
}
```

## Usage Examples

### cURL
```bash
curl -X POST http://localhost:8080/api/auth/resend-otp \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com"
  }'
```

### JavaScript (Fetch API)
```javascript
const response = await fetch('http://localhost:8080/api/auth/resend-otp', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-APP-KEY': 'your-app-key-here'
  },
  body: JSON.stringify({
    email: 'user@example.com'
  })
});

const data = await response.json();
console.log(data);
```

### Axios
```javascript
const axios = require('axios');

try {
  const response = await axios.post(
    'http://localhost:8080/api/auth/resend-otp',
    { email: 'user@example.com' },
    {
      headers: {
        'Content-Type': 'application/json',
        'X-APP-KEY': 'your-app-key-here'
      }
    }
  );
  console.log(response.data);
} catch (error) {
  console.error('Error:', error.response.data);
}
```

## Activity Logging
All resend OTP requests are automatically logged to the `user_activity_log` table with:
- **Activity**: `RESEND_OTP`
- **Status**: `SUCCESS` or `FAILURE`
- **Username**: The email address
- **User Pool ID**: From the app configuration
- **App Name**: From the app configuration
- **IP Address**: Client's IP address
- **Error Message**: Included if the request fails

## Implementation Details

### Files Modified/Created
1. **ResendOtpRequest.java** (NEW)
   - Location: `src/main/java/com/oldmutual/AwsCognitoMiddleware/dto/ResendOtpRequest.java`
   - DTO for resend OTP request validation

2. **CognitoService.java** (MODIFIED)
   - Location: `src/main/java/com/oldmutual/AwsCognitoMiddleware/service/CognitoService.java`
   - Added `resendConfirmationCode()` method

3. **AuthController.java** (MODIFIED)
   - Location: `src/main/java/com/oldmutual/AwsCognitoMiddleware/controller/AuthController.java`
   - Added `/resend-otp` endpoint

### AWS Cognito Integration
The endpoint uses AWS Cognito's `ResendConfirmationCode` API to trigger a new confirmation code to be sent to the user's email address.

## Testing
You can test the endpoint using the following scenarios:

### Test Case 1: Valid Request
- **Pre-condition**: User has registered but not verified their email
- **Request**: POST with valid email
- **Expected**: 200 OK with delivery details

### Test Case 2: User Not Found
- **Pre-condition**: Email does not exist in the system
- **Request**: POST with non-existent email
- **Expected**: 400 Bad Request with UserNotFoundException

### Test Case 3: Invalid Email Format
- **Pre-condition**: N/A
- **Request**: POST with invalid email format
- **Expected**: 400 Bad Request with validation error

### Test Case 4: Missing APP-KEY
- **Pre-condition**: N/A
- **Request**: POST without X-APP-KEY header
- **Expected**: 400 Bad Request with configuration error

## Notes
- The OTP code sent is typically 6 digits
- The code expires after a configurable time (default: 24 hours in Cognito)
- Users can request a new code multiple times, but AWS Cognito has rate limiting
- All requests require the X-APP-KEY header for multi-tenant support
