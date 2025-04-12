# Implementation Plan for WhatsApp Integration

## Current Understanding

We have the original WhatsApp Business Java API in the `src/main` directory, which we need to:
1. Copy into the `whatsapp-client` subdirectory
2. Extend with multi-tenant support, conversation tracking, and embedded signup flow
3. Make easy to integrate into other Spring Boot applications

## Directory Structure

We'll organize the WhatsApp client integration as follows:

```
whatsapp-client/
├── pom.xml                                # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── lodgio/
│   │   │           └── whatsapp/
│   │   │               ├── api/          # Core WhatsApp API wrapper
│   │   │               ├── conversation/ # Conversation management
│   │   │               ├── tenant/       # Multi-tenant management
│   │   │               ├── webhook/      # Webhook handling
│   │   │               └── config/       # Configuration
│   │   └── resources/
│   │       └── db/
│   │           └── migration/           # Database migration scripts
│   └── test/
│       └── java/
│           └── com/
│               └── lodgio/
│                   └── whatsapp/         # Tests
```

## Implementation Steps

### 1. Project Setup
- [ ] Create Maven project structure
- [ ] Create pom.xml with dependencies
- [ ] Copy core WhatsApp API client

### 2. Database Schema
- [ ] Create migration script for PostgreSQL
- [ ] Define JPA entities for:
  - [ ] WhatsAppTenant
  - [ ] Conversation
  - [ ] Message
  - [ ] WebhookEvent

### 3. Multi-tenant Management
- [ ] WhatsAppTenant entity and repository
- [ ] WhatsAppTenantService for managing connections
- [ ] OAuth flow implementation
- [ ] Token management

### 4. Conversation Management
- [ ] Conversation and Message entities and repositories
- [ ] ConversationService for tracking conversations
- [ ] MessageService for sending/receiving messages

### 5. Webhook Processing
- [ ] WebhookController for receiving events
- [ ] WebhookService for processing events
- [ ] Event handlers for different event types

### 6. Integration Layer
- [ ] API for sending messages
- [ ] Webhook registration
- [ ] Status checking
- [ ] Example integration

## Dependencies

We'll use the following major dependencies:
- Spring Boot (Web, JPA, Security)
- Lombok for reducing boilerplate
- PostgreSQL for database
- Retrofit/OkHttp (from the original client)
- Jackson for JSON processing

## Configuration

We'll need the following configuration properties:
- Meta App ID and Secret
- OAuth redirect URI
- Webhook URL and verification token
- Database connection details

## Integration Approach

To integrate this into other Spring Boot applications:
1. Copy the `whatsapp-client` directory into the target project
2. Run the database migration scripts
3. Configure the application properties
4. Use the provided services for managing WhatsApp interactions