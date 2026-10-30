package com.freshcart.exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
