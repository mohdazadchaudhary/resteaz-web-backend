package com.restaurant.backend.service;

public class InvalidImageUrlException extends IllegalArgumentException {
    public InvalidImageUrlException(String message) {
        super(message);
    }
}
