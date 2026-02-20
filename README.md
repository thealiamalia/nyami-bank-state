# Expose Bank State

Expose bank open/closed state over localhost HTTP for overlays

## Endpoint
- GET http://127.0.0.1:8337/state
- Returns JSON: { ""bankOpen"": true/false }

## Security
This plugin binds to 127.0.0.1 only (localhost).
