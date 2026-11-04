# AgroPrecision backend

The backend uses the configuration and application structure on the `dev` branch. Run it from this directory so Spring Boot can load the optional local `.env` file:

```powershell
.\mvnw.cmd spring-boot:run
```

Run the backend tests with:

```powershell
.\mvnw.cmd test
```

## Password reset email

Add SMTP settings to `backend/.env` or the process environment. The local `.env` file is ignored by Git.

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-address@gmail.com
MAIL_PASSWORD=your-google-app-password
MAIL_FROM=your-address@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

For Gmail, use a Google App Password (requires 2-Step Verification), not the regular account password. Never commit or share SMTP credentials. Restart the backend after changing the settings. Reset codes expire after 30 minutes, are stored as SHA-256 hashes, and are not logged or included in API responses. The reset endpoint returns `503 Service Unavailable` if email is not configured or delivery fails.
