package com.whatsapp.client.common;

/**
 * Constants for WhatsApp integration
 */
public final class WhatsAppConstants {

    private WhatsAppConstants() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * API endpoints and common values
     */
    public static final class Api {
        public static final String GRAPH_API_BASE = "https://graph.facebook.com";
        public static final String API_VERSION = "v22.0";
        public static final String GRAPH_API_URL = GRAPH_API_BASE + "/" + API_VERSION;
        public static final String OAUTH_TOKEN_ENDPOINT = GRAPH_API_URL + "/oauth/access_token";
        public static final String OAUTH_AUTHORIZE_URL = "https://www.facebook.com/dialog/oauth";
    }
    
    /**
     * OAuth-related constants
     */
    public static final class OAuth {
        public static final String SCOPE_WHATSAPP_BUSINESS_MANAGEMENT = "whatsapp_business_management";
        public static final String SCOPE_WHATSAPP_BUSINESS_MESSAGING = "whatsapp_business_messaging";
        public static final String OAUTH_SCOPES = SCOPE_WHATSAPP_BUSINESS_MANAGEMENT + "," + SCOPE_WHATSAPP_BUSINESS_MESSAGING;
        
        public static final String OAUTH_AUTHORIZE_URL = "https://www.facebook.com/dialog/oauth";
        
        public static final String PARAM_CLIENT_ID = "client_id";
        public static final String PARAM_CLIENT_SECRET = "client_secret";
        public static final String PARAM_REDIRECT_URI = "redirect_uri";
        public static final String PARAM_CODE = "code";
        public static final String PARAM_STATE = "state";
        public static final String PARAM_SCOPE = "scope";
        public static final String PARAM_GRANT_TYPE = "grant_type";
        public static final String PARAM_REFRESH_TOKEN = "refresh_token";
        
        public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
        
        public static final String RESPONSE_ACCESS_TOKEN = "access_token";
        public static final String RESPONSE_REFRESH_TOKEN = "refresh_token";
        public static final String RESPONSE_EXPIRES_IN = "expires_in";
    }
    
    /**
     * Webhook-related constants
     */
    public static final class Webhook {
        public static final String OBJECT_TYPE = "object";
        public static final String OBJECT_WHATSAPP = "whatsapp_business_account";
        public static final String ENTRY = "entry";
        public static final String CHANGES = "changes";
        public static final String FIELD = "field";
        public static final String VALUE = "value";
        public static final String ID = "id";
        
        public static final String FIELD_ACCOUNT_UPDATE = "account_update";
        public static final String FIELD_MESSAGES = "messages";
        public static final String FIELD_TEMPLATE_STATUS = "message_template_status_update";
        
        public static final String EVENT = "event";
        public static final String EVENT_PARTNER_ADDED = "PARTNER_ADDED";
        public static final String EVENT_ACCOUNT_UPDATE = "ACCOUNT_UPDATE";
        public static final String EVENT_VERIFIED_ACCOUNT = "VERIFIED_ACCOUNT";
        public static final String EVENT_ACCOUNT_VERIFIED = "ACCOUNT_VERIFIED";
        public static final String EVENT_DISABLED_UPDATE = "DISABLED_UPDATE";
        
        public static final String WABA_INFO = "waba_info";
        public static final String WABA_ID = "waba_id";
        
        public static final String VERIFY_MODE = "hub.mode";
        public static final String VERIFY_TOKEN = "hub.verify_token";
        public static final String VERIFY_CHALLENGE = "hub.challenge";
        public static final String VERIFY_MODE_SUBSCRIBE = "subscribe";
        
        public static final String RESPONSE_SUCCESS = "EVENT_RECEIVED";
    }
    
    /**
     * Message-related constants
     */
    public static final class Message {
        public static final String MESSAGES = "messages";
        public static final String STATUSES = "statuses";
        public static final String CONTACTS = "contacts";
        public static final String ID = "id";
        public static final String TYPE = "type";
        public static final String FROM = "from";
        public static final String TIMESTAMP = "timestamp";
        
        public static final String CONTACT_WAID = "wa_id";
        public static final String CONTACT_PROFILE = "profile";
        public static final String CONTACT_NAME = "name";
        
        public static final String TYPE_TEXT = "text";
        public static final String TYPE_IMAGE = "image";
        public static final String TYPE_AUDIO = "audio";
        public static final String TYPE_VIDEO = "video";
        public static final String TYPE_DOCUMENT = "document";
        public static final String TYPE_LOCATION = "location";
        public static final String TYPE_BUTTON = "button";
        public static final String TYPE_INTERACTIVE = "interactive";
        public static final String TYPE_STICKER = "sticker";
        
        public static final String TEXT_BODY = "body";
        
        public static final String MEDIA_ID = "id";
        public static final String MEDIA_LINK = "link";
        public static final String MEDIA_MIMETYPE = "mime_type";
        public static final String MEDIA_FILENAME = "filename";
        public static final String MEDIA_CAPTION = "caption";
        
        public static final String LOCATION_LATITUDE = "latitude";
        public static final String LOCATION_LONGITUDE = "longitude";
        public static final String LOCATION_NAME = "name";
        public static final String LOCATION_ADDRESS = "address";
        
        public static final String INTERACTIVE_TYPE = "type";
        public static final String INTERACTIVE_BUTTON_REPLY = "button_reply";
        public static final String INTERACTIVE_LIST_REPLY = "list_reply";
        public static final String INTERACTIVE_TITLE = "title";
        
        public static final String STATUS = "status";
        public static final String STATUS_SENT = "sent";
        public static final String STATUS_DELIVERED = "delivered";
        public static final String STATUS_READ = "read";
        public static final String STATUS_FAILED = "failed";
    }
    
    /**
     * API request/response constants
     */
    public static final class Request {
        public static final String MESSAGING_PRODUCT = "messaging_product";
        public static final String MESSAGING_PRODUCT_WHATSAPP = "whatsapp";
        public static final String PIN = "pin";
        public static final String WABA_SUBSCRIBED_APPS = "/subscribed_apps";
        public static final String REGISTER = "/register";
        
        public static final String SUCCESS = "success";
    }
}