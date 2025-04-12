# WhatsApp Integration Project Plan

## Project Overview
Transform the current WhatsApp Business Java API into a reusable, multi-tenant wrapper solution that can be integrated into multiple repositories (PMS, CRM, etc.). The solution will enable business customers to connect their WhatsApp Business accounts through an embedded signup flow, manage conversations with persistence, and track customer communications.

## Architecture Changes

### 1. Repository Restructuring
- Move existing API client into a `whatsapp-client` subdirectory
- Create a modular structure for easy integration into other projects
- Setup proper dependency management for the client module

### 2. Conversation Management
- Design PostgreSQL database schema for conversation persistence:
  - Conversations table with tenant ID, customer info, status
  - Messages table with metadata (sender, timestamp, status)
  - Participants tracking
- Create migration scripts for easy database setup

### 3. Multi-tenant Customer Management
- Design PostgreSQL schema for multi-tenant WhatsApp integration:
  - Tenant table for business customers' information
  - WABA (WhatsApp Business Account) tracking
  - OAuth token storage (encrypted)
  - Phone number registration status
- Implement WhatsApp embedded signup as a Technology Provider:
  - Frontend components for initiating signup
  - OAuth authorization flow implementation
  - Token exchange and management
  - Webhook handling for signup events

### 4. Integration Layer
- Simplified client interface for plugging into other applications
- Connection status indicators for customer dashboards
- Webhook processing for incoming messages
- Message sending interface for application actions

## Technical Implementation Details

### Embedded Signup Flow Implementation
1. **Frontend Components**:
   - Add Facebook SDK integration code
   - Create "Connect WhatsApp" button
   - Implement OAuth callback handling

2. **Backend Services**:
   - Exchange token code for business token
   - Subscribe to webhooks on customer's WABA
   - Register customer's phone number
   - Store token and WABA information securely

3. **Webhook Management**:
   - Implement webhook receiver endpoint
   - Process `account_update` events for onboarding status
   - Handle message events for conversation tracking
   - Store conversation history

### Database Schema (PostgreSQL)
- **Tenants Table**: Store business customer information
  - OAuth tokens (encrypted)
  - WABA ID
  - Business phone number ID
  - Connection status
  
- **Conversations Table**: Track customer conversations
  - Tenant ID (for multi-tenancy)
  - Customer contact info
  - Conversation status
  - Created/updated timestamps
  
- **Messages Table**: Store message history
  - Conversation ID
  - Direction (incoming/outgoing)
  - Message type (text, media, etc.)
  - Message content
  - Status (sent, delivered, read)
  - Timestamps

### Migration Scripts
- Create initial schema setup scripts
- Include indexes for performance
- Add foreign key constraints for data integrity

## Implementation Phases

### Phase 1: Repository Restructuring
- Move existing code to `whatsapp-client` directory
- Setup Maven project structure
- Create database migration scripts

### Phase 2: Multi-tenant Management
- Implement tenant data model
- Create OAuth flow for embedded signup
- Build token management system
- Implement webhook handling for signup events

### Phase 3: Conversation Management
- Implement conversation persistence
- Create message tracking system
- Build webhook processor for incoming messages
- Implement conversation query API

### Phase 4: Integration Layer
- Create simplified client interface
- Implement connection status indicators
- Build example integration code
- Add documentation for integration

## Requirements for Integration
- PostgreSQL database availability
- Meta Business Manager account setup as Tech Provider
- Valid SSL for webhook endpoints
- Configuration for OAuth redirect URIs