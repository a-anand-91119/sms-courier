# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in SMS Courier, please report it responsibly.

**Do NOT:**
- Open a public GitHub/GitLab issue
- Post about it on social media
- Share details publicly before a fix is available

**Do:**
- Email the maintainer directly with details
- Include steps to reproduce the vulnerability
- Allow reasonable time for a fix before disclosure

## Security Features

SMS Courier implements several security measures:

### Password Protection
- Bcrypt hashing (cost factor 12) for all passwords
- Passwords never stored in plaintext
- Failed attempt tracking with progressive lockout

### Message Encryption
- Optional AES-256-CBC encryption for forwarded messages
- PBKDF2 key derivation with 10,000 iterations
- Unique salt and IV per session

### Authentication
- Challenge-response protocol prevents replay attacks
- HMAC-SHA256 verification
- Time-limited authentication challenges

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest  | Yes       |
| Older   | No        |

We recommend always using the latest version of SMS Courier.

## Security Best Practices

When using SMS Courier:

1. **Use strong passwords** - At least 8 characters with mixed case, numbers, and symbols
2. **Keep devices secure** - Use device lock screens and don't leave devices unattended
3. **Review paired devices** - Regularly check your paired devices list and remove any you don't recognize
4. **Update regularly** - Install updates promptly to get security fixes
