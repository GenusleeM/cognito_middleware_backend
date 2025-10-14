# EMAIL_OTP MFA Authentication Guide

## Overview
This guide explains how to handle EMAIL_OTP Multi-Factor Authentication (MFA) during login. EMAIL_OTP is different from the initial account verification - it's an additional security layer that requires a code each time you log in.

## Understanding the Two Different Flows

### Flow 1: Initial Account Verification (One-Time)
This happens **once** after registration:
1. Register → `/api/auth/register`
2. Verify account → `/api/auth/verify` (with code from email)
3. ✅ Account is now verified

### Flow 2: EMAIL_OTP MFA (Every Login)
This happens **every time** you log in (if MFA is enabled):
1. Login → `/api/auth/login`
2. Receive EMAIL_OTP challenge
3. Verify MFA → `/api/auth/mfa/verify` (with code from email)
4. ✅ Receive access tokens

## ⚠️ Important: Which Endpoint to Use?

| Scenario | Endpoint | Code Type |
|----------|----------|-----------|
| First time verifying account after registration | `/api/auth/verify` | Account verification code |
| Logging in with EMAIL_OTP MFA enabled | `/api/auth/mfa/verify` | MFA/OTP code |
| Logging in with SMS MFA enabled | `/api/auth/mfa/verify` | MFA/OTP code |
| Logging in with Authenticator App | `/api/auth/mfa/verify` | TOTP code |

## Complete EMAIL_OTP MFA Login Flow

### Step 1: Login Request
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com",
    "password": "YourPassword123!"
  }'
```

### Step 2: Receive Challenge Response
If EMAIL_OTP MFA is enabled, you'll receive:

```json
{
  "success": true,
  "message": "MFA verification required. Please provide the MFA code.",
  "data": {
    "challengeName": "EMAIL_OTP",
    "session": "AYABeC3wy3Wg1MdfDIBUKWX6S3IAHQABAAdTZXJ2aWNl...",
    "challengeParameters": {
      "CODE_DELIVERY_DELIVERY_MEDIUM": "EMAIL",
      "CODE_DELIVERY_DESTINATION": "u***@e***.com",
      "USER_ID_FOR_SRP": "1215c4c4-e021-7054-3c68-94858df7c169"
    }
  }
}
```

**Key fields:**
- `challengeName`: "EMAIL_OTP" - tells you which type of MFA is required
- `session`: **REQUIRED** - must be sent with the MFA code
- `challengeParameters.CODE_DELIVERY_DESTINATION`: Where the code was sent

### Step 3: Check Your Email
You'll receive an email with a 6-digit code like: **264581**

### Step 4: Verify MFA Code
**IMPORTANT: Use the `/api/auth/mfa/verify` endpoint (NOT `/api/auth/verify`)**

```bash
curl -X POST http://localhost:8080/api/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com",
    "session": "AYABeC3wy3Wg1MdfDIBUKWX6S3IAHQABAAdTZXJ2aWNl...",
    "mfaCode": "264581",
    "challengeName": "EMAIL_OTP"
  }'
```

**Request Fields:**
- `email`: Your email address
- `session`: The session string from Step 2 response (REQUIRED!)
- `mfaCode`: The 6-digit code from your email
- `challengeName`: "EMAIL_OTP" (must match the challenge from Step 2)

### Step 5: Receive Authentication Tokens
Upon successful verification:

```json
{
  "success": true,
  "message": "MFA verification successful",
  "data": {
    "accessToken": "eyJraWQiOiJxxx...",
    "refreshToken": "eyJjdHkiOiJKV1Q...",
    "idToken": "eyJraWQiOiJCTXxx...",
    "expiresIn": "3600"
  }
}
```

## Common Errors and Solutions

### Error 1: "Invalid MFA code" (CodeMismatchException)

**Possible Causes:**
1. ❌ Using `/api/auth/verify` instead of `/api/auth/mfa/verify`
2. ❌ Wrong code entered
3. ❌ Code has expired (typically valid for 3-5 minutes)
4. ❌ Using an old code (if you requested multiple codes)

**Solution:**
```bash
# 1. Make sure you're using the CORRECT endpoint
curl -X POST http://localhost:8080/api/auth/mfa/verify \  # ← Note: /mfa/verify
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com",
    "session": "YOUR_SESSION_HERE",
    "mfaCode": "264581",
    "challengeName": "EMAIL_OTP"
  }'

