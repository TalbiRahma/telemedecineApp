package com.telemedecine.api.exception;

public record ApiErrorResponse(int status, String error, String message) {
}
