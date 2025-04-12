Okay, I will organize the provided content into Frontend and Backend implementation sections, ensuring all information is retained and categorized 
appropriately.

**WhatsApp Business Platform - Implementation Documentation**

**1. Implementation - WhatsApp Business Platform**

**Overall Notes:** This document explains how to implement Embedded Signup and capture the data it generates to onboard business customers onto the 
WhatsApp Business Platform.

**Before you start:**

  * You must already be a Solution Partner or Tech Provider.
  * If your business customers will be using your app to send and receive messages, you should already know how to use the API to send and receive 
messages, create and manage templates, and have a webhooks callback endpoint properly set up.
  * You must be subscribed to the **account\_update** webhook.
  * If you are a Solution Partner, you must already have a line of credit.
  * The server where you will be hosting Embedded Signup must have a valid SSL certificate.

**Frontend Implementation**

**Step 1: Add allowed domains**

  * Load your app in the App Dashboard and navigate to **Facebook Login for Business** \> **Settings** \> **Client OAuth settings**.
  * Set the following toggles to **Yes**:
      * **Client OAuth login**
      * **Web OAuth login**
      * **Enforce HTTPS**
      * **Embedded Browser OAuth Login**
      * **use Strict Mode for redirect URIs**
      * **Login with the JavaScript SDK**
  * Add any domains where you plan on hosting Embedded Signup, including any development domains where you will be testing the flow, to the **Allowed 
domains** and **Valid OAuth redirect URIs** fields. Only domains that have enabled **https** are supported.

**Step 3: Add Embedded Signup to your website**

  * Add the HTML and JavaScript code to your website (code not provided in the original document, but instructions on its use are)
      * SDK loading
      * SDK initialization
      * Session logging message event listener
      * Response callback
      * Launch method and callback registration
      * Launch button
  * Testing

**Backend Implementation**

**Step 2: Create a Facebook Login for Business configuration**

  * A Facebook Login for Business configuration defines which permissions to request, and what additional information to collect, from business 
customers who access Embedded Signup.
  * Navigate to **Facebook Login for Business** \> **Configurations** and click the **+ Create configuration** button to access the configuration flow.
  * Use a name that will help you differentiate this configuration from any others you may create in the future. When completing the flow, be sure to 
select the **WhatsApp Embedded Signup** login variation.
  * When choosing assets and permissions, select only those assets and permissions that you will actually need from your business customers.
  * When you complete the configuration flow, capture your configuration ID, as you will need it in the next step.
  * Onboarding business customers

**2. Onboarding for Tech Providers - WhatsApp Business Platform**

**Overall Notes:** This document describes the steps Tech Providers and Tech Partners must perform to onboard new business customers who have completed 
the Embedded Signup flow.

**What you will need:**

  * the business customer's WABA ID
  * the business customer's business phone number ID
  * your app ID
  * your app secret
  * (Optional) A WhatsApp phone number that can already send and receive messages from other WhatsApp numbers.
  * Perform all of the requests described below using server-to-server requests. Do not use client-side requests.

**Backend Implementation**

**Step 1: Exchange the token code for a business token**

  * Use the **GET /oauth/access\_token** endpoint to exchange the token code returned by Embedded Signup for a business integration system user access 
token ("business token").

      * **Request:**

    <!-- end list -->

    ```
    curl --get 'https://graph.facebook.com/v21.0/oauth/access\_token' \
    -d 'client\_id=<APP\_ID>' \
    -d 'client\_secret=<APP\_SECRET>' \
    -d 'code=<CODE>'
    ```

      * **Request parameters:**

          * `<APP_ID>` (Required): Your app ID.
          * `<APP_SECRET>` (Required): Your app secret.
          * `<CODE>` (Required): The code returned by Embedded Signup.

      * **Response:**

          * `<BUSINESS_TOKEN>`

      * **Response parameters:**

          * `<BUSINESS_TOKEN>`: The customer's business token.

**Step 2: Subscribe to webhooks on the customer's WABA**

  * Use the **POST /\<WABA\_ID\>/subscribed\_apps** endpoint to subscribe your app to webhooks on the business customer's WABA.

      * **Request:**

    <!-- end list -->

    ```
    curl -X POST 'https://graph.facebook.com/<API_VERSION>/<WABA_ID>/subscribed_apps' \
    -H 'Authorization: Bearer <BUSINESS_TOKEN>'
    ```

      * **Request parameters:**

          * `<API_VERSION>` (Optional): Graph API version.
          * `<BUSINESS_TOKEN>` (Required): The customer's business token.
          * `<WABA_ID>` (Required): WhatsApp Business Account ID.

      * **Response:**

    <!-- end list -->

    ```json
    { "success": true }
    ```

