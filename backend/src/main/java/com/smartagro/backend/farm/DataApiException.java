package com.smartagro.backend.farm;

import org.springframework.http.HttpStatus;

public class DataApiException extends RuntimeException {

	private final HttpStatus status;

	public DataApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
