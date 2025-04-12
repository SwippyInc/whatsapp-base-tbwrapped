-- WhatsApp Integration Database Migration
-- This script creates the necessary tables for WhatsApp integration
-- including tenant management, conversation tracking, and message history

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schema for WhatsApp integration
CREATE SCHEMA IF NOT EXISTS whatsapp_integration;

-- Create tenants table to store business customers' WhatsApp connection info
CREATE TABLE whatsapp_integration.tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_name VARCHAR(255) NOT NULL,
    waba_id VARCHAR(255),
    business_phone_number_id VARCHAR(255),
    phone_number VARCHAR(50),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    connection_status VARCHAR(50) NOT NULL DEFAULT 'DISCONNECTED',
    webhook_secret VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_phone_number UNIQUE (phone_number),
    CONSTRAINT unique_waba_id UNIQUE (waba_id)
);

-- Create conversations table to track customer conversations
CREATE TABLE whatsapp_integration.conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES whatsapp_integration.tenants(id) ON DELETE CASCADE,
    customer_wa_id VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(50) NOT NULL,
    customer_name VARCHAR(255),
    customer_profile_pic_url TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_tenant_customer UNIQUE (tenant_id, customer_wa_id)
);

-- Create messages table to store conversation history
CREATE TABLE whatsapp_integration.messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES whatsapp_integration.conversations(id) ON DELETE CASCADE,
    whatsapp_message_id VARCHAR(255),
    direction VARCHAR(20) NOT NULL CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    message_type VARCHAR(50) NOT NULL,
    content TEXT,
    media_url TEXT,
    media_mime_type VARCHAR(100),
    media_filename VARCHAR(255),
    media_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    status_updated_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    failed_reason TEXT,
    CONSTRAINT unique_whatsapp_message_id UNIQUE (whatsapp_message_id)
);

-- Create table for storing message templates
CREATE TABLE whatsapp_integration.templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES whatsapp_integration.tenants(id) ON DELETE CASCADE,
    template_name VARCHAR(255) NOT NULL,
    template_id VARCHAR(255) NOT NULL,
    language_code VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    components JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_tenant_template UNIQUE (tenant_id, template_name, language_code)
);

-- Create table for tracking webhook events
CREATE TABLE whatsapp_integration.webhook_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES whatsapp_integration.tenants(id) ON DELETE SET NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_conversations_tenant_id ON whatsapp_integration.conversations(tenant_id);
CREATE INDEX idx_conversations_customer_phone ON whatsapp_integration.conversations(customer_phone);
CREATE INDEX idx_conversations_last_message_at ON whatsapp_integration.conversations(last_message_at);
CREATE INDEX idx_messages_conversation_id ON whatsapp_integration.messages(conversation_id);
CREATE INDEX idx_messages_direction ON whatsapp_integration.messages(direction);
CREATE INDEX idx_messages_sent_at ON whatsapp_integration.messages(sent_at);
CREATE INDEX idx_templates_tenant_id ON whatsapp_integration.templates(tenant_id);
CREATE INDEX idx_webhook_events_tenant_id ON whatsapp_integration.webhook_events(tenant_id);
CREATE INDEX idx_webhook_events_event_type ON whatsapp_integration.webhook_events(event_type);
CREATE INDEX idx_webhook_events_created_at ON whatsapp_integration.webhook_events(created_at);

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION whatsapp_integration.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at columns
CREATE TRIGGER update_tenants_updated_at
    BEFORE UPDATE ON whatsapp_integration.tenants
    FOR EACH ROW EXECUTE FUNCTION whatsapp_integration.update_updated_at_column();

CREATE TRIGGER update_conversations_updated_at
    BEFORE UPDATE ON whatsapp_integration.conversations
    FOR EACH ROW EXECUTE FUNCTION whatsapp_integration.update_updated_at_column();

CREATE TRIGGER update_templates_updated_at
    BEFORE UPDATE ON whatsapp_integration.templates
    FOR EACH ROW EXECUTE FUNCTION whatsapp_integration.update_updated_at_column();