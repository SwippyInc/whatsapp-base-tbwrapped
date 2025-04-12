-- Fix token columns to ensure they can handle long OAuth tokens
ALTER TABLE public.tenants ALTER COLUMN access_token TYPE TEXT;
ALTER TABLE public.tenants ALTER COLUMN refresh_token TYPE TEXT;