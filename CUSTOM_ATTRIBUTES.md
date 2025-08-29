# Custom User Attributes in AWS Cognito Middleware

This document provides guidance on how to use custom attributes with the AWS Cognito Middleware.

## Overview

The middleware now supports custom user attributes during registration. These attributes can vary per application and are passed to AWS Cognito as part of the user creation process.

## Example Request

Here's an example of how to register a user with custom attributes:

```http
POST /api/auth/register
X-APP-KEY: 123e4567-e89b-12d3-a456-426614174000
Content-Type: application/json

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
    "custom:verification_status": "verified"
  }
}
```

## Supported Attributes

The middleware supports all standard AWS Cognito attributes as well as custom attributes. Here are some common attributes:

### Standard Attributes
- `name`: User's full name
- `given_name`: User's first name
- `family_name`: User's last name
- `middle_name`: User's middle name
- `nickname`: User's nickname
- `preferred_username`: User's preferred username
- `profile`: URL to user's profile page
- `picture`: URL to user's profile picture
- `website`: URL to user's website
- `email`: User's email address (required)
- `gender`: User's gender
- `birthdate`: User's birthdate
- `zoneinfo`: User's timezone
- `locale`: User's locale
- `phone_number`: User's phone number
- `address`: User's address
- `updated_at`: Time the user's information was last updated

### Custom Attributes
Custom attributes must be prefixed with `custom:` and can include any data you need. For example:
- `custom:phone_number`: Alternative format for phone number
- `custom:National_ID`: National ID number
- `custom:passport_number`: Passport number
- `custom:dob`: Date of birth
- `custom:nationality`: Nationality
- `custom:country_of_origin`: Country of origin
- `custom:verification_status`: Verification status

## Important Notes

1. **Attribute Limits**: AWS Cognito has limits on the number and size of custom attributes. Check the [AWS documentation](https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-settings-attributes.html) for current limits.

2. **Attribute Naming**: Custom attributes must be prefixed with `custom:` and can only contain alphanumeric characters and underscores.

3. **Attribute Values**: All attribute values are stored as strings, even if they represent numbers or dates.

4. **Required Attributes**: The only required attributes are `email` and `password`. All other attributes are optional.

5. **Application-Specific Attributes**: Different applications may require different sets of attributes. The middleware is designed to be flexible and handle any set of attributes.

## Example Use Cases

### Basic User Registration
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "attributes": {
    "name": "John Doe"
  }
}
```

### Financial Application
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "attributes": {
    "name": "John Doe",
    "custom:tax_id": "TAX12345",
    "custom:account_type": "premium",
    "custom:risk_profile": "moderate"
  }
}
```

### Healthcare Application
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "attributes": {
    "name": "John Doe",
    "custom:medical_id": "MED12345",
    "custom:insurance_provider": "HealthCare Inc",
    "custom:policy_number": "POL12345"
  }
}
```

### Government Application
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "attributes": {
    "name": "John Doe",
    "custom:National_ID": "ID12345",
    "custom:passport_number": "PASS12345",
    "custom:dob": "1990-01-01",
    "custom:nationality": "USA",
    "custom:verification_status": "verified"
  }
}
```