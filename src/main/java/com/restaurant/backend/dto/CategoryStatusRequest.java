package com.restaurant.backend.dto;

public class CategoryStatusRequest {
    private boolean active;

    public CategoryStatusRequest() {
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
