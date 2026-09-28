package com.restaurant.backend.dto;

public class MenuItemAvailabilityRequest {
    private boolean available;

    public MenuItemAvailabilityRequest() {
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
