package com.smartagro.backend.auth;

public class PasswordResetEmailUnavailableException extends RuntimeException {

	public PasswordResetEmailUnavailableException() {
		super("Password reset email delivery is not configured. Contact the administrator.");
	}

	public PasswordResetEmailUnavailableException(Throwable cause) {
		super("Password reset email could not be sent. Please try again later.", cause);
	}
}