**Step 3: Register the customer's phone number**

  * Use the **POST /\<BUSINESS\_PHONE\_NUMBER\_ID\>/register** endpoint to register the business customer's business phone number for use with Cloud 
API.

      * **Request:**

    <!-- end list -->

    ```
    curl 'https://graph.facebook.com/v21.0/<BUSINESS_CUSTOMER_PHONE_NUMBER_ID>/register' \
    -H 'Content-Type: application/json' \
    -H 'Authorization: Bearer <BUSINESS_TOKEN>' \
    -d '{ "messaging_product": "whatsapp", "pin": "<DESIRED_PIN>" }'
    ```

      * **Request parameters:**

          * `<BUSINESS_CUSTOMER_PHONE_NUMBER_ID>` (Required): The business customer's business phone number ID.
          * `<BUSINESS_TOKEN>` (Required): The business customer's business token.
          * `<DESIRED_PIN>` (Required): A 6-digit number for the business phone number's two-step verification PIN.

      * **Response:**

    <!-- end list -->

    ```json
    { "success": true }
    ```

**Step 4: Send a test message** (Optional)

  * If you wish to test the messaging capabilities, send a message to the customer's number from your own WhatsApp number.

  * Use the **POST /\<BUSINESS\_PHONE\_NUMBER\_ID\>/messages** endpoint to send a text message in response.

      * **Request:**

    <!-- end list -->

    ```
    curl 'https://graph.facebook.com/v21.0/<BUSINESS_CUSTOMER_PHONE_NUMBER_ID>/messages' \
    -H 'Content-Type: application/json' \
    -H 'Authorization: Bearer <BUSINESS_TOKEN>' \
    -d '{ "messaging_product": "whatsapp", "recipient_type": "individual", "to": "<WHATSAPP_USER_NUMBER>", "type": "text", "text": { "body": 
"<BODY_TEXT>" } }'
    ```

      * **Request parameters:**

          * `<BODY_TEXT>` (Required): Message body text (max 4096 characters).
          * `<BUSINESS_CUSTOMER_PHONE_NUMBER_ID>` (Required): The business customer's business phone number ID.
          * `<BUSINESS_TOKEN>` (Required): The business customer's business token.
          * `<WHATSAPP_USER_NUMBER>` (Required): Your WhatsApp phone number that can send and receive messages.

      * **Response:**

    <!-- end list -->

    ```json
    {
      "messaging_product": "whatsapp",
      "contacts": [
        {
          "input": "<WHATSAPP_USER_NUMBER>",
          "wa_id": "<WHATSAPP_USER_ID>"
        }
      ],
      "messages": [
        {
          "id": "<WHATSAPP_MESSAGE_ID>"
        }
      ]
    }
    ```

      * **Response parameters:**

          * `<WHATSAPP_MESSAGE_ID>`: WhatsApp message ID.
          * `<WHATSAPP_USER_ID>`: Your WhatsApp user ID.
          * `<WHATSAPP_USER_NUMBER>`: Your WhatsApp phone number that the message was sent to.

**Step 5: Instruct the customer to add a payment method**

  * Instruct your customer to use the WhatsApp Manager to add a payment method.
  * Provide them with the following Help Center link: `https://www.facebook.com/business/help/488291839463771`
  * Alternatively, you can instruct them to:
    1.  Access the **WhatsApp Manager** \> **Overview** panel at `https://business.facebook.com/wa/manage/home/`
    2.  Click the **Add payment method** button
    3.  Complete the flow

**3. Default flow - WhatsApp Business Platform**

**Overall Notes:** This document describes the default screens that your business customers will be presented with as they navigate the Embedded Signup 
flow.

**Frontend (UI) - Screens**

  * **Authentication screen:** Authenticates business customers using their Facebook or Meta Business Suite credentials.
  * **Authorization screen:** Describes the data the business customer will be permitting your app to access.
  * **Business portfolio screen:** Gathers information about your business customer's business and displays existing business portfolios.
  * **WABA selection screen:** Displays existing WhatsApp Business Accounts (WABA) and allows for creating a new WABA.
  * **WABA creation screen:** Allows the business customer to set a name, category, and description for their public profile.
  * **Phone number addition screen:** Allows the business customer to enter a new business phone number and choose how to receive their verification 
code.
  * **Phone number verification screen:** Allows the business customer to verify ownership of the business phone number.
  * **Permissions review screen:** Provides a summary of the permissions the business customer will be granting to your app.
  * **Success screen:** Indicates that all assets were successfully created and associated. Triggers a message event containing the customer's WABA ID 
and business phone number ID.

**4. Webhooks - WhatsApp Business Platform**

