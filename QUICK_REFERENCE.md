# Quick Reference Card - Authentication Endpoints

## 🚨 MOST COMMON MISTAKE
**Using `/api/auth/verify` for MFA login codes** ❌

✅ **Correct:**
- Account verification (after registration): `/api/auth/verify`
- MFA verification (during login): `/api/auth/mfa/verify`

---

## Complete Authentication Flows

### Flow 1: New User Registration & Verification

```bash
# 1. Register
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "Pass123!",
  "attributes": { "name": "John" }
}

# 2. Verify account (code from email)
POST /api/auth/verify
{
  "email": "user@example.com",
  "confirmationCode": "123456"
}

# 3. If code expired, resend
POST /api/auth/resend-otp
{
  "email": "user@example.com"
}
```

### Flow 2: Login WITHOUT MFA

```bash
# Login
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "Pass123!"
}

# Response: tokens directly
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "idToken": "..."
  }
}
```

### Flow 3: Login WITH EMAIL_OTP MFA ⭐

```bash
# 1. Login
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "Pass123!"
}

# Response: MFA challenge
{
  "data": {
    "challengeName": "EMAIL_OTP",
    "session": "AYABeC3wy...",  # ← SAVE THIS
    "challengeParameters": {
      "CODE_DELIVERY_DESTINATION": "u***@e***.com"
    }
  }
}

# 2. Check email for 6-digit code

# 3. Verify MFA
POST /api/auth/mfa/verify  # ← NOTE: /mfa/verify not /verify
{
  "email": "user@example.com",
  "session": "AYABeC3wy...",    # ← FROM STEP 1
  "mfaCode": "264581",           # ← FROM EMAIL
  "challengeName": "EMAIL_OTP"   # ← MUST MATCH
}

# Response: tokens
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "idToken": "..."
  }
}
```

---

## Endpoint Quick Reference

| Endpoint | Purpose | Session Required? | When to Use |
|----------|---------|-------------------|-------------|
| `/api/auth/register` | Create account | No | First time users |
| `/api/auth/verify` | Verify account email | No | After registration |
| `/api/auth/resend-otp` | Resend verification code | No | If verification code expired |
| `/api/auth/login` | Authenticate | No | Every login |
| `/api/auth/mfa/verify` | Verify MFA code | **Yes** | After login if MFA enabled |
| `/api/auth/forgot-password` | Request password reset | No | Forgot password |
| `/api/auth/confirm-forgot-password` | Complete password reset | No | With reset code |

---

## Error Messages Decoder

### "Invalid confirmation code"
**Problem:** Using wrong endpoint or wrong code

**Solutions:**
- Initial verification? Use `/api/auth/verify`
- Login MFA? Use `/api/auth/mfa/verify`
- Code expired? Get new one

### "Verification code has expired"
**Solution:**
```bash
POST /api/auth/resend-otp
{ "email": "user@example.com" }
```

### "User is already verified"
**Solution:** No action needed, just log in!
```bash
POST /api/auth/login
{ "email": "user@example.com", "password": "..." }
```

### "MFA verification required"
**Solution:** Use `/api/auth/mfa/verify` with the session from login response

---

## Code Type Identification

| Code Source | Endpoint to Use | Session Needed? |
|-------------|-----------------|-----------------|
| "Welcome! Verify your email" | `/api/auth/verify` | ❌ No |
| "Here's your login code" | `/api/auth/mfa/verify` | ✅ Yes |
| "Reset your password" | `/api/auth/confirm-forgot-password` | ❌ No |

---

## Testing Scripts

```bash
# Test registration and verification
./test-resend-otp.sh user@example.com your-app-key

# Test EMAIL_OTP MFA login
./test-email-otp-login.sh user@example.com password123 your-app-key
```

---

## Common Patterns

### JavaScript/TypeScript

```typescript
// Login with MFA handling
async function loginWithMfa(email: string, password: string) {
  // Step 1: Login
  const loginRes = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-APP-KEY': appKey },
    body: JSON.stringify({ email, password })
  });
  const loginData = await loginRes.json();

  // Check for MFA challenge
  if (loginData.data.challengeName === 'EMAIL_OTP') {
    // Save session
    const session = loginData.data.session;

    // Prompt user for code
    const mfaCode = prompt('Enter code from email:');

    // Step 2: Verify MFA
    const mfaRes = await fetch('/api/auth/mfa/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-APP-KEY': appKey },
      body: JSON.stringify({
        email,
        session,
        mfaCode,
        challengeName: 'EMAIL_OTP'
      })
    });

    return await mfaRes.json();
  }

  // No MFA, return login response
  return loginData;
}
```

---

## Headers Required

All endpoints require:
```
Content-Type: application/json
X-APP-KEY: your-app-key-here
```

---

## Activity Logs

Check what happened:
```bash
# All logs
GET /api/logs

# By user
GET /api/logs/user/{email}

# By app
GET /api/logs/app/{appName}

# By date range
GET /api/logs/date-range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59
```

---

## Troubleshooting Flowchart

```
Code not working?
├─ Is it during first-time verification?
│  ├─ Yes → Use /api/auth/verify
│  └─ No → Continue
│
├─ Is it during login (MFA)?
│  ├─ Yes → Use /api/auth/mfa/verify (with session!)
│  └─ No → Continue
│
├─ Code expired?
│  ├─ For verification → /api/auth/resend-otp
│  └─ For MFA → Login again to get new code
│
└─ Still failing?
   └─ Check activity logs: GET /api/logs/user/{email}
```

---

## Support Files

- `EMAIL_OTP_MFA_GUIDE.md` - Detailed EMAIL_OTP guide
- `VERIFICATION_CODE_TROUBLESHOOTING.md` - Verification issues
- `RESEND_OTP_ENDPOINT.md` - Resend OTP documentation
- `test-email-otp-login.sh` - Test MFA login
- `test-resend-otp.sh` - Test resend functionality
