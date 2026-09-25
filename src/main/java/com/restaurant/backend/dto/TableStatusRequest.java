package com.restaurant.backend.dto;

public class TableStatusRequest {
    private boolean active;

    public TableStatusRequest() {
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
