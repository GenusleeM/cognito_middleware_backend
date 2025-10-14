# Verification Code Troubleshooting Guide

## Understanding the Error: "Invalid code provided, please request a code again"

This error (`ExpiredCodeException`) occurs when the verification code entered is expired or invalid. AWS Cognito verification codes have a limited lifespan (typically 24 hours) and can only be used once.

## Common Scenarios and Solutions

### Scenario 1: Code Has Expired
**Error Message:**
```json
{
  "success": false,
  "message": "Verification code has expired. Please request a new code.",
  "data": {
    "errorType": "ExpiredCodeException",
    "errorMessage": "Your verification code has expired",
    "action": "Please use the resend OTP endpoint to get a new code",
    "resendEndpoint": "/api/auth/resend-otp"
  }
}
```

**Solution:** Request a new verification code using the resend OTP endpoint.

**How to Fix:**
```bash
curl -X POST http://localhost:8080/api/auth/resend-otp \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Confirmation code resent successfully to email",
  "data": {
    "deliveryMedium": "EMAIL",
    "destination": "u***@e***.com",
    "message": "A new verification code has been sent to your email"
  }
}
```

### Scenario 2: User Already Verified
**Error Message from Verify Endpoint:**
```json
{
  "success": false,
  "message": "User is already verified or cannot be verified",
  "data": {
    "errorType": "NotAuthorizedException",
    "errorMessage": "User is already verified or cannot be verified",
    "action": "Try logging in - your account may already be verified"
  }
}
```

**Error Message from Resend OTP Endpoint:**
```json
{
  "success": false,
  "message": "User is already verified. Please log in.",
  "data": {
    "errorType": "NotAuthorizedException",
    "errorMessage": "User is already verified",
    "action": "Your account is already verified. You can log in directly."
  }
}
```

**Solution:** The user is already verified and can log in directly. No action needed.

**How to Log In:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com",
    "password": "yourPassword123"
  }'
```

### Scenario 3: Wrong Verification Code
**Error Message:**
```json
{
  "success": false,
  "message": "Invalid confirmation code. Please check and try again.",
  "data": {
    "errorType": "CodeMismatchException",
    "errorMessage": "The verification code you entered is incorrect",
    "action": "Please check the code and try again, or request a new code",
    "resendEndpoint": "/api/auth/resend-otp"
  }
}
```

**Solution:**
1. Double-check the code from your email
2. Make sure there are no extra spaces
3. Verify you're using the latest code (if you requested multiple codes, only the latest one works)
4. If still failing, request a new code using the resend OTP endpoint

### Scenario 4: User Not Found
**Error Message:**
```json
{
  "success": false,
  "message": "User not found",
  "data": {
    "errorType": "UserNotFoundException",
    "errorMessage": "No user found with this email address",
    "action": "Please check your email address or register a new account"
  }
}
```

**Solution:**
1. Verify the email address is correct
2. Check if you registered with a different email
3. If you haven't registered yet, use the register endpoint first

## Complete User Registration Flow

### Step 1: Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com",
    "password": "YourPassword123!",
    "attributes": {
      "name": "John Doe"
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully. Please check your email for verification code",
  "data": {
    "username": "user-sub-id",
    "status": "UNCONFIRMED",
    "userConfirmationNecessary": "true"
  }
}
```

### Step 2: Verify (within 24 hours of receiving the code)
```bash
curl -X POST http://localhost:8080/api/auth/verify \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com",
    "confirmationCode": "123456"
  }'
```

**Success Response:**
```json
{
  "success": true,
  "message": "User verified successfully",
  "data": {
    "status": "CONFIRMED"
  }
}
```

### Step 2b: If Code Expires - Resend OTP
```bash
curl -X POST http://localhost:8080/api/auth/resend-otp \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com"
  }'
```

Then retry Step 2 with the new code.

### Step 3: Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: your-app-key-here" \
  -d '{
    "email": "user@example.com",
    "password": "YourPassword123!"
  }'
```

## Important Notes

### Verification Code Behavior
1. **One-Time Use:** Each verification code can only be used once
2. **Expires After 24 Hours:** Codes are typically valid for 24 hours
3. **Latest Code Only:** If you request multiple codes, only the most recent one is valid
4. **Case Sensitive:** Verification codes are case-sensitive (though usually numeric)

### Best Practices
1. **Verify Immediately:** Complete verification as soon as you receive the code
2. **Check Spam Folder:** Verification emails might end up in spam
3. **Request New Code:** If more than 24 hours have passed, always request a new code
4. **Don't Retry Old Codes:** If a code fails, request a new one rather than retrying the old one

### Rate Limiting
- AWS Cognito has rate limiting on resend operations
- If you request too many codes in a short period, you may need to wait before requesting another
- Typical limit: 5 requests per hour per user

## Debugging Tips

### Check User Status in AWS Cognito Console
1. Go to AWS Cognito Console
2. Select your User Pool
3. Find the user by email
4. Check the "Account status" field:
   - `UNCONFIRMED`: User needs to verify email
   - `CONFIRMED`: User is verified and can log in
   - `FORCE_CHANGE_PASSWORD`: User needs to change password

### Check Application Logs
The application logs provide detailed information about what's happening:

```bash
# View logs in real-time
tail -f logs/application.log

# Search for specific user's verification attempts
grep "rukaramatosamson@gmail.com" logs/application.log

# Check for verification errors
grep "ExpiredCodeException\|CodeMismatchException" logs/application.log
```

### Activity Logs
Check the activity logs table in your database to see the history of verification attempts:

```sql
SELECT * FROM user_activity_log
WHERE username = 'user@example.com'
AND activity IN ('REGISTER', 'VERIFY', 'RESEND_OTP')
ORDER BY created_at DESC;
```

Or via the API:
```bash
curl http://localhost:8080/api/logs/user/user@example.com
```

## Support Contact
If you continue to experience issues after following this guide:
1. Check the activity logs at `/api/logs`
2. Verify your AWS Cognito configuration
3. Ensure the user pool settings allow email verification
4. Contact your system administrator with the error details