**Overall Notes:** WhatsApp Business Accounts (WABAs) and their assets are objects in the Facebook Social Graph. When a trigger event occurs to one of 
those objects, Facebook sends a notification to the webhook URL specified in your Facebook App's dashboard.

**Backend Implementation**

**Managing Webhooks**

  * You need to individually subscribe to every WABA for which you wish to receive Webhooks.
  * See [Webhooks for WhatsApp Business Accounts](https://developers.facebook.com/docs/graph-api/webhooks/getting-started/webhooks-for-whatsapp) for 
more information.

**Subscribing to webhooks on a business customer's WABA**

  * Use the **POST /\<WABA\_ID\>/subscribed\_apps** endpoint to subscribe your app to webhooks on the business customer's WABA.

      * **Request:**

    <!-- end list -->

    ```
    curl -X POST 'https://graph.facebook.com/<API_VERSION>/<WABA_ID>/subscribed_apps' \
    -H 'Authorization: Bearer <BUSINESS_TOKEN>'
    ```

      * **Response:**

    <!-- end list -->

    ```json
    { "success": true }
    ```

**Get all subscriptions on a WABA**

  * To get a list of apps subscribed to webhooks for a WABA, send a GET request to the `subscribed_apps` endpoint on the WABA:

      * **Request Syntax:**

    <!-- end list -->

    ```
    GET https://graph.facebook.com/<API_VERSION>/<WABA_ID>/subscribed_apps
    ```

      * **Sample Request:**

    <!-- end list -->

    ```
    curl \
    'https://graph.facebook.com/`v22.0`/102289599326934/subscribed\_apps' \
    -H 'Authorization: Bearer EAAJi...'
    ```

      * **Sample Response:**

    <!-- end list -->

    ```json
    {
      "data" : [
        {
          "whatsapp_business_api_data" : {
            "id" : "67084...",
            "link" : "https://www.facebook.com/games/?app\_id=67084...",
            "name" : "Jaspers Market"
          }
        },
        {
          "whatsapp_business_api_data" : {
            "id" : "52565...",
            "link" : "https://www.facebook.com/games/?app\_id=52565...",
            "name" : "Jaspers Fresh Finds"
          }
        }
      ]
    }
    ```

**Unsubscribe from a WABA**

  * To unsubscribe your app from webhooks for a WhatsApp Business Account, send a DELETE request to the `subscribed_apps` endpoint on the WABA.

      * **Request Syntax:**

    <!-- end list -->

    ```
    DELETE https://graph.facebook.com/<API_VERSION>/<WABA_ID>/subscribed_apps
    ```

      * **Sample Request:**

    <!-- end list -->

    ```
    curl -X DELETE \
    'https://graph.facebook.com/`v22.0`/102289599326934/subscribed\_apps' \
    -H 'Authorization: Bearer EAAJi...'
    ```

      * **Sample Response:**

    <!-- end list -->

    ```json
    { "success" : true }
    ```

**Overriding the callback URL**

  * See [Webhooks Overrides](https://developers.facebook.com/docs/whatsapp/embedded-signup/webhooks/override).

**Set up notifications**

  * You can set up webhooks to send you notifications of changes to your subscribed WhatsApp Business Accounts.

      * **Available Subscription Fields:**

          * `account_alerts`
          * `account_review_update`
          * `account_update`
          * `business_capability_update`
          * `message_template_components_update`
          * `message_template_quality_update`
          * `message_template_status_update`
          * `messages`
          * `phone_number_name_update`
          * `phone_number_quality_update`
          * `security`
          * `template_category_update`

  * Visit the [WhatsApp Business Account Webhooks 
Reference](https://developers.facebook.com/docs/graph-api/webhooks/reference/whatsapp-business-account/) and the [WhatsApp Cloud API Webhooks 
Reference](https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components) for more information.

  * See the [Webhooks for WhatsApp Business Accounts](https://developers.facebook.com/docs/graph-api/webhooks/getting-started/webhooks-for-whatsapp) 
documentation for more information.

**Webhooks format**

  * You get notifications in the following general format:

    ```json
    {
      "object": "whatsapp\_business\_account",
      "entry": [
        {
          // entry object, containing changes
          "changes": [
            {
              // changes object, containing value
              "value": {
                // value object
              }
            }
          ]
        }
      ]
    }
    ```

  * See more details about each field:

      * 
[`object`](https://www.google.com/search?q=%5Bhttps://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components%5D\(https://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components\))
      * 
[`entry`](https://www.google.com/search?q=%5Bhttps://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components%23entry-object%5D\(https://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components%23entry-object\))
      * 
[`changes`](https://www.google.com/search?q=%5Bhttps://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components%23changes-object%5D\(https://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components%23changes-object\))
      * 
[`value`](https://www.google.com/search?q=%5Bhttps://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components%23value-object%5D\(https://developers.facebook.com/docs/whatsapp/business-management-api/webhooks/components%23value-object\))

**Examples**

  * **Onboarded business customer:** `account_update` webhook with `event` set to `PARTNER_ADDED`.

      * **Syntax:**

    <!-- end list -->

    ```json
    {
      "entry": [
        {
          "id": "<BUSINESS_PORTFOLIO_ID>",
          "time": <WEBHOOK_SENT_TIMESTAMP>,
          "changes": [
            {
              "value": {
                "event": "<EVENT>",
                "waba_info": {
                  "waba_id": "<CUSTOMER_WABA_ID>",
                  "owner_business_id": "<CUSTOMER_BUSINESS_PORTFOLIO_ID>"
                }
              },
              "field": "account_update"
            }
          ]
        }
      ],
      "object": "whatsapp_business_account"
    }
    ```

      * **Example:**

    <!-- end list -->

    ```json
    {
      "entry": [
        {
          "id": "35602282435505",
          "time": 1731617831,
          "changes": [
            {
              "value": {
                "event": "PARTNER_ADDED",
                "waba_info": {
                  "waba_id": "495709166956424",
                  "owner_business_id": "942647313864044"
                }
              },
              "field": "account_update"
            }
          ]
        }
      ],
      "object": "whatsapp_business_account"
    }
    ```

  * **Phone Number Updates**

      * **Name Update Received**

    <!-- end list -->

    ```json
    {
      "object": "whatsapp_business_account",
      "entry": [
        {
          "id": "WHATSAPP-BUSINESS-ACCOUNT-ID",
          "time": TIMESTAMP,
          "changes": [
            {
              "field": "phone_number_name_update",
              "value": {
                "display_phone_number": "PHONE_NUMBER",
                "decision": "APPROVED",
                "requested_verified_name": "WhatsApp",
                "rejection_reason": null
              }
            }
          ]
        }
      ]
    }
    ```

      * **Quality Update Received**

    <!-- end list -->

    ```json
    {
      "object": "whatsapp_business_account",
      "entry": [
        {
          "id": "WHATSAPP-BUSINESS-ACCOUNT-ID",
          "time": TIMESTAMP,
          "changes": [
            {
              "field": "phone_number_quality_update",
              "value": {
                "display_phone_number": "PHONE_NUMBER",
                "event": "FLAGGED",
                "current_limit": "TIER_10K"
              }
            }
          ]
        }
      ]
    }
    ```

  * **WABA Updates**

      * **Sandbox number upgraded to Verified Account**

    <!-- end list -->

    ```json
    {
      "object": "whatsapp_business_account",
      "entry": [
        {
          "id": "WHATSAPP-BUSINESS-ACCOUNT-ID",
          "time": TIMESTAMP,
          "changes": [
            {
              "field": "account_update",
              "value": {
                "phone_number": "PHONE_NUMBER",
                "event": "VERIFIED_ACCOUNT"
              }
            }
          ]
        }
      ]
    }
    ```

      * **WhatsApp Business Account Banned**

    <!-- end list -->

    ```json
    {
      "object": "whatsapp_business_account",
      "entry": [
        {
          "id": "WHATSAPP-BUSINESS-ACCOUNT-ID",
          "time": TIMESTAMP,
          "changes": [
            {
              "field": "account_update",
              "value": {
                "event": "DISABLED_UPDATE" "ban_info": {
                  "waba_ban_state": ["SCHEDULE_FOR_DISABLE", "DISABLE", "REINSTATE"],
                  "waba_ban_date": "DATE"
                }
              }
            }
          ]
        }
      ]
    }
    ```

      * **WhatsApp Business Account Review Completed**

    <!-- end list -->

    ```json
    {
      "object": "whatsapp_business_account",
      "entry": [
        {
          "id": "WHATSAPP-BUSINESS-ACCOUNT-ID",
          "time": TIMESTAMP,
          "changes": [
            {
              "field": "account_review_update",
              "value": {
                "decision": "APPROVED"
              }
            }
          ]
        }
      ]
    }
    ```

  * **Message Template Updates**

      * **Approved**

    <!-- end list -->

    ```json
    {
      "entry": [
        {
          "id": "<WHATSAPP_BUSINESS_ACCOUNT_ID>",
          "time": <TIMESTAMP>,
          "changes": [
            {
              "value": {
                "event": "APPROVED",
                "message_template_id": <TEMPLATE_ID>,
                "message_template_name": "<TEMPLATE_NAME>",
                "message_template_language": "<LANGUAGE_AND_LOCALE_CODE>",
                "reason": "NONE"
              },
              "field": "message_template_status_update"
            }
          ]
        }
      ],
      "object": "whatsapp_business_account"
    }
    ```
