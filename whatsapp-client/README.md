# WhatsApp Client Multi-tenant Integration

A Java client for the WhatsApp Business API with multi-tenant support, conversation tracking, and embedded signup flow.

## Features

- **Multi-tenant Architecture**: Connect and manage multiple WhatsApp Business accounts
- **Conversation Tracking**: Persistent tracking of WhatsApp conversations and messages
- **Embedded Signup Flow**: Integrated WhatsApp Business Platform signup as Technology Provider
- **Message Templates**: Support for creating and sending template messages
- **Media Support**: Send and receive images, videos, documents, and other media
- **Webhook Processing**: Handle incoming messages and status updates

## Installation

### 1. Copy the WhatsApp Client

Copy the `whatsapp-client` directory into your Spring Boot project.

### 2. Database Migration

Run the database migration script to create the necessary tables:

```bash
psql -U username -d database_name -f whatsapp-client/src/main/resources/db/migration/V1__whatsapp_integration_schema.sql
```

Alternatively, you can use Spring's Flyway integration by copying the migration script to your project's `src/main/resources/db/migration` directory.

### 3. Configure Your Application

Add the following properties to your `application.properties` or `application.yml`:

```properties
# WhatsApp API Configuration
whatsapp.app-id=your_meta_app_id
whatsapp.app-secret=your_meta_app_secret
whatsapp.redirect-uri=https://your-domain.com/api/whatsapp/callback
whatsapp.webhook-url=https://your-domain.com/api/webhook
whatsapp.webhook-verify-token=your_webhook_secret_token
```

### 4. Register Required Beans

Create a configuration class to register the required beans with qualifiers to avoid conflicts:

```java
@Configuration
public class WhatsAppConfig {
    
    @Bean("whatsAppRestTemplate")
    public RestTemplate whatsAppRestTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Bean("whatsAppObjectMapper")
    public ObjectMapper whatsAppObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
```

Then update the service implementations to use the qualified beans:

```java
@Service
public class YourWhatsAppService {
    
    @Autowired
    @Qualifier("whatsAppRestTemplate")
    private RestTemplate restTemplate;
    
    @Autowired
    @Qualifier("whatsAppObjectMapper")
    private ObjectMapper objectMapper;
    
    // Service implementation
}
```

### 5. Import Components in Your Spring Boot Application

Add component scanning for the WhatsApp client package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourcompany.yourapp", "com.whatsapp.client"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## Usage

### Initializing WhatsApp Connection

```java
@Service
public class YourService {

    private final WhatsAppTenantService whatsAppTenantService;
    
    @Autowired
    public YourService(WhatsAppTenantService whatsAppTenantService) {
        this.whatsAppTenantService = whatsAppTenantService;
    }
    
    public String connectWhatsApp(UUID tenantId, String businessName) {
        // This returns the OAuth URL that the customer needs to visit
        return whatsAppTenantService.initializeConnection(tenantId, businessName);
    }
}
```

### Handling OAuth Callback

```java
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppCallbackController {

    private final WhatsAppTenantService whatsAppTenantService;
    
    @Autowired
    public WhatsAppCallbackController(WhatsAppTenantService whatsAppTenantService) {
        this.whatsAppTenantService = whatsAppTenantService;
    }
    
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        
        whatsAppTenantService.handleOAuthCallback(code, state);
        return ResponseEntity.ok("WhatsApp connected successfully!");
    }
}
```

### Sending Messages

```java
@Service
public class YourService {

    private final ConversationService conversationService;
    
    @Autowired
    public YourService(ConversationService conversationService) {
        this.conversationService = conversationService;
    }
    
    public void sendMessage(UUID tenantId, String phoneNumber, String message) {
        conversationService.sendTextMessage(tenantId, phoneNumber, message);
    }
}
```

### Checking Connection Status

```java
@Service
public class YourService {

    private final WhatsAppTenantService whatsAppTenantService;
    
    @Autowired
    public YourService(WhatsAppTenantService whatsAppTenantService) {
        this.whatsAppTenantService = whatsAppTenantService;
    }
    
    public boolean isWhatsAppConnected(UUID tenantId) {
        return whatsAppTenantService.isConnected(tenantId);
    }
}
```

## Meta Business Manager Setup

Before using this integration, you need to set up the following in Meta Business Manager:

1. Create a Meta App
2. Configure WhatsApp Business as a product
3. Set up Webhook URL
4. Configure OAuth redirect URLs
5. Create a Facebook Login for Business configuration

Refer to Meta's documentation for details on setting up as a Technology Provider.

## License

MIT License - See LICENSE file for details.

## Acknowledgements

This client is built on top of the [WhatsApp Business Java SDK](https://github.com/Bindambc/whatsapp-business-java-sdk).