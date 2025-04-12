# WhatsApp Business Embedded Signup Flow - Frontend Integration Plan

## Overview

This document outlines the integration steps and API endpoints for embedding WhatsApp Business API signup into your application as a Technology Provider. This multi-tenant solution allows your application to manage WhatsApp connections for multiple business customers.

## WhatsApp Business Onboarding Flow

### 1. Initialization (Frontend → Your Backend)

**API Call:**
```
POST /api/whatsapp/tenant/connect/{tenantId}?businessName=Business%20Name
```

**Description:**
- Frontend initiates the connection process for a specific tenant
- Backend creates a tenant record with status `DISCONNECTED`
- Backend generates a Facebook OAuth URL with your app's credentials

**Response:**
```json
{
  "success": true,
  "authorizationUrl": "https://www.facebook.com/dialog/oauth?client_id=YOUR_APP_ID&redirect_uri=YOUR_REDIRECT_URI&state=TENANT_ID&scope=whatsapp_business_management,whatsapp_business_messaging",
  "message": "WhatsApp connection initialized. Redirect user to the authorization URL."
}
```

**Frontend Action:**
- Redirect the user to the provided `authorizationUrl` (opens Facebook/Meta's page)

### 2. User Authorization on Facebook (User → Meta Platform)

**Description:**
- This step happens on Facebook/Meta's platform, not in your application
- User logs in to their Facebook account (if not already logged in)
- User selects which WhatsApp Business Account to connect, or creates a new one
- User authorizes your application to access their WhatsApp Business account
- Facebook redirects back to your backend's registered redirect URI with code and state parameters

**Redirect Back to Backend:**
```
GET your-backend-redirect-uri?code=AUTHORIZATION_CODE&state=TENANT_ID
```
*Note: This redirect goes to your backend endpoint, not the frontend. The redirect URI must match exactly what you registered in your Meta app settings.*

### 3. OAuth Callback Processing (Meta → Your Backend)

**API Endpoint (Handled by Backend):**
```
GET /api/whatsapp/tenant/oauth/callback?code=xxx&state=tenantId
```

**Description:**
- Your backend receives the authorization code from Meta
- Backend exchanges the code for access and refresh tokens by calling Meta's API
- Backend stores these tokens securely associated with the tenant
- Tenant status is updated to `VERIFICATION_NEEDED`
  - **Note:** At this point, we have the API tokens but still need additional information (WABA ID, Phone Number ID) and need to complete phone verification before the connection is fully functional

**Response:**
- A success page that the user can close: "WhatsApp connection in progress. You can now close this window."

**Frontend Action:**
- During this process, the user will see a callback success page from your backend
- Your main application should poll the tenant status endpoint to detect when this step completes
- Once status changes to VERIFICATION_NEEDED, proceed to next steps

### 4. Webhook Integration (Meta → Your Backend)

**Webhook Endpoint:**
```
POST /api/webhook
```

**Description:**
- Meta sends webhook events to your registered webhook endpoint
- Backend verifies the webhook signature and processes events
- When a WABA creation or account update event is received, it extracts:
  - WhatsApp Business Account ID (WABA ID)
  - Phone Number ID
- Backend updates the tenant record with this information
- Internally calls the completion endpoint:
  ```
  POST /api/whatsapp/tenant/{tenantId}/complete-onboarding
  ```

**Frontend Action:**
- No direct frontend interaction in this step
- Frontend can poll the status endpoint to detect when webhook events are processed

### 5. Phone Verification (Frontend → Your Backend)

**API Call:**
```
POST /api/whatsapp/tenant/{tenantId}/register-pin
```

**Request Body:**
```json
{
  "pin": "123456"  // 6-digit PIN
}
```

**Description:**
- User must provide a 6-digit PIN for 2-step verification of their WhatsApp Business phone number
- This PIN will be required if they need to reverify their phone number in the future
- Backend registers the phone number with the PIN by calling WhatsApp's API
- Tenant status is updated to `CONNECTED` if successful

**Response:**
```json
{
  "success": true
}
```

**Frontend Action:**
- Collect a 6-digit PIN from the user
- Submit the PIN to the API
- Display success or error message based on response

### 6. Connection Status Checking (Frontend → Your Backend)

**API Call:**
```
GET /api/whatsapp/tenant/{tenantId}/status
```

**Response:**
```json
{
  "tenantId": "uuid-here",
  "status": "CONNECTED",
  "connected": true
}
```

**Frontend Action:**
- Poll this endpoint to check connection status during setup
- Update UI based on current status:
  - `DISCONNECTED`: Not connected, show connect button
  - `CONNECTING`: In progress, show waiting indicator
  - `VERIFICATION_NEEDED`: Need PIN, show PIN entry form
  - `CONNECTED`: Fully connected, show success and messaging UI
  - `ERROR`: Connection failed, show error and retry option

## Sending Messages

Once a tenant is connected, messages can be sent using these endpoints:

### Text Messages

**API Call:**
```
POST /api/whatsapp/message/{tenantId}/text
```

**Request Body:**
```json
{
  "recipientPhone": "+1234567890",
  "text": "Hello! This is a text message."
}
```

### Template Messages

**API Call:**
```
POST /api/whatsapp/message/{tenantId}/template
```

**Request Body:**
```json
{
  "to": "+1234567890",
  "template": {
    "name": "template_name",
    "language": {
      "code": "en_US"
    },
    "components": [
      {
        "type": "body",
        "parameters": [
          {
            "type": "text",
            "text": "parameter1"
          }
        ]
      }
    ]
  }
}
```

### Media Messages

**API Call:**
```
POST /api/whatsapp/message/{tenantId}/media/{mediaType}
```

Where `mediaType` is one of: `image`, `document`, `audio`, `video`

**Request Body (for image):**
```json
{
  "to": "+1234567890",
  "image": {
    "link": "https://example.com/image.jpg",
    "caption": "Optional caption"
  }
}
```

## Connection States Explained

1. **DISCONNECTED**
   - Initial state
   - No WhatsApp connection exists for this tenant
   - No valid tokens are available

2. **CONNECTING**
   - OAuth flow has started
   - Waiting for user to complete authorization on Facebook
   - Temporary state during the initial connection process

3. **VERIFICATION_NEEDED**
   - OAuth tokens have been obtained
   - We have long-lived access and refresh tokens at this point
   - Waiting for webhook events to provide WABA ID and Phone Number ID
   - Waiting for 2-step verification PIN to register the phone number
   - This state indicates that additional steps are needed to complete the connection

4. **CONNECTED**
   - Fully connected and operational
   - All required IDs are present
   - Phone number has been verified
   - Ready to send and receive messages

5. **ERROR**
   - Connection failed due to an error
   - Check logs for detailed error information
   - User may need to restart the connection process

## Integration Sequence Diagram

```
Frontend                  Your Backend                 Facebook/Meta
   |                           |                            |
   |---Connect Tenant--------->|                            |
   |<--Authorization URL-------|                            |
   |                           |                            |
   |---Redirect to Facebook-------------------------->|
   |                           |                      User authorizes
   |                           |<--Redirect with code-----|
   |                           |                            |
   |                           |---Exchange code for token->|
   |                           |<--Tokens------------------|
   |                           |                            |
   |                           |<--Webhook events----------|
   |                           |   (WABA & Phone Number)   |
   |                           |                            |
   |---Check Status----------->|                            |
   |<--Status (need PIN)-------|                            |
   |                           |                            |
   |---Submit PIN------------->|                            |
   |                           |---Register phone---------->|
   |                           |<--Registration complete----|
   |                           |                            |
   |---Check Status----------->|                            |
   |<--Status (connected)------|                            |
   |                           |                            |
   |---Send Messages---------->|                            |
   |                           |---WhatsApp API calls------>|
   |<--Message responses-------|<--API responses------------|
   |                           |                            |
   |                           |<--Incoming messages--------|
   |                           |   (via webhook)            |
```