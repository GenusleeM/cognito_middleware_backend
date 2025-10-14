#!/bin/bash

# Test script for EMAIL_OTP MFA Login Flow
# Usage: ./test-email-otp-login.sh <email> <password> <app-key>

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if required arguments are provided
if [ "$#" -lt 3 ]; then
    echo -e "${RED}Error: Missing required arguments${NC}"
    echo "Usage: $0 <email> <password> <app-key>"
    echo "Example: $0 user@example.com MyPassword123! your-app-key-here"
    exit 1
fi

EMAIL=$1
PASSWORD=$2
APP_KEY=$3
API_URL="${4:-http://localhost:8080}"

echo -e "${BLUE}=== Testing EMAIL_OTP MFA Login Flow ===${NC}"
echo "Email: $EMAIL"
echo "API URL: $API_URL"
echo ""

# Step 1: Login
echo -e "${YELLOW}Step 1: Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: $APP_KEY" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")

echo "$LOGIN_RESPONSE" | jq .

# Check if MFA challenge is required
CHALLENGE_NAME=$(echo "$LOGIN_RESPONSE" | jq -r '.data.challengeName')
SUCCESS=$(echo "$LOGIN_RESPONSE" | jq -r '.success')

if [ "$SUCCESS" == "false" ]; then
    echo -e "${RED}✗ Login failed${NC}"
    ERROR_MSG=$(echo "$LOGIN_RESPONSE" | jq -r '.message')
    echo -e "${RED}Error: $ERROR_MSG${NC}"
    exit 1
fi

if [ "$CHALLENGE_NAME" == "EMAIL_OTP" ]; then
    echo -e "${GREEN}✓ Login successful - MFA challenge received!${NC}"
    echo ""

    # Extract session
    SESSION=$(echo "$LOGIN_RESPONSE" | jq -r '.data.session')
    DESTINATION=$(echo "$LOGIN_RESPONSE" | jq -r '.data.challengeParameters.CODE_DELIVERY_DESTINATION')

    echo -e "${YELLOW}MFA Challenge Details:${NC}"
    echo "Challenge Type: EMAIL_OTP"
    echo "Code sent to: $DESTINATION"
    echo ""

    # Step 2: Prompt for MFA code
    echo -e "${YELLOW}Step 2: MFA Verification${NC}"
    echo "Please check your email and enter the 6-digit code:"
    read -r MFA_CODE

    echo ""
    echo -e "${YELLOW}Verifying MFA code: $MFA_CODE${NC}"

    # Step 3: Verify MFA
    MFA_RESPONSE=$(curl -s -X POST "$API_URL/api/auth/mfa/verify" \
      -H "Content-Type: application/json" \
      -H "X-APP-KEY: $APP_KEY" \
      -d "{
        \"email\": \"$EMAIL\",
        \"session\": \"$SESSION\",
        \"mfaCode\": \"$MFA_CODE\",
        \"challengeName\": \"EMAIL_OTP\"
      }")

    echo "$MFA_RESPONSE" | jq .

    MFA_SUCCESS=$(echo "$MFA_RESPONSE" | jq -r '.success')

    if [ "$MFA_SUCCESS" == "true" ]; then
        echo ""
        echo -e "${GREEN}✓✓✓ MFA verification successful! Login complete! ✓✓✓${NC}"
        echo ""

        # Extract tokens
        ACCESS_TOKEN=$(echo "$MFA_RESPONSE" | jq -r '.data.accessToken')
        REFRESH_TOKEN=$(echo "$MFA_RESPONSE" | jq -r '.data.refreshToken')
        ID_TOKEN=$(echo "$MFA_RESPONSE" | jq -r '.data.idToken')
        EXPIRES_IN=$(echo "$MFA_RESPONSE" | jq -r '.data.expiresIn')

        echo -e "${GREEN}Authentication Tokens Received:${NC}"
        echo "Access Token: ${ACCESS_TOKEN:0:50}..."
        echo "Refresh Token: ${REFRESH_TOKEN:0:50}..."
        echo "ID Token: ${ID_TOKEN:0:50}..."
        echo "Expires In: $EXPIRES_IN seconds"
        echo ""
        echo -e "${GREEN}You are now fully authenticated!${NC}"
    else
        ERROR_TYPE=$(echo "$MFA_RESPONSE" | jq -r '.data.errorType')
        ERROR_MSG=$(echo "$MFA_RESPONSE" | jq -r '.message')

        echo ""
        echo -e "${RED}✗ MFA verification failed${NC}"
        echo -e "${RED}Error Type: $ERROR_TYPE${NC}"
        echo -e "${RED}Message: $ERROR_MSG${NC}"
        echo ""

        if [ "$ERROR_TYPE" == "CodeMismatchException" ]; then
            echo -e "${YELLOW}Suggestions:${NC}"
            echo "1. Check that you entered the correct code from your email"
            echo "2. Make sure you're using the latest code (codes expire quickly)"
            echo "3. Check for any extra spaces or characters"
            echo "4. If code expired, run this script again to get a new code"
        fi
    fi

elif [ "$CHALLENGE_NAME" == "null" ]; then
    # No MFA required - user logged in successfully
    echo -e "${GREEN}✓ Login successful without MFA!${NC}"
    echo ""

    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken')
    REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.refreshToken')
    ID_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.idToken')
    EXPIRES_IN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.expiresIn')

    echo -e "${GREEN}Authentication Tokens Received:${NC}"
    echo "Access Token: ${ACCESS_TOKEN:0:50}..."
    echo "Refresh Token: ${REFRESH_TOKEN:0:50}..."
    echo "ID Token: ${ID_TOKEN:0:50}..."
    echo "Expires In: $EXPIRES_IN seconds"
    echo ""
    echo -e "${GREEN}You are now authenticated!${NC}"
else
    echo -e "${YELLOW}⚠ Unknown challenge type: $CHALLENGE_NAME${NC}"
    echo "This script supports EMAIL_OTP. Your challenge type may require different handling."
fi

echo ""
echo -e "${BLUE}=== Test Complete ===${NC}"
