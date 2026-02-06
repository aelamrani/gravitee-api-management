# Content Security Policy Configuration

## Current CSP Policy

The default CSP policy includes `'unsafe-inline'` and `'unsafe-eval'` for script execution:

```yaml
csp:
  policy: "default-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; ..."
```

## Why These Unsafe Directives Are Needed

### `'unsafe-inline'`
Required for:
- Inline scripts in Console UI `index.html` (e.g., IE detection script)
- Angular inline event handlers in templates
- Third-party components that use inline scripts

### `'unsafe-eval'`
Required for:
- Angular's Just-In-Time (JIT) compilation in development mode
- Dynamic template compilation
- Some Angular Material components

## Security Implications

While `'unsafe-inline'` and `'unsafe-eval'` reduce CSP effectiveness against XSS attacks, they are currently necessary for the application to function. The policy still provides:
- ✅ Protection against loading external scripts
- ✅ Protection against clickjacking (frame-ancestors)
- ✅ Protection against base tag injection (base-uri)
- ✅ Controlled resource loading
- ✅ Form submission restrictions

This is significantly more secure than the previous policy which only had `frame-ancestors 'self';`

## Hardening Recommendations for Production

### Option 1: Nonce-Based CSP (Recommended)
Implement dynamic nonce generation similar to Gravitee AM:

```yaml
csp:
  enabled: true
  script-inline-nonce: true
  directives:
    - "default-src 'none';"
    - "script-src 'self' 'nonce-{NONCE}';"
    - "style-src 'self' 'nonce-{NONCE}';"
    - "img-src 'self' data:;"
    - "font-src 'self';"
    - "connect-src 'self';"
    - "frame-ancestors 'self';"
    - "base-uri 'none';"
    - "form-action 'self';"
    - "object-src 'none';"
```

This requires:
1. Server-side nonce generation per request
2. Injecting nonce into HTML templates
3. Adding nonce attribute to inline scripts/styles

### Option 2: Script Hashes
Calculate SHA-256 hashes for specific inline scripts:

```yaml
csp:
  policy: "script-src 'self' 'sha256-{HASH1}' 'sha256-{HASH2}';"
```

This requires:
1. Calculating hash for each inline script
2. Updating CSP when scripts change
3. May still need `'unsafe-eval'` for Angular JIT

### Option 3: Remove Inline Scripts
1. Move all inline scripts from `index.html` to external files
2. Use Ahead-of-Time (AOT) compilation for Angular (eliminates `'unsafe-eval'`)
3. Configure Angular to avoid inline styles

### Option 4: Custom CSP Per Environment
Allow different CSP policies for development vs production:

```yaml
# Development
csp:
  policy: "script-src 'self' 'unsafe-inline' 'unsafe-eval';"

# Production  
csp:
  policy: "script-src 'self' 'nonce-{NONCE}';"
```

## Implementation Steps for Nonce-Based CSP

1. Add nonce generation in `SecureHeadersConfigurer.java`
2. Store nonce in request context
3. Update HTML templates to inject nonce
4. Add nonce to inline scripts/styles
5. Update CSP policy to use nonce
6. Remove `'unsafe-inline'` and `'unsafe-eval'`

## References

- [CSP Level 3 Specification](https://www.w3.org/TR/CSP3/)
- [Mozilla CSP Documentation](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [Angular CSP Guide](https://angular.io/guide/security#content-security-policy)
- [OWASP CSP Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html)
- Gravitee AM CSP Configuration (see documentation/production-ready guide)

## Related Issues

- APIM-12035: UI Console - Insufficient Content Security Policy