# 2. If still failing, log in again to get a fresh code
```

### Error 2: Missing or Invalid Session

**Error:**
```json
{
  "success": false,
  "message": "Session is required",
  "data": {
    "errorType": "ValidationError"
  }
}
```

**Solution:** Make sure you include the `session` from the login response.

### Error 3: Code Expired

**Solution:** Log in again to receive a fresh code. Each login generates a new EMAIL_OTP code.

## JavaScript/Frontend Example

```javascript
// Step 1: Login
const loginResponse = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-APP-KEY': 'your-app-key-here'
  },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'YourPassword123!'
  })
});

const loginData = await loginResponse.json();

// Check if MFA is required
if (loginData.data.challengeName === 'EMAIL_OTP') {
  console.log('MFA required. Check your email for the code.');

  // Save the session
  const session = loginData.data.session;

  // Step 2: Prompt user for MFA code
  const mfaCode = prompt('Enter the code from your email:');

  // Step 3: Verify MFA
  const mfaResponse = await fetch('http://localhost:8080/api/auth/mfa/verify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-APP-KEY': 'your-app-key-here'
    },
    body: JSON.stringify({
      email: 'user@example.com',
      session: session,
      mfaCode: mfaCode,
      challengeName: 'EMAIL_OTP'
    })
  });

  const mfaData = await mfaResponse.json();

  if (mfaData.success) {
    console.log('Login successful!');
    console.log('Access Token:', mfaData.data.accessToken);
    // Store tokens and proceed
  } else {
    console.error('MFA verification failed:', mfaData.message);
  }
} else {
  // No MFA required, tokens are in loginData.data
  console.log('Login successful without MFA!');
}
```

## Comparison: Account Verification vs MFA

### Account Verification (`/api/auth/verify`)
- ✅ Used **once** after registration
- ✅ Verifies email ownership
- ✅ No session required
- ✅ Can resend code with `/api/auth/resend-otp`
- ❌ **NOT for login MFA**

**Request:**
```json
{
  "email": "user@example.com",
  "confirmationCode": "123456"
}
```

### MFA Verification (`/api/auth/mfa/verify`)
- ✅ Used **every time** you log in (if MFA enabled)
- ✅ Additional security layer
- ✅ **Requires session** from login response
- ✅ **For EMAIL_OTP, SMS_MFA, SOFTWARE_TOKEN_MFA**
- ❌ Cannot resend (must login again for new code)

**Request:**
```json
{
  "email": "user@example.com",
  "session": "AYABeC3wy...",
  "mfaCode": "264581",
  "challengeName": "EMAIL_OTP"
}
```

## Supported MFA Types

The `/api/auth/mfa/verify` endpoint supports:

1. **EMAIL_OTP** - Code sent via email (this guide)
2. **SMS_MFA** - Code sent via SMS
3. **SOFTWARE_TOKEN_MFA** - Code from authenticator app (Google Authenticator, Authy, etc.)

All use the same endpoint with different `challengeName` values.

## Activity Logging

All MFA verification attempts are logged:

```bash
# Check MFA verification logs
curl http://localhost:8080/api/logs/user/user@example.com \
  -H "X-APP-KEY: your-app-key-here"
```

Look for activities:
- `LOGIN` - Initial login attempt
- `MFA_VERIFY` - MFA code verification attempts (SUCCESS/FAILURE)

## Testing Your Current Scenario

Based on your login response, here's the exact request you should make:

```bash
curl -X POST http://localhost:8080/api/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "rukaramatosamson@gmail.com",
    "session": "AYABeC3wy3Wg1MdfDIBUKWX6S3IAHQABAAdTZXJ2aWNl...",
    "mfaCode": "264581",
    "challengeName": "EMAIL_OTP"
  }'
```

Replace:
- `session` with the full session string from your login response
- `mfaCode` with the actual code from your email

## Quick Reference

| Endpoint | Purpose | Session Required? | Resend Available? |
|----------|---------|-------------------|-------------------|
| `/api/auth/verify` | Initial account verification | No | Yes (`/api/auth/resend-otp`) |
| `/api/auth/mfa/verify` | Login MFA verification | Yes | No (login again) |

## Troubleshooting Checklist

- [ ] Using `/api/auth/mfa/verify` (not `/api/auth/verify`)
- [ ] Including the `session` from login response
- [ ] Using `challengeName: "EMAIL_OTP"`
- [ ] Code is from the most recent email
- [ ] Code hasn't expired (typically 3-5 minutes)
- [ ] Email address matches the one used for login
- [ ] APP-KEY header is present and correct
