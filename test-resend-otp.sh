#!/bin/bash

# Test script for Resend OTP endpoint
# Usage: ./test-resend-otp.sh <email> <app-key>

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if required arguments are provided
if [ "$#" -lt 2 ]; then
    echo -e "${RED}Error: Missing required arguments${NC}"
    echo "Usage: $0 <email> <app-key>"
    echo "Example: $0 user@example.com your-app-key-here"
    exit 1
fi

EMAIL=$1
APP_KEY=$2
API_URL="${3:-http://localhost:8080}"

echo -e "${YELLOW}=== Testing Resend OTP Endpoint ===${NC}"
echo "Email: $EMAIL"
echo "API URL: $API_URL"
echo ""

# Test 1: Resend OTP
echo -e "${YELLOW}Test 1: Requesting new verification code...${NC}"
RESPONSE=$(curl -s -X POST "$API_URL/api/auth/resend-otp" \
  -H "Content-Type: application/json" \
  -H "X-APP-KEY: $APP_KEY" \
  -d "{
    \"email\": \"$EMAIL\"
  }")

echo "$RESPONSE" | jq .

# Check if request was successful
SUCCESS=$(echo "$RESPONSE" | jq -r '.success')
if [ "$SUCCESS" == "true" ]; then
    echo -e "${GREEN}✓ OTP resent successfully!${NC}"
    echo ""
    echo -e "${GREEN}Please check your email for the new verification code.${NC}"
    echo ""

    # Prompt for verification
    echo -e "${YELLOW}Would you like to test the verification endpoint now? (y/n)${NC}"
    read -r TEST_VERIFY

    if [ "$TEST_VERIFY" == "y" ] || [ "$TEST_VERIFY" == "Y" ]; then
        echo "Enter the verification code from your email:"
        read -r CODE

        echo -e "${YELLOW}Test 2: Verifying with code: $CODE${NC}"
        VERIFY_RESPONSE=$(curl -s -X POST "$API_URL/api/auth/verify" \
          -H "Content-Type: application/json" \
          -H "X-APP-KEY: $APP_KEY" \
          -d "{
            \"email\": \"$EMAIL\",
            \"confirmationCode\": \"$CODE\"
          }")

        echo "$VERIFY_RESPONSE" | jq .

        VERIFY_SUCCESS=$(echo "$VERIFY_RESPONSE" | jq -r '.success')
        if [ "$VERIFY_SUCCESS" == "true" ]; then
            echo -e "${GREEN}✓ User verified successfully!${NC}"
            echo -e "${GREEN}You can now log in with your credentials.${NC}"
        else
            ERROR_TYPE=$(echo "$VERIFY_RESPONSE" | jq -r '.data.errorType')
            ERROR_MSG=$(echo "$VERIFY_RESPONSE" | jq -r '.message')
            echo -e "${RED}✗ Verification failed${NC}"
            echo -e "${RED}Error Type: $ERROR_TYPE${NC}"
            echo -e "${RED}Message: $ERROR_MSG${NC}"

            if [ "$ERROR_TYPE" == "ExpiredCodeException" ]; then
                echo -e "${YELLOW}The code has expired. Run this script again to get a new code.${NC}"
            elif [ "$ERROR_TYPE" == "CodeMismatchException" ]; then
                echo -e "${YELLOW}The code is incorrect. Please check your email and try again.${NC}"
            fi
        fi
    fi
else
    ERROR_TYPE=$(echo "$RESPONSE" | jq -r '.data.errorType')
    ERROR_MSG=$(echo "$RESPONSE" | jq -r '.message')
    ACTION=$(echo "$RESPONSE" | jq -r '.data.action')

    echo -e "${RED}✗ Failed to resend OTP${NC}"
    echo -e "${RED}Error Type: $ERROR_TYPE${NC}"
    echo -e "${RED}Message: $ERROR_MSG${NC}"

    if [ "$ACTION" != "null" ]; then
        echo -e "${YELLOW}Suggested Action: $ACTION${NC}"
    fi

    if [ "$ERROR_TYPE" == "NotAuthorizedException" ]; then
        echo ""
        echo -e "${GREEN}Good news! Your account is already verified.${NC}"
        echo -e "${GREEN}You can log in directly using the /api/auth/login endpoint.${NC}"
    fi
fi

echo ""
echo -e "${YELLOW}=== Test Complete ===${NC}"
