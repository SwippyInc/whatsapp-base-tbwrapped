# WhatsApp Client Integration Library

A multi-tenant WhatsApp Business API client that can be integrated into any Java application. This library wraps the WhatsApp Business API with additional features for customer onboarding, conversation tracking, and message management.

## Features

- **WhatsApp API Client**: Core SDK for interacting with WhatsApp Business API
- **Multi-tenant Support**: Manage multiple business customers with their own WhatsApp accounts
- **Embedded Signup Flow**: Allow your customers to connect their WhatsApp Business accounts
- **Conversation Persistence**: Track and store WhatsApp conversations
- **Webhook Processing**: Handle incoming messages and account events
- **Template Management**: Create and send message templates

## Repository Structure

```
whatsapp-client/
├── api/                     # Core WhatsApp API client (existing code)
├── conversation/            # Conversation management
├── tenant/                  # Multi-tenant customer management
├── webhook/                 # Webhook processing
├── database/                # Database migration scripts
├── examples/                # Integration examples
└── README.md                # Documentation
```

## Integration Steps

### 1. Add the Library to Your Project

Add the WhatsApp client library to your project as a Maven dependency:

```xml
<dependency>
    <groupId>com.whatsapp.api</groupId>
    <artifactId>whatsapp-business-java-api</artifactId>
    <version>v0.6.1</version>
</dependency>
```

### 2. Setup Database

Run the database migration script to create the necessary tables:

```bash
psql -U username -d database_name -f database_migration.sql
```

### 3. Configure Your Application

Add the following configuration to your application:

```properties
# WhatsApp API Configuration
whatsapp.app.id=your_meta_app_id
whatsapp.app.secret=your_meta_app_secret
whatsapp.redirect.uri=https://your-domain.com/whatsapp/callback

# Database Configuration
whatsapp.db.url=jdbc:postgresql://localhost:5432/your_database
whatsapp.db.username=your_db_username
whatsapp.db.password=your_db_password

# Webhook Configuration
whatsapp.webhook.url=https://your-domain.com/whatsapp/webhook
whatsapp.webhook.secret=your_webhook_secret
```

### 4. Implement OAuth Callback Endpoint

Create an endpoint to handle the OAuth callback after customers complete the embedded signup flow:

```java
@RestController
public class WhatsAppCallbackController {
    @Autowired
    private WhatsAppTenantService tenantService;
    
    @GetMapping("/whatsapp/callback")
    public ResponseEntity<String> handleCallback(@RequestParam String code, @RequestParam String state) {
        tenantService.handleOAuthCallback(code, state);
        return ResponseEntity.ok("WhatsApp connected successfully!");
    }
}
```

### 5. Implement Webhook Endpoint

Create an endpoint to receive webhook events from WhatsApp:

```java
@RestController
public class WhatsAppWebhookController {
    @Autowired
    private WhatsAppWebhookService webhookService;
    
    @PostMapping("/whatsapp/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        webhookService.processWebhook(payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
    
    @GetMapping("/whatsapp/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if (webhookService.verifyWebhook(mode, token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
```

### 6. Add the Connect Button to Your UI

Add the "Connect WhatsApp" button to your customer dashboard:

```html
<script>
window.fbAsyncInit = function() {
    FB.init({
        appId: 'YOUR_APP_ID',
        xfbml: true,
        version: 'v21.0'
    });
};

function connectWhatsApp(tenantId) {
    // Call your backend to get the authorization URL
    fetch('/api/tenants/' + tenantId + '/whatsapp/auth-url')
        .then(response => response.json())
        .then(data => {
            window.location.href = data.authUrl;
        });
}
</script>

<button onclick="connectWhatsApp('tenant-123')">Connect WhatsApp</button>
```

### 7. Send Messages

Use the client to send messages to your customers:

```java
@Service
public class WhatsAppMessageService {
    @Autowired
    private WhatsAppClientFactory clientFactory;
    
    public void sendTextMessage(UUID tenantId, String phoneNumber, String message) {
        WhatsappBusinessCloudApi api = clientFactory.getClientForTenant(tenantId);
        
        TextMessage textMessage = new TextMessage.Builder()
            .to(phoneNumber)
            .body(message)
            .build();
            
        api.sendMessage(textMessage);
    }
}
```

## Examples

Check the `examples` directory for complete integration examples.

## Troubleshooting

- **Token Expiration**: Tokens expire periodically. The library automatically refreshes them if possible.
- **Webhook Verification**: Ensure your webhook URL is publicly accessible and has a valid SSL certificate.
- **Template Approval**: Message templates must be approved before use. Check template status in Meta Business Manager.

## License

MIT License